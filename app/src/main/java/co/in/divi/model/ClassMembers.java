package co.in.divi.model;

public class ClassMembers {

    public ClassMember[] members;

    public static class ClassMember {
        public static final String ROLE_TEACHER = "teacher";
        public static final String ROLE_STUDENT = "student";
        public static final String ROLE_TESTER = "tester";

        public String uid;
        public String name;
        public String role;
        public String profilePic;

        public SyncTimes lastSyncTimes;
    }

    public static class SyncTimes {
        public long attempts;
        public long commands;
        public long content;
        public long logs;
        public long reports;
    }
}
