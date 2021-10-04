/*
 * core: org.nrg.xdat.servlet.XDATServlet
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xft.db;

import com.google.common.io.CharStreams;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringSubstitutor;
import org.nrg.framework.beans.XnatPluginBeanManager;
import org.nrg.framework.generics.GenericUtils;
import org.nrg.framework.orm.DatabaseHelper;
import org.nrg.framework.utilities.BasicXnatResourceLocator;
import org.nrg.framework.utilities.OrderedProperties;
import org.nrg.framework.utilities.OrderedPropertiesLookup;
import org.nrg.framework.utilities.Reflection;
import org.nrg.xdat.display.DisplayManager;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.generators.SQLCreateGenerator;
import org.nrg.xft.generators.SQLUpdateGenerator;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperUtils;
import org.nrg.xft.schema.XFTManager;
import org.nrg.xft.utils.DateUtils;
import org.nrg.xft.utils.XftStringUtils;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Tim
 */
@Slf4j
public class DatabaseUpdater {
    public static final String               INIT_SQL_PATTERN         = "classpath*:META-INF/xnat/**/init_*.sql";
    public static final Pattern              ORDERED_INIT_SQL_PATTERN = Pattern.compile("^.*(\\d\\d\\d).sql$");
    public static final Pattern              INIT_SCRIPT_SQL_PATTERN  = Pattern.compile("^.*/scripts/([a-z_]+).sql$");
    public static final Comparator<Resource> NULL_COMPARATOR          = (o1, o2) -> 0;

    private final List<String> _additionalStatements = new ArrayList<>();

    private final String                _configurationFolder;
    private final JdbcTemplate          _template;
    private final DatabaseHelper        _helper;
    private final XnatPluginBeanManager _pluginBeanManager;
    private final String                _databaseUser;
    private final OrderedProperties     _initPrefs;
    private final Path                  _xnatHome;
    private final List<Path>            _configFiles;

    private boolean _shouldUpdateViews;
    private boolean _isDatabasePopulateOrUpdateCompleted;

    public DatabaseUpdater(final JdbcTemplate template) {
        this(template, null, null, null, null, null, null);
    }

    public DatabaseUpdater(final JdbcTemplate template, final String configurationFolder) {
        this(template, configurationFolder, null, null, null, null, null);
    }

    public DatabaseUpdater(final JdbcTemplate template, final String configurationFolder, final XnatPluginBeanManager pluginBeanManager, final String databaseUser, final OrderedProperties initPrefs, final Path xnatHome, final List<Path> configFiles) {
        _template = template;
        _helper = new DatabaseHelper(_template);
        _configurationFolder = StringUtils.defaultIfBlank(configurationFolder, ".");
        _pluginBeanManager = Optional.ofNullable(pluginBeanManager).orElseGet(XnatPluginBeanManager::new);
        _databaseUser = StringUtils.defaultIfBlank(databaseUser, "xnat");
        _initPrefs = Optional.ofNullable(initPrefs).orElseGet(OrderedProperties::new);
        _xnatHome = Optional.ofNullable(xnatHome).orElseGet(() -> Paths.get("."));
        _configFiles = Optional.ofNullable(configFiles).orElseGet(Collections::emptyList);

        log.info("Creating new instance of database updater with configuration folder {}", _configurationFolder);
    }

    public boolean shouldUpdateViews() {
        return _shouldUpdateViews;
    }

    public boolean isDatabasePopulateOrUpdateCompleted() {
        return _isDatabasePopulateOrUpdateCompleted;
    }

    /**
     * Adds additional statements to the database updater, to be run after any generated SQL.
     *
     * @param additionalStatements One or more SQL statements
     */
    public void addAdditionalStatements(final List<String> additionalStatements) {
        _additionalStatements.addAll(additionalStatements);
    }

