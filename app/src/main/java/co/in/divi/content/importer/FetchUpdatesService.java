package co.in.divi.content.importer;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import co.in.divi.ContentUpdateManager;
import co.in.divi.DiviApplication;
import co.in.divi.DiviService;
import co.in.divi.UserSessionProvider;
import co.in.divi.background.UniversalSyncCheckReceiver;
import co.in.divi.content.Book;
import co.in.divi.content.DatabaseHelper;
import co.in.divi.model.ContentUpdates;
import co.in.divi.model.ContentUpdates.Update;
import co.in.divi.model.UserData.Metadata;
import co.in.divi.model.UserData;
import co.in.divi.util.LogConfig;
import co.in.divi.util.ServerConfig;

import com.android.volley.Request.Method;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.google.gson.Gson;

public class FetchUpdatesService extends DiviService {

	static final String		TAG				= FetchUpdatesService.class.getSimpleName();

	Handler					handler;
	ContentUpdateManager	contentManager;
	UserSessionProvider		userSessionProvider;
	DatabaseHelper			dbHelper;

	FetchUpdateThread		workerThread	= null;

	@Override
	public void onCreate() {
		super.onCreate();
		if (LogConfig.DEBUG_CONTENT_IMPORT)
			Log.d(TAG, "service created");
		dbHelper = DatabaseHelper.getInstance(this);
		contentManager = ContentUpdateManager.getInstance(this);
		userSessionProvider = UserSessionProvider.getInstance(this);
		handler = new Handler(Looper.getMainLooper());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// ensure login
		if (!userSessionProvider.isLoggedIn()) {
			stopSelf();
		} else {
			// cancel any existing requests and start
			if (workerThread != null) {
				workerThread.interrupt();
			}
			acquireLocks();
			DiviApplication.get().getRequestQueue().cancelAll(this);
			workerThread = new FetchUpdateThread();
			workerThread.start();
		}
		UniversalSyncCheckReceiver.completeWakefulIntent(intent);
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (LogConfig.DEBUG_CONTENT_IMPORT)
			Log.d(TAG, "onDestroy");
		DiviApplication.get().getRequestQueue().cancelAll(this);
		releaseLocks();
	}

	private class FetchUpdateThread extends Thread {

		HashMap<String, Integer>	bookVersions	= new HashMap<String, Integer>();
		JSONObject					jsonRequest;

		public FetchUpdateThread() {
			try {
				JSONArray versionsArray = new JSONArray();
				for (String courseId : userSessionProvider.getAllCourseIds()) {
					ArrayList<Book> books = dbHelper.getBooks(courseId);
					for (Book book : books) {
						JSONObject bookObject = new JSONObject();
						bookObject.put("courseId", courseId);
						bookObject.put("bookId", book.id);
						bookObject.put("version", book.version);
						versionsArray.put(bookObject);
						bookVersions.put(courseId + book.id, book.version); // hack
					}
				}
				jsonRequest = new JSONObject();
				jsonRequest.put("uid", userSessionProvider.getUserData().uid);
				jsonRequest.put("token", userSessionProvider.getUserData().token);

				JSONObject tabObject = new JSONObject();
				DiviApplication app = (DiviApplication) getApplication();
				tabObject.put("device_id", app.deviceId());
				tabObject.put("device_tag", app.getDeviceTag());
				JSONObject versionsObject = new JSONObject();
				versionsObject.put("versions", versionsArray);
				tabObject.put("content", versionsObject);
				jsonRequest.put("tablet", tabObject);
			} catch (Exception e) {
				Log.e(TAG, "Error fetching content updates", e);
				contentManager.cancelUpdate();
				stopSelf();
			}
		}

		@Override
		public void run() {
			try {
				String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_GETCONTENTUPDATES;
				if (LogConfig.DEBUG_CONTENT_IMPORT)
					Log.d(TAG, "posting:   " + jsonRequest.toString());

				RequestFuture<JSONObject> future = RequestFuture.newFuture();
				JsonObjectRequest fetchUpdatesRequest = new JsonObjectRequest(Method.POST, url, jsonRequest, future, future);
				fetchUpdatesRequest.setShouldCache(false);
				DiviApplication.get().getRequestQueue().add(fetchUpdatesRequest).setTag(this);

				JSONObject response = future.get();
				if (LogConfig.DEBUG_CONTENT_IMPORT)
					Log.d(TAG, "got response:\n" + response.toString());
				UserData.Metadata metadata = userSessionProvider.getUserData().getMetadata();
				ContentUpdates contentUpdates = new Gson().fromJson(response.toString(), ContentUpdates.class);
				for (Update update : contentUpdates.updates) {
					String bookKey = update.courseId + update.bookId;
					if (bookVersions.containsKey(bookKey) && update.bookVersion <= bookVersions.get(bookKey)) {
						update.isApplicable = false;
					} else {
						update.isApplicable = true;
						if (metadata != null && metadata.skipBooks != null) {
							for (Metadata.SkipBook skipBook : metadata.skipBooks) {
								if (LogConfig.DEBUG_CONTENT_IMPORT)
									Log.d(TAG, "skip book:" + skipBook.courseId + "::" + skipBook.bookId);
								if (update.courseId.equals(skipBook.courseId) && update.bookId.equals(skipBook.bookId)) {
									update.isApplicable = false;
									continue;
								}
							}
						}
					}
				}
				final ContentUpdates cu = contentUpdates;
				handler.post(new Runnable() {
					@Override
					public void run() {
						contentManager.setContentUpdates(cu);
					}
				});
			} catch (Exception e) {
				Log.e(TAG, "Error fetching content updates", e);
				handler.post(new Runnable() {
					@Override
					public void run() {
						contentManager.cancelUpdate();
					}
				});
			} finally {
				releaseLocks();
				stopSelf();
			}
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
}
