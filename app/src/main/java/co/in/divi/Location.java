package co.in.divi;

import co.in.divi.content.DiviReference;

/**
 * Created by Indra on 4/7/2015.
 */
public class Location {
    // OFF - screen off or locked
    public static enum LOCATION_TYPE {
        TOPIC, ASSESSMENT, HOME, BLACKOUT, OFF, UNKNOWN
    }

    public static enum LOCATION_SUBTYPE {
        TOPIC_TOPIC, TOPIC_VIDEO, TOPIC_AUDIO, TOPIC_IMAGE, TOPIC_IMAGESET, TOPIC_VM, ASSESSMENT_QUIZ, ASSESSMENT_EXERCISE, ASSESSMENT_TEST, APP
    }

    private ProtectedResourceMetadata unlockData;
    private DiviReference curLocationRef;
    private LOCATION_TYPE type;
    private LOCATION_SUBTYPE subType;
    private Breadcrumb breadcrumb;
    // external app details (when location unknown)
    private String appPackageName;
    private String appName;

    private Location() {
        // use factory method
    }

    public static Location getUnknownLocation() {
        Location loc = new Location();
        loc.type = LOCATION_TYPE.UNKNOWN;
        return loc;
    }

    public static Location getLocation(Location.LOCATION_TYPE type, Location.LOCATION_SUBTYPE subType, DiviReference newRef, Location.Breadcrumb breadcrumb,
                                       Location.ProtectedResourceMetadata unlockData) {
        Location loc = new Location();
        loc.curLocationRef = newRef;
        loc.breadcrumb = breadcrumb;
        loc.type = type;
        loc.subType = subType;
        loc.unlockData = unlockData;
        return loc;
    }

    public DiviReference getLocationRef() {
        return curLocationRef;
    }

    public boolean isLocationLocked() {
        return unlockData != null;
    }

    public String getAppPackageName() {
        return appPackageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppDetails(String pkgName, String appName) {
        this.appPackageName = pkgName;
        this.appName = appName;
    }

    public ProtectedResourceMetadata getProtectedResourceMetadata() {
        return unlockData;
    }

    public LOCATION_TYPE getLocationType() {
        return type;
    }

    public LOCATION_SUBTYPE getLocationSubType() {
        return subType;
    }

    public Breadcrumb getBreadcrumb() {
        return breadcrumb;
    }

    public boolean isLocationStreamable() {
        return (subType == LOCATION_SUBTYPE.TOPIC_VIDEO || subType == LOCATION_SUBTYPE.TOPIC_IMAGESET);
    }

    public static class Breadcrumb {
        public String courseName, bookName, chapterName, itemName, subItemName;

        public static Breadcrumb get(String courseName, String bookName, String chapterName, String itemName, String subItemName) {
            Breadcrumb b = new Breadcrumb();
            b.courseName = courseName;
            b.bookName = bookName;
            b.chapterName = chapterName;
            b.itemName = itemName;
            b.subItemName = subItemName;
            return b;
        }

        private Breadcrumb() {
        }

        public String[] getBreadcrumbArray() {
            return new String[]{courseName, bookName, chapterName, itemName, subItemName};
        }

        @Override
        public String toString() {
            String ret = "" + courseName + " > " + bookName + " > " + chapterName + " > " + itemName;
            if (subItemName != null)
                ret = ret + " > " + subItemName;
            return ret;
        }

        public String toItemString() {
            String ret = "" + chapterName + " > " + itemName;
            if (subItemName != null)
                ret = ret + " > " + subItemName;
            return ret;
        }
    }

    public static class ProtectedResourceMetadata {
        public String name;
        public int itemType;    // refers to command itemType
        public long duration;
        public String data;

        public ProtectedResourceMetadata(String name, int itemType, long duration, String data) {
            this.name = name;
            this.itemType = itemType;
            this.duration = duration;
            this.data = data;
        }
    }
}