    /**
     * Method used to manage auto-updates to the database.  It will only auto update the database, if the auto-update
     * parameter is null or true. It uses a query on the user table to identify if the user table to see if users have been populated.
     *
     * @throws Exception When an error occurs.
     */
    public void populateOrUpdateDatabase() throws Exception {
        log.info("Starting database checks");

        final DatabaseHelper helper = new DatabaseHelper(_template);

        // This should use the configuration service, but I couldn't get it to work because of servlet init issues.
        final Properties properties        = new Properties();
        final File       configurationFile = Paths.get(_configurationFolder, "properties", "database.properties").toFile();
        if (configurationFile.exists()) {
            try (final InputStream input = new FileInputStream(configurationFile)) {
                properties.load(input);
                log.info("Loaded {} configuration properties from file at {}", properties.size(), configurationFile);
            }
        } else {
            log.debug("Didn't find configuration file {}, proceeding", configurationFile);
        }

        final AtomicBoolean shouldRefreshElementSecurity = new AtomicBoolean();
        try {
            if (BooleanUtils.toBoolean(properties.getProperty("auto-update", "true"))) {
                log.debug("Auto-update property is set to true or not set at all (defaults to true), doing auto-update now");
                final Path    generatedSqlLogPath = getGeneratedSqlLogPath();
                final Integer userCount           = !helper.tablesExist("xdat_user") ? null : helper.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM xdat_user", Integer.class);
                if (userCount != null) {
                    log.info("Found {} users, preparing to update database", userCount);
                    processInitSqlScripts();
                    updateOidFunctions(helper);

                    log.debug("Preparing to generate SQL");
                    //noinspection rawtypes
                    final List[]       createSql = SQLUpdateGenerator.GetSQLCreate();
                    final List<String> sql       = GenericUtils.convertToTypedList(createSql[0], String.class);
                    final List<String> optional  = GenericUtils.convertToTypedList(createSql[1], String.class);
                    if (!optional.isEmpty()) {
                        log.error("Preparing create SQL and got {} optional SQL statements. Please review this optional SQL and consider executing it if required:\n\nOptional SQL:\n\n{}", optional.size(), String.join("\n", optional));
                    }
                    if (sql.isEmpty()) {
                        log.debug("SQL generation complete, no statements were generated so I'm just validating the views now");
                        shouldRefreshElementSecurity.set(validateViews());
                    } else {
                        log.debug("SQL generation complete, got a total of {} statements, processing now", sql.size());
                        shouldRefreshElementSecurity.set(updateDatabase(userCount, generatedSqlLogPath, sql));
                    }
                } else {
                    log.info("User count was null, which indicates the table doesn't exist yet. Preparing to initialize and populate database");
                    shouldRefreshElementSecurity.set(populateDatabase(generatedSqlLogPath));
                }
            } else {
                shouldRefreshElementSecurity.set(validateViews());
            }
        } catch (Throwable e) {
            log.error("An error occurred trying to populate or update the database", e);
            shouldRefreshElementSecurity.set(false);
        } finally {
            log.debug("All processing completed, checking for referenced secure data types that haven't been activated");
            ElementSecurity.checkForReferencedDataModels(_template, _pluginBeanManager);
        }

        if (shouldRefreshElementSecurity.get()) {
            log.info("Someone said I should refresh element security (probably because something important in the database changed), so I'm doing that now");
            ElementSecurity.refresh();
        }
    }

