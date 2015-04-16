package co.in.divi.diary;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

import co.in.divi.Location;
import co.in.divi.LocationManager;
import co.in.divi.UserSessionProvider;
import co.in.divi.model.UserData;

public class DiaryManager {

    private static final String TAG = DiaryManager.class.getSimpleName();

    private static DiaryManager instance = null;


    public static interface DiaryListener {
        //        public void onHomeworkPickerStatusChange();
        public void onDiaryStateChange();
    }

    public static DiaryManager getInstance(Context context) {
        if (instance == null) {
            instance = new DiaryManager(context);
        }
        return instance;
    }

    public enum DIARY_STATE {NORMAL, COMPOSE, PICKING_HOMEWORK}

    private DIARY_STATE state;
    private LocationManager locationManager;
    private Context context;
    private DiaryEntry currentEntry;

    private ArrayList<DiaryListener> listeners;

    private DiaryManager(Context context) {
        this.context = context;
        locationManager = LocationManager.getInstance(context);
        listeners = new ArrayList<DiaryListener>();
        state = DIARY_STATE.NORMAL;
    }

    public void addListener(DiaryListener listener) {
        if (!this.listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeListener(DiaryListener listener) {
        listeners.remove(listener);
    }

    public boolean isPickingHomework() {
        return state == DIARY_STATE.PICKING_HOMEWORK;
    }

    public boolean isComposing() {
        return state != DIARY_STATE.NORMAL;
    }

    public DiaryEntry getCurrentEntry() {
        return currentEntry;
    }

    public void pingListeners() {
        if (isComposing()) {
            callListeners();
        }
    }

    public void clearCurrentEntry() {
        this.currentEntry = null;
        state = DIARY_STATE.NORMAL;
        callListeners();
    }

    public void startNewEntry(DiaryEntry.ENTRY_TYPE entryType) {
        UserData userData = UserSessionProvider.getInstance(context).getUserData();
        if(userData==null) {
            Log.w(TAG,"user logged out, cannot create diary entry");
            return;
        }
        currentEntry = new DiaryEntry(entryType, userData.uid, userData.name);
        state = DIARY_STATE.COMPOSE;
        callListeners();
    }

    public void startPicking() {
        state = DIARY_STATE.PICKING_HOMEWORK;
        Intent homeworkPickerService = new Intent(context, HomeworkPickerUIService.class);
        context.startService(homeworkPickerService);
        callListeners();
    }

    public void finishPicking() {
        state = DIARY_STATE.COMPOSE;
        callListeners();
    }

    public void addResourceToHomework() {
        Location loc = locationManager.getLocation();
        Log.d(TAG, "will add: " + loc.getBreadcrumb());
        if (loc.getLocationType() == Location.LOCATION_TYPE.ASSESSMENT || loc.getLocationType() == Location.LOCATION_TYPE.TOPIC) {
            DiaryEntry.Resource r = new DiaryEntry.Resource();
            r.locationType = loc.getLocationType();
            r.locationSubType = loc.getLocationSubType();
            r.uri = loc.getLocationRef().getUri().toString();
            r.breadcrumb = loc.getBreadcrumb();
            currentEntry.resources.add(r);

            callListeners();
        }
    }

    private void callListeners() {
        for (DiaryListener l : listeners)
            l.onDiaryStateChange();
    }
}