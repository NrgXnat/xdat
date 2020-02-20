/*
 * core: org.nrg.xft.db.FavEntries
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xft.db;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.security.UserI;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.nrg.xft.db.PoolDBUtils.SEARCH_SCHEMA_NAME;

@SuppressWarnings("DuplicateThrows")
@Slf4j
public class FavEntries {
	public final static String FAV_ENTRIES_TABLE = "xs_fav_entries";

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		PoolDBUtils.CheckSpecialSQLChars(dataType);
		this.dataType = dataType;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public UserI getUser() {
		return user;
	}

	public void setUser(UserI user) {
		this.user = user;
	}

	public Integer getXdat_user_id() {
		return _xdatUserId;
	}

	public void setXdat_user_id(final Integer xdatUserId) {
		_xdatUserId = xdatUserId;
	}

	public void save() throws SQLException, Exception{
		VerifyTableExistence(user);

		if (!ObjectUtils.allNotNull(dataType, id, user)) {
			throw new NullPointerException();
		}

		if (_xdatUserId == null) {
			_xdatUserId = user.getID();
		}

		if (FavEntries.GetFavoriteEntries(dataType, id, user) == null) {
			PoolDBUtils.ExecuteNonSelectQuery(String.format(QUERY_INSERT_FAV_ENTRY, dataType, id, _xdatUserId), user.getDBName(), user.getLogin());
		}
	}
	
	public void delete() throws SQLException,Exception{
		VerifyTableExistence(user);
		if(dataType==null || id==null || (user==null)){
			throw new NullPointerException();
		}
		
		if(_xdatUserId == null){
			_xdatUserId =user.getID();
		}
		
		PoolDBUtils.ExecuteNonSelectQuery(String.format(QUERY_DELETE_FAV_ENTRY, dataType, id, _xdatUserId), user.getDBName(), user.getLogin());
	}
	
	public static XFTTable GetFavoriteEntries(final String dataType, final UserI user) throws DBPoolException,SQLException{
		VerifyTableExistence(user);
		return XFTTable.Execute(String.format(QUERY_GET_FAV_ENTRIES, dataType, user.getID()), user.getDBName(), user.getLogin());
	}
	
	public static FavEntries GetFavoriteEntries(final String dataType, final String id, final UserI user) throws DBPoolException,SQLException{
		VerifyTableExistence(user);
		final XFTTable table= XFTTable.Execute(String.format(QUERY_GET_FAV_ENTRY_BY_ID, dataType, id, user.getID()), user.getDBName(), user.getLogin());
		if (table.size() == 0) {
			return null;
		}
		final FavEntries favEntries = new FavEntries();
		favEntries.setId(id);
		favEntries.setDataType(dataType);
		favEntries.setUser(user);
		return favEntries;
	}

	public static void VerifyTableExistence(UserI user){
		try {
            if (!EXISTS.get()){
        		PoolDBUtils.CreateTempSchema(user.getDBName(), user.getLogin());
        		if (PoolDBUtils.ReturnStatisticQuery(QUERY_FAV_ENTRIES_EXISTS, "relname", user.getDBName(), user.getLogin()) == null){
                    PoolDBUtils.ExecuteNonSelectQuery(QUERY_CREATE_FAV_ENTRIES, user.getDBName(), user.getLogin());
                }
                EXISTS.set(true);
            }
		} catch (SQLException e) {
			log.error("An database or SQL error occurred trying to check if the XNAT item cache table exists.", e);
		} catch (Exception e) {
			log.error("An unknown error occurred trying to check if the XNAT item cache table exists.", e);
        }
	}

	private final static String QUERY_INSERT_FAV_ENTRY    = "INSERT INTO " + SEARCH_SCHEMA_NAME + "." + FAV_ENTRIES_TABLE + " (dataType, id, xdat_user_id) VALUES ('%s', '%s', %d)";
	private final static String QUERY_GET_FAV_ENTRIES     = "SELECT datatype, id FROM " + SEARCH_SCHEMA_NAME + "." + FAV_ENTRIES_TABLE + " WHERE dataType = '%s' AND xdat_user_id = %d";
	private final static String QUERY_GET_FAV_ENTRY_BY_ID = "SELECT datatype, id FROM " + SEARCH_SCHEMA_NAME + "." + FAV_ENTRIES_TABLE + " WHERE dataType = '%s' AND id = '%s' AND xdat_user_id = %d";
	private final static String QUERY_DELETE_FAV_ENTRY    = "DELETE FROM " + SEARCH_SCHEMA_NAME + "." + FAV_ENTRIES_TABLE + " WHERE dataType = '%s' AND id = '%s' AND xdat_user_id = %d";
	private final static String QUERY_FAV_ENTRIES_EXISTS  = "SELECT relname FROM pg_catalog.pg_class WHERE relname = LOWER('" + FAV_ENTRIES_TABLE + "')";
	private final static String QUERY_CREATE_FAV_ENTRIES  = "CREATE TABLE " + SEARCH_SCHEMA_NAME + "." + FAV_ENTRIES_TABLE + "("
															+ "    dataType VARCHAR(255),"
															+ "    id VARCHAR(255),"
															+ "    xdat_user_id integer)";

	private static final AtomicBoolean EXISTS = new AtomicBoolean();

	private UserI   user        =null;
	private String  dataType    =null;
	private String  id          = null;
	private Integer _xdatUserId =null;
}
