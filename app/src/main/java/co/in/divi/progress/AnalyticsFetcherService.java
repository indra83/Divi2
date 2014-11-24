package co.in.divi.progress;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import co.in.divi.DiviService;
import co.in.divi.UserSessionProvider;
import co.in.divi.background.UniversalSyncCheckReceiver;
import co.in.divi.util.LogConfig;
import co.in.divi.util.ServerConfig;
import co.in.divi.util.Util;
import co.in.divi.util.Week;

import com.google.gson.Gson;

public class AnalyticsFetcherService extends DiviService {
	private static final String	TAG					= AnalyticsFetcherService.class.getSimpleName();

	private static final String	KEEN_API_ENDPOINT	= "https://api.keen.io/3.0/projects/" + ServerConfig.KEEN_PROJECT_ID + "/queries";

	private AnalyticsManager	analyticsManager;
	private boolean				gotNewIntent		= true;
	private SyncThread			syncThread;
	private Handler				handler;

	@Override
	public void onCreate() {
		super.onCreate();
		if (LogConfig.DEBUG_ANALYTICS)
			Log.d(TAG, "onCreate");
		analyticsManager = AnalyticsManager.getInstance(this);
		handler = new Handler();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		gotNewIntent = true;

		if (!Util.isNetworkOn(this) || !UserSessionProvider.getInstance(this).isLoggedIn()) {
			Log.w(TAG, "No network, skipping sync(analytics)...");
			stopSelf();
			return START_REDELIVER_INTENT;
		}

		if (syncThread == null) {
			acquireLocks();
			// TODO: change to get only fresh data
			new SyncThread(analyticsManager.getAllWeeks(), UserSessionProvider.getInstance(this).getUserData().uid).start();
		} else {
			Log.w(TAG, "sync(analytics) already in progress??");
		}
		UniversalSyncCheckReceiver.completeWakefulIntent(intent);
		return START_REDELIVER_INTENT;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		releaseLocks();
	}

	private class SyncThread extends HandlerThread {
		private ArrayList<Week>	weeksToFetch;
		private String			uid;

		SyncThread(ArrayList<Week> weeksToFetch, String uid) {
			super("Sync Thread");
			this.weeksToFetch = weeksToFetch;
			this.uid = uid;
		}