    private void updateOidFunctions(final DatabaseHelper helper) {
        final int oidCount = helper.getJdbcTemplate().queryForObject(QUERY_FIND_OID_FUNCTIONS, Integer.class);
        if (oidCount > 0) {
            log.info("Found {} functions that contained OID references, preparing to execute functions to convert to non-OID references", oidCount);
            try (final PoolDBUtils.Transaction transaction = PoolDBUtils.open(helper.getJdbcTemplate().getDataSource())) {
                if (helper.tableExists("xs_item_cache") && StringUtils.isBlank(helper.columnExists("xs_item_cache", "id"))) {
                    log.info("Adding 'id' primary key column to xs_item_cache table");
                    transaction.execute(PoolDBUtils.QUERY_ITEM_CACHE_ADD_ID);
                }
                final NamedParameterJdbcTemplate        template          = helper.getParameterizedTemplate();
                final Map<String, Pair<String, String>> oidsFunctions     = template.queryForList(QUERY_DROP_OID_FUNCTIONS, EmptySqlParameterSource.INSTANCE, String.class).stream().map(OID_FUNCTIONS_PATTERN::matcher).filter(Matcher::matches).collect(Collectors.toMap(matcher -> matcher.group("function"), matcher -> Pair.of(matcher.group("param1"), matcher.group("param2"))));
                final List<String>                      dropOidsFunctions = oidsFunctions.entrySet().stream().map(entry -> String.format("DROP FUNCTION IF EXISTS %s(%s, %s));", entry.getKey(), entry.getValue().getLeft(), entry.getValue().getRight())).collect(Collectors.toList());
                if (log.isDebugEnabled()) {
                    log.debug("Running the following {} commands to drop deprecated OID functions:\n\n{}", dropOidsFunctions.size(), String.join("\n", dropOidsFunctions));
                } else {
                    log.info("Running {} commands to drop deprecated OID functions", dropOidsFunctions.size());
                }
                transaction.execute(dropOidsFunctions);

                final List<String> restoreOidsFunctions = new ArrayList<>();
                final List<String> dataTypes            = oidsFunctions.keySet().stream().map(DATA_TYPE_PATTERN::matcher).filter(Matcher::matches).map(matcher -> matcher.group("table")).map(tableName -> template.queryForObject(QUERY_FIND_DATA_TYPE, new MapSqlParameterSource("tableName", tableName), String.class)).collect(Collectors.toList());
                log.debug("Found {} data types for the dropped OID functions", dataTypes.size());
                dataTypes.stream().map(DatabaseUpdater::getElementSafely).filter(Objects::nonNull).forEach(element -> {
                    try {
                        final List<String>[] functions = GenericWrapperUtils.GetUpdateFunctions(element);
                        try (final Stream<String> stream = Stream.concat(functions[0].stream(), functions[1].stream());
                             final StringWriter stringWriter = new StringWriter();
                             final PrintWriter writer = new PrintWriter(stringWriter)) {
                            stream.forEach(writer::println);
                            restoreOidsFunctions.add(stringWriter.toString());
                        } catch (IOException e) {
                            log.error("An error occurred trying to create a string writer", e);
                        }
                    } catch (XFTInitException e) {
                        log.error("Error initializing XFT", e);
                    } catch (ElementNotFoundException e) {
                        log.error("Element not found: {}", e.ELEMENT, e);
                    }
                });
                if (log.isDebugEnabled()) {
                    log.debug("Running the following {} commands to replace dropped OID functions with non-OID versions:\n\n{}", restoreOidsFunctions.size(), String.join("\n", restoreOidsFunctions));
                } else {
                    log.info("Running {} commands to replace dropped OID functions with non-OID versions", restoreOidsFunctions.size());
                }
                transaction.execute(restoreOidsFunctions);

                log.info("Preparing to commit the SQL transactions for dropping and restoring OID functions with non-OID versions");
                transaction.commit();
            } catch (SQLException | DBPoolException e) {
                log.error("An error occurred trying to run the SQL init scripts", e);
            }
        }
    }

    private void processInitSqlScripts() {
        final Map<String, String> statements = getInitScripts(INIT_SCRIPT_SQL_PATTERN, null);
        if (!statements.isEmpty()) {
            log.info("Found {} SQL init scripts, preparing to execute:\n* {}", statements.size(), String.join("\n* ", statements.keySet()));
            try (final PoolDBUtils.Transaction transaction = PoolDBUtils.open()) {
                for (final String file : statements.keySet()) {
                    try {
                        transaction.execute(statements.get(file));
                    } catch (SQLException e) {
                        log.error("An error occurred trying to run the SQL init script from file {}", file, e);
                    }
                }
                log.info("Preparing to commit the SQL transactions for SQL init scripts");
                transaction.commit();
            } catch (SQLException e) {
                log.error("An error occurred trying to commit transactions from the SQL init scripts", e);
            } catch (DBPoolException e) {
                log.error("An error occurred trying to open a transaction to execute the SQL init scripts", e);
            }
        }
    }

    private boolean populateDatabase(final Path generatedSqlLogPath) throws Exception {
        _shouldUpdateViews = false;
        log.info("========================================");
        log.info("New Database -- BEGINNING Initialization");
        log.info("========================================");
        final DatabaseUpdateExecutor databaseUpdater = new DatabaseUpdateExecutor(_helper, _configurationFolder, generatedSqlLogPath, "-- Generated SQL for initializing new XNAT database schema");

        final List<String> sql = SQLCreateGenerator.GetSQLCreate(false);

        log.info("Found {} SQL statements from the SQL create generator and {} configuration statements added to the updater.", sql.size(), _additionalStatements.size());

        databaseUpdater.addInitializationStatements(sql);
        databaseUpdater.addConfigurationStatements(_additionalStatements);

        log.info("Starting processing of all SQL statements now");
        final StopWatch stopWatch = StopWatch.createStarted();
        databaseUpdater.runUpdate();
        stopWatch.stop();

        log.info("========================================================");
        log.info("Database initialization complete in {} seconds.", XftStringUtils.displayMillisecondsAsSeconds(stopWatch.getTime()));
        log.info("========================================================");
        return true;
    }

