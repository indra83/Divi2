package co.in.divi;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import co.in.divi.content.DiviReference;
import co.in.divi.logs.DiviLog;
import co.in.divi.logs.LogsRecorderService;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Util;

public class LocationManager {
	private static final String	TAG							= LocationManager.class.getSimpleName();

	public static final int		LOGS_QUEUE_SIZE				= 12;

	// timestamp of last logs save
	private long				lastLogsRecordedTimestamp	= 0;

	// OFF - screen off or locked
	public static enum LOCATION_TYPE {
		TOPIC, ASSESSMENT, HOME, BLACKOUT, OFF, UNKNOWN
	}

	public static enum LOCATION_SUBTYPE {
		TOPIC_TOPIC, TOPIC_VIDEO, TOPIC_AUDIO, TOPIC_IMAGE, TOPIC_IMAGESET, TOPIC_VM, ASSESSMENT_QUIZ, ASSESSMENT_EXERCISE, ASSESSMENT_TEST,
	}

	public interface DiviLocationChangeListener {
		public void onLocationChange(DiviReference newRef, Breadcrumb breadcrumb);
	}

	private static LocationManager	instance	= null;

	public static LocationManager getInstance(Context context) {
		if (instance == null) {
			instance = new LocationManager(context);
		}
		return instance;
	}

	public void addListener(DiviLocationChangeListener listener) {
		if (!this.listeners.contains(listener))
			listeners.add(listener);
	}

	public void removeListener(DiviLocationChangeListener listener) {
		listeners.remove(listener);
	}

	public boolean hasLocation() {
		return curLocation != null;
	}

	public DiviReference getLocationRef() {
		return curLocation;
	}

	public boolean isLocationLocked() {
		return unlockData != null;
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

	public void setNewLocation(LOCATION_TYPE type, LOCATION_SUBTYPE subType, DiviReference newRef, Breadcrumb breadcrumb,
			ProtectedResourceMetadata unlockData) {
		if (curLocation != null && newRef != null && curLocation.equals(newRef))
			return;
		if (LogConfig.DEBUG_LOCATION) {
			Log.d(TAG, "type:" + type);
			// Log.d(TAG, "setting new location:" + type + ", " + subType + ", " + newRef + ", " + breadcrumb);
			if (newRef != null)
				Log.d(TAG, newRef.getUri().toString());
		}
		this.curLocation = newRef;
		this.breadcrumb = breadcrumb;
		this.type = type;
		this.subType = subType;
		this.unlockData = unlockData;
		notifyListeners();

		// logging
		// 1. update duration and close prev log
		if (curLog != null) {
			curLog.updateDuration();
			logs.add(curLog);
			curLog = null;
		}
		// 2. start new log
		if (type == LOCATION_TYPE.ASSESSMENT || type == LOCATION_TYPE.TOPIC) {
			curLog = new DiviLog(userSessionProvider.getUserData().uid, userSessionProvider.getUserData().token, curLocation.getUri()
					.toString(), DiviLog.LOG_TYPE_TIMESPENT, subType);
		}
		// 3. Check if we have enough logs & insert into DB
		if (logs.size() > LOGS_QUEUE_SIZE || Util.getTimestampMillis() - lastLogsRecordedTimestamp > 30 * 1000) {
			lastLogsRecordedTimestamp = Util.getTimestampMillis();
			Intent recordLogsIntent = new Intent(context, LogsRecorderService.class);
			recordLogsIntent.putParcelableArrayListExtra(LogsRecorderService.INTENT_EXTRA_LOGS, logs);
			context.startService(recordLogsIntent);
			logs.clear();
		}
	}

	private Context									context;
	private UserSessionProvider						userSessionProvider;
	private ArrayList<DiviLocationChangeListener>	listeners;
	private DiviReference							curLocation;
	private ProtectedResourceMetadata				unlockData;
	private LOCATION_TYPE							type;
	private LOCATION_SUBTYPE						subType;
	private Breadcrumb								breadcrumb;
	private BroadcastReceiver						mPowerKeyReceiver	= null;

	// for logging
	private ArrayList<DiviLog>						logs;
	private DiviLog									curLog;

	private LocationManager(Context context) {
		this.context = context;
		userSessionProvider = UserSessionProvider.getInstance(context);
		listeners = new ArrayList<LocationManager.DiviLocationChangeListener>();
		logs = new ArrayList<DiviLog>();
		curLog = null;
		// register for screen off
		final IntentFilter theFilter = new IntentFilter();
		/** System Defined Broadcast */
		theFilter.addAction(Intent.ACTION_SCREEN_ON);
		theFilter.addAction(Intent.ACTION_SCREEN_OFF);

		mPowerKeyReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String strAction = intent.getAction();

				if (strAction.equals(Intent.ACTION_SCREEN_OFF)) {
					setNewLocation(LOCATION_TYPE.OFF, null, null, null, null);
				} else if (strAction.equals(Intent.ACTION_SCREEN_ON) && getLocationType() == LOCATION_TYPE.OFF) {
					setNewLocation(LOCATION_TYPE.UNKNOWN, null, null, null, null);
				}
			}
		};

		context.getApplicationContext().registerReceiver(mPowerKeyReceiver, theFilter);
	}

	private void notifyListeners() {
		for (DiviLocationChangeListener listener : listeners) {
			listener.onLocationChange(curLocation, breadcrumb);
		}
	}

	public static class Breadcrumb {
		public String	courseName, bookName, chapterName, itemName, subItemName;

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
			return new String[] { courseName, bookName, chapterName, itemName, subItemName };
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
		public String	name;
		public int		itemType;	// refers to command itemType
		public long		duration;
		public String	data;

		public ProtectedResourceMetadata(String name, int itemType, long duration, String data) {
			this.name = name;
			this.itemType = itemType;
			this.duration = duration;
			this.data = data;
		}
	}
}
