package co.in.divi.util;

import co.in.divi.BuildConfig;
import co.in.divi.CustomSettings;

public final class ServerConfig {

    public static final String SERVER_ENDPOINT = BuildConfig.SERVER_ENDPOINT;

    public static final String KEEN_PROJECT_ID = CustomSettings.KEEN_PROJECT_ID;
    public static final String KEEN_API_KEY = CustomSettings.KEEN_API_KEY;
    public static final String KEEN_WRITE_KEY = CustomSettings.KEEN_WRITE_KEY;
    public static final String KEEN_READ_KEY = CustomSettings.KEEN_READ_KEY;

    public static final String KEEN_EVENT_COLLECTION = "logs";
    public static final String KEEN_API_ENDPOINT = "https://api.keen.io/3.0/projects/" + KEEN_PROJECT_ID
            + "/events?api_key=" + KEEN_API_KEY;

    public static final String PUBNUB_PUBLISH_KEY = "pub-c-92cb1e82-71a1-4fd0-81ed-8cce0f25e4a3";
    public static final String PUBNUB_SUBSCRIBE_KEY = "sub-c-56e5356a-5071-11e3-98c1-02ee2ddab7fe";
    public static final String PUBNUB_SECRET_KEY = "sec-c-YzIxZmMzNjktMmM0My00NDMzLTllYWItMDRmYmRkMjA0ZGY5";

    public static final String YOUTUBE_DEVELOPER_KEY = BuildConfig.YOUTUBE_DEVELOPER_KEY;

    public static final String PUBNUB_TEACHER_CHANNEL_POSTFIX = "_teacher";

    public static final String METHOD_LOGIN = "/loginUser";
    public static final String METHOD_GETCONTENTUPDATES = "/getContentUpdates";
    public static final String METHOD_CREATELECTURE = "/createLecture";
    public static final String METHOD_GETLECTURES = "/getLectures";
    public static final String METHOD_ENDLECTURE = "/endLecture";
    public static final String METHOD_SENDINSTRUCTION = "/sendInstruction";
    public static final String METHOD_GETINSTRUCTIONS = "/getInstructions";
    public static final String METHOD_GETLECTUREMEMBERS = "/getLectureMembers";
    public static final String METHOD_GETCLASSMEMBERS = "/getClassRoomMembers";

    public static final String METHOD_GETDASHBOARDDATA = "/dashboardScores.json";
    public static final String METHOD_GETSCORES = "/getScores";

    public static final String METHOD_SYNCUP = "/syncUp";
    public static final String METHOD_SYNCDOWN = "/syncDown";
    public static final String METHOD_HEARTBEAT = "/tabCheckIn";
    public static final String METHOD_PROVISIONING = "/checkLabProvisioning";

    public static final String METHOD_GOOGLELOGIN = "/loginGoogleUser";
    public static final String METHOD_CREATECLASSROOM = "/createClassRoom";
    public static final String METHOD_JOINCLASSROOM = "/joinClassRoom";
}