    private boolean validateViews() {
        log.info("Database up to date, validating views.");
        // The database tables are up-to-date. But, we should still make sure the functions and views are up-to-date.
        // However, this can be done in a separate thread so that the user can start using the site.
        _shouldUpdateViews = true;
        _isDatabasePopulateOrUpdateCompleted = true;
        (new DelayedSequenceChecker()).start();//this isn't necessary if we did the du.start();
        return false;
    }

    private boolean updateDatabase(final Integer userCount, final Path generatedSqlLogPath, final List<String> sql) {
        final DatabaseUpdateExecutor databaseUpdater = new DatabaseUpdateExecutor(_helper, userCount == 0 ? _configurationFolder : null, generatedSqlLogPath, "-- Generated SQL for updating XNAT database schema");//user_count==0 means users need to be created.
        log.info("===========================");
        log.info("Database out of date... updating\n  {}\n===========================", String.join("\n  ", sql));
        // changes have been made to the actual db schema, they should be done before we continue
        // the user can't use the site until these are completed.
        databaseUpdater.addInitializationStatements(sql);
        databaseUpdater.addConfigurationStatements(_additionalStatements);
        //noinspection CallToThreadRun
        databaseUpdater.run();// use run to prevent a second thread.
        _shouldUpdateViews = true;
        return true;
    }

    public static class DelayedSequenceChecker extends Thread {
        public void run() {
            log.info("Adjusting database sequences");
            DBAction.AdjustSequences();
            log.info("Running any dynamic synchronization implementations I can find");
            Reflection.injectDynamicImplementations("org.nrg.xnat.extensions.server.startup.sync", null);
        }
    }

    /**
     * When updating the database, the function and views are always recreated.  The tables are only created if
     * necessary. The statements to create the tables are passed in via the addStatements method.
     */
    @SuppressWarnings("UnstableApiUsage")
    public class DatabaseUpdateExecutor extends Thread {
        private final List<String> _initStatements   = new ArrayList<>();
        private final List<String> _configStatements = new ArrayList<>();

        private final DatabaseHelper _helper;
        private final String         _configurationFolder;
        private final Path           _generatedSqlLogPath;
        private final List<String>   _generatedSqlLogHeaders;

        /**
         * Creates a new instance of the updater class.
         *
         * @param configurationFolder    Location of the WEB-INF/conf.  Set to NULL to skip init scripts.
         * @param generatedSqlLogPath    The path to a file where SQL statements are logged. If null, no logging occurs.
         * @param generatedSqlLogHeaders Any optional headers that should be added to the generated SQL logs.
         */
        public DatabaseUpdateExecutor(final DatabaseHelper helper, final String configurationFolder, final Path generatedSqlLogPath, final String... generatedSqlLogHeaders) {
            _helper = helper;
            _configurationFolder = configurationFolder;
            _generatedSqlLogPath = validateGeneratedSqlLogPath(generatedSqlLogPath);
            _generatedSqlLogHeaders = Arrays.asList(generatedSqlLogHeaders);
        }

        /**
         * Adds the statements to the SQL statements to be executed for initializing the deployment. Note that the SQL
         * statements are executed in the order given. All statements are contained and committed in a single transaction,
         * so any error in an of the SQL statements will result in a rollback of <i>all</i> of the statements.
         *
         * @param statements The core SQL statements to be executed.
         */
        public final void addInitializationStatements(final List<String> statements) {
            _initStatements.addAll(statements);
            log.info("Added {} statements to the initialization SQL", statements.size());
        }

        /**
         * Adds the statements to the SQL statements to be executed for configuring the deployment after initialization. Note
         * that the SQL statements are executed in the order given. All statements are contained and committed in a single
         * transaction, so an error in any of the SQL statements will result in a rollback of <i>all</i> of the statements.
         *
         * @param statements The SQL statements to be executed after initialization.
         */
        public final void addConfigurationStatements(final List<String> statements) {
            _configStatements.addAll(statements);
            log.info("Added {} statements to the configuration SQL", statements.size());
        }

