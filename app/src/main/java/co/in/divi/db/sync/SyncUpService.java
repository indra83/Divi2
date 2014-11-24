package co.in.divi.db.sync;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import co.in.divi.DiviApplication;
import co.in.divi.DiviService;
import co.in.divi.SyncManager;
import co.in.divi.SyncManager.SyncStatus;
import co.in.divi.UserSessionProvider;
import co.in.divi.background.UniversalSyncCheckReceiver;
import co.in.divi.db.UserDBContract;
import co.in.divi.db.UserDBContract.Attempts;
import co.in.divi.util.LogConfig;
import co.in.divi.util.ServerConfig;
import co.in.divi.util.Util;

public class SyncUpService extends DiviService {
	private static final String	TAG							= SyncUpService.class.getSimpleName();
	private static final int	ROWS_TO_SYNC_PER_REQUEST	= 100;

	public static final String	INTENT_EXTRA_STOP_SYNC		= "INTENT_EXTRA_STOP_SYNC";

	private UserSessionProvider	userSessionProvider;
	private SyncManager			syncManager;
	private ContentResolver		contentResolver;
	private boolean				gotNewIntent				= true;								// use this to track
																									// new requests
																									// while the sync is
																									// still
																									// running.
	private SyncThread			syncThread;
	private Handler				handler;

	@Override
	public void onCreate() {
		super.onCreate();
		if (LogConfig.DEBUG_SYNC)
			Log.d(TAG, "onCreate");
		contentResolver = getContentResolver();
		userSessionProvider = UserSessionProvider.getInstance(this);
		syncManager = SyncManager.getInstance(this);
		handler = new Handler();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getBooleanExtra(INTENT_EXTRA_STOP_SYNC, false)) {
			if (syncThread != null) {
				syncThread.interrupt();
			}
			stopSelf();
			return START_NOT_STICKY;
		}
		gotNewIntent = true;

		if (!Util.isNetworkOn(this)) {
			Log.w(TAG, "No network, skipping sync...");
			stopSelf();
			return START_NOT_STICKY;
		}

