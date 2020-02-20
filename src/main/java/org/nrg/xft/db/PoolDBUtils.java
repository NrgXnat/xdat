/*
 * core: org.nrg.xft.db.PoolDBUtils
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xft.db;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.xdat.XDAT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.*;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperField;
import org.nrg.xft.utils.XftStringUtils;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"DuplicateThrows", "RedundantThrows"})
@Slf4j
public class PoolDBUtils {
    public static final String SEARCH_SCHEMA_NAME = "xdat_search";

    /**
     * @author tim@deck5consulting.com
     *
     *         The transaction class is used to process db transactions which cannot be passed as a batch (include SELECTs).
     *         It maintains a single open connection (locked) until the close method is called.
     */
    public static class Transaction {
        PoolDBUtils pooledConnection = new PoolDBUtils();//pooled connection manager
        Connection  con;
        Statement   st;

        public void start() throws SQLException, DBPoolException {
            con = pooledConnection.getConnection();
            con.setAutoCommit(false);

            st = pooledConnection.getStatement();
        }

        public void execute(Collection<String> statements) throws SQLException {
            for (final String statement : statements) {
                try {
                    st.execute(statement);
                } catch (SQLException e) {
                    log.error("An error occurred trying to execute the SQL statement: '{}'", statement);
                    throw e;
                }
            }
        }

        public void commit() throws SQLException {
            con.commit();
        }

        public void rollback() throws SQLException {
            con.rollback();
        }

        public void close() {
            try {
                con.setAutoCommit(true);//reset pooled connection to auto-commit for next consumer
            } catch (SQLException ignored) {
            }

            pooledConnection.closeConnection(null);//use the pool manager to close the connection
        }
    }

    public static synchronized Object GetNextID(String db, String table, String pk, String sequence) throws SQLException, Exception {
        return (new PoolDBUtils()).getNextID(db, table, pk, sequence);
    }

    public static void ExecuteNonSelectQuery(final String query, String db, String userName) throws SQLException, Exception {
        new PoolDBUtils().executeNonSelectQuery(query, db, userName);
    }

    public static void ExecuteBatch(final List<String> queries, final String db, final String userName) throws SQLException, Exception {
        new PoolDBUtils().sendBatch(queries, db, userName, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
    }

    public static Object ReturnStatisticQuery(String query, String column, String db, String userName) throws SQLException, Exception {
        return new PoolDBUtils().returnStatisticQuery(query, column, db, userName);
    }

    /**
     * Returns the number of Rows in a given ResultSet
     *
     * @param results The result set to check.
     *
     * @return Returns the number of Rows in a given ResultSet
     */
    public static int GetResultSetSize(final ResultSet results) {
        try {
            try {
                return results.last() ? results.getRow() : 0;
            } finally {
                results.beforeFirst();
            }
        } catch (SQLException e) {
            log.error("An error occurred trying to get the result set's size", e);
        }
        return 0;
    }

    /**
     * Creates the XNAT item cache table.
     *
     * @param dbName The name of the database in which the cache should be created.
     * @param login  The login name of the user creating the cache.
     */
    public static void CreateCache(String dbName, String login) {
        try {
            synchronized (ITEM_CACHE_MUTEX) {
                if (!ITEM_CACHE_EXISTS.get()) {
                    final String exists = (String) PoolDBUtils.ReturnStatisticQuery(QUERY_ITEM_CACHE_EXISTS, "relname", dbName, login);

                    ITEM_CACHE_EXISTS.set(true);
                    if (exists == null) {
                        PoolDBUtils.ExecuteNonSelectQuery(QUERY_CREATE_ITEM_CACHE, dbName, login);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("An database or SQL error occurred trying to validate or create the XNAT item cache.", e);
        } catch (Exception e) {
            log.error("An unknown error occurred trying to create the XNAT item cache.", e);
        }
    }

    /**
     * @param dbName
     * @param login
     */
    public static void ClearCache(String dbName, String login) {
        try {
            if (ITEM_CACHE_EXISTS.get()) {
                PoolDBUtils.ExecuteNonSelectQuery(QUERY_CLEAR_ITEM_CACHE, dbName, login);
            }
        } catch (SQLException e) {
            log.error("An database or SQL error occurred trying to clear the XNAT item cache.", e);
        } catch (Exception e) {
            log.error("An unknown error occurred trying to clear the XNAT item cache.", e);
        }
    }

    /**
     * @param dbName
     * @param login
     */
    public static void ClearCache(String dbName, String login, String xsiType) {
        try {
            if (ITEM_CACHE_EXISTS.get()) {
                PoolDBUtils.ExecuteNonSelectQuery(String.format(QUERY_CLEAR_ITEM_IN_CACHE, xsiType), dbName, login);
            }
        } catch (SQLException e) {
            log.error("An database or SQL error occurred trying to delete all items of type {} from the XNAT item cache.", xsiType, e);
        } catch (Exception e) {
            log.error("An unknown error occurred trying to delete all items of type {} from the XNAT item cache.", xsiType, e);
        }
    }

    /**
     * @param rootElement
     * @param ids
     * @param functionQuery
     * @param functionName
     * @param login
     *
     * @return Returns the item expressed as a string
     */
    public static String RetrieveItemString(String rootElement, String ids, String functionQuery, String functionName, String login) {
        String itemString = null;
        String query      = "";
        try {
            GenericWrapperElement e = GenericWrapperElement.GetElement(rootElement);

            CreateCache(e.getDbName(), login);

            itemString = (String) PoolDBUtils.ReturnStatisticQuery("SELECT contents FROM xs_item_cache WHERE elementName='" + rootElement + "' AND ids='" + ids + "';", "contents", e.getDbName(), login);
            if (itemString == null) {
                itemString = (String) PoolDBUtils.ReturnStatisticQuery(functionQuery, functionName, e.getDbName(), login);
                if (itemString != null) {
                    // itemString.replaceAll("\\\\", "\\\\\\\\") is to escape backslashes in Windows paths,
                    // "'", "''" is to replace single quotes with escaped single quotes, e.g. "O'Connor" becomes
                    // "O''Connor".
                    query = "INSERT INTO xs_item_cache (elementName,ids,contents) VALUES ('" + rootElement + "','" + ids + "','" + itemString.replaceAll("\\\\", "\\\\\\\\").replaceAll("'", "''") + "');";
                    PoolDBUtils.ExecuteNonSelectQuery(query, e.getDbName(), login);
                }
            }
        } catch (SQLException e) {
            log.error("An database or SQL error occurred trying to insert an item or items of type {} into the XNAT item cache: {}\n\nQuery: {}", rootElement, ids, query, e);
        } catch (Exception e) {
            log.error("An unknown error occurred trying to insert an item or items of type {} into the XNAT item cache: {}\n\nQuery: {}", rootElement, ids, query, e);
        }

        return itemString;
    }

    /**
     * @param item  The item that was updated.
     * @param login The user updating the item.
     *
     * @throws SQLException When an error occurs executing the database transaction.
     */
    @SuppressWarnings("rawtypes")
    public static void PerformUpdateTrigger(final XFTItem item, final String login) throws SQLException {
        final Date   start = Calendar.getInstance().getTime();
        final String query = performUpdateTrigger(item, login);
        log.debug(getTimeDiff(start, Calendar.getInstance().getTime()) + " ms" + " (" + login + "): " + StringUtils.replace(query, "\n", " "));
    }

    /**
     * @param dbName
     * @param login
     */
    public static void CreateCustomSearchLog(String dbName, String login) {
        if (!CUSTOM_SEARCH_LOG_EXISTS.get()) {
            try {
                final String exists = returnSafeStatisticQuery(QUERY_CUSTOM_SEARCHES_CLASS, "relname", dbName, login, String.class);
                if (StringUtils.isNotBlank(exists)) {
                    final Boolean columnExists = returnSafeStatisticQuery(QUERY_CUSTOM_SEARCHES_COLUMN, "exists", dbName, login, Boolean.class);
                    if (columnExists == null || !columnExists) {
                        PoolDBUtils.ExecuteNonSelectQuery(QUERY_ADD_RANDOM_ID_TO_CUSTOM_SEARCHES, dbName, login);
                    }
                    CUSTOM_SEARCH_LOG_EXISTS.set(true);
                } else {
                    PoolDBUtils.ExecuteNonSelectQuery(QUERY_CREATE_CUSTOM_SEARCHES_TABLE, dbName, login);
                    CUSTOM_SEARCH_LOG_EXISTS.set(true);
                }
            } catch (Exception e) {
                if (StringUtils.isNotBlank(e.getMessage())) {
                    log.error("An error occurred trying to create the custom search log", e);
                }
            }
        }
    }

    /**
     * @param login     The login name of the user creating the cache.
     * @param searchXml The XML for the custom search.
     * @param dbName    The name of the database to be searched.
     *
     * @return Returns the ID of the newly logged custom search
     */
    public static Object LogCustomSearch(final String login, final String searchXml, final String dbName) throws SQLException, DBPoolException, Exception {
        CreateCustomSearchLog(dbName, login);
        Object o    = PoolDBUtils.ReturnStatisticQuery("SELECT nextval('xs_custom_searches_id_seq');", "nextval", dbName, login);
        Object uuid = UUID.randomUUID().toString();

        String  columnQuery  = "SELECT TRUE AS exists FROM pg_attribute WHERE attrelid = 'xs_custom_searches'::regclass AND attname = 'random_id_string' AND NOT attisdropped;";
        Boolean columnExists = (Boolean) PoolDBUtils.ReturnStatisticQuery(columnQuery, "exists", dbName, login);

        if (columnExists == null || !columnExists) {
            String addColumnQuery = "ALTER TABLE xs_custom_searches ADD COLUMN random_id_string VARCHAR(255);";
            PoolDBUtils.ExecuteNonSelectQuery(addColumnQuery, dbName, login);
        }

        String query = "INSERT INTO xs_custom_searches (id,username,search_xml, random_id_string) VALUES (" + XftStringUtils.CleanForSQLValue(o.toString()) + ",'" + login + "','" + XftStringUtils.CleanForSQLValue(searchXml) + "','" + uuid + "');";
        PoolDBUtils.ExecuteNonSelectQuery(query, dbName, login);

        return uuid;
    }

    /**
     * @param login    The login name of the user creating the cache.
     * @param dbName   The name of the database to be searched.
     * @param searchId The ID of the search to retrieve.
     *
     * @return Returns the logged custom search xml
     */
    public static String RetrieveLoggedCustomSearch(final String login, final String dbName, final Object searchId) throws SQLException, DBPoolException, Exception {
        CreateCustomSearchLog(dbName, login);
        return (String) PoolDBUtils.ReturnStatisticQuery("SELECT search_xml FROM xs_custom_searches WHERE random_id_string='" + XftStringUtils.CleanForSQLValue(searchId.toString()) + "';", "search_xml", dbName, login);
    }

    /**
     * @param dbName The name of the database in which the temporary schema should be created.
     * @param login  The login name of the user creating the temporary schema.
     */
    public static void CreateTempSchema(final String dbName, final String login) {
        try {
            final String exists = returnSafeStatisticQuery(QUERY_GET_SEARCH_SCHEMA, "nspname", dbName, login, String.class);
            if (!TEMP_SCHEMA_EXISTS.get()) {
                if (StringUtils.isBlank(exists)) {
                    PoolDBUtils.ExecuteNonSelectQuery(QUERY_CREATE_SEARCH_SCHEMA, dbName, login);
                }
                TEMP_SCHEMA_EXISTS.set(true);
            }
        } catch (SQLException e) {
            log.error("An error occurred running the query: {}", QUERY_CREATE_SEARCH_SCHEMA, e);
        } catch (Exception e) {
            log.error("An unknown error occurred trying to create the search schema", e);
        }
    }

    public static boolean HackCheck(final Iterable<String> values) {
        for (final String value : values) {
            if (HackCheck(value)) {
                return true;
            }
        }
        return false;
    }

    public static boolean HackCheck(final String value) {
        if (value.matches("^[a-zA-z0-9 _\\\\.]*[^\\\\]$")) {
            return false;
        }

        final String normalized = value.toUpperCase();

        return normalized.matches("^.*[\\\\]$") ||
               normalized.matches("<*SCRIPT") ||
               StringContains(value, "SELECT") ||
               StringContains(value, "INSERT") ||
               StringContains(value, "UPDATE") ||
               StringContains(value, "DELETE") ||
               StringContains(value, "DROP") ||
               StringContains(value, "ALTER") ||
               StringContains(value, "CREATE");
    }

    public static boolean StringContains(String value, String s) {
        if (!value.contains(s + ' ')) {
            return false;
        }
        if (value.startsWith(s + ' ')) {
            return true;
        }
        return StringUtils.containsAny(value, ' ' + s + ' ', '(' + s + ' ', '[' + s + ' ', '\'' + s + ' ', '\n' + s + ' ', '\t' + s + ' ');
    }

    public static void CheckSpecialSQLChars(final String s) {
        if (StringUtils.contains(s, "'")) {
            throw new IllegalArgumentException(s);
        }
    }

    public static float getDatabaseVersion() {
        if (VERSION.get() == 0) {
            try {
                final PoolDBUtils      instance         = new PoolDBUtils();
                final DatabaseMetaData databaseMetaData = instance.getDatabaseMetaData();
                VERSION.set(Float.floatToIntBits(Float.parseFloat(databaseMetaData.getDatabaseMajorVersion() + "." + databaseMetaData.getDatabaseMinorVersion())));
            } catch (SQLException e) {
                log.error("An SQL error occurred trying to get a database connection. There's probably something wrong somewhere.", e);
            }
        }
        return Float.intBitsToFloat(VERSION.get());
    }

    /**
     * Check if the database type exists
     *
     * @param clazz The class name to test for.
     *
     * @return Returns whether the database type exists
     *
     * @throws Exception When an unknown error occurs.
     */
    public static boolean checkIfTypeExists(final String clazz) throws Exception {
        return (Long) PoolDBUtils.ReturnStatisticQuery("SELECT COUNT(*) AS count FROM pg_catalog.pg_type WHERE typname = LOWER('" + clazz + "')", "count", null, null) > 0;
    }

    /**
     * Check if the database class exists
     *
     * @param clazz The class name to test for.
     *
     * @return Returns whether the database class exists
     *
     * @throws Exception When an unknown error occurs.
     */
    @SuppressWarnings("unused")
    public static boolean checkIfClassExists(final String clazz) throws Exception {
        return (Long) PoolDBUtils.ReturnStatisticQuery("SELECT COUNT(*) AS count FROM pg_catalog.pg_class WHERE relname = LOWER('" + clazz + "') GROUP BY relname", "count", null, null) > 0;
    }

    public static long getTimeDiff(Date start, Date end) {
        long time1 = start.getTime();
        long time2 = end.getTime();

        if (time1 > time2) {
            return -1;
        }

        return ((time2 - time1));
    }

    public static String getDefaultDBName() {
        return "";
    }

    public static Transaction getTransaction() {
        return new Transaction();
    }

    /**
     * Processes the specified query on the specified db with a pooled connection.
     * The new Primary Key is returned using a <strong>SELECT currval()</strong> query with
     * the 'table'_'pk'_seq.
     *
     * @param query    SQL query to run
     * @param db       The database to use
     * @param table    The table being accessed
     * @param pk       The primary key of the object.
     * @param sequence The sequence to be used to generate the primary key.
     *
     * @return Returns the current value of the sequence after running the query.
     *
     * @throws SQLException When an error occurs executing the database transaction.
     * @throws Exception    When an unknown error occurs.
     */
    public Object insertNativeItem(String query, String db, String table, String pk, String sequence) throws SQLException, Exception {
        Object    o  = null;
        ResultSet rs = null;
        try {
            if (StringUtils.isNotBlank(sequence)) {
                log.debug("QUERY: {}", query);
                executeQuery(db, query, "");
                _statement.execute(query);
                String newQuery = "SELECT currval('" + sequence + "') AS " + pk;
                try {
                    rs = executeQuery(db, newQuery, "");
                    if (rs.first()) {
                        o = rs.getObject(pk);
                    }
                } catch (SQLException e1) {
                    newQuery = "SELECT currval('" + table + "_" + pk + "_seq') AS " + pk;
                    try {
                        rs = executeQuery(db, newQuery, "");
                        if (rs.first()) {
                            o = rs.getObject(pk);
                        }
                    } catch (SQLException e2) {
                        newQuery = "SELECT currval('" + table + "_" + table + "_seq') AS " + pk;
                        try {
                            rs = executeQuery(db, newQuery, "");
                            if (rs.first()) {
                                o = rs.getObject(pk);
                            }
                        } catch (SQLException e3) {
                            newQuery = "SELECT pg_get_serial_sequence('" + table + "', '" + StringUtils.lowerCase(pk) + "') AS col_name";
                            try {
                                rs = executeQuery(db, newQuery, "");
                                if (rs.first()) {
                                    String colName = rs.getObject("col_name").toString();
                                    newQuery = "SELECT currval('" + colName + "') AS " + pk;
                                    rs = executeQuery(db, newQuery, "");
                                    if (rs.first()) {
                                        o = rs.getObject(pk);
                                    }
                                }
                            } catch (Exception e4) {
                                log.error("POSTGRES - SEQUENCE BUG", e1);
                                log.error("POSTGRES - SEQUENCE BUG", e2);
                                log.error("POSTGRES - SEQUENCE BUG", e3);
                                throw e1;
                            }
                        }
                    }
                }
            } else {
                rs = executeQuery(db, query, "");
                String newQuery = "SELECT currval('" + table + "_" + pk + "_seq') AS " + pk;
                try {
                    rs = executeQuery(db, newQuery, "");
                    if (rs.first()) {
                        o = rs.getObject(pk);
                    }
                } catch (SQLException e1) {
                    newQuery = "SELECT currval('" + table + "_" + table + "_seq') AS " + pk;
                    try {
                        rs = executeQuery(db, newQuery, "");
                        if (rs.first()) {
                            o = rs.getObject(pk);
                        }
                    } catch (SQLException e2) {
                        log.error("POSTGRES - SEQUENCE BUG", e1);
                        log.error("POSTGRES - SEQUENCE BUG", e2);
                        throw e1;
                    }
                }
            }
        } catch (SQLException | DBPoolException e) {
            log.error("An error occurred trying to execute the query: {}", query, e);
            throw e;
        } finally {
            closeConnection(rs);
        }

        return o;
    }

    public Object getNextID(String db, String table, String pk, String sequence) throws SQLException, Exception {
        Object    o  = null;
        ResultSet rs = null;
        try {
            if (StringUtils.isNotBlank(sequence)) {
                String newQuery = "SELECT nextval('" + sequence + "') AS " + pk;
                try {
                    rs = executeQuery(db, newQuery, "");
                    if (rs.first()) {
                        o = rs.getObject(pk);
                    }
                } catch (SQLException e1) {
                    newQuery = "SELECT nextval('" + table + "_" + pk + "_seq') AS " + pk;
                    try {
                        rs = executeQuery(db, newQuery, "");
                        if (rs.first()) {
                            o = rs.getObject(pk);
                        }
                    } catch (SQLException e2) {
                        newQuery = "SELECT nextval('" + table + "_" + table + "_seq') AS " + pk;
                        try {
                            rs = executeQuery(db, newQuery, "");
                            if (rs.first()) {
                                o = rs.getObject(pk);
                            }
                        } catch (SQLException e3) {
                            newQuery = "SELECT nextval('" + table + "','" + pk + "') AS col_name";
                            try {
                                rs = executeQuery(db, newQuery, "");
                                if (rs.first()) {
                                    String colName = rs.getObject("col_name").toString();
                                    newQuery = "SELECT nextval('" + colName + "') AS " + pk;
                                    rs = executeQuery(db, newQuery, "");
                                    if (rs.first()) {
                                        o = rs.getObject(pk);
                                    }
                                }
                            } catch (Exception e4) {
                                log.error("POSTGRES - SEQUENCE BUG", e1);
                                log.error("POSTGRES - SEQUENCE BUG", e2);
                                log.error("POSTGRES - SEQUENCE BUG", e3);
                                throw e1;
                            }
                        }
                    }
                }
            } else {
                String newQuery = "SELECT nextval('" + table + "_" + pk + "_seq') AS " + pk;
                try {
                    rs = executeQuery(db, newQuery, "");
                    if (rs.first()) {
                        o = rs.getObject(pk);
                    }
                } catch (SQLException e1) {
                    newQuery = "SELECT nextval('" + table + "_" + table + "_seq') AS " + pk;
                    try {
                        rs = executeQuery(db, newQuery, "");
                        if (rs.first()) {
                            o = rs.getObject(pk);
                        }
                    } catch (SQLException e2) {
                        log.error("POSTGRES - SEQUENCE BUG", e1);
                        log.error("POSTGRES - SEQUENCE BUG", e2);
                        throw e1;
                    }
                }
            }
        } catch (SQLException | DBPoolException e) {
            throw e;
        } finally {
            closeConnection(rs);
        }

        return o;
    }

    public void sendBatch(final DBItemCache cache, final String db, final String userName) throws SQLException, Exception {
        //noinspection FinalizeCalledExplicitly
        cache.finalize();
        sendBatch(cache.getStatements(), db, userName, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        cache.reset();
    }

    @SuppressWarnings("unused")
    public void sendBatch(final List<String> statements, String db, String userName, int resultSetType, int resultSetConcurrency) throws SQLException, Exception {
        if (log.isInfoEnabled() && Iterables.any(statements, Predicates.contains(PATTERN_DELETE_FROM))) {
            log.info("Preparing to execute a batch of queries that includes at least one delete operation:\n * {}", StringUtils.join(statements, "\n * "));
        } else if (log.isDebugEnabled()) {
            log.debug("Preparing to execute a batch of queries:\n * {}", StringUtils.join(statements, "\n * "));
        }
        try {
            sendBatchExec(statements, userName, resultSetType, resultSetConcurrency);
        } catch (SQLException e) {
            if (e.getMessage().contains("Connection reset")) {
                sendBatchExec(statements, userName, resultSetType, resultSetConcurrency);
            } else {
                log.error("", e);
                throw e;
            }
        }
    }

    /**
     * Executes the given query with a pooled connection and closes the connection.
     *
     * @param query SQL query to run
     * @param cache The database item cache
     */
    @SuppressWarnings("unused")
    public void insertItem(String query, String db, String userName, DBItemCache cache) throws SQLException, Exception {
        log.debug("Request to insert item with query: {}", query);
        cache.addStatement(query);
    }

    /**
     * Executes the given query with a pooled connection and closes the connection.
     *
     * @param query SQL query to run
     * @param db    The database to use
     *
     * @throws SQLException When an error occurs executing the database transaction.
     * @throws Exception    When an unknown error occurs.
     */
    @SuppressWarnings("unused")
    public void updateItem(String query, String db, String userName, DBItemCache cache) throws SQLException, Exception {
        log.debug("Request to update item with query: {}", query);
        cache.addStatement(query);
    }

    /**
     * Executes the given query with a pooled connection and closes the connection.
     *
     * @param query    SQL query to run
     * @param db       The database to use
     * @param userName The name of the user running the query.
     *
     * @throws SQLException When an error occurs executing the database transaction.
     * @throws Exception    When an unknown error occurs.
     */
    @SuppressWarnings("unused")
    public void executeNonSelectQuery(final String query, final String db, final String userName) throws SQLException, Exception {
        if (PATTERN_DELETE_FROM.matcher(query).matches()) {
            log.info("Preparing to execute a delete query: {}", query);
        } else {
            log.debug("Preparing to execute a non-select query: {}", query);
        }
        try {
            execute(query, userName);
        } catch (SQLException | DBPoolException e) {
            log.error(query);
            throw e;
        } finally {
            closeConnection(null);
        }
    }

    public Object returnStatisticQuery(String query, String column, String db, String userName) throws SQLException, Exception {
        log.debug("Preparing to execute a statistic query: {}", query);
        try (final ResultSet results = executeQuery(db, query, userName)) {
            return results.first() ? results.getObject(column) : null;
        } catch (SQLException e) {
            if (!PATTERN_TABLES_DO_NOT_EXIST.matcher(e.getMessage()).matches()) {
                log.error(query);
            }
            throw e;
        } catch (DBPoolException e) {
            log.error(query);
            throw e;
        }
    }

    /**
     * Executes the selected query and transfers the data from a ResultSet to
     * a XFTTable.
     *
     * @param query SQL query to run
     * @param db    The database to use
     *
     * @return Returns the XFTTable containing the results of the query
     *
     * @throws SQLException    When an error occurs executing the database transaction.
     * @throws DBPoolException When an error occurs accessing the database pooled connections
     */
    public XFTTable executeSelectQuery(String query, String db, String userName) throws SQLException, DBPoolException {
        ResultSet rs      = null;
        XFTTable  results = new XFTTable();

        try {
            rs = executeQuery(db, query, userName);

            final String[] columns = new String[rs.getMetaData().getColumnCount()];
            for (int i = 1; i <= columns.length; i++) {
                columns[i - 1] = rs.getMetaData().getColumnName(i);
            }

            results.initTable(columns);

            while (rs.next()) {
                Object[] row = new Object[columns.length];
                for (int i = 1; i <= columns.length; i++) {
                    try {
                        Object o = rs.getObject(i);
                        row[i - 1] = o;
                    } catch (Exception e1) {
                        log.error("", e1);
                    }
                }
                results.insertRow(row);
            }

            //logger.debug("AFTER XFTTable");
        } catch (SQLException e) {
            if (!PATTERN_TABLES_DO_NOT_EXIST.matcher(e.getMessage()).matches()) {
                log.error(query);
            }
            throw e;
        } catch (DBPoolException e) {
            log.error(query);
            throw e;
        } finally {
            closeConnection(rs);
        }

        return results;
    }

    public void closeConnection(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Throwable e) {
                log.error("An error occurred trying to close the result set's connection", e);
            }
        }
        closeConnection();
    }

    public void closeConnection() {
        if (_statement != null) {
            try {
                _statement.close();
            } catch (Throwable e) {
                log.error("An error occurred trying to close the statement", e);
            }
        }
        if (_connection != null) {
            try {
                _connection.close();
            } catch (Throwable e) {
                log.error("An error occurred trying to close the connection", e);
            }
        }
    }

    /**
     * Gets a database connection from the configured data source.
     *
     * @return A connection to the database.
     *
     * @throws SQLException When an error occurs trying to get the database connection.
     */
    public Connection getConnection() throws SQLException {
        if (_connection == null) {
            final DataSource dataSource = XDAT.getDataSource();
            if (dataSource != null) {
                _connection = dataSource.getConnection();
            } else {
                log.warn("Couldn't load the data source to create a connection.");
            }
        }
        return _connection;
    }

    /**
     * Gets the metadata for the configured data source.
     *
     * @return Metadata for the database.
     *
     * @throws SQLException When an error occurs trying to get the database connection.
     */
    public DatabaseMetaData getDatabaseMetaData() throws SQLException {
        if (_databaseMetaData == null) {
            final Connection connection = getConnection();
            if (connection != null) {
                _databaseMetaData = connection.getMetaData();
            } else {
                log.warn("Couldn't load the data source to create a connection.");
            }
        }
        return _databaseMetaData;
    }

    public Statement getStatement() throws DBPoolException, SQLException {
        return getConnection().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
    }

    public PreparedStatement getPreparedStatement(String db, String sql) throws SQLException, DBPoolException {
        return getConnection().prepareStatement(sql);
    }

    public ResultSet executeQuery(String db, String query, String userName) throws SQLException, DBPoolException {
        if (log.isInfoEnabled() && PATTERN_DELETE_FROM.matcher(query).matches()) {
            log.info("Preparing to execute a delete query: {}", query);
        } else {
            log.debug("Preparing to execute a query: {}", query);
        }
        _statement = getStatement();
        final Date start = Calendar.getInstance().getTime();

        try {
            return _statement.executeQuery(query);
        } catch (SQLException e) {
            final String message = e.getMessage();
            if (message.contains("Connection reset")) {
                closeConnection();
                resetConnections();
                _statement = getStatement();
                return _statement.executeQuery(query);
            } else if (message.matches(EXPR_COLUMN_NOT_FOUND)) {
                final Matcher matcher = PATTERN_COLUMN_NOT_FOUND.matcher(message);
                log.error("Got an exception indicating that the column \"" + matcher.group(1) + "\" does not exist. The attempted query is:\n\n" + query);
                return null;
            } else if (PATTERN_TABLES_DO_NOT_EXIST.matcher(message).matches()) {
                // Just rethrow and let someone else handle it. This is probably because the system is initializing
                // and, if it's not, plenty else will go wrong to indicate the problem.
                throw e;
            } else {
                log.error("An error occurred trying to execute the user " + userName + " query: " + query, e);
                throw e;
            }
        } finally {
            log.debug(getTimeDiff(start, Calendar.getInstance().getTime()) + " ms" + " (" + userName + "): " + StringUtils.replace(query, "\n", " "));
        }
    }

    private void execute(final String query, final String userName) throws SQLException, DBPoolException {
        if (log.isInfoEnabled() && PATTERN_DELETE_FROM.matcher(query).matches()) {
            log.info("Preparing to execute a delete query: {}", query);
        } else {
            log.debug("Preparing to execute a query: {}", query);
        }
        _statement = getStatement();
        final Date start = Calendar.getInstance().getTime();

        try {
            _statement.execute(query);
        } catch (SQLException e) {
            final String message = e.getMessage();
            if (PATTERN_TABLES_DO_NOT_EXIST.matcher(message).matches()) {
                // Honestly, we don't care much about this. It goes away once the metadata table is initialized,
                // has no real effect beforehand, and is a pretty scary message with an enormous stacktrace.
                log.debug("Metadata error occurred: {}", message);
            } else if (message.contains("Connection reset")) {
                closeConnection();
                resetConnections();
                _statement = getStatement();
                _statement.execute(query);
            } else if (message.matches(EXPR_COLUMN_NOT_FOUND)) {
                final Matcher matcher = PATTERN_COLUMN_NOT_FOUND.matcher(message);
                log.error("Got an exception indicating that the column \"{}\" does not exist. The attempted query is:\n\n{}", matcher.group("column"), query);
            } else if (message.matches(EXPR_TABLE_NOT_FOUND)) {
                final Matcher matcher = PATTERN_DROP_TABLE_QUERY.matcher(query);
                if (matcher.find()) {
                    log.debug("Got an exception that the table \"{}\" does not exist, but that's probably OK: it's trying to drop that table. This occurs during a race condition trying to drop a materialized view twice.", matcher.group("table"));
                } else {
                    log.error("Got an exception indicating that the table \"{}\" does not exist. The attempted query is:\n\n{}", matcher.group("table"), query);
                }
            } else {
                log.error("An error occurred trying to execute the user {} query: {}", userName, query, e);
                throw e;
            }
        } finally {
            if (getTimeDiff(start, Calendar.getInstance().getTime()) > 1000) {
                log.error(getTimeDiff(start, Calendar.getInstance().getTime()) + " ms" + " (" + userName + "): " + StringUtils.replace(query, "\n", " "));
            }

            log.debug(getTimeDiff(start, Calendar.getInstance().getTime()) + " ms" + " (" + userName + "): " + StringUtils.replace(query, "\n", " "));
        }
    }

    private void sendBatchExec(List<String> statements, String userName, int resultSetType, int resultSetConcurrency) throws Exception {
        final Date start = Calendar.getInstance().getTime();
        try (final Connection connection = getConnection()) {
            try {
                connection.setAutoCommit(false);

                _statement = connection.createStatement(resultSetType, resultSetConcurrency);
                _statement.clearBatch();
                for (String stmt : statements) {
                    _statement.addBatch(stmt);
                }

                _statement.executeBatch();

                log.debug(getTimeDiff(start, Calendar.getInstance().getTime()) + " ms" + " (" + userName + "): " + StringUtils.replace("BATCH", "\n", " "));

                _statement.clearBatch();

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                log.error(statements.toString());
                log.error(e.getMessage());
                throw e.getNextException();
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void resetConnections() {
        log.warn("WARNING: DB CONNECTION FAILURE: Resetting all DB connections!!!!!!");
        _connection = null;
    }

    private static String performUpdateTrigger(final XFTItem item, final String login) throws SQLException {
        CreateCache(item.getDBName(), login);

        final GenericWrapperElement element;
        try {
            element = item.getGenericSchemaElement();
        } catch (ElementNotFoundException e) {
            log.error("Couldn't find the specified element: {}", e.ELEMENT, e);
            return null;
        }

        final String query = "SELECT update_ls_" + element.getFormattedName() + "(" + getItemIds(item, element) + ", NULL)";

        try (final Connection connection = XDAT.getDataSource().getConnection()) {
            try (final CallableStatement statement = connection.prepareCall(query)) {
                statement.execute();
            } catch (SQLException e) {
                log.error("An error occurred trying to run the query: {}", query, e);
            }
        }
        return query;
    }

    private static String getItemIds(final XFTItem item, final GenericWrapperElement element) {
        return StringUtils.join(Iterables.filter(Lists.transform(element.getAllPrimaryKeys(), new Function<GenericWrapperField, String>() {
            @Nullable
            @Override
            public String apply(final GenericWrapperField field) {
                final Object id;
                try {
                    id = getFieldId(item, field);
                } catch (ElementNotFoundException e) {
                    log.error("Couldn't find the specified element: {}", e.ELEMENT, e);
                    return null;
                } catch (FieldNotFoundException e) {
                    log.error("Field not found: {}", e.FIELD, e);
                    return null;
                } catch (XFTInitException e) {
                    log.error("An error occurred accessing XFT", e);
                    return null;
                }
                try {
                    return DBAction.ValueParser(id, field, true);
                } catch (InvalidValueException e) {
                    log.error("The value {} is invalid for the field {}", id, field, e);
                } catch (XFTInitException e) {
                    log.error("An error occurred accessing XFT", e);
                }
                return null;
            }
        }), Predicates.notNull()), ", ");
    }

    private static Object getFieldId(final XFTItem item, final GenericWrapperField field) throws ElementNotFoundException, XFTInitException, FieldNotFoundException {
        final Object id = item.getProperty(field);
        return id != null ? id : item.getProperty(field.getXMLPathString(item.getGenericSchemaElement().getXSIType()));
    }

    private static <T> T returnSafeStatisticQuery(final String query, final String column, final String dbName, final String login, final Class<T> returnType) throws Exception {
        try {
            return returnType.cast(ReturnStatisticQuery(query, column, dbName, login));
        } catch (SQLException e) {
            log.error("An error occurred trying to execute the SQL statement: '{}'", query);
        } catch (Exception e) {
            log.error("An unknown error occurred", e);
        }
        throw new Exception();
    }

    private static final String  EXPR_COLUMN_NOT_FOUND                  = "column \"(?<column>[A-z0-9_-]+)\" does not exist";
    private static final String  EXPR_TABLE_NAME                        = "(?<table>_[A-z0-9_]+_[\\d]+)";
    private static final String  EXPR_DROP_TABLE_QUERY                  = "DROP TABLE xdat_search\\." + EXPR_TABLE_NAME;
    private static final String  EXPR_TABLE_NOT_FOUND                   = "^.*table \"" + EXPR_TABLE_NAME + "\" does not exist.*$";
    private static final Pattern PATTERN_DELETE_FROM                    = Pattern.compile("^.*DELETE FROM .*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_TABLES_DO_NOT_EXIST            = Pattern.compile("^.*relation \"(?<relation>xdat_(user|element_security)|[a-z0-9_]+_meta_data)\" does not exist.*[\\r\\n]*.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_COLUMN_NOT_FOUND               = Pattern.compile(EXPR_COLUMN_NOT_FOUND);
    private static final Pattern PATTERN_DROP_TABLE_QUERY               = Pattern.compile(EXPR_DROP_TABLE_QUERY);
    private static final String  QUERY_CUSTOM_SEARCHES_CLASS            = "SELECT relname FROM pg_catalog.pg_class WHERE relname=LOWER('xs_custom_searches')";
    private static final String  QUERY_CUSTOM_SEARCHES_COLUMN           = "SELECT TRUE AS exists FROM pg_attribute WHERE attrelid = 'xs_custom_searches'::regclass AND attname = 'random_id_string' AND NOT attisdropped";
    private static final String  QUERY_ADD_RANDOM_ID_TO_CUSTOM_SEARCHES = "ALTER TABLE xs_custom_searches ADD COLUMN random_id_string VARCHAR(255)";
    private static final String  QUERY_CREATE_CUSTOM_SEARCHES_TABLE     = "CREATE TABLE xs_custom_searches (id  serial, create_date timestamp DEFAULT now(), username VARCHAR(255), search_xml text, random_id_string VARCHAR(255)) WITH OIDS";
    private static final String  QUERY_GET_SEARCH_SCHEMA                = "SELECT nspname FROM pg_namespace WHERE has_schema_privilege(nspname, 'USAGE') AND nspname='" + SEARCH_SCHEMA_NAME + "'";
    private static final String  QUERY_CREATE_SEARCH_SCHEMA             = "CREATE SCHEMA " + SEARCH_SCHEMA_NAME;
    private static final String  QUERY_ITEM_CACHE_EXISTS                = "SELECT relname FROM pg_catalog.pg_class WHERE  relname=LOWER('xs_item_cache');";
    private static final String  QUERY_CLEAR_ITEM_CACHE                 = "DELETE FROM xs_item_cache";
    private static final String  QUERY_CLEAR_ITEM_IN_CACHE              = QUERY_CLEAR_ITEM_CACHE + " WHERE elementname='%s';";
    private static final String  QUERY_CREATE_ITEM_CACHE                = "CREATE TABLE xs_item_cache" +
                                                                          "\n(" +
                                                                          "\n  elementName varchar(255) NOT NULL," +
                                                                          "\n  ids varchar(255) NOT NULL," +
                                                                          "\n  create_date timestamp DEFAULT now()," +
                                                                          "\n  contents text" +
                                                                          "\n) " +
                                                                          "\nWITH OIDS;";

    private static final Object        ITEM_CACHE_MUTEX         = new Object();
    private static final AtomicBoolean ITEM_CACHE_EXISTS        = new AtomicBoolean();
    private static final AtomicBoolean CUSTOM_SEARCH_LOG_EXISTS = new AtomicBoolean();
    private static final AtomicBoolean TEMP_SCHEMA_EXISTS       = new AtomicBoolean();
    private static final AtomicInteger VERSION                  = new AtomicInteger();

    private Connection       _connection       = null;
    private Statement        _statement        = null;
    private DatabaseMetaData _databaseMetaData = null;
}