        /**
         * Runs the SQL statements that have been added to the database update executor. Note that, because this method
         * overrides the <pre>Thread.run()</pre> method, this runs asynchronously and returns immediately. To run the
         * database update synchronously, call the {@link #runUpdate()} method instead.
         */
        @Override
        public void run() {
            runUpdate();
        }

        /**
         * Runs the SQL statements that have been added to the database update executor. Note that this method doesn't
         * override the <pre>Thread.run()</pre> method, so it runs synchronously and returns only when the update is
         * completed. To run the database update asynchronously, call the {@link #run()} method instead.
         */
        public void runUpdate() {
            final boolean shouldWriteLog = _generatedSqlLogPath != null;

            final PoolDBUtils.Transaction transaction = PoolDBUtils.getTransaction(_template.getDataSource());
            try (final PrintWriter writer = new PrintWriter(shouldWriteLog ? new BufferedWriter(new FileWriter(_generatedSqlLogPath.toFile())) : CharStreams.nullWriter())) {
                transaction.start();
                //prep accessory tables, may involve procedure calls
                log.info("Initializing administrative functions...");
                if (shouldWriteLog && !_generatedSqlLogHeaders.isEmpty()) {
                    log.debug("Adding {} SQL log headers to output", _generatedSqlLogHeaders.size());
                    _generatedSqlLogHeaders.stream().map(header -> StringUtils.prependIfMissing("-- ", header)).forEach(writer::println);
                }

                final Collection<String> extensionTables = GenericWrapperUtils.GetExtensionTables();
                if (log.isDebugEnabled()) {
                    log.debug("Found {} extension table statements:\n\n{}", extensionTables.size(), String.join("\n", extensionTables));
                } else {
                    log.info("Found {} extension table statements", extensionTables.size());
                }
                execute(transaction, writer, extensionTables);

                //manually execute create statements
                if (!_initStatements.isEmpty()) {
                    if (log.isTraceEnabled()) {
                        log.trace("Initializing database schema with {} statements:\n\n{}", _initStatements.size(), String.join("\n", _initStatements));
                    } else {
                        log.debug("Initializing database schema with {} statements", _initStatements.size());
                    }
                    try {
                        execute(transaction, writer, _initStatements);
                    } catch (Throwable t) {
                        final String error = t.getMessage();
                        if (CANNOT_DROP_MESSAGE.matcher(error).find()) {
                            //In cases where it is failing to modify a table because one or more views depend on it, simply drop all XNAT views and try again.
                            log.info("Got \"cannot drop\" error, which usually indicates that a view depends on an existing table. Rolling back transaction to drop all views and try again.");
                            transaction.rollback();
                            transaction.execute(SQLUpdateGenerator.getViewDropSql(_databaseUser));
                            execute(transaction, writer, extensionTables);
                            execute(transaction, writer, _initStatements);
                        } else {
                            throw t;
                        }
                    }
                }

                // Update the stored functions used for retrieving XFTItems and manipulating them. This used to build
                // one big SQL file, but it was way too big, so now it does one element at a time.
                final List<String> runAfter = new ArrayList<>();
                log.info("Initializing database functions...");
                for (final GenericWrapperElement element : GenericUtils.convertToTypedList(XFTManager.GetInstance().getOrderedElements(), GenericWrapperElement.class)) {
                    final List<String>[] functions = GenericWrapperUtils.GetFunctionStatements(element);
                    log.debug("Found {} functions and {} run-after functions for the data type element {}", functions[0].size(), functions[1].size(), element.getName());
                    execute(transaction, writer, functions[0]);
                    runAfter.addAll(functions[1]);
                }

                if (_configurationFolder != null) {
                    //create the views defined in the display documents
                    log.info("Initializing database views...");
                    execute(transaction, writer, DisplayManager.GetCreateViewsSQL());
                }

                log.info("Now running {} run-after functions", runAfter.size());
                execute(transaction, writer, runAfter);

                final Map<String, String> initScripts = getInitScripts(ORDERED_INIT_SQL_PATTERN, Comparator.comparing(resource -> ORDERED_INIT_SQL_PATTERN.matcher(resource.getFilename()).group(1)));
                if (!initScripts.isEmpty()) {
                    log.info("Found {} init script statements, preparing to run", initScripts.size());
                    for (final String file : initScripts.keySet()) {
                        try {
                            execute(transaction, writer, initScripts.get(file));
                        } catch (SQLException e) {
                            log.error("An error occurred trying to run the SQL init script from file {}", file, e);
                        }
                    }
                } else {
                    log.info("No init script statements found");
                }

                if (!_configStatements.isEmpty()) {
                    log.info("Found {} configuration statements, preparing to run", _configStatements.size());
                    execute(transaction, writer, _configStatements);
                } else {
                    log.info("No configuration statements found");
                }

                log.info("Committing all previous transactions");
                transaction.commit();
            } catch (XFTInitException e) {
                log.error("An error occurred initializing XFT, now attempting rolling back the current transactions", e);
                rollback(transaction);
            } catch (SQLException e) {
                log.error("An error occurred creating or updating the database elements schemas and views, now attempting rolling back the current transactions", e);
                rollback(transaction);
            } catch (DBPoolException e) {
                log.error("An error occurred when trying to start the transaction for creating or updating the database elements schemas and views, now attempting rolling back the current transactions", e);
                rollback(transaction);
            } catch (Exception e) {
                log.error("An unknown error occurred when trying to create or update the database elements schemas and views, now attempting rolling back the current transactions", e);
                rollback(transaction);
            } finally {
                transaction.close();
            }

            try {
                // When the database is updated directly (rather than through XFT), the sequences can get out of
                // sync. This would cause issues down the road, so we fix them here.
                DBAction.AdjustSequences();
            } catch (Exception e) {
                log.error("An error occurred trying to adjust database sequences", e);
            }

            // Process init SQL scripts, which include functions required for data-type registration in next step.
            processInitSqlScripts();

            // code will dynamically register data types
            try {
                //this is to trigger a review of the Data Types that have been defined in jar configurations.
                //it needs to run after all the db initialization is done.
                final List<String> newTypes = ElementSecurity.registerNewTypes(_pluginBeanManager, _helper);
                if (!newTypes.isEmpty()) {
                    // When the database is updated directly (rather than through XFT), the sequences can get out of
                    // sync.  This would cause issues down the road, so we fix them here.
                    log.info("Created {} new data types in the system: \"{}\"", newTypes.size(), StringUtils.join(newTypes, "\", \""));
                    DBAction.AdjustSequences();
                }
            } catch (Throwable e) {
                log.error("An error occurred whilst trying to register new datatypes and adjust database sequences", e);
            }
            _isDatabasePopulateOrUpdateCompleted = true;
        }