		@Override
		public void run() {
			try {
				// begin sync
				while (gotNewIntent) {
					gotNewIntent = false;

					for (int i = 0; i < weeksToFetch.size(); i++) {
						Week week = weeksToFetch.get(i);
						WeeklyTimeReport weekReport = new WeeklyTimeReport();
						weekReport.weekBeginTimestamp = week.weekBeginTimestamp;
						if (LogConfig.DEBUG_ANALYTICS)
							Log.d(TAG, "fetching for week - " + week);

						Date[] bounds = week.getBounds();
						JSONObject timeframeObject = new JSONObject();
						timeframeObject.put("start", Week.isoFormat.format(bounds[0]));
						timeframeObject.put("end", Week.isoFormat.format(bounds[1]));

						JSONArray filtersArray = new JSONArray();
						JSONObject filterObject = new JSONObject();
						filterObject.put("property_name", "user.uid");
						filterObject.put("operator", "eq");
						filterObject.put("property_value", uid);
						filtersArray.put(filterObject);
						// 1. fetch time spent per book
						Uri.Builder uriBuilder = Uri.parse(KEEN_API_ENDPOINT).buildUpon();
						uriBuilder.appendPath("sum");
						uriBuilder.appendQueryParameter("api_key", ServerConfig.KEEN_READ_KEY);
						uriBuilder.appendQueryParameter("event_collection", ServerConfig.KEEN_EVENT_COLLECTION);
						uriBuilder.appendQueryParameter("filters", filtersArray.toString());
						uriBuilder.appendQueryParameter("timeframe", timeframeObject.toString());
						uriBuilder.appendQueryParameter("target_property", "duration");
						uriBuilder.appendQueryParameter("group_by", "resource.bookId");

						boolean queryOneSuccess = false;
						JSONObject response1 = getQueryResult(uriBuilder.build().toString());
						if (response1 != null && response1.has("result")) {
							queryOneSuccess = true;
						} else {// try again
							response1 = getQueryResult(uriBuilder.build().toString());
							if (response1 != null && response1.has("result")) {
								queryOneSuccess = false;
							}
						}

						if (!queryOneSuccess) {// break if we fail twice
							Log.w(TAG, "Analytics Fetch Failed!");
							return;
						}

						// 2. fetch stats per day
						uriBuilder = Uri.parse(KEEN_API_ENDPOINT).buildUpon();
						uriBuilder.appendPath("sum");
						uriBuilder.appendQueryParameter("api_key", ServerConfig.KEEN_READ_KEY);
						uriBuilder.appendQueryParameter("event_collection", ServerConfig.KEEN_EVENT_COLLECTION);
						uriBuilder.appendQueryParameter("filters", filtersArray.toString());
						uriBuilder.appendQueryParameter("timeframe", timeframeObject.toString());
						uriBuilder.appendQueryParameter("target_property", "duration");
						uriBuilder.appendQueryParameter("group_by", "resource.type");
						uriBuilder.appendQueryParameter("interval", "daily");

						boolean queryTwoSuccess = false;
						JSONObject response2 = getQueryResult(uriBuilder.build().toString());
						if (response2 != null && response2.has("result")) {
							queryTwoSuccess = true;
						} else {// try again
							response2 = getQueryResult(uriBuilder.build().toString());
							if (response2 != null && response2.has("result")) {
								queryTwoSuccess = false;
							}
						}
						if (!queryTwoSuccess) {// break if we fail twice
							Log.w(TAG, "Analytics Fetch Failed!");
							return;
						}

						// 3. Process all data and fill WeekReport
						ArrayList<WeeklyTimeReport.BookTime> bookTimes = new ArrayList<WeeklyTimeReport.BookTime>();
						JSONArray bookTimesArray = response1.getJSONArray("result");
						int totalTime = 0;
						for (int j = 0; j < bookTimesArray.length(); j++) {
							WeeklyTimeReport.BookTime bookTimeEntry = new WeeklyTimeReport.BookTime();
							bookTimeEntry.bookId = bookTimesArray.getJSONObject(j).getString("resource.bookId");
							bookTimeEntry.timeSpent = bookTimesArray.getJSONObject(j).getInt("result");
							totalTime += bookTimeEntry.timeSpent;
							bookTimes.add(bookTimeEntry);
						}
						weekReport.totalTime = totalTime;
						weekReport.bookTimes = bookTimes.toArray(new WeeklyTimeReport.BookTime[0]);

						ArrayList<WeeklyTimeReport.DayTime> dayTimes = new ArrayList<WeeklyTimeReport.DayTime>();
						JSONArray dayTimesArray = response2.getJSONArray("result");
						for (int j = 0; j < dayTimesArray.length(); j++) {
							JSONObject dayObject = dayTimesArray.getJSONObject(j);
							WeeklyTimeReport.DayTime dayTimeEntry = new WeeklyTimeReport.DayTime();
							Calendar c = Util.iso8601ToCalendar(dayObject.getJSONObject("timeframe").getString("start"));
							dayTimeEntry.dayOfWeek = c.get(Calendar.DAY_OF_WEEK);

							for (int k = 0; k < dayObject.getJSONArray("value").length(); k++) {
								String resType = dayObject.getJSONArray("value").getJSONObject(k).getString("resource.type");
								int time = dayObject.getJSONArray("value").getJSONObject(k).getInt("result");
								if (resType.equalsIgnoreCase("TOPIC_VIDEO")) {
									dayTimeEntry.videoTime += time;
								} else if (resType.startsWith("TOPIC")) {
									dayTimeEntry.learnTime += time;
								} else if (resType.startsWith("ASSESSMENT")) {
									dayTimeEntry.assessmentTime += time;
								}
							}
							dayTimes.add(dayTimeEntry);
						}
						weekReport.dayTimes = dayTimes.toArray(new WeeklyTimeReport.DayTime[0]);

						// 4. Save WeekReport to file system
						File reportFile = analyticsManager.getWeekReportFile(uid, week);
						String reportJson = new Gson().toJson(weekReport);
						if (LogConfig.DEBUG_ANALYTICS)
							Log.d(TAG, "saving" + reportJson + "       to file:" + reportFile.getCanonicalPath());
						writeToFile(reportFile, reportJson);
					}

					if (LogConfig.DEBUG_ANALYTICS)
						Log.d(TAG, "Analytics Fetch completed successfully");
					handler.post(new Runnable() {
						@Override
						public void run() {
							AnalyticsManager.getInstance(AnalyticsFetcherService.this).setLastSyncTime(Util.getTimestampMillis());
						}
					});
				}

			} catch (Exception e) {
				Log.e(TAG, "", e);
			} finally {
				releaseLocks();
				stopSelf();
				handler.post(new Runnable() {
					@Override
					public void run() {
						// syncManager.setSyncStatus(SyncStatus.NONE);
					}
				});
			}
		}
	}

	private JSONObject getQueryResult(String url) throws IOException {
		if (LogConfig.DEBUG_ANALYTICS)
			Log.d(TAG, "getting:" + url);
		InputStream is = null;
		try {
			URL getURL = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) getURL.openConnection();
			conn.setReadTimeout(5000 /* milliseconds */);
			conn.setConnectTimeout(5000 /* milliseconds */);
			conn.setRequestMethod("GET");
			// request header
			conn.setRequestProperty("Accept", "application/json");

			// read response
			int response = conn.getResponseCode();
			if (LogConfig.DEBUG_ANALYTICS)
				Log.d(TAG, "The response is: " + response);
			is = conn.getInputStream();

			// process response
			String resp = Util.getInputString(is);
			if (LogConfig.DEBUG_ANALYTICS)
				Log.d(TAG, "got resp:" + resp);
			if (conn.getResponseCode() == 200) {
				return new JSONObject(resp);
			} else {
				Log.w(TAG, "fetch failed with code:" + conn.getResponseCode());
				return null;
			}
		} catch (Exception e) {
			Log.e(TAG, "got error posting:", e);
			return null;
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	static void writeToFile(File file, String data) throws IOException {
		file.getParentFile().mkdirs();
		FileWriter fw = new FileWriter(file);
		fw.write(data);
		fw.close();
	}

	static String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
		Reader reader = null;
		reader = new InputStreamReader(stream, "UTF-8");
		char[] buffer = new char[len];
		reader.read(buffer);
		return new String(buffer);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
