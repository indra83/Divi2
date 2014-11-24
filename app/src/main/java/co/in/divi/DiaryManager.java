package co.in.divi;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import co.in.divi.DiaryManager.DiaryEntry.ENTRY_TYPE;
import co.in.divi.LocationManager.LOCATION_TYPE;
import co.in.divi.db.model.Command;
import co.in.divi.logs.LogsManager;
import co.in.divi.util.DiviCalendar;
import co.in.divi.util.Util;
import co.in.divi.util.Week;

public class DiaryManager {

	private static final String	TAG			= DiaryManager.class.getSimpleName();

	private static DiaryManager	instance	= null;

	public static interface DiaryChangeListener {
		public void onHomeworkPickerStatusChange();
	}

	public static DiaryManager getInstance(Context context) {
		if (instance == null) {
			instance = new DiaryManager(context);
		}
		return instance;
	}

	private LocationManager					locationManager;
	private Context							context;
	private DiaryEntry						currentEntry;

	private ArrayList<DiaryChangeListener>	listeners;

	private DiaryManager(Context context) {
		this.context = context;
		locationManager = LocationManager.getInstance(context);
		listeners = new ArrayList<DiaryManager.DiaryChangeListener>();
	}

	public void addListener(DiaryChangeListener listener) {
		if (!this.listeners.contains(listener))
			listeners.add(listener);
	}

	public void removeListener(DiaryChangeListener listener) {
		listeners.remove(listener);
	}

	// get weeks from start week to curWeek/lastSyncWeek
	public ArrayList<Week> getAllWeeks() {
		ArrayList<Week> ret = new ArrayList<Week>();
		DiviCalendar syncCal = DiviCalendar.get();
		syncCal.setTimeInMillis(Math.min(Util.getTimestampMillis(), LogsManager.getInstance(context).getLastSyncTime()));
		Week syncWeek = Week.getWeek(syncCal);
		Week w = getRecordsStartWeek();
		Log.d(TAG, "start:" + w);
		Log.d(TAG, "end:" + syncWeek);
		while (w.weekBeginTimestamp <= syncWeek.weekBeginTimestamp) {
			ret.add(w);
			w = w.nextWeek();
		}
		return ret;
	}

	public boolean isPickingHomework() {
		return getCurrentEntry() != null && getCurrentEntry().entryType == ENTRY_TYPE.HOMEWORK;
	}

	public DiaryEntry getCurrentEntry() {
		return currentEntry;
	}

	public void saveCurrentEntry(DiaryEntry entry) {
		this.currentEntry = entry;
	}

	public void clearCurrentEntry() {
		this.currentEntry = null;
		for (DiaryChangeListener l : listeners)
			l.onHomeworkPickerStatusChange();
	}

	public void startNewEntry() {
		currentEntry = new DiaryEntry();
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

	private Week getRecordsStartWeek() {
		long startTimestamp = Math.max(1403905216000l, UserSessionProvider.getInstance(context).getUserData().reportStartsAt);
		DiviCalendar cal = DiviCalendar.get();
		cal.setTimeInMillis(startTimestamp);
		return Week.getWeek(cal);
	}

	public static class DiaryEntry {
		public ENTRY_TYPE			entryType;
		public ArrayList<String>	recipientIds;
		public RECIPIENT_TYPE		recipientType;

		public String				message;

		public ArrayList<Resource>	resources;

		public DiaryEntry() {
			recipientType = RECIPIENT_TYPE.CLASS;
			entryType = ENTRY_TYPE.HOMEWORK;
			resources = new ArrayList<DiaryManager.DiaryEntry.Resource>();
		}

		public static class Resource {
			public LocationManager.LOCATION_TYPE	locationType;
			public LocationManager.LOCATION_SUBTYPE	locationSubType;
			public String							uri;
			public LocationManager.Breadcrumb		breadcrumb;

			public Command							unlockCommand;
		}

		public enum ENTRY_TYPE {
			HOMEWORK, ANNOUNCEMENT
		}

		public enum RECIPIENT_TYPE {
			CLASS, STUDENT
		}
	}
}