        private Path validateGeneratedSqlLogPath(final Path generatedSqlLogPath) {
            if (generatedSqlLogPath == null) {
                return null;
            }
            final File file = generatedSqlLogPath.toFile();
            if (file.exists()) {
                if (file.isDirectory()) {
                    return generatedSqlLogPath.resolve(getGeneratedSqlLogFilename());
                }
                if (file.canWrite()) {
                    return generatedSqlLogPath;
                }
                log.warn("I was asked to log generated SQL queries to the file {}, but I can't write to that.", generatedSqlLogPath);
                return null;
            }
            final File parent = file.getParentFile();
            if (parent.exists()) {
                if (parent.isFile()) {
                    log.warn("I was asked to log generated SQL queries to the file {}, but the parent is a file (must be a directory).", generatedSqlLogPath);
                    return null;
                }
                return generatedSqlLogPath;
            }
            final boolean success = parent.mkdirs();
            if (!success) {
                log.warn("I was asked to log generated SQL queries to the file {}, but the parent directory doesn't exist and I can't seem to create it.", generatedSqlLogPath);
                return null;
            }
            return generatedSqlLogPath;
        }

        private void rollback(final PoolDBUtils.Transaction transaction) {
            try {
                log.debug("Rolling back the transaction");
                transaction.rollback();
            } catch (SQLException e) {
                log.error("An error occurred trying to roll back a database transaction", e);
            }
        }

        private void execute(final PoolDBUtils.Transaction transaction, final PrintWriter writer, final String statement) throws SQLException {
            execute(transaction, writer, Collections.singletonList(statement));
        }

        private void execute(final PoolDBUtils.Transaction transaction, final PrintWriter writer, final Collection<String> statements) throws SQLException {
            if (_generatedSqlLogPath != null) {
                for (final String statement : statements) {
                    writer.println(statement);
                }
            }
            log.debug("Executing {} statements", statements.size());
            transaction.execute(statements);
        }
    }

