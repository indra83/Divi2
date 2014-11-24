package co.in.divi.db.sync;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import co.in.divi.DiviService;
import co.in.divi.UserSessionProvider;
import co.in.divi.background.UniversalSyncCheckReceiver;
import co.in.divi.db.UserDBContract;
import co.in.divi.db.UserDBContract.Attempts;
import co.in.divi.db.UserDBContract.Commands;
import co.in.divi.db.model.Attempt;
import co.in.divi.db.model.Command;
import co.in.divi.model.SyncDownResponse;
import co.in.divi.util.LogConfig;
import co.in.divi.util.ServerConfig;
import co.in.divi.util.Util;

import com.google.gson.Gson;

public class SyncDownService extends DiviService {
	private static final String	TAG							= SyncDownService.class.getSimpleName();

	public static final String	INTENT_EXTRA_STOP_SYNC		= "INTENT_EXTRA_STOP_SYNC";
	public static final String	INTENT_EXTRA_ONLY_COMMAND	= "INTENT_EXTRA_ONLY_COMMAND";

	private static final int	ITEMS_PER_PAGE				= 100;

	private UserSessionProvider	userSessionProvider;
	private ContentResolver		contentResolver;
	private SyncThread			syncThread;
	private Handler				handler;
	private boolean				syncSuccess;
	private boolean				onlyCommands;
	private String				uid, token;

	@Override
	public void onCreate() {
		super.onCreate();
		if (LogConfig.DEBUG_SYNC)
			Log.d(TAG, "onCreate");
		contentResolver = getContentResolver();
		userSessionProvider = UserSessionProvider.getInstance(this);
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
		onlyCommands = intent.getBooleanExtra(INTENT_EXTRA_ONLY_COMMAND, false);

		if (!Util.isNetworkOn(this) || !userSessionProvider.hasUserData()) {
			Log.i(TAG, "No network, skipping sync...");
			stopSelf();
			return START_REDELIVER_INTENT;
		}

		if (syncThread == null) {
			acquireLocks();
			syncSuccess = false;
			uid = userSessionProvider.getUserData().uid;
			token = userSessionProvider.getUserData().token;
			new SyncThread().start();
		} else {
			// repeat request from activity, ignore?
			Log.w(TAG, "sync already in progress??");
		}
		UniversalSyncCheckReceiver.completeWakefulIntent(intent);
		return START_REDELIVER_INTENT;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (LogConfig.DEBUG_SYNC)
			Log.d(TAG, "finishing");
		// on success, move to loggedin status
		if (!onlyCommands) { // make sure its login sync
			if (syncSuccess) {
				if (LogConfig.DEBUG_SYNC)
					Log.d(TAG, "Sync completed");
				userSessionProvider.setSyncDone();
			} else {
				Log.w(TAG, "Sync failed!");
				Toast.makeText(this, "Sync failed...", Toast.LENGTH_LONG).show();
				userSessionProvider.logout();
			}
		}
		releaseLocks();
	}

	private class SyncThread extends HandlerThread {

		SyncThread() {
			super("Sync Thread");
		}

		@Override
		public void run() {
			try {
				boolean hasMore = fetchItems();
				while (hasMore)
					hasMore = fetchItems();
			} catch (Exception e) {
				Log.e(TAG, "", e);
			} finally {
				releaseLocks();
				stopSelf();
			}
		}