		if (syncThread == null) {
			acquireLocks();
			new SyncThread().start();
			syncManager.setSyncStatus(SyncStatus.SYNCING);
		} else {
			Log.w(TAG, "sync already in progress??");
		}
		UniversalSyncCheckReceiver.completeWakefulIntent(intent);
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		DiviApplication.get().getSyncQueue().cancelAll(this);
		syncManager.setSyncStatus(SyncStatus.NONE);
		// make sure we release
		releaseLocks();
	}

	private class SyncThread extends HandlerThread {

		SyncThread() {
			super("Sync Thread");
		}

		@Override
		public void run() {
			try {
				// begin sync
				while (gotNewIntent) {
					gotNewIntent = false;

					boolean syncSucceeded = true;
					boolean hasItemsToSync = true;
					while (hasItemsToSync) {
						hasItemsToSync = false;
						// 2.a get attempts to sync
						Cursor c = contentResolver.query(Attempts.CONTENT_URI, Attempts.PROJECTION_ALL, Attempts.UID + " = ? AND "
								+ Attempts.SYNC_STATUS + " = ? ", new String[] { userSessionProvider.getUserData().uid,
								"" + Attempts.SYNC_TO_SYNC }, Attempts.LAST_UPDATED + " LIMIT " + ROWS_TO_SYNC_PER_REQUEST);
						if (LogConfig.DEBUG_SYNC)
							Log.d(TAG, "rows to sync: " + c.getCount());
						final ArrayList<Integer> ids = new ArrayList<Integer>();
						if (c != null && c.getCount() > 0) {
							// 2.b send json to server
							try {
								JSONObject jsonRequest = new JSONObject();

								jsonRequest.put("uid", userSessionProvider.getUserData().uid);
								jsonRequest.put("token", userSessionProvider.getUserData().token);
								jsonRequest.put("attempts", getAttemptsJSON(c, ids));
								String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_SYNCUP;
								
								boolean postSuccess = postSyncData(url, jsonRequest.toString());
								if (!postSuccess) {
									sleep(2000);// wait and try again.
									postSuccess = postSyncData(url, jsonRequest.toString());
								}

								if (postSuccess) {
									// 2.c on success, mark attempts as synced.
									ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
									String WHERE_CLAUSE = Attempts._ID + " = ?";
									for (int id : ids) {
										ops.add(ContentProviderOperation.newUpdate(Attempts.CONTENT_URI)
												.withSelection(WHERE_CLAUSE, new String[] { "" + id })
												.withValue(Attempts.SYNC_STATUS, Attempts.SYNC_COMPLETE).build());
									}
									contentResolver.applyBatch(UserDBContract.AUTHORITY, ops);
									if (c.getCount() >= ROWS_TO_SYNC_PER_REQUEST)
										hasItemsToSync = true;
									else
										hasItemsToSync = false;
								} else {
									Log.w(TAG, "Error posting sync data; quit for now");
									syncSucceeded = false;
									break;
								}
							} catch (Exception e) {
								Log.e(TAG, "Error sending sync", e);
								syncSucceeded = false;
								break;
							}
						}
					}
					if (syncSucceeded) {
						if (LogConfig.DEBUG_SYNC)
							Log.d(TAG, "sync completed successfully");
						handler.post(new Runnable() {
							@Override
							public void run() {
								syncManager.setLastSyncTime(Util.getTimestampMillis());
							}
						});
					}
				}

			} catch (Exception e) {
				Log.e(TAG, "", e);
			} finally {
				handler.post(new Runnable() {
					@Override
					public void run() {
						syncManager.setSyncStatus(SyncStatus.NONE);
					}
				});
				releaseLocks();
				stopSelf();
			}
		}
	}

	private boolean postSyncData(String url, String jsonData) throws IOException {
		if (LogConfig.DEBUG_SYNC)
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
			conn.setRequestProperty("Content-Type", "application/json");

			// send post
			DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
			wr.writeBytes(jsonData);
			wr.flush();
			wr.close();

			// read response
			int response = conn.getResponseCode();
			if (LogConfig.DEBUG_SYNC)
				Log.d(TAG, "The response is: " + response);
			is = conn.getInputStream();

			// ignore returned
			// String contentAsString = readIt(is, len);
			if (conn.getResponseCode() == 200)
				return true;
			else {
				Log.w(TAG, "sync failed with code:" + conn.getResponseCode());
				return false;
			}
		} catch (Exception e) {
			Log.e(TAG, "got error posting:", e);
			return false;
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	static JSONArray getAttemptsJSON(Cursor c, ArrayList<Integer> ids) throws JSONException {
		int col_id = c.getColumnIndex(Attempts._ID);
		int col_courseId = c.getColumnIndex(Attempts.COURSE_ID);
		int col_bookId = c.getColumnIndex(Attempts.BOOK_ID);
		int col_assessmentId = c.getColumnIndex(Attempts.ASSESSMENT_ID);
		int col_questionId = c.getColumnIndex(Attempts.QUESTION_ID);
		int col_total_points = c.getColumnIndex(Attempts.TOTAL_POINTS);
		int col_subquestions = c.getColumnIndex(Attempts.SUBQUESTIONS);
		int col_correct_attempts = c.getColumnIndex(Attempts.CORRECT_ATTEMPTS);
		int col_wrong_attempts = c.getColumnIndex(Attempts.WRONG_ATTEMPTS);
		int col_data = c.getColumnIndex(Attempts.DATA);
		int col_lastUpdated = c.getColumnIndex(Attempts.LAST_UPDATED);
		int col_solvedAt = c.getColumnIndex(Attempts.SOLVED_AT);
		JSONArray arrayObject = new JSONArray();
		boolean hasMoreData = c.moveToFirst();
		while (hasMoreData) {
			JSONObject attemptObject = new JSONObject();
			attemptObject.put("courseId", c.getString(col_courseId));
			attemptObject.put("bookId", c.getString(col_bookId));
			attemptObject.put("assessmentId", c.getString(col_assessmentId));
			attemptObject.put("questionId", c.getString(col_questionId));
			attemptObject.put("totalPoints", c.getString(col_total_points));
			attemptObject.put("subquestions", c.getString(col_subquestions));
			attemptObject.put("correctAttempts", c.getString(col_correct_attempts));
			attemptObject.put("wrongAttempts", c.getString(col_wrong_attempts));
			attemptObject.put("data", c.getString(col_data));
			attemptObject.put("lastUpdatedAt", c.getString(col_lastUpdated));
			attemptObject.put("solvedAt", c.getString(col_solvedAt));
			arrayObject.put(attemptObject);
			ids.add(c.getInt(col_id));
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
	public IBinder onBind(Intent arg0) {
		return null;
	}
}