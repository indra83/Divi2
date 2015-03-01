package co.in.divi;

/**
 * Created by Indra on 2/25/2015.
 */
public final class CustomSettings {
    public static final boolean USE_CDN = false;
    public static final boolean ENABLE_OTP = false;
    public static final boolean ENABLE_EXTERNAL_APP_SHARING = true;
    // live lectures
    public static final long LECTURE_SYNC_THROTTLE_TIME = 12 * 1000;                            // seconds
    public static final long LECTURE_LOCATION_POST_THROTTLE_TIME = 12 * 1000;                            // ms
    public static final long DELAY_CHATHEAD_AUTO_OPEN = 3 * 1000;                            // ms
    public static final long DELAY_INSTRUCTION_AUTO_OPEN = 3 * 1000;                            // ms
    public static final long DASHBOARD_SCORES_REFRESH_INTERVAL = 10 * 1000;
    // content update frequency
    public static final long INTERVAL_ALARM_SECONDS = 15 * 60;
    public static final long INTERVAL_CONTENT_UPDATE = 30 * 60 * 1000;
    public static final long INTERVAL_ATTEMPTS_UPDATE = 30 * 60 * 1000;
    public static final long INTERVAL_COMMANDS_UPDATE = 30 * 60 * 1000;
    public static final long INTERVAL_LOGS_UPDATE = 30 * 60 * 1000;
    public static final long INTERVAL_REPORTS_UPDATE = 30 * 60 * 1000;
    public static final long INTERVAL_HEARTBEAT_UPDATE = 30 * 60 * 1000;
    // Open app settings (remember to remove play services library link)
    public static final boolean IS_PLAYSTORE_APP = false;
    public static final boolean IS_TEACHER_ONLY = false;// false for student
    public static final String IGNORE_CLASS_ID = "1";

    public static final long CONTENT_UPDATE_WARN = 3 * 24 * 60 * 60 * 1000;                // 3 DAY

    // keen settings
    public static final String KEEN_PROJECT_ID = "";
    public static final String KEEN_API_KEY = "";
    public static final String KEEN_WRITE_KEY = "";
    public static final String KEEN_READ_KEY = "";

}
