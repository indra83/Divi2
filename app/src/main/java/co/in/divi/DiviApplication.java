package co.in.divi;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.LruBitmapCache;
import com.android.volley.toolbox.Volley;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import co.in.divi.background.UniversalSyncCheckReceiver;
import co.in.divi.util.Config;

@ReportsCrashes(formKey = "", // This is required for backward compatibility but not used
formUri = "https://collector.tracepot.com/9e7647a9")
public class DiviApplication extends Application {
	private static final String		TAG					= DiviApplication.class.getSimpleName();

	private static final String		PREFS_FILE			= "DIVI_PREFS";
	private static final String		PREFS_TIME_DIFF		= "time_diff";
	private static final String		PREFS_DEVICE_ID		= "device_id";
	private static final String		PREFS_DEVICE_TAG	= "PREFS_DEVICE_TAG";

	private static DiviApplication	instance;

	// Must be Application Level (not user level)
	private Long					timeDiff			= null;
	private UUID					uuid				= null;
	private String					tag;
	private File					booksBase			= null;
	private File					tempBase			= null;
	private File					downloadsBase		= null;
	private File					reportsBase			= null;

	private RequestQueue			requestQueue;
	private RequestQueue			syncQueue;
	private ImageLoader				imageLoader;

	public static DiviApplication get() {
		return instance;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// error logging (if NOT playstore app)
        if(!Config.IS_PLAYSTORE_APP)
            ACRA.init(this);

		instance = this;
		requestQueue = Volley.newRequestQueue(this);
		syncQueue = Volley.newRequestQueue(this);
		imageLoader = new ImageLoader(requestQueue, new LruBitmapCache(8 * 1024 * 1024));

		// schedule background tasks (alarms)
		UniversalSyncCheckReceiver.scheduleAlarms(this);
	}

	public long getTimeDiff() {
		if (timeDiff == null) {
			final SharedPreferences prefs = getSharedPreferences(PREFS_FILE, 0);
			timeDiff = prefs.getLong(PREFS_TIME_DIFF, 0);
		}
		return timeDiff;
	}

	public void setTimeDiff(long timeDiff) {
		this.timeDiff = timeDiff;
		final SharedPreferences prefs = getSharedPreferences(PREFS_FILE, 0);
		prefs.edit().putLong(PREFS_TIME_DIFF, timeDiff).apply();
	}

	public RequestQueue getRequestQueue() {
		return requestQueue;
	}

	public RequestQueue getSyncQueue() {
		return syncQueue;
	}

	public ImageLoader getImageLoader() {
		return imageLoader;
	}

	public String deviceId() {
		if (uuid == null) {
			final SharedPreferences prefs = getSharedPreferences(PREFS_FILE, 0);
			final String id = prefs.getString(PREFS_DEVICE_ID, null);

			if (id != null) {
				uuid = UUID.fromString(id);
			} else {
				uuid = UUID.randomUUID();
				prefs.edit().putString(PREFS_DEVICE_ID, uuid.toString()).apply();
			}
		}
		return uuid.toString();
	}

	public String getDeviceTag() {
		if (tag == null) {
			final SharedPreferences prefs = getSharedPreferences(PREFS_FILE, 0);
			tag = prefs.getString(PREFS_DEVICE_TAG, "n/a");
		}
		return tag;
	}

	public void setDeviceTag(String tag) {
		this.tag = tag;
		final SharedPreferences prefs = getSharedPreferences(PREFS_FILE, 0);
		prefs.edit().putString(PREFS_DEVICE_TAG, tag).commit();
	}

	public File getBooksBaseDir(String courseId) {
		if (this.booksBase == null) {
			File bookBase = new File(getBaseDir(), Config.BOOKS_LOCATION);
//            File bookBase = getExternalFilesDir(null);
			if (!bookBase.exists()) {
				bookBase.mkdirs();
				try {
					new File(bookBase, ".nomedia").createNewFile();
				} catch (IOException e) {
					Log.w(TAG, "failed creating .nomedia at" + bookBase.toString(), e);
				}
			}
			this.booksBase = bookBase;
		}
        if(courseId!=null)
		    return new File(this.booksBase, courseId);
        else
            return this.booksBase;// If the user doesn't have any courses...
	}

	public File getTempDir() {
		if (this.tempBase == null || !this.tempBase.exists()) {
			File tempBase = new File(getBaseDir(), Config.TEMP_LOCATION);
			if (!tempBase.exists()) {
				tempBase.mkdirs();
				try {
					new File(tempBase, ".nomedia").createNewFile();
				} catch (IOException e) {
					Log.w(TAG, "failed creating .nomedia at" + tempBase.toString(), e);
				}
			}
			this.tempBase = tempBase;
		}
		return tempBase;
	}

	public File getDownloadsDir() {
		if (this.downloadsBase == null || !this.downloadsBase.exists()) {
			File downloadsBase = new File(Environment.getExternalStorageDirectory(), Config.TEMP_LOCATION);
			if (!downloadsBase.exists()) {
				downloadsBase.mkdirs();
				try {
					new File(downloadsBase, ".nomedia").createNewFile();
				} catch (IOException e) {
					Log.w(TAG, "failed creating .nomedia at" + downloadsBase.toString(), e);
				}
			}
			this.downloadsBase = downloadsBase;
		}
		return downloadsBase;
	}

	public File getReportsDir() {
		if (this.reportsBase == null || !this.reportsBase.exists()) {
			File reportsBase = new File(Environment.getExternalStorageDirectory(), Config.PROGRESS_REPORTS_LOCATION);
			if (!reportsBase.exists()) {
				reportsBase.mkdirs();
				try {
					new File(reportsBase, ".nomedia").createNewFile();
				} catch (IOException e) {
					Log.w(TAG, "failed creating .nomedia at" + downloadsBase.toString(), e);
				}
			}
			this.reportsBase = reportsBase;
		}
		return reportsBase;
	}

	private File getBaseDir() {
		if (Config.USE_HARDCODED_LOCATION_MICROMAX) {
			return new File("/mnt/extsd");
		} else {
//			return Environment.getExternalStorageDirectory();
            return getExternalFilesDir(null);
		}
	}
}
