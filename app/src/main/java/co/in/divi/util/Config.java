package co.in.divi.util;

import co.in.divi.BuildConfig;
import co.in.divi.CustomSettings;

public final class Config {
    //
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Demo settings
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // CDN
    public static final boolean USE_CDN = CustomSettings.USE_CDN;

    // OTP locking
    public static final boolean ENABLE_OTP = CustomSettings.ENABLE_OTP;

    // External app sharing
    public static final boolean ENABLE_EXTERNAL_APP_SHARING = CustomSettings.ENABLE_EXTERNAL_APP_SHARING;

    // lab deployment
    public static final boolean ENABLE_PROVISIONING = CustomSettings.ENABLE_PROVISIONING;

    // live lectures
    public static final long LECTURE_SYNC_THROTTLE_TIME = CustomSettings.LECTURE_SYNC_THROTTLE_TIME;                            // seconds
    public static final long LECTURE_LOCATION_POST_THROTTLE_TIME = CustomSettings.LECTURE_LOCATION_POST_THROTTLE_TIME;                            // ms
    public static final long DELAY_CHATHEAD_AUTO_OPEN = CustomSettings.DELAY_CHATHEAD_AUTO_OPEN;                            // ms
    public static final long DELAY_INSTRUCTION_AUTO_OPEN = CustomSettings.DELAY_INSTRUCTION_AUTO_OPEN;                            // ms
    public static final long DASHBOARD_SCORES_REFRESH_INTERVAL = CustomSettings.DASHBOARD_SCORES_REFRESH_INTERVAL;

    // content update frequency
    public static final long INTERVAL_ALARM_SECONDS = CustomSettings.INTERVAL_ALARM_SECONDS;
    public static final long INTERVAL_CONTENT_UPDATE = CustomSettings.INTERVAL_CONTENT_UPDATE;
    public static final long INTERVAL_ATTEMPTS_UPDATE = CustomSettings.INTERVAL_ATTEMPTS_UPDATE;
    public static final long INTERVAL_COMMANDS_UPDATE = CustomSettings.INTERVAL_COMMANDS_UPDATE;
    public static final long INTERVAL_LOGS_UPDATE = CustomSettings.INTERVAL_LOGS_UPDATE;
    public static final long INTERVAL_REPORTS_UPDATE = CustomSettings.INTERVAL_REPORTS_UPDATE;
    public static final long INTERVAL_HEARTBEAT_UPDATE = CustomSettings.INTERVAL_HEARTBEAT_UPDATE;

    // Open app settings (remember to remove play services library link)
    public static final boolean IS_PLAYSTORE_APP = CustomSettings.IS_PLAYSTORE_APP;
    public static final boolean IS_TEACHER_ONLY = CustomSettings.IS_TEACHER_ONLY;// false for student
    public static final String IGNORE_CLASS_ID = CustomSettings.IGNORE_CLASS_ID;

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

    public static final long CONTENT_UPDATE_WARN = CustomSettings.CONTENT_UPDATE_WARN;                // 3 DAY


    // max time diff allowed
    public static final long MAX_TIME_DIFFERENCE = 60 * 1000;                            // 1 min

    public static final boolean DEBUG_HILIGHT = false;

    // debug content
    public static final boolean SHOW_CONTENT_CLEAN_BUTTON = true;
    public static final boolean SHOW_CONTENT_IMPORT_BUTTON = true;
    // button on home screen
    public static final boolean SHOW_DEBUG_BUTTON = false;

    public static final String BOOKS_LOCATION = "books/";
    public static final String TEMP_LOCATION = "temp/";
    public static final String PROGRESS_REPORTS_LOCATION = "reports/";

    // Storage location
    public static final boolean USE_HARDCODED_LOCATION_MICROMAX = false;

    // WiFi pass auto-clearing
    public static final boolean AUTO_CLEAR_WIFI_PASSWORDS = false;
}
