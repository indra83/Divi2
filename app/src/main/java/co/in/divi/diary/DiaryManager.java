package co.in.divi.diary;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

import co.in.divi.diary.DiaryManager.DiaryEntry.ENTRY_TYPE;
import co.in.divi.LocationManager;
import co.in.divi.LocationManager.LOCATION_TYPE;
import co.in.divi.UserSessionProvider;
import co.in.divi.db.model.Command;
import co.in.divi.logs.LogsManager;
import co.in.divi.util.DiviCalendar;
import co.in.divi.util.Util;
import co.in.divi.util.Week;

public class DiaryManager {

    private static final String TAG = DiaryManager.class.getSimpleName();

    private static DiaryManager instance = null;

    public static interface DiaryChangeListener {
        public void onHomeworkPickerStatusChange();
    }

    public static DiaryManager getInstance(Context context) {
        if (instance == null) {
            instance = new DiaryManager(context);
        }
        return instance;
    }

    public enum DIARY_STATE {NORMAL, COMPOSE_HOMEWORK}

    private DIARY_STATE state;
    private LocationManager locationManager;
    private Context context;
    private DiaryEntry currentEntry;

    private ArrayList<DiaryChangeListener> listeners;

    private DiaryManager(Context context) {
        this.context = context;
        locationManager = LocationManager.getInstance(context);
        listeners = new ArrayList<DiaryManager.DiaryChangeListener>();
        state = DIARY_STATE.NORMAL;
    }

    public void addListener(DiaryChangeListener listener) {
        if (!this.listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeListener(DiaryChangeListener listener) {
        listeners.remove(listener);
    }

    public boolean isPickingHomework() {
        return state == DIARY_STATE.COMPOSE_HOMEWORK;
    }

    public DiaryEntry getCurrentEntry() {
        return currentEntry;
    }

    public void clearCurrentEntry() {
        this.currentEntry = null;
        for (DiaryChangeListener l : listeners)
            l.onHomeworkPickerStatusChange();
    }

    public void startNewEntry(ENTRY_TYPE entryType) {
        currentEntry = new DiaryEntry(entryType);
        for (DiaryChangeListener l : listeners)
            l.onHomeworkPickerStatusChange();
    }

    public void addResourceToHomework() {
        // TODO Auto-generated method stub
        Log.d(TAG, "will add: " + locationManager.getBreadcrumb());
        if (locationManager.getLocationType() == LOCATION_TYPE.ASSESSMENT || locationManager.getLocationType() == LOCATION_TYPE.TOPIC) {
            DiaryEntry.Resource r = new DiaryEntry.Resource();
            r.locationType = locationManager.getLocationType();
            r.locationSubType = locationManager.getLocationSubType();
            r.uri = locationManager.getLocationRef().getUri().toString();
            r.breadcrumb = locationManager.getBreadcrumb();

            currentEntry.resources.add(r);
        }
    }

    public static class DiaryEntry {
        public ENTRY_TYPE entryType;
        public String classId;
        public String message;
        public ArrayList<Resource> resources;

        public DiaryEntry(ENTRY_TYPE type) {
            entryType = type;
            resources = new ArrayList<DiaryManager.DiaryEntry.Resource>();
        }

        public static class Resource {
            public LocationManager.LOCATION_TYPE locationType;
            public LocationManager.LOCATION_SUBTYPE locationSubType;
            public String uri;
            public LocationManager.Breadcrumb breadcrumb;

            public Command unlockCommand;
        }

        public enum ENTRY_TYPE {
            HOMEWORK, ANNOUNCEMENT
        }
    }
}