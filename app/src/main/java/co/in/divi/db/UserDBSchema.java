package co.in.divi.db;

import android.provider.BaseColumns;

interface UserDBSchema {
	String	DB_NAME							= "userdb.db";

	String	TBL_ATTEMPTS					= "attempts";
	String	COL_ID							= BaseColumns._ID;
	String	COL_ATTEMPT_UID					= "uid";
	String	COL_ATTEMPT_COURSE_ID			= "course_id";
	String	COL_ATTEMPT_BOOK_ID				= "book_id";
	String	COL_ATTEMPT_ASSESSMENT_ID		= "assessment_id";
	String	COL_ATTEMPT_QUESTION_ID			= "question_id";
	String	COL_ATTEMPT_TOTAL_POINTS		= "total_points";
	String	COL_ATTEMPT_SUBQUESTIONS		= "subquestions";
	String	COL_ATTEMPT_CORRECT_ATTEMPTS	= "correct_attempts";
	String	COL_ATTEMPT_WRONG_ATTEMPTS		= "wrong_attempts";
	String	COL_ATTEMPT_DATA				= "data";
	String	COL_ATTEMPT_SYNC_STATUS			= "sync_status";
	String	COL_ATTEMPT_LAST_UPDATED		= "last_updated";
	String	COL_ATTEMPT_SOLVED_AT			= "solved_at";

	String	DDL_CREATE_TBL_ATTEMPTS			= "CREATE TABLE "
													+ TBL_ATTEMPTS
													+ " ("
													+ "_id                                   INTEGER  PRIMARY KEY AUTOINCREMENT, \n"
													+ "uid                                   TEXT NOT NULL,\n"
													+ "course_id                             TEXT NOT NULL,\n"
													+ "book_id                               TEXT NOT NULL,\n"
													+ "assessment_id                         TEXT NOT NULL,\n"
													+ "question_id                           TEXT NOT NULL,\n"
													+ "total_points                          INTEGER,\n"
													+ "subquestions                          INTEGER,\n"
													+ "correct_attempts                      INTEGER,\n"
													+ "wrong_attempts                        INTEGER,\n"
													+ "data                                  TEXT,\n"
													+ "sync_status                           INTEGER,\n"
													+ "last_updated                          INTEGER, \n"
													+ "solved_at                             INTEGER, \n"
													+ "UNIQUE (\"course_id\",\"book_id\",\"assessment_id\",\"question_id\",\"uid\") ON CONFLICT REPLACE)";

	String	DDL_DROP_TBL_ATTEMPTS			= "DROP TABLE IF EXISTS " + TBL_ATTEMPTS;

	String	TBL_COMMANDS					= "commands";
	String	COL_COMMAND_ID					= "id";
	String	COL_COMMAND_UID					= "uid";
	String	COL_COMMAND_CLASS_ID			= "class_id";
	String	COL_COMMAND_TEACHER_ID			= "teacher_id";
	String	COL_COMMAND_COURSE_ID			= "course_id";
	String	COL_COMMAND_BOOK_ID				= "book_id";
	String	COL_COMMAND_ITEM_ID				= "item_id";
	String	COL_COMMAND_TYPE				= "type";
	String	COL_COMMAND_ITEM_TYPE			= "item_type";
	String	COL_COMMAND_STATUS				= "status";
	String	COL_COMMAND_CREATE_TIMESTAMP	= "create_timestamp";
	String	COL_COMMAND_APPLY_TIMESTAMP		= "apply_timestamp";
	String	COL_COMMAND_END_TIMESTAMP		= "end_timestamp";
	String	COL_COMMAND_DATA				= "data";
	String	COL_COMMAND_LAST_UPDATED		= "last_updated";

	String	DDL_CREATE_TBL_COMMANDS			= "CREATE TABLE " + TBL_COMMANDS + " ("
													+ "_id                                   INTEGER  PRIMARY KEY AUTOINCREMENT, \n"
													+ "id                                    TEXT NOT NULL,\n"
													+ "uid                                   TEXT NOT NULL,\n"
													+ "class_id                              TEXT NOT NULL,\n"
													+ "teacher_id                            TEXT NOT NULL,\n"
													+ "course_id                             TEXT,\n"
													+ "book_id                               TEXT,\n"
													+ "item_id                               TEXT,\n"
													+ "type                                  INTEGER,\n"
													+ "item_type                             INTEGER,\n"
													+ "status                                INTEGER,\n"
													+ "create_timestamp                      INTEGER,\n"
													+ "apply_timestamp                       INTEGER,\n"
													+ "end_timestamp                         INTEGER,\n"
													+ "last_updated                          INTEGER, \n"
													+ "data                                  TEXT, \n"
													+ "UNIQUE (\"id\") ON CONFLICT REPLACE)";

	String	DDL_DROP_TBL_COMMANDS			= "DROP TABLE IF EXISTS " + TBL_COMMANDS;
}
