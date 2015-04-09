package co.in.divi.diary;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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
    public String dueDate;

    private transient Date dueDateDate;

    public DiaryEntry(ENTRY_TYPE type) {
        entryType = type;
        resources = new ArrayList<DiaryEntry.Resource>();
    }

    public Date getDueDate() {
        if (dueDateDate == null) {
            try {
                dueDateDate = format.parse(dueDate);
            } catch (Exception e) {
                dueDateDate = new Date();
            }
        }
        return dueDateDate;
    }

    public void setDueDate(Date date) {
        dueDateDate = date;
        dueDate = format.format(date);
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

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
}
