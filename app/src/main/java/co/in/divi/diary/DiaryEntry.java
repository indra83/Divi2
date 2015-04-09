package co.in.divi.diary;

import java.util.ArrayList;

import co.in.divi.Location;
import co.in.divi.db.model.Command;

/**
 * Created by Indra on 4/8/2015.
 */
public class DiaryEntry {
    public ENTRY_TYPE entryType;
    public String title;
    public String classId;
    public String message;
    public ArrayList<Resource> resources;

    public DiaryEntry(ENTRY_TYPE type) {
        entryType = type;
        resources = new ArrayList<DiaryEntry.Resource>();
    }

    public static class Resource {
        public Location.LOCATION_TYPE locationType;
        public Location.LOCATION_SUBTYPE locationSubType;
        public String uri;
        public Location.Breadcrumb breadcrumb;

        public transient Command unlockCommand;
    }

    public enum ENTRY_TYPE {
        HOMEWORK, ANNOUNCEMENT
    }
}
