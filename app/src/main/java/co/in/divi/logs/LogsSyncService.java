package co.in.divi.logs;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import co.in.divi.DiviApplication;
import co.in.divi.DiviService;
import co.in.divi.Location;
import co.in.divi.LocationManager;
import co.in.divi.background.UniversalSyncCheckReceiver;
import co.in.divi.content.DiviReference;
import co.in.divi.logs.LogsDBContract.Logs;
import co.in.divi.util.LogConfig;
import co.in.divi.util.ServerConfig;
import co.in.divi.util.Util;

/**
 * Sync for logs
 *
 * @author indraneel
 *
 */
public class LogsSyncService extends DiviService {
	private static final String	TAG							= LogsSyncService.class.getSimpleName();

	private static final int	ROWS_TO_SYNC_PER_REQUEST	= 50;

	private ContentResolver		contentResolver;

	// use this to track new requests while the sync is still running.
	private boolean				gotNewIntent				= true;

	private SyncThread			syncThread;
	private Handler				handler;

	@Override
	public void onCreate() {
		super.onCreate();
		if (LogConfig.DEBUG_LOGS)
			Log.d(TAG, "onCreate");
		contentResolver = getContentResolver();
		handler = new Handler();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		gotNewIntent = true;

		if (!Util.isNetworkOn(this)) {
			Log.w(TAG, "No network, skipping sync...");
			stopSelf();
			return START_REDELIVER_INTENT;
		}

		if (syncThread == null) {
			acquireLocks();
			new SyncThread(DiviApplication.get().deviceId()).start();
			// syncManager.setSyncStatus(SyncStatus.SYNCING);
		} else {
			Log.w(TAG, "sync already in progress??");
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
		private static final int	MAX_FAIL_COUNT	= 3;

		private String				deviceId;

		private int					failCount;

		SyncThread(String deviceId) {
			super("Sync Thread");
			this.deviceId = deviceId;
			this.failCount = 0;
		}

		@Override
		public void run() {
			try {
				// begin sync
				while (gotNewIntent) {
					gotNewIntent = false;

					boolean syncSucceeded = true;
					boolean hasItemsToSync = true;
					while (hasItemsToSync && failCount < MAX_FAIL_COUNT) {
						hasItemsToSync = false;
						// 2.a get logs to sync
						Cursor c = contentResolver.query(Logs.CONTENT_URI, Logs.PROJECTION_ALL, Logs.SYNC_STATUS + " = ? ",
								new String[] { "" + Logs.SYNC_TO_SYNC }, Logs.SORT_ORDER_DEFAULT + " LIMIT " + ROWS_TO_SYNC_PER_REQUEST);
						if (LogConfig.DEBUG_LOGS)
							Log.d(TAG, "rows to sync: " + c.getCount());
						final ArrayList<Integer> ids = new ArrayList<Integer>();
						if (c != null && c.getCount() > 0) {
							// 2.b send json to server
							try {
								JSONObject jsonRequest = new JSONObject();
								jsonRequest.put(ServerConfig.KEEN_EVENT_COLLECTION, getLogsJSON(c, ids, deviceId));

								ArrayList<Boolean> postSuccess = postSyncData(ServerConfig.KEEN_API_ENDPOINT, jsonRequest.toString());

								if (postSuccess != null && ids.size() == postSuccess.size()) {
									// 2.c on success, mark attempts as synced.
									ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
									String WHERE_CLAUSE = Logs._ID + " = ?";
									for (int i = 0; i < postSuccess.size(); i++) {
										if (postSuccess.get(i)) {
											ops.add(ContentProviderOperation.newDelete(Logs.CONTENT_URI)
													.withSelection(WHERE_CLAUSE, new String[] { "" + ids.get(i) }).build());
										} else {
											// don't delete log if insert failed
											Log.w(TAG, "log sync failed for id - " + ids.get(i));
											failCount++;
										}
									}
									contentResolver.applyBatch(LogsDBContract.AUTHORITY, ops);
									if (c.getCount() >= ROWS_TO_SYNC_PER_REQUEST)
										hasItemsToSync = true;
									else
										hasItemsToSync = false;
								} else {
									Log.w(TAG, "Error posting sync data; quit for now");
									syncSucceeded = false;
									failCount++;
								}
							} catch (Exception e) {
								Log.e(TAG, "Error sending sync", e);
								syncSucceeded = false;
								failCount++;
							}
						}
					}
					if (syncSucceeded && failCount < MAX_FAIL_COUNT) {
						if (LogConfig.DEBUG_LOGS)
							Log.d(TAG, "sync completed successfully");
						handler.post(new Runnable() {
							@Override
							public void run() {
								LogsManager.getInstance(LogsSyncService.this).setLastSyncTime(Util.getTimestampMillis());
							}
						});
					}
				}

			} catch (Exception e) {
				Log.e(TAG, "", e);
			} finally {
				releaseLocks();
				stopSelf();

			}
		}
	}

	private ArrayList<Boolean> postSyncData(String url, String jsonData) throws IOException {
		if (LogConfig.DEBUG_LOGS)
			Log.d(TAG, "posting:" + jsonData);
		InputStream is = null;
		try {
			URL postSyncURL = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) postSyncURL.openConnection();
			conn.setReadTimeout(5000 /* milliseconds */);
			conn.setConnectTimeout(5000 /* milliseconds */);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			// request header
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Content-Type", "application/json");

			// send post
			DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
			wr.writeBytes(jsonData);
			wr.flush();
			wr.close();

			// read response
			int response = conn.getResponseCode();
			if (LogConfig.DEBUG_LOGS)
				Log.d(TAG, "The response is: " + response);
			is = conn.getInputStream();

			// process response
			String resp = Util.getInputString(is);
			if (LogConfig.DEBUG_LOGS)
				Log.d(TAG, "got resp:" + resp);
			if (conn.getResponseCode() == 200) {
				ArrayList<Boolean> insertStatus = new ArrayList<Boolean>();
				JSONObject respObj = new JSONObject(resp);
				JSONArray respArray = respObj.getJSONArray(ServerConfig.KEEN_EVENT_COLLECTION);
				for (int i = 0; i < respArray.length(); i++) {
					insertStatus.add(respArray.getJSONObject(i).getBoolean("success"));
				}
				return insertStatus;
			} else {
				Log.w(TAG, "sync failed with code:" + conn.getResponseCode());
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

	static JSONArray getLogsJSON(Cursor c, ArrayList<Integer> ids, String deviceId) throws JSONException {
		JSONArray arrayObject = new JSONArray();
		boolean hasMoreData = c.moveToFirst();
		while (hasMoreData) {
			DiviLog log = DiviLog.fromCursor(c);
			JSONObject logObject = new JSONObject();
			JSONObject keenProp = new JSONObject();
			JSONObject resourceDetails = new JSONObject();
			JSONObject userDetails = new JSONObject();

			userDetails.put("uid", log.uid);
			userDetails.put("token", log.token);

			TimeZone tz = TimeZone.getTimeZone("UTC");
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"); // SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
			df.setTimeZone(tz);
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(log.openedAt);
			keenProp.put("timestamp", df.format(cal.getTime()));
			// keenProp.put("id", deviceId + "__" + log.id);

            try {
                DiviReference diviRef = new DiviReference(Uri.parse(log.uri));
                resourceDetails.put("courseId", diviRef.courseId);
                resourceDetails.put("bookId", diviRef.bookId);
                resourceDetails.put("itemId", diviRef.itemId);
                resourceDetails.put("subItemId", diviRef.subItemId);
                resourceDetails.put("fragment", diviRef.fragment);
                resourceDetails.put("type", log.resourceType.toString());
            }catch(IllegalArgumentException iae) {
                // app usage logging, ignore.
                resourceDetails.put("type", Location.LOCATION_SUBTYPE.APP);
            }

			logObject.put("user", userDetails);
			logObject.put("resource", resourceDetails);
			logObject.put("keen", keenProp);
			logObject.put("duration", log.duration);
			logObject.put("type", log.type);
			logObject.put("uri", log.uri);

			arrayObject.put(logObject);
			ids.add(log.id);

			hasMoreData = c.moveToNext();
		}
		return arrayObject;
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
