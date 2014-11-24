package co.in.divi.util;

public final class Config {
	//
	// /////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Demo settings
	// /////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// CDN
	public static final boolean	USE_CDN								= false;

	// OTP locking
	public static final boolean	ENABLE_OTP							= false;

	// External app sharing
	public static final boolean	ENABLE_EXTERNAL_APP_SHARING			= true;

	// live lectures
	public static final long	LECTURE_SYNC_THROTTLE_TIME			= 12 * 1000;							// seconds
	public static final long	LECTURE_LOCATION_POST_THROTTLE_TIME	= 12 * 1000;							// ms
	public static final long	DELAY_CHATHEAD_AUTO_OPEN			= 3 * 1000;							// ms
	public static final long	DELAY_INSTRUCTION_AUTO_OPEN			= 3 * 1000;							// ms
	public static final long	DASHBOARD_SCORES_REFRESH_INTERVAL	= 10 * 1000;

	// content update frequency
	public static final long	INTERVAL_ALARM_SECONDS				= 5 * 60;
	public static final long	INTERVAL_CONTENT_UPDATE				= 30 * 60 * 1000;
	public static final long	INTERVAL_ATTEMPTS_UPDATE			= 10 * 60 * 1000;
	public static final long	INTERVAL_COMMANDS_UPDATE			= 10 * 60 * 1000;
	public static final long	INTERVAL_LOGS_UPDATE				= 04 * 60 * 1000;
	public static final long	INTERVAL_REPORTS_UPDATE				= 10 * 60 * 1000;
	public static final long	INTERVAL_HEARTBEAT_UPDATE			= 03 * 60 * 1000;

	// Open app settings (remember to remove play services library link)
	public static final boolean	IS_PLAYSTORE_APP					= false;
	public static final boolean	IS_TEACHER_ONLY						= true;								// false for
																											// student

	// // /////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// // Production settings
	// // ///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// // CDN
	// public static final boolean USE_CDN = true;
	//
	// // OTP locking
	// public static final boolean ENABLE_OTP = true;
	//
	// // live lectures
	// public static final long LECTURE_SYNC_THROTTLE_TIME = 30 * 1000; // seconds
	// public static final long LECTURE_LOCATION_POST_THROTTLE_TIME = 30 * 1000; // ms
	// public static final long DELAY_CHATHEAD_AUTO_OPEN = 5 * 1000; // ms
	// public static final long DELAY_INSTRUCTION_AUTO_OPEN = 5 * 1000; // ms
	// public static final long DASHBOARD_SCORES_REFRESH_INTERVAL = 20 * 1000;
	//
	// // content update frequency
	// public static final long INTERVAL_ALARM_SECONDS = 10 * 60; // 10 min
	// public static final long INTERVAL_CONTENT_UPDATE = 50 * 60 * 1000; // 50 mins
	// public static final long INTERVAL_ATTEMPTS_UPDATE = 20 * 60 * 1000; // 30 mins
	// public static final long INTERVAL_COMMANDS_UPDATE = 20 * 60 * 1000; // 30 mins
	// public static final long INTERVAL_LOGS_UPDATE = 30 * 60 * 1000; // 30 mins
	// public static final long INTERVAL_REPORTS_UPDATE = 60 * 60 * 1000; // 30 mins
	// public static final long INTERVAL_HEARTBEAT_UPDATE = 10 * 60 * 1000; // 30 mins

	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// END
	// ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final long	CONTENT_UPDATE_WARN					= 3 * 24 * 60 * 60 * 1000;				// 3 DAY

	// Development settings - bypass version check during update
	public static final boolean	BLIND_UPDATE						= true;

	// max time diff allowed
	public static final long	MAX_TIME_DIFFERENCE					= 60 * 1000;							// 1 min

	public static final boolean	DEBUG_HILIGHT						= false;

	// debug content
	public static final boolean	SHOW_CONTENT_CLEAN_BUTTON			= true;
	public static final boolean	SHOW_CONTENT_IMPORT_BUTTON			= true;
	// button on home screen
	public static final boolean	SHOW_DEBUG_BUTTON					= false;

	// use with getExternalStorageDirectory(), workaround for bug where android flushes the dir on update.
	public static final String	EXTERNAL_FILES_LOCATION				= "/app/co.in.divi/";
	public static final String	BOOKS_LOCATION						= EXTERNAL_FILES_LOCATION + "books/";
	public static final String	TEMP_LOCATION						= EXTERNAL_FILES_LOCATION + "temp/";
	public static final String	PROGRESS_REPORTS_LOCATION			= EXTERNAL_FILES_LOCATION + "reports/";

	// Storage location
	public static final boolean	USE_HARDCODED_LOCATION_MICROMAX		= false;

	// WiFi pass auto-clearing
	public static final boolean	AUTO_CLEAR_WIFI_PASSWORDS			= false;
}
