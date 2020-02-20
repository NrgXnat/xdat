/*
 * core: org.nrg.xft.db.views.LegacyMaterializedViewImpl
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xft.db.views;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.xdat.search.DisplaySearch;
import org.nrg.xdat.security.XdatStoredSearch;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.db.MaterializedViewI;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.db.views.service.MaterializedViewManager;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.XftStringUtils;

import java.sql.SQLException;
import java.util.*;

import static org.nrg.xft.db.PoolDBUtils.SEARCH_SCHEMA_NAME;
import static org.nrg.xft.db.views.service.MaterializedViewManager.SCHEMA_AND_TABLE;
import static org.nrg.xft.db.views.service.MaterializedViewManager.getMaterializedViewManager;

@SuppressWarnings("DuplicateThrows")
@Slf4j
public class LegacyMaterializedViewImpl implements MaterializedViewI {
    public LegacyMaterializedViewImpl(final UserI user) {
        this(null, user);
    }

    public LegacyMaterializedViewImpl(final Hashtable table, final UserI user) {
        if (user == null) {
            throw new NullPointerException();
        }
        setUser(user);
        if (table != null) {
            setCreated((Date) table.get("created"));
            setLast_access((Date) table.get("last_access"));
            setSearch_id((String) table.get("search_id"));
            setSearch_sql((String) table.get("search_sql"));
            setSearch_xml((String) table.get("search_xml"));
            setTable_name((String) table.get("table_name"));
            setTag((String) table.get("tag"));
            setUser_name((String) table.get("username"));
        } else {
            setUser_name(user.getUsername());
        }
    }

    @Override
    public String getCode() {
        return MaterializedView.DEFAULT_MATERIALIZED_VIEW_SERVICE_CODE;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getUser()
     */
    @Override
    public UserI getUser() {
        return _user;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#setUser(org.nrg.xdat.security.UserI)
     */
    @Override
    public void setUser(UserI user) {
        _user = user;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getCreated()
     */
    @Override
    public Date getCreated() {
        return _created;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#setCreated(java.util.Date)
     */
    @Override
    public void setCreated(Date created) {
        _created = created;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getLast_access()
     */
    @Override
    public Date getLast_access() {
        return _lastAccess;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#setLast_access(java.util.Date)
     */
    @Override
    public void setLast_access(Date last_access) {
        _lastAccess = last_access;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getSearch_id()
     */
    @Override
    public String getSearch_id() {
        return _searchId;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#setSearch_id(java.lang.String)
     */
    @Override
    public void setSearch_id(String search_id) {
        PoolDBUtils.CheckSpecialSQLChars(search_id);
        _searchId = search_id;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getSearch_sql()
     */
    @Override
    public String getSearch_sql() {
        return _searchSql;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#setSearch_sql(java.lang.String)
     */
    @Override
    public void setSearch_sql(String search_sql) {
        _searchSql = search_sql;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getSearch_xml()
     */
    @Override
    public String getSearch_xml() {
        return _searchXml;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#setSearch_xml(java.lang.String)
     */
    @Override
    public void setSearch_xml(String search_xml) {
        _searchXml = search_xml;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getTable_name()
     */
    @Override
    public String getTable_name() {
        return _tableName;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#setTable_name(java.lang.String)
     */
    @Override
    public void setTable_name(final String tableName) {
        PoolDBUtils.CheckSpecialSQLChars(tableName);
        _tableName = tableName;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getTag()
     */
    @Override
    public String getTag() {
        return _tag;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#setTag(java.lang.String)
     */
    @Override
    public void setTag(String tag) {
        _tag = tag;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getUser_name()
     */
    @Override
    public String getUser_name() {
        return _username;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#setUser_name(java.lang.String)
     */
    @Override
    public void setUser_name(String user_name) {
        _username = user_name;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getSize()
     */
    @Override
    public Long getSize() throws Exception {
        return getSize(null);
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getData(java.lang.String, java.lang.Integer, java.lang.Integer)
     */
    @Override
    public XFTTable getData(String sortBy, Integer offset, Integer limit) throws Exception {
        return getData(sortBy, offset, limit, null);
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getData(java.lang.String, java.lang.Integer, java.lang.Integer, java.util.Map)
     */
    @Override
    public XFTTable getData(String sortBy, Integer offset, Integer limit, Map<String, Object> filters) throws Exception {
        StringBuilder query = new StringBuilder("SELECT * FROM " + SEARCH_SCHEMA_NAME + "." + _tableName);

        if (filters != null && filters.size() > 0) {
            validateColumns(filters.keySet(), this);

            query.append(" WHERE ");
            int count = 0;
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
				if (count++ > 0) {
					query.append(" AND ");
				}

                query.append(buildComparison(entry.getKey(), entry.getValue()));
            }
        }

        if (sortBy != null) {
            query.append(" ORDER BY ").append(sortBy).append(", key");
        }

        if (offset != null) {
            query.append(" OFFSET ").append(offset);
        }
        if (limit != null) {
            query.append(" LIMIT ").append(limit);
        }

        final XFTTable table = XFTTable.Execute(query.toString(), PoolDBUtils.getDefaultDBName(), _user.getUsername());
        new DBMaterializedViewManager(_tableName, PoolDBUtils.getDefaultDBName()).start();
        return table;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getSize(java.util.Map)
     */
    @Override
    public Long getSize(Map<String, Object> filters) throws Exception {
        StringBuilder query = new StringBuilder("SELECT COUNT(*) AS RECORD_COUNT FROM " + SEARCH_SCHEMA_NAME + "." + _tableName);
        if (filters != null && filters.size() > 0) {
            validateColumns(filters.keySet(), this);

            query.append(" WHERE ");
            int count = 0;
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
				if (count++ > 0) {
					query.append(" AND ");
				}

                query.append(buildComparison(entry.getKey(), entry.getValue()));
            }
        }
        return (Long) PoolDBUtils.ReturnStatisticQuery(query + ";", "RECORD_COUNT", PoolDBUtils.getDefaultDBName(), _user.getUsername());
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getColumnNames()
     */
    @Override
    public List<String> getColumnNames() throws Exception {
        if (_cachedColumnNames.isEmpty()) {
            //noinspection unchecked
            _cachedColumnNames.addAll(XFTTable.Execute(String.format(QUERY_GET_COLUMNS, _tableName.toLowerCase()), PoolDBUtils.getDefaultDBName(), _user.getUsername()).convertColumnToArrayList("col_name"));
        }
        return _cachedColumnNames;
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getColumnValues(java.lang.String)
     */
    @Override
    public XFTTable getColumnValues(final String column) throws SQLException, Exception {
        return XFTTable.Execute(String.format(QUERY_GET_COLUMN_VALUES, column, _tableName), PoolDBUtils.getDefaultDBName(), _user.getUsername());
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getColumnsValues(java.lang.String)
     */
    @Override
    public XFTTable getColumnsValues(String column) throws Exception {
        return getColumnsValues(column, null);
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getColumnsValues(java.lang.String, java.util.Map)
     */
    @Override
    public XFTTable getColumnsValues(final String column, final Map<String, Object> filters) throws Exception {
        final String where;
        if (filters != null && filters.size() > 0) {
            validateColumns(filters.keySet(), this);
            final List<String> criteria = new ArrayList<>();
            for (final String key : filters.keySet()) {
                criteria.add(buildComparison(key, filters.get(key)));
            }
            where = "WHERE " + StringUtils.join(criteria, " AND ");
        } else {
            where = "";
        }

        return XFTTable.Execute(String.format(QUERY_GET_COLUMNS_VALUES, column, _tableName, where), PoolDBUtils.getDefaultDBName(), _user.getUsername());
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#delete()
     */
    @Override
    public void delete() throws Exception {
        String drop = "DROP TABLE " + SEARCH_SCHEMA_NAME + "." + _tableName + ";";
        PoolDBUtils.ExecuteNonSelectQuery(drop, PoolDBUtils.getDefaultDBName(), _user.getUsername());

        MaterializedViewManager.Delete(this);
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#save()
     */
    @Override
    public void save() throws Exception {
        if (_searchSql == null) {
            throw new NullPointerException();
        }
        if (_user == null) {
            throw new NullPointerException();
        }


        if (_tableName == null) {
			if (_searchId != null) {
                _tableName = "_" + DisplaySearch.cleanColumnName(_searchId) + "_" + DisplaySearch.cleanColumnName(_user.getUsername()) + "_" + Calendar.getInstance().getTimeInMillis();
			} else {
                _tableName = "_" + DisplaySearch.cleanColumnName(_user.getUsername()) + "_" + Calendar.getInstance().getTimeInMillis();
			}
        }
        String select = "SELECT relname FROM pg_catalog.pg_class WHERE  relname=LOWER('" + _tableName + "');";
        Object o      = PoolDBUtils.ReturnStatisticQuery(select, "relname", PoolDBUtils.getDefaultDBName(), _user.getUsername());
        if (o == null) {
            _searchSql = StringUtils.replace(_searchSql, ";", "");
            String create = "CREATE TABLE " + SEARCH_SCHEMA_NAME + "." + _tableName + " AS " + StringUtils.replace(_searchSql, "''", "'") + ";";


            try {
				if (XFT.VERBOSE) {
					System.out.println("Creating Materialized View: " + _tableName);
				}
                PoolDBUtils.ExecuteNonSelectQuery(create, PoolDBUtils.getDefaultDBName(), _user.getUsername());

                MaterializedViewManager.Register(this);
            } catch (Exception e) {
                if (e.getMessage().contains("pg_type_typname_nsp_index")) {
                    //retry
                    log.info("Duplicate materialized view.");
					if (XFT.VERBOSE) {
						System.out.println("Duplicate materialized view.");
					}
                } else {
                    throw e;
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.nrg.xft.db.MaterializedViewI#getDisplaySearch(org.nrg.xdat.security.UserI)
     */
    @Override
    public DisplaySearch getDisplaySearch(final UserI user) throws Exception {
        return new XdatStoredSearch(XFTItem.PopulateItemFromFlatString(getSearch_xml(), user, true)).getDisplaySearch(user);
    }

    public static String buildComparison(final String key, final Object valuesObject) {
        final List<String>  values      = XftStringUtils.CommaDelimitedStringToArrayList(valuesObject.toString());
        final List<String>  validValues = new ArrayList<>();
        final List<String> clauses = new ArrayList<>();

        int count = 0;
        for (final String value : values) {
            if (!PoolDBUtils.HackCheck(value)) {
                final String cleaned;
                switch (value) {
                    case "''":
                    case "'NULL'":
                        cleaned = "NULL";
                        break;
                    case "'NOT NULL'":
                        cleaned = "NOT NULL";
                        break;
                    default:
                        cleaned = value;
                }
                if (cleaned.startsWith("'") && cleaned.endsWith("'")) {
                    validValues.add(StringUtils.unwrap(cleaned, "'"));
                } else if (cleaned.equalsIgnoreCase("NULL")) {
                    clauses.add(" (" + key + " IS NULL) ");
                } else if (cleaned.equalsIgnoreCase("NOT NULL")) {
                    clauses.add(" (" + key + " IS NOT NULL) ");
                } else if (cleaned.contains("-")) {
                    final String[] atoms = StringUtils.split(cleaned, "-", 2);
                    String value1 = cleaned.substring(0, cleaned.indexOf("-"));
                    String value2 = cleaned.substring(cleaned.indexOf("-") + 1);
                    clauses.add(" (" + key + " BETWEEN '" + atoms[0] + "' AND '" + atoms[1] + "') ");
                } else if (cleaned.startsWith("<=")) {
                    clauses.add(" (" + key + " <= '" + cleaned.substring(2) + "') ");
                } else if (cleaned.startsWith("<")) {
                    clauses.add(" (" + key + " < '" + cleaned.substring(1) + "') ");
                } else if (cleaned.startsWith(">=")) {
                    clauses.add(" (" + key + " >= '" + cleaned.substring(2) + "') ");
                } else if (cleaned.startsWith(">")) {
                    clauses.add(" (" + key + " > '" + cleaned.substring(1) + "') ");
                } else {
                    validValues.add(cleaned);
                }
            }
        }

        return "(" + (clauses.isEmpty() ? "" : StringUtils.join(clauses, " OR ")) + (validValues.isEmpty() ? "" : (clauses.isEmpty() ? "" : " OR ") + " (" + key + " IN ('" + StringUtils.join(validValues, "', '") + "'))") + ")";
    }

    public static class DBMaterializedViewManager extends Thread {
        public DBMaterializedViewManager(final String currentView, final String dbName) {
            super();
            _currentView = currentView;
            _dbName = dbName;
        }

        @Override
        public void run() {
            try {
                if (_currentView != null) {
                    PoolDBUtils.ExecuteNonSelectQuery(String.format(QUERY_UPDATE_LAST_ACCESS, _currentView), _dbName, "system");
                }

                final XFTTable table = XFTTable.Execute(QUERY_GET_RECENTLY_ACCESSED, _dbName, "system");

                for (final Hashtable row : table.rowHashs()) {
                    try {
                        PoolDBUtils.ExecuteNonSelectQuery(String.format(QUERY_DROP_TABLE, row.get("table_name")), _dbName, "system");
                    } catch (Throwable e) {
                        continue;
                    }
                    try {
                        getMaterializedViewManager().delete((String) row.get("table_name"), "system");
                    } catch (Throwable e) {
                        log.error("An error occurred trying to delete the materialized view {}", row.get("table_name"), e);
                    }
                }
            } catch (Throwable e) {
                log.error("", e);
            }
        }

        private final String _dbName;
        private final String _currentView;
    }

    private static void validateColumns(final Collection<String> columns, final MaterializedViewI view) throws Exception {
        final Set<String> badColumns = Sets.difference(new HashSet<>(columns), new HashSet<>(view.getColumnNames()));
        if (!badColumns.isEmpty()) {
            throw new Exception("Invalid column(s) in request: " + StringUtils.join(badColumns, ", "));
        }
    }

    private static final String QUERY_UPDATE_LAST_ACCESS    = "UPDATE " + SCHEMA_AND_TABLE + " SET last_access=NOW() WHERE table_name='%s'";
    private static final String QUERY_GET_RECENTLY_ACCESSED = "SELECT * FROM " + SCHEMA_AND_TABLE + " WHERE last_access + INTERVAL '1 hour' < NOW()";
    private static final String QUERY_DROP_TABLE            = "DROP TABLE " + SEARCH_SCHEMA_NAME + ".%s";
    private static final String QUERY_GET_COLUMNS       = "SELECT LOWER(attname) AS col_name FROM pg_attribute, pg_class, pg_type WHERE attrelid = pg_class.oid AND atttypid = pg_type.oid AND attnum > 0 AND LOWER(relname) = '%s'";
    private static final String QUERY_GET_COLUMN_VALUES = "SELECT %1$s AS VALUES, COUNT(*) FROM " + SEARCH_SCHEMA_NAME + ".%2$s GROUP BY %1$s ORDER BY %1$s";
    private static final String QUERY_GET_COLUMNS_VALUES = "SELECT %1$s, COUNT(*) FROM " + SEARCH_SCHEMA_NAME + ".%2$s %3$s GROUP BY %1$s ORDER BY %1$s";

    private final List<String> _cachedColumnNames = new ArrayList<>();

    private String _tableName;
    private String _username;
    private String _searchId;
    private String _tag;
    private String _searchSql;
    private String _searchXml;
    private Date  _created;
    private Date  _lastAccess;
    private UserI _user;
}