    private Map<String, String> getInitScripts(final Pattern filter, final Comparator<Resource> comparator) {
        // Get the init prefs ordered properties from context and create a substitutor.
        final StringSubstitutor substitutor = new StringSubstitutor(new OrderedPropertiesLookup(_initPrefs), "${", "}", '\\');

        final Map<String, String> statements = new HashMap<>();
        try {
            final List<Resource> resources = filterAndSortInitSqlResources(BasicXnatResourceLocator.getResources(INIT_SQL_PATTERN), filter, comparator);
            log.debug("Found {} resources that match the pattern {} and filter {}", resources.size(), INIT_SQL_PATTERN, filter.pattern());
            for (final Resource resource : resources) {
                log.debug("Now processing the resource {}", resource.getFilename());
                final Path path = resource.getFile().toPath();
                try (final Stream<String> stream = Files.lines(path);
                     final StringWriter stringWriter = new StringWriter();
                     final PrintWriter writer = new PrintWriter(stringWriter)) {
                    stream.map(substitutor::replace).forEach(writer::println);
                    statements.put(path.toString(), stringWriter.toString());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("An error occurred trying to locate XNAT module definitions.");
        }
        log.debug("Found {} statements in init scripts", statements.size());
        return statements;
    }

    /**
     * Tests each resource to see if the name matches the pattern for SQL initialization resources, init_XXX_NNN.sql,
     * where XXX is some string to indicate the purpose of the initialization and NNN is a three-digit ordinal value
     * indicating the script's place in the initialization order.
     * <p>
     * Once the resources have been filtered and any non-compliant resources have been removed and logged, the list is
     * sorted based on the resources' ordinal values.
     *
     * @param resources  The array of resources to be sorted and filtered.
     * @param filter     A regex pattern to further refine the resources.
     * @param comparator The comparator to use when sorting the resources (or <b>null</b> if sorting isn't required)
     *
     * @return The list of resources after sorting and filtering.
     */
    private List<Resource> filterAndSortInitSqlResources(final List<Resource> resources, final Pattern filter, final Comparator<Resource> comparator) {
        return resources.stream().filter(new ResourcePatternMatcher(filter)).sorted(comparator != null ? comparator : NULL_COMPARATOR).collect(Collectors.toList());
    }

    private static GenericWrapperElement getElementSafely(final String dataType) {
        try {
            return GenericWrapperElement.GetElement(dataType);
        } catch (XFTInitException e) {
            log.error("Got an exception trying to retrieve the element for data type {}", dataType, e);
        } catch (ElementNotFoundException e) {
            log.error("Couldn't find element {} trying to retrieve the element for data type {}", e.ELEMENT, dataType, e);
        }
        return null;
    }

    /**
     * Checks whether the XNAT configuration property <b>xnat.database.sql.log</b> is set to <b>true</b>. If not, this
     * method returns null and generated SQL shouldn't be logged. The folder for this output can be specified with the
     * <b>xnat.database.sql.log.folder</b> property, but defaults to the <b>xnat.home</b> folder. In either case, the
     * filename is <b>xnat-<i>timestamp</i>.sql</b>. You can specify the full path, including the file name, with the
     * <b>xnat.database.sql.log.file</b> property.
     *
     * @return The path to the log file for generated SQL if specified, null otherwise.
     */
    private Path getGeneratedSqlLogPath() {
        final Properties properties = new Properties();
        for (final Path path : _configFiles) {
            final File file = path.toFile();
            if (file.exists()) {
                try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    properties.load(reader);
                } catch (IOException e) {
                    log.error("An error occurred trying to read the properties file {}", file.toURI(), e);
                }
            }
        }

        final String  shouldLogSqlValue = properties.getProperty("xnat.database.sql.log");
        final Boolean shouldLogSql      = BooleanUtils.toBooleanObject(shouldLogSqlValue);
        final String  sqlLogFile        = properties.getProperty("xnat.database.sql.log.file");
        final String  sqlLogFolder      = properties.getProperty("xnat.database.sql.log.folder");
        log.debug("Found properties xnat.database.sql.log=\"{}\", xnat.database.sql.log.file=\"{}\", and xnat.database.sql.log.file=\"{}\"", shouldLogSqlValue, sqlLogFile, sqlLogFolder);

        if (BooleanUtils.isFalse(shouldLogSql) || shouldLogSql == null && StringUtils.isAllBlank(sqlLogFile, sqlLogFolder)) {
            log.info("Either xnat.database.sql.log is set to false or xnat.database.sql.log isn't set at all and no value is set for xnat.database.sql.log.file or xnat.database.sql.log.folder, so no generated SQL log path considered.");
            return null;
        }
        if (StringUtils.isNoneBlank(sqlLogFile, sqlLogFolder)) {
            log.warn("Found values for both \"xnat.database.sql.log.file\" and \"xnat.database.sql.log.folder\". You should only specify one of these properties. Using the value for \"xnat.database.sql.log.file\": {}", sqlLogFile);
        }

        final Path sqlLogFilePath;
        if (StringUtils.isNotBlank(sqlLogFile)) {
            final Path path = Paths.get(sqlLogFile);
            if (path.isAbsolute()) {
                sqlLogFilePath = path;
            } else {
                sqlLogFilePath = getSqlLogFolder(sqlLogFolder).resolve(sqlLogFile);
            }
        } else {
            sqlLogFilePath = getSqlLogFolder(sqlLogFolder).resolve(getGeneratedSqlLogFilename());
        }

        log.info("Found path specified for generated SQL log path: {}", sqlLogFilePath);
        return sqlLogFilePath;
    }

    private Path getSqlLogFolder(final String configuredSqlLogFolder) {
        if (StringUtils.isNotBlank(configuredSqlLogFolder)) {
            return Paths.get(configuredSqlLogFolder);
        }
        return _xnatHome.resolve("sql");
    }

    private static String getGeneratedSqlLogFilename() {
        return "xnat-" + DateUtils.getMsTimestamp() + ".sql";
    }

    private static class ResourcePatternMatcher implements Predicate<Resource> {
        private final Pattern _filter;

        ResourcePatternMatcher(final Pattern filter) {
            _filter = filter;
        }

        @Override
        public boolean test(final Resource resource) {
            try {
                return _filter.matcher(resource.getURI().toString()).find();
            } catch (IOException e) {
                log.error("An error occurred trying to get the URI for the resource {}", resource, e);
                return false;
            }
        }
    }

    private static final Pattern OID_FUNCTIONS_PATTERN    = Pattern.compile("^(?<function>[^,]+),?(<param1>[^,]+),?(<param2>[^,]+)$");
    private static final Pattern DATA_TYPE_PATTERN        = Pattern.compile("^update_ls_(ext_)?(?<table>.*)$");
    private static final String  QUERY_FIND_OID_FUNCTIONS = "SELECT count(*) FROM information_schema.routines WHERE routine_name LIKE 'update_ls_%' AND routine_definition LIKE '%SELECT oid FROM xs_item_cache%'";
    private static final String  QUERY_DROP_OID_FUNCTIONS = "WITH " +
                                                            "    fns_and_params AS ( " +
                                                            "        SELECT " +
                                                            "            r.routine_name, " +
                                                            "            p.ordinal_position, " +
                                                            "            p.parameter_name, " +
                                                            "            p.data_type " +
                                                            "        FROM " +
                                                            "            information_schema.parameters p " +
                                                            "                LEFT JOIN information_schema.routines r ON p.specific_name = r.specific_name " +
                                                            "        WHERE " +
                                                            "            p.parameter_mode = 'IN' AND " +
                                                            "            r.routine_schema = 'public' AND " +
                                                            "            r.routine_name LIKE 'update_ls_%' " +
                                                            "        ORDER BY " +
                                                            "            r.routine_name, " +
                                                            "            p.ordinal_position) " +
                                                            "SELECT " +
                                                            "    routine_name || ',' || array_to_string(array_agg(data_type::TEXT), ',')" +
                                                            "FROM " +
                                                            "    fns_and_params " +
                                                            "GROUP BY " +
                                                            "    routine_name";
    private static final String  QUERY_FIND_DATA_TYPE     = "SELECT element_name FROM xdat_meta_element WHERE element_name ~* ('^' || replace(:tableName, '_', '.') || '$');";
    private static final Pattern CANNOT_DROP_MESSAGE      = Pattern.compile("^.*cannot\\s+drop\\s+(table|column)(?s).*because other objects depend on it.*$");
}