		private boolean fetchItems() throws Exception {
			try {
				// begin sync
				// 1.a get last update time.
				long lastUpdateTime_attempt = 0;
				long lastUpdateTime_command = 0;
				if (!onlyCommands) {
					Cursor c = contentResolver.query(Attempts.CONTENT_URI, Attempts.PROJECTION_BASIC, Attempts.SYNC_STATUS + " = ? AND "
							+ Attempts.UID + " = ? ", new String[] { "" + Attempts.SYNC_COMPLETE, uid }, Attempts.SORT_ORDER_LATEST_FIRST);
					if (c.moveToFirst()) {
						lastUpdateTime_attempt = c.getLong(c.getColumnIndex(Attempts.LAST_UPDATED));
					}
					if (LogConfig.DEBUG_SYNC)
						Log.d(TAG, "last synced time: " + lastUpdateTime_attempt);
				}
				{ // fetch
					Cursor c = contentResolver.query(Commands.CONTENT_URI, Commands.PROJECTION_ALL, Commands.UID + " = ? ",
							new String[] { uid }, Commands.SORT_ORDER_LATEST_FIRST);
					if (c.moveToFirst()) {
						lastUpdateTime_command = c.getLong(c.getColumnIndex(Commands.LAST_UPDATED));
					}
				}
				if (LogConfig.DEBUG_SYNC)
					Log.d(TAG, "last synced time: " + lastUpdateTime_command);
				// 1.b Now get the records to sync from the server
				JSONObject jsonRequest = new JSONObject();

				jsonRequest.put("uid", uid);
				jsonRequest.put("token", token);
				JSONObject lastUpdateTimes = new JSONObject();
				lastUpdateTimes.put("commands", lastUpdateTime_command);
				if (!onlyCommands)
					lastUpdateTimes.put("attempts", lastUpdateTime_attempt);
				jsonRequest.put("lastSyncTime", lastUpdateTimes);
				jsonRequest.put("itemsPerPage", ITEMS_PER_PAGE);

				String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_SYNCDOWN;
				// 1.c insert the records to our db

				// 1.d delete any existing un synced records (??)
				// TODO: is this required? If yes, remove the else clause
				SyncDownResponse response = fetchSyncData(url, jsonRequest.toString());
				if (response == null) {
					sleep(2000);// wait and try again.
					response = fetchSyncData(url, jsonRequest.toString());
				}
				if (response != null) {
					ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
					if (!onlyCommands) {
						for (Attempt item : response.attempts) {
							ContentValues values = new ContentValues();
							values.put(Attempts.UID, uid);
							values.put(Attempts.COURSE_ID, item.courseId);
							values.put(Attempts.BOOK_ID, item.bookId);
							values.put(Attempts.ASSESSMENT_ID, item.assessmentId);
							values.put(Attempts.QUESTION_ID, item.questionId);
							values.put(Attempts.TOTAL_POINTS, item.totalPoints);
							values.put(Attempts.SUBQUESTIONS, item.subquestions);
							values.put(Attempts.CORRECT_ATTEMPTS, item.correctAttempts);
							values.put(Attempts.WRONG_ATTEMPTS, item.wrongAttempts);
							values.put(Attempts.DATA, item.data);
							values.put(Attempts.LAST_UPDATED, item.lastUpdatedAt);
							values.put(Attempts.SOLVED_AT, item.solvedAt);
							values.put(Attempts.SYNC_STATUS, Attempts.SYNC_COMPLETE);
							ops.add(ContentProviderOperation.newInsert(Attempts.CONTENT_URI).withValues(values).build());
						}
						if (ops.size() > 0)
							contentResolver.applyBatch(UserDBContract.AUTHORITY, ops);
					}
					if (response.commands != null) {
						// sync commands
						ops.clear();
						for (Command command : response.commands) {
							// TODO: this shouldn't happen!
							if (command.id == null)
								continue;
							ContentValues values = new ContentValues();
							values.put(Commands.ID, command.id);
							values.put(Commands.UID, uid);
							values.put(Commands.COURSE_ID, command.courseId);
							values.put(Commands.BOOK_ID, command.bookId);
							values.put(Commands.ITEM_ID, command.itemCode);
							values.put(Commands.TEACHER_ID, command.teacherId);
							values.put(Commands.CLASS_ID, command.classRoomId);
							values.put(Commands.TYPE, command.category);
							values.put(Commands.ITEM_TYPE, command.itemCategory);
							values.put(Commands.STATUS, command.status);
							values.put(Commands.DATA, command.data);
							values.put(Commands.CREATE_TIMESTAMP, command.createdAt);
							values.put(Commands.APPLY_TIMESTAMP, command.appliedAt);
							values.put(Commands.END_TIMESTAMP, command.endsAt);
							values.put(Commands.LAST_UPDATED, command.updatedAt);
							ops.add(ContentProviderOperation.newInsert(Commands.CONTENT_URI).withValues(values).build());
						}
						if (ops.size() > 0)
							contentResolver.applyBatch(UserDBContract.AUTHORITY, ops);
					}
					if (response.hasMoreData)
						return true;
					else {
						syncSuccess = true;
						userSessionProvider.setTimestamp(UserSessionProvider.LAST_SYNC_COMMANDS_TIMESTAMP, Util.getTimestampMillis());
						stopSelf();
						return false;
					}
				} else {
					handler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(getBaseContext(), "Error performing sync, terminating login..", Toast.LENGTH_LONG).show();
						}
					});
					stopSelf();
				}
			} catch (Exception e) {
				throw e;
			}
			return false;
		}

	}

	// fetch attempts
	private SyncDownResponse fetchSyncData(String url, String jsonData) throws IOException {
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
				Log.d(TAG, "The response code is: " + response);
			is = conn.getInputStream();

			// ignore returned
			String contentAsString = Util.getInputString(is);
			if (LogConfig.DEBUG_SYNC)
				Log.d(TAG, "The response is: " + contentAsString);
			if (conn.getResponseCode() == 200) {
				return new Gson().fromJson(contentAsString, SyncDownResponse.class);
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

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
