package co.in.divi.logs;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class LogsDBContract {
	/**
	 * The authority of the UserDB provider.
	 */
	public static final String	AUTHORITY	= "co.in.divi.logsdb";
	/**
	 * The content URI for the top-level UserDB authority.
	 */
	public static final Uri		CONTENT_URI	= Uri.parse("content://" + AUTHORITY);

	/**
	 * Constants for the Logs table of the LogsDB provider.
	 */
	public static final class Logs implements CommonColumns {
		public static final String		CONTENT_NAME			= "logs";
		public static final Uri			CONTENT_URI				= Uri.withAppendedPath(LogsDBContract.CONTENT_URI, CONTENT_NAME);
		public static final String		CONTENT_TYPE			= ContentResolver.CURSOR_DIR_BASE_TYPE + "/co.in.divi.db_logs";
		public static final String		CONTENT_ITEM_TYPE		= ContentResolver.CURSOR_ITEM_BASE_TYPE + "/co.in.divi.db_logs";

		// columns
		public static final String		UID						= LogsDBSchema.COL_LOG_UID;
		public static final String		TYPE					= LogsDBSchema.COL_LOG_TYPE;
		public static final String		URI						= LogsDBSchema.COL_LOG_URI;
		public static final String		RESOURCE_TYPE			= LogsDBSchema.COL_LOG_RESOURCE_TYPE;
		public static final String		OPENED_AT				= LogsDBSchema.COL_LOG_OPENEDAT;
		public static final String		DURATION				= LogsDBSchema.COL_LOG_DURATION;
		public static final String		TOKEN					= LogsDBSchema.COL_LOG_SESSION_TOKEN;								// points
																																	// of
																																	// question
		public static final String		SYNC_STATUS				= LogsDBSchema.COL_LOG_SYNC_STATUS;								// elements

		public static final String[]	PROJECTION_ALL			= { _ID, UID, TYPE, URI, RESOURCE_TYPE, OPENED_AT, DURATION, TOKEN,
																		SYNC_STATUS, LAST_UPDATED };

		public static final String		SORT_ORDER_DEFAULT		= _ID + " ASC";
		public static final String		SORT_ORDER_LATEST_FIRST	= LAST_UPDATED + " DESC LIMIT 1";

		public static final int			SYNC_TO_SYNC			= 0;
		public static final int			SYNC_COMPLETE			= 1;
	}

	/**
	 * This interface defines common columns found in multiple tables.
	 */
	public static interface CommonColumns extends BaseColumns {
		public static final String	LAST_UPDATED	= "last_updated";
	}
}
