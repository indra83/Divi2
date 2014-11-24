package co.in.divi.background;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.BatteryManager;
import android.util.Log;
import co.in.divi.DiviApplication;
import co.in.divi.DiviService;
import co.in.divi.UserSessionProvider;
import co.in.divi.content.Book;
import co.in.divi.content.DatabaseHelper;
import co.in.divi.util.LogConfig;
import co.in.divi.util.ServerConfig;
import co.in.divi.util.Util;

import com.android.volley.Request.Method;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;

public class HeartbeatService extends DiviService {
	private static final String	TAG	= HeartbeatService.class.getSimpleName();

	private UserSessionProvider	userSessionProvider;
	private DatabaseHelper		dbHelper;

	private DevicePolicyManager	mDPM;
	private ComponentName		mDeviceAdmin;

	private PostHeartbeatThread	workerThread;

	@Override
	public void onCreate() {
		super.onCreate();
		if (LogConfig.DEBUG_SYNC)
			Log.d(TAG, "begin heartbeat");
		userSessionProvider = UserSessionProvider.getInstance(this);
		dbHelper = DatabaseHelper.getInstance(this);

		mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mDeviceAdmin = new ComponentName("co.in.divi.launcher", "co.in.divi.launcher.DiviDeviceAdmin");
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
			workerThread = new PostHeartbeatThread();
			workerThread.start();
		}
		UniversalSyncCheckReceiver.completeWakefulIntent(intent);
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (LogConfig.DEBUG_SYNC)
			Log.d(TAG, "onDestroy");
		DiviApplication.get().getRequestQueue().cancelAll(this);
		releaseLocks();
	}

	private class PostHeartbeatThread extends Thread {

		HashMap<String, Integer>	bookVersions		= new HashMap<String, Integer>();
		JSONObject					jsonRequest;

		String						currentHomePackage	= "n/a";
		boolean						isDeviceAdmin;

		public PostHeartbeatThread() {
			try {
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_HOME);
				ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
				currentHomePackage = resolveInfo.activityInfo.packageName;

				isDeviceAdmin = mDPM.isAdminActive(mDeviceAdmin);
			} catch (Exception e) {
				Log.w(TAG, "error checking default app & device admin", e);
			}
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

				// get battery level
				Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
				int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				int percent = (int) (level * 100.0 / scale);

				JSONObject timestampsObject = new JSONObject();
				timestampsObject.put("content", userSessionProvider.getTimestamp(UserSessionProvider.LAST_SYNC_CONTENT_TIMESTAMP));
				timestampsObject.put("attempts", userSessionProvider.getTimestamp(UserSessionProvider.LAST_SYNC_ATTEMTPS_TIMESTAMP));
				timestampsObject.put("commands", userSessionProvider.getTimestamp(UserSessionProvider.LAST_SYNC_COMMANDS_TIMESTAMP));
				timestampsObject.put("logs", userSessionProvider.getTimestamp(UserSessionProvider.LAST_SYNC_LOGS_TIMESTAMP));
				timestampsObject.put("reports", userSessionProvider.getTimestamp(UserSessionProvider.LAST_SYNC_REPORTS_TIMESTAMP));

				JSONObject tabObject = new JSONObject();
				DiviApplication app = (DiviApplication) getApplication();
				tabObject.put("device_id", app.deviceId());
				tabObject.put("device_tag", app.getDeviceTag());
				tabObject.put("batteryLevel", percent);
				JSONObject versionsObject = new JSONObject();
				versionsObject.put("versions", versionsArray);
				versionsObject.put("timestamps", timestampsObject);
				tabObject.put("content", versionsObject);
				jsonRequest.put("tablet", tabObject);
				jsonRequest.put("timestamps", timestampsObject);
				// locking check
				jsonRequest.put("currentHomePackage", currentHomePackage);
				jsonRequest.put("isDeviceAdmin", isDeviceAdmin);
				try {
					PackageInfo pkInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
					jsonRequest.put("appVersionName", pkInfo.versionName);
					jsonRequest.put("appVersionCode", pkInfo.versionCode);
				} catch (NameNotFoundException e) {
					Log.w(TAG, "error getting version name", e);
				}
			} catch (Exception e) {
				Log.e(TAG, "Error fetching content updates", e);
				stopSelf();
			}
		}

		@Override
		public void run() {
			try {
				String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_HEARTBEAT;
				if (LogConfig.DEBUG_SYNC)
					Log.d(TAG, "posting:   " + jsonRequest.toString());

				RequestFuture<JSONObject> future = RequestFuture.newFuture();
				JsonObjectRequest fetchUpdatesRequest = new JsonObjectRequest(Method.POST, url, jsonRequest, future, future);
				fetchUpdatesRequest.setShouldCache(false);
				DiviApplication.get().getRequestQueue().add(fetchUpdatesRequest).setTag(this);

				JSONObject response = future.get();
				if (LogConfig.DEBUG_SYNC)
					Log.d(TAG, "got response:\n" + response.toString());

				UserSessionProvider.getInstance(HeartbeatService.this).setTimestamp(UserSessionProvider.LAST_SYNC_HEARTBEAT_TIMESTAMP,
						Util.getTimestampMillis());

			} catch (Exception e) {
				Log.e(TAG, "Error fetching content updates", e);
			} finally {
				releaseLocks();
				stopSelf();
			}
		}
	}

}
