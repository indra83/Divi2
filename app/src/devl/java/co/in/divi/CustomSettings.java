package co.in.divi;

/**
 * Created by Indra on 2/25/2015.
 */
public final class CustomSettings {
    public static final boolean USE_CDN = true;
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
    public static final String KEEN_PROJECT_ID = "54ec585a90e4bd195a46052b";
    public static final String KEEN_API_KEY = "5D9AF63A162275AB6D27D49AC5198141";
    public static final String KEEN_WRITE_KEY = "a9a499e176f67088f1b87f6c7be8d14706ff0a8e3391f0e8841217b0b64c56e02824f094e0977085e325a7b5c46c10e50d2ee1751292fe847a8818bf5d4731e026905474face2584ef1fc182d41ed6b615dc68406d24b749a25b601e7f66cefdff9d4dcb5f11f535f9a40204b0e31d60";
    public static final String KEEN_READ_KEY = "d96613dbea552e0a35a65d675fa4006aa7a829b97a33db0c7b4b7759b058e430962481c18f156963c410e8318b1537c8ad83f3df9614752e4dd52ea32a3b115a2ffe1e6fa77a6388526e159f52dcd4ebe8c786545a99e4f2ddd794887b998d1d518ba1967660283cc2135e230fb5c250";

}
