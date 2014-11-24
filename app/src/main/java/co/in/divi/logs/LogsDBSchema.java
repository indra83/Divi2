package co.in.divi.logs;

import android.provider.BaseColumns;

interface LogsDBSchema {
	String	DB_NAME					= "logsdb.db";

	String	TBL_LOGS				= "logs";
	String	COL_ID					= BaseColumns._ID;
	String	COL_LOG_UID				= "uid";
	String	COL_LOG_TYPE			= "type";
	String	COL_LOG_URI				= "uri";
	String	COL_LOG_RESOURCE_TYPE	= "resource_type";
	String	COL_LOG_OPENEDAT		= "opened_at";
	String	COL_LOG_DURATION		= "duration";
	String	COL_LOG_SESSION_TOKEN	= "session_token";
	String	COL_LOG_SYNC_STATUS		= "sync_status";

	String	COL_LOG_LAST_UPDATED	= "last_updated";

	String	DDL_CREATE_TBL_LOGS		= "CREATE TABLE " + TBL_LOGS + " ("
											+ "_id                                   INTEGER  PRIMARY KEY AUTOINCREMENT, \n"
											+ "uid                                   TEXT NOT NULL,\n"
											+ "uri                                   TEXT NOT NULL,\n"
											+ "resource_type                         TEXT NOT NULL,\n"
											+ "session_token                         TEXT NOT NULL,\n"
											+ "type                                  INTEGER,\n"
											+ "opened_at                             INTEGER,\n"
											+ "duration                              INTEGER,\n"
											+ "last_updated                          INTEGER, \n"
											+ "sync_status                           INTEGER )";

	String	DDL_DROP_TBL_LOGS		= "DROP TABLE IF EXISTS " + TBL_LOGS;
}
