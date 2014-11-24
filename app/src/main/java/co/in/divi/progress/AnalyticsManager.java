package co.in.divi.progress;

import java.io.File;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import co.in.divi.DiviApplication;
import co.in.divi.UserSessionProvider;
import co.in.divi.logs.LogsManager;
import co.in.divi.util.DiviCalendar;
import co.in.divi.util.Util;
import co.in.divi.util.Week;

/**
 * 1. Keep track of logs sync 2. Keep track of reports sync 3. Other mgmt. tasks
 * 
 * @author indraneel
 * 
 */
public class AnalyticsManager {
	private static final String		TAG			= AnalyticsManager.class.getSimpleName();

	private static AnalyticsManager	instance	= null;

	public static AnalyticsManager getInstance(Context context) {
		if (instance == null) {
			instance = new AnalyticsManager(context);
		}
		return instance;
	}

	private Context				context;
	private SharedPreferences	prefs;

	private AnalyticsManager(Context context) {
		this.context = context;
	}

	public long getLastSyncTime() {
		return UserSessionProvider.getInstance(context).getTimestamp(UserSessionProvider.LAST_SYNC_REPORTS_TIMESTAMP);
	}

	public void setLastSyncTime(long syncTime) {
		UserSessionProvider.getInstance(context).setTimestamp(UserSessionProvider.LAST_SYNC_REPORTS_TIMESTAMP, Util.getTimestampMillis());
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

	public File getWeekReportFile(String uid, Week week) {
		return new File(DiviApplication.get().getReportsDir() + "/" + uid + "/" + week.weekBeginTimestamp + ".json");
	}

	private Week getRecordsStartWeek() {
		long startTimestamp = Math.max(1403905216000l, UserSessionProvider.getInstance(context).getUserData().reportStartsAt);
		DiviCalendar cal = DiviCalendar.get();
		cal.setTimeInMillis(startTimestamp);
		return Week.getWeek(cal);
	}
}
