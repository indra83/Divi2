package co.in.divi;

/**
 * Created by Indra on 2/25/2015.
 */
public final class CustomSettings {
    public static final boolean USE_CDN = false;
    public static final boolean ENABLE_OTP = false;
    public static final boolean ENABLE_EXTERNAL_APP_SHARING = true;
    public static final boolean ENABLE_PROVISIONING = false;
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
    public static final String KEEN_PROJECT_ID = "53756d2c00111c1eb700000e";
    public static final String KEEN_API_KEY = "A74108790CE9CDCF894BFFC4CC8861F4";
    public static final String KEEN_WRITE_KEY = "3306e3ce7e04bbfca8ab2e6cea1afaba5ad463a475858b17f4874ea6bc573083fd9cfa7f90332c030644b30d10fb45db0c72779a4dc7d151d5dd88c96c423f203902a8e40f34e899e0baf038c74bd4bb44cb509f6b05d1c3738ed5b7f45128e8bf3eefe89e52e3ea57854e326c34b94f";
    public static final String KEEN_READ_KEY = "2f66acfc3274d6fbaa685fb39906ad7bc9bfd88842089523208548d7092eb95e4f627a236e859e570c232c21798082d5798e32715e87ce847cb7d947a94295db58e45c2486568a4090438d82e98e53d7d6d815a2976ad66ebff3c006c02a51a4e74339c9665f9fc9dbf909225b87f25c";

}
