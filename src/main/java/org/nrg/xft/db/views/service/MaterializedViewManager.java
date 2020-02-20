/*
 * core: org.nrg.xft.db.views.service.MaterializedViewManager
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xft.db.views.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.MaterializedViewI;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.security.UserI;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.nrg.xft.db.PoolDBUtils.SEARCH_SCHEMA_NAME;

@SuppressWarnings("DuplicateThrows")
@Slf4j
public class MaterializedViewManager {
    public static final String MATERIALIZED_VIEWS_TABLE = "xs_materialized_views";
    public static final String SCHEMA_AND_TABLE         = SEARCH_SCHEMA_NAME + "." + MATERIALIZED_VIEWS_TABLE;

    public static MaterializedViewManager getMaterializedViewManager() {
        try {
            if (manager == null) {
                PoolDBUtils.CreateTempSchema(PoolDBUtils.getDefaultDBName(), null);
                if (PoolDBUtils.ReturnStatisticQuery(QUERY_VIEWS_TABLE_EXISTS, "relname", PoolDBUtils.getDefaultDBName(), null) == null) {
                    PoolDBUtils.ExecuteNonSelectQuery(QUERY_CREATE_VIEWS_TABLE, PoolDBUtils.getDefaultDBName(), null);
                }
                manager = new MaterializedViewManager();
            }
        } catch (SQLException e) {
            log.error("An database or SQL error occurred trying to validate or create the materialized view manager table.", e);
        } catch (Exception e) {
            log.error("An unknown error occurred trying to validate or create the materialized view manager table.", e);
        }

        return manager;
    }

    public List<MaterializedViewI> getViewsByUser(final UserI user, final MaterializedViewServiceI service) throws Exception {
        final List<MaterializedViewI> views = new ArrayList<>();
        final XFTTable                table = XFTTable.Execute(String.format(QUERY_GET_USER_VIEWS, user.getUsername()), PoolDBUtils.getDefaultDBName(), user.getUsername());
        if (table.size() > 0) {
            while (table.hasMoreRows()) {
                views.add(service.populateView(table.nextRowHash(), user));
            }
        }
        return views;
    }

    public MaterializedViewI getViewBySearchID(final String searchId, UserI user, MaterializedViewServiceI service) throws Exception {
        final XFTTable table = XFTTable.Execute(String.format(QUERY_GET_VIEW_BY_ID, searchId), PoolDBUtils.getDefaultDBName(), user.getUsername());
        return table.size() > 0 ? service.populateView(table.nextRowHash(), user) : null;
    }

    public MaterializedViewI getViewByTablename(final String tableName, final UserI user, final MaterializedViewServiceI service) throws Exception {
        final XFTTable table = XFTTable.Execute(String.format(QUERY_GET_VIEW_BY_TABLENAME, tableName), PoolDBUtils.getDefaultDBName(), user.getUsername());
        return table.size() > 0 ? service.populateView(table.nextRowHash(), user) : null;
    }

    public void delete(final MaterializedViewI view) throws SQLException, Exception {
        delete(view.getTable_name(), view.getUser_name());
    }

    public void delete(final String viewName, final String username) throws SQLException, Exception {
        PoolDBUtils.ExecuteNonSelectQuery(String.format(QUERY_DELETE_VIEW, viewName), PoolDBUtils.getDefaultDBName(), username);
    }

    public void register(final MaterializedViewI view) throws SQLException, Exception {
        PoolDBUtils.ExecuteNonSelectQuery(String.format(QUERY_INSERT_VIEW, view.getTable_name(), view.getUser_name(), wrap(view.getSearch_id()), wrap(view.getTag()), wrap(view.getSearch_sql()), wrap(view.getSearch_xml())), PoolDBUtils.getDefaultDBName(), view.getUser_name());
    }

    public static void Register(final MaterializedViewI view) throws SQLException, Exception {
        getMaterializedViewManager().register(view);
    }

    public static void Delete(final MaterializedViewI view) throws SQLException, Exception {
        getMaterializedViewManager().delete(view);
    }

    private String wrap(final String value) {
        if (StringUtils.isBlank(value)) {
            return "NULL";
        }
        return "'" + StringUtils.replaceChars(value, "'", "''") + "'";
    }

    private static final String QUERY_GET_USER_VIEWS        = "SELECT * FROM " + SCHEMA_AND_TABLE + " WHERE username='%s'";
    private static final String QUERY_DELETE_VIEW           = "DELETE FROM " + SCHEMA_AND_TABLE + " WHERE table_name ='%s'";
    private static final String QUERY_VIEWS_TABLE_EXISTS    = "SELECT relname FROM pg_catalog.pg_class WHERE relname = LOWER('" + MATERIALIZED_VIEWS_TABLE + "')";
    private static final String QUERY_CREATE_VIEWS_TABLE    = "CREATE TABLE " + SCHEMA_AND_TABLE + " (" +
                                                              "    table_name VARCHAR(255)," +
                                                              "    created timestamp DEFAULT now()," +
                                                              "    last_access timestamp DEFAULT now()," +
                                                              "    username VARCHAR(255)," +
                                                              "    search_id text," +
                                                              "    tag VARCHAR(255)," +
                                                              "    search_sql text," +
                                                              "    search_xml text)";
    private static final String QUERY_GET_VIEW_BY_ID        = "SELECT * FROM " + SCHEMA_AND_TABLE + " WHERE search_id = '%s'";
    private static final String QUERY_GET_VIEW_BY_TABLENAME = "SELECT * FROM " + SCHEMA_AND_TABLE + " WHERE table_name = '%s'";
    private static final String QUERY_INSERT_VIEW           = "INSERT INTO " + SCHEMA_AND_TABLE + " (table_name,created,last_access,username,search_id,tag,search_sql,search_xml) VALUES ('%s', NOW(), NOW(), '%s', '%s', '%s', '%s', '%s')";

    private static MaterializedViewManager manager = null;
}
