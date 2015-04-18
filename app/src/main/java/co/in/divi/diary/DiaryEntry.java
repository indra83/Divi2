package co.in.divi.diary;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    public String teacherId;
    public String teacherName;
    public ArrayList<Resource> resources;

    // mapped to the endsAt in Command.
    public transient Date dueDate;
    public transient long createdAt;

    public DiaryEntry(ENTRY_TYPE type, String teacherId, String teacherName) {
        entryType = type;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        resources = new ArrayList<DiaryEntry.Resource>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR,24);
        dueDate = cal.getTime();
    }
/*
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

*/
    public void setDueDate(Date date) {
        dueDate = date;
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

//    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
}
