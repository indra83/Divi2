package co.in.divi.db;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class UserDBContract {
	/**
	 * The authority of the UserDB provider.
	 */
	public static final String	AUTHORITY	= "co.in.divi.userdb";
	/**
	 * The content URI for the top-level UserDB authority.
	 */
	public static final Uri		CONTENT_URI	= Uri.parse("content://" + AUTHORITY);

	/**
	 * Constants for the (assessment question) Attempts table of the UserDB provider.
	 */
	public static final class Attempts implements CommonColumns {
		public static final String		CONTENT_NAME			= "attempts";
		public static final Uri			CONTENT_URI				= Uri.withAppendedPath(UserDBContract.CONTENT_URI, CONTENT_NAME);
		public static final String		CONTENT_TYPE			= ContentResolver.CURSOR_DIR_BASE_TYPE + "/co.in.divi.db_attempts";
		public static final String		CONTENT_ITEM_TYPE		= ContentResolver.CURSOR_ITEM_BASE_TYPE + "/co.in.divi.db_attempts";

		// columns
		public static final String		UID						= UserDBSchema.COL_ATTEMPT_UID;
		public static final String		COURSE_ID				= UserDBSchema.COL_ATTEMPT_COURSE_ID;
		public static final String		BOOK_ID					= UserDBSchema.COL_ATTEMPT_BOOK_ID;
		public static final String		ASSESSMENT_ID			= UserDBSchema.COL_ATTEMPT_ASSESSMENT_ID;
		public static final String		QUESTION_ID				= UserDBSchema.COL_ATTEMPT_QUESTION_ID;
		public static final String		TOTAL_POINTS			= UserDBSchema.COL_ATTEMPT_TOTAL_POINTS;								// points
																																		// of
																																		// question
		public static final String		SUBQUESTIONS			= UserDBSchema.COL_ATTEMPT_SUBQUESTIONS;								// elements
																																		// in
																																		// question
																																		// (e.g.
																																		// no.
																																		// of
																																		// labels)
		public static final String		CORRECT_ATTEMPTS		= UserDBSchema.COL_ATTEMPT_CORRECT_ATTEMPTS;							// correct
																																		// attempt
																																		// count
		public static final String		WRONG_ATTEMPTS			= UserDBSchema.COL_ATTEMPT_WRONG_ATTEMPTS;								// wrong
																																		// attempt
																																		// count
		public static final String		DATA					= UserDBSchema.COL_ATTEMPT_DATA;
		public static final String		SYNC_STATUS				= UserDBSchema.COL_ATTEMPT_SYNC_STATUS;
		public static final String		LAST_UPDATED			= UserDBSchema.COL_ATTEMPT_LAST_UPDATED;
		public static final String		SOLVED_AT				= UserDBSchema.COL_ATTEMPT_SOLVED_AT;

		public static final String[]	PROJECTION_ALL			= { _ID, UID, COURSE_ID, BOOK_ID, ASSESSMENT_ID, QUESTION_ID, TOTAL_POINTS,
																		SUBQUESTIONS, CORRECT_ATTEMPTS, WRONG_ATTEMPTS, DATA, LAST_UPDATED,
																		SOLVED_AT };

		public static final String[]	PROJECTION_BASIC		= { _ID, QUESTION_ID, TOTAL_POINTS, SUBQUESTIONS, CORRECT_ATTEMPTS,
																		WRONG_ATTEMPTS, DATA, LAST_UPDATED, SOLVED_AT };

		public static final String		SORT_ORDER_DEFAULT		= _ID + " ASC";
		public static final String		SORT_ORDER_LATEST_FIRST	= LAST_UPDATED + " DESC LIMIT 1";

		public static final int			SYNC_TO_SYNC			= 0;
		public static final int			SYNC_COMPLETE			= 1;
	}

	/**
	 * Constants for the (assessment) Unlocks table of the UserDB provider.
	 */
	public static final class Commands implements CommonColumns {
		public static final String		CONTENT_NAME			= "commands";
		public static final Uri			CONTENT_URI				= Uri.withAppendedPath(UserDBContract.CONTENT_URI, CONTENT_NAME);
		public static final String		CONTENT_TYPE			= ContentResolver.CURSOR_DIR_BASE_TYPE + "/co.in.divi.db_commands";
		public static final String		CONTENT_ITEM_TYPE		= ContentResolver.CURSOR_ITEM_BASE_TYPE + "/co.in.divi.db_commands";

		// columns
		public static final String		ID						= UserDBSchema.COL_COMMAND_ID;
		public static final String		UID						= UserDBSchema.COL_COMMAND_UID;
		public static final String		TEACHER_ID				= UserDBSchema.COL_COMMAND_TEACHER_ID;
		public static final String		CLASS_ID				= UserDBSchema.COL_COMMAND_CLASS_ID;
		public static final String		COURSE_ID				= UserDBSchema.COL_COMMAND_COURSE_ID;
		public static final String		BOOK_ID					= UserDBSchema.COL_COMMAND_BOOK_ID;
		public static final String		ITEM_ID					= UserDBSchema.COL_COMMAND_ITEM_ID;
		public static final String		TYPE					= UserDBSchema.COL_COMMAND_TYPE;
		public static final String		ITEM_TYPE				= UserDBSchema.COL_COMMAND_ITEM_TYPE;
		public static final String		STATUS					= UserDBSchema.COL_COMMAND_STATUS;
		public static final String		CREATE_TIMESTAMP		= UserDBSchema.COL_COMMAND_CREATE_TIMESTAMP;
		public static final String		APPLY_TIMESTAMP			= UserDBSchema.COL_COMMAND_APPLY_TIMESTAMP;
		public static final String		END_TIMESTAMP			= UserDBSchema.COL_COMMAND_END_TIMESTAMP;
		public static final String		DATA					= UserDBSchema.COL_COMMAND_DATA;

		public static final String[]	PROJECTION_ALL			= { _ID, ID, UID, TEACHER_ID, CLASS_ID, COURSE_ID, BOOK_ID, ITEM_ID, TYPE,
																		ITEM_TYPE, STATUS, CREATE_TIMESTAMP, APPLY_TIMESTAMP,
																		END_TIMESTAMP, DATA, LAST_UPDATED };

		public static final String		SORT_ORDER_DEFAULT		= _ID + " ASC";
		public static final String		SORT_ORDER_LATEST_FIRST	= LAST_UPDATED + " DESC LIMIT 1";
	}

	/**
	 * This interface defines common columns found in multiple tables.
	 */
	public static interface CommonColumns extends BaseColumns {
		public static final String	LAST_UPDATED	= "last_updated";
	}
}
