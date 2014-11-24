package co.in.divi.util;

public final class ServerConfig {

    public static final String SERVER_ENDPOINT = "http://divi-playstore.herokuapp.com/v2";
    // public static final String SERVER_ENDPOINT = "http://divi-staging.herokuapp.com/v1";

    public static final String KEEN_PROJECT_ID = "53756d2c00111c1eb700000e";
    public static final String KEEN_WRITE_KEY = "3306e3ce7e04bbfca8ab2e6cea1afaba5ad463a475858b17f4874ea6bc573083fd9cfa7f90332c030644b30d10fb45db0c72779a4dc7d151d5dd88c96c423f203902a8e40f34e899e0baf038c74bd4bb44cb509f6b05d1c3738ed5b7f45128e8bf3eefe89e52e3ea57854e326c34b94f";
    public static final String KEEN_READ_KEY = "2f66acfc3274d6fbaa685fb39906ad7bc9bfd88842089523208548d7092eb95e4f627a236e859e570c232c21798082d5798e32715e87ce847cb7d947a94295db58e45c2486568a4090438d82e98e53d7d6d815a2976ad66ebff3c006c02a51a4e74339c9665f9fc9dbf909225b87f25c";

    public static final String KEEN_EVENT_COLLECTION = "logs";
    public static final String KEEN_API_ENDPOINT = "https://api.keen.io/3.0/projects/" + KEEN_PROJECT_ID
            + "/events?api_key=A74108790CE9CDCF894BFFC4CC8861F4";

    public static final String PUBNUB_PUBLISH_KEY = "pub-c-92cb1e82-71a1-4fd0-81ed-8cce0f25e4a3";
    public static final String PUBNUB_SUBSCRIBE_KEY = "sub-c-56e5356a-5071-11e3-98c1-02ee2ddab7fe";
    public static final String PUBNUB_SECRET_KEY = "sec-c-YzIxZmMzNjktMmM0My00NDMzLTllYWItMDRmYmRkMjA0ZGY5";
    // public static final String PUBNUB_PUBLISH_KEY = "pub-306005c1-4e13-4c51-bb93-122eb5c8fbfc";
    // public static final String PUBNUB_SUBSCRIBE_KEY = "sub-00cde023-ceee-11e1-a24f-d7f9c3daaffd";

    public static final String YOUTUBE_DEVELOPER_KEY = "AIzaSyDl8dNER4ln12hoCL53HSatQLtsUyKlW8c";

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

    public static final String METHOD_GOOGLELOGIN = "/loginGoogleUser";
    public static final String METHOD_CREATECLASSROOM = "/createClassRoom";
    public static final String METHOD_JOINCLASSROOM = "/joinClassRoom";
}