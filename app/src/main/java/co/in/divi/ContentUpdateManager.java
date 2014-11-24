package co.in.divi;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import co.in.divi.UserSessionProvider.UserSessionChangeListener;
import co.in.divi.content.Book;
import co.in.divi.content.DatabaseHelper;
import co.in.divi.content.importer.ContentImportService;
import co.in.divi.content.importer.FetchUpdatesService;
import co.in.divi.model.ContentUpdates;
import co.in.divi.util.Config;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Util;

import com.google.gson.Gson;

public class ContentUpdateManager implements UserSessionChangeListener {
	private static final String					TAG							= ContentUpdateManager.class.getSimpleName();
	private static final String					PREFS_FILE					= "CONTENTUPDATE_PREFS";
	private static final String					PREF_CONTENT_UPDATES		= "CONTENT_UPDATES";
	private static final String					PREF_UPDATE_INDEX			= "UPDATE_INDEX";
	private static final String					PREF_UPDATE_ERRORS			= "PREF_UPDATE_ERRORS";
	private static final String					PREF_CURRENT_DOWNLOAD_ID	= "CURRENT_DOWNLOAD_ID";

	private Context								context;
	private SharedPreferences					prefs;
	private ArrayList<ContentUpdateListener>	listeners;

	private long								updateStartTime;

	// status for UI
	private UpdateStatus						updateStatus;
	private String								updateMessage;

	// current update details
	private ContentUpdates						contentUpdates;
	private int									updateIndex;

	public interface ContentUpdateListener {
		public void onBookUpdating(String bookId);

		public void onContentUIChange();
	}

	private static ContentUpdateManager	instance	= null;

	public static ContentUpdateManager getInstance(Context context) {
		if (instance == null) {
			instance = new ContentUpdateManager(context);
		}
		return instance;
	}

	public static enum UpdateStatus {
		NONE, FETCHING_UPDATES, DOWNLOADING_UPDATE, IMPORTING_UPDATE
	}

	private ContentUpdateManager(Context context) {
		this.context = context;
		this.prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
		listeners = new ArrayList<ContentUpdateListener>();
		updateStatus = UpdateStatus.NONE;
		// init any previous state
		// init data
		if (prefs.contains(PREF_CONTENT_UPDATES)) {
			try {
				String persistedContentUpdates = prefs.getString(PREF_CONTENT_UPDATES, null);
				if (LogConfig.DEBUG_CONTENT_IMPORT)
					Log.d(TAG, "persisted PREF_CONTENT_UPDATES data:" + persistedContentUpdates);
				contentUpdates = new Gson().fromJson(persistedContentUpdates, ContentUpdates.class);
				updateIndex = prefs.getInt(PREF_UPDATE_INDEX, 0);
				if (prefs.contains(PREF_CURRENT_DOWNLOAD_ID)) {
					// download was under progress, check status of it
					long downloadId = prefs.getLong(PREF_CURRENT_DOWNLOAD_ID, -1);
					DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
					Cursor c = dm.query(new DownloadManager.Query().setFilterById(downloadId));
					if (c.moveToFirst()) {
						int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
						if (status == DownloadManager.STATUS_FAILED || status == DownloadManager.STATUS_SUCCESSFUL) {
							removeDownloadId(dm, downloadId);
						} else {
							this.updateStatus = UpdateStatus.DOWNLOADING_UPDATE;
						}
					} else {
						removeDownloadId(dm, downloadId);
					}
					c.close();
				}
			} catch (Exception e) {
				Log.e(TAG, "error parsing login details", e);
				prefs.edit().remove(PREF_CONTENT_UPDATES).remove(PREF_UPDATE_INDEX).apply();
			}
		}
		UserSessionProvider.getInstance(context).addListener(this);
	}

	public UpdateStatus getUpdateStatus() {
		return updateStatus;
	}

	public void addListener(ContentUpdateListener listener) {
		if (!this.listeners.contains(listener))
			listeners.add(listener);
	}

	public void removeListener(ContentUpdateListener listener) {
		listeners.remove(listener);
	}

	public ContentUpdates.Update getCurrentUpdate() {
		if (updateStatus == UpdateStatus.DOWNLOADING_UPDATE || updateStatus == UpdateStatus.IMPORTING_UPDATE) {
			return contentUpdates.updates[updateIndex];
		}
		return null;
	}

	public boolean isUpdateRequired() {
		long timeElapsed = (Util.getTimestampMillis() - getLastUpdateTime());
		return (timeElapsed > Config.INTERVAL_CONTENT_UPDATE) ? true : false;
	}

	public long getLastUpdateTime() {
		return UserSessionProvider.getInstance(context).getTimestamp(UserSessionProvider.LAST_SYNC_CONTENT_TIMESTAMP);
	}

	public long getCurrentDownloadId() {
		return prefs.getLong(PREF_CURRENT_DOWNLOAD_ID, -1);
	}

	public String getStatusString() {
		switch (updateStatus) {
		case NONE:
			SimpleDateFormat sdf = new SimpleDateFormat("dd MMM hh:mm");
			Calendar lastUpdateCal = Calendar.getInstance();
			lastUpdateCal.setTimeInMillis(getLastUpdateTime());
			return "Updated : " + sdf.format(lastUpdateCal.getTime());
		case FETCHING_UPDATES:
			return "Checking for update...";
		case DOWNLOADING_UPDATE:
			return "Downloading ";
		case IMPORTING_UPDATE:
			return "Importing...";
		}
		return "N/A";
	}

	public void startContentUpdates(boolean forceUpdate) {
		if (forceUpdate)
			clearUpdateData();// clear any existing updates..
		if (updateStatus == UpdateStatus.NONE) {
			if (LogConfig.DEBUG_CONTENT_IMPORT)
				Log.d(TAG, "starting content update!");
			updateStatus = UpdateStatus.FETCHING_UPDATES;
			updateStartTime = Util.getTimestampMillis();
			Intent service = new Intent(context, FetchUpdatesService.class);
			context.startService(service);
		} else {
			Log.i(TAG, "update already running, ignore?");
		}
		notifyUIListeners();
	}

	// to be called from ContentChecker (WakefulBroadcastReceiver)
	public Intent getContentUpdateIntent() {
		if (updateStatus == UpdateStatus.NONE) {
			if (LogConfig.DEBUG_CONTENT_IMPORT)
				Log.d(TAG, "starting content update!");
			updateStatus = UpdateStatus.FETCHING_UPDATES;
			updateStartTime = Util.getTimestampMillis();
			notifyUIListeners();
			Intent service = new Intent(context, FetchUpdatesService.class);
			return service;
		} else {
			Log.i(TAG, "update already running, ignore?");
		}
		return null;
	}

	public void setContentUpdates(ContentUpdates contentUpdates) {
		boolean isDropboxUpdate = false;
		if (contentUpdates != null && contentUpdates.updates.length > 0 && contentUpdates.updates[0].isDropboxImport)
			isDropboxUpdate = true;
		if (isDropboxUpdate || updateStatus == UpdateStatus.FETCHING_UPDATES) {
			this.contentUpdates = contentUpdates;
			updateIndex = -1;
			// PERSIST
			String contentUpdateString = new Gson().toJson(contentUpdates);
			if (LogConfig.DEBUG_CONTENT_IMPORT)
				Log.d(TAG, "content update string:" + contentUpdateString);
			prefs.edit().putString(PREF_CONTENT_UPDATES, contentUpdateString).apply();

			beginNextUpdateDownload();
		} else {
			Log.w(TAG, "ignoring content updates, (how did we come here?)");
		}
	}

	public void downloadCompleted(Uri downloadedUpdateUri) {
		updateStatus = UpdateStatus.IMPORTING_UPDATE;
		Intent service = new Intent(context, ContentImportService.class);
		service.setData(downloadedUpdateUri);
		context.startService(service);
		notifyUIListeners();
	}

	public void downloadFailed(int errorCode) {
		Log.w(TAG, "download failed with code: " + errorCode);
		prefs.edit().remove(PREF_CURRENT_DOWNLOAD_ID).apply();
		// TODO: retry? different cdn? remove download.
		cancelUpdate();
	}

	public void importCompleted() {
		beginNextUpdateDownload();
	}

	public void importFailed(String msg) {
		addError();
		beginNextUpdateDownload();
	}

	public void cancelUpdate() {
		if (updateStatus != UpdateStatus.NONE) {
			Toast.makeText(context, "Update interrupted.", Toast.LENGTH_LONG).show();
			clearUpdateData();
		}
	}

	private void clearUpdateData() {
		updateStatus = UpdateStatus.NONE;
		if (prefs.contains(PREF_CURRENT_DOWNLOAD_ID))
			((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).remove(prefs.getLong(PREF_CURRENT_DOWNLOAD_ID, -1));
		prefs.edit().remove(PREF_CONTENT_UPDATES).remove(PREF_CURRENT_DOWNLOAD_ID).remove(PREF_UPDATE_ERRORS).remove(PREF_UPDATE_INDEX)
				.apply();
		notifyUIListeners();
	}

	private void beginNextUpdateDownload() {
		updateIndex++;
		while (updateIndex < contentUpdates.updates.length) {
			if (LogConfig.DEBUG_CONTENT_IMPORT)
				Log.d(TAG, "update - " + contentUpdates.updates[updateIndex].courseId + "::" + contentUpdates.updates[updateIndex].courseId
						+ ", " + contentUpdates.updates[updateIndex].fileName + "   -   "
						+ contentUpdates.updates[updateIndex].isApplicable);
			if (contentUpdates.updates[updateIndex].isApplicable) {
				// check if we have pre-requisite version
				if (contentUpdates.updates[updateIndex].strategy.equalsIgnoreCase("patch")) {
					ArrayList<Book> books = DatabaseHelper.getInstance(context).getBooks(contentUpdates.updates[updateIndex].courseId);
					int curVersion = -1;
					for (Book b : books) {
						if (b.id.equals(contentUpdates.updates[updateIndex].bookId)) {
							curVersion = b.version;
							break;
						}
					}
					if (curVersion != contentUpdates.updates[updateIndex].bookFromVersion) {
						// we don't have required version, skip
						addError();
						Toast.makeText(context, "Book 'patch' failed, required version missing...", Toast.LENGTH_SHORT).show();
						updateIndex++;
						continue;
					}
				}
				break;
			} else
				updateIndex++;
		}
		if (updateIndex >= contentUpdates.updates.length) {
			String message;
			if (prefs.getInt(PREF_UPDATE_ERRORS, 0) > 0) {
				message = "Finished sync, few updates failed...";
			} else {
				UserSessionProvider.getInstance(context).setTimestamp(UserSessionProvider.LAST_SYNC_CONTENT_TIMESTAMP,
						Util.getTimestampMillis());
				message = "Finished sync successfully!";
			}
			Toast.makeText(context, message, Toast.LENGTH_LONG).show();
			clearUpdateData();
			return;
		}

		DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

		// TODO: remove ALL downloads
		// check existing downloads and remove (incomplete ones)
		long enqueue = prefs.getLong(PREF_CURRENT_DOWNLOAD_ID, -1);
		if (enqueue >= 0) {
			removeDownloadId(dm, enqueue);
		}

		Uri downloadUri;
		if (Config.USE_CDN && !contentUpdates.updates[updateIndex].isDropboxImport) {
			if (contentUpdates.cdn.length > 0) {
				// TODO: add more robustness
				Random ran = new Random();
				int randomCDN = ran.nextInt(contentUpdates.cdn.length);
				downloadUri = Uri.withAppendedPath(Uri.parse(contentUpdates.cdn[randomCDN]), contentUpdates.updates[updateIndex].fileName);
			} else {
				Toast.makeText(context, "CDN is down, aborting.", Toast.LENGTH_LONG).show();
				clearUpdateData();
				return;
			}
		} else
			downloadUri = Uri.parse(contentUpdates.updates[updateIndex].webUrl);

		// fix url encoding
		Log.d(TAG, "downloadUri--" + downloadUri);
		downloadUri = Uri.parse(Util.convertToURLEscapingIllegalCharacters(downloadUri.toString()).toString());
		Log.d(TAG, "downloadUri--" + downloadUri);
		Request request = new Request(downloadUri);
		File downloadFolder = ((DiviApplication) context.getApplicationContext()).getDownloadsDir();
		if (downloadFolder.exists())
			Util.deleteRecursive(downloadFolder);
		if (LogConfig.DEBUG_CONTENT_IMPORT)
			Log.d(TAG, "downloadFolder exists? " + downloadFolder.exists());
		downloadFolder = ((DiviApplication) context.getApplicationContext()).getDownloadsDir();
		if (LogConfig.DEBUG_CONTENT_IMPORT)
			Log.d(TAG, "downloadFolder exists? " + downloadFolder.exists());
		request.setDestinationUri(Uri.fromFile(downloadFolder));
		request.setNotificationVisibility(Request.VISIBILITY_HIDDEN).setVisibleInDownloadsUi(false);
		enqueue = dm.enqueue(request);
		if (LogConfig.DEBUG_CONTENT_IMPORT) {
			Log.d(TAG, "storing download id:" + enqueue);
			Log.d(TAG, "downloading from:" + downloadUri);
		}
		prefs.edit().putLong(PREF_CURRENT_DOWNLOAD_ID, enqueue).apply();
		updateStatus = UpdateStatus.DOWNLOADING_UPDATE;
		notifyUIListeners();
	}

	private void removeDownloadId(DownloadManager dm, long id) {
		dm.remove(id);
		prefs.edit().remove(PREF_CURRENT_DOWNLOAD_ID).apply();
	}

	private void addError() {
		prefs.edit().putInt(PREF_UPDATE_ERRORS, 1 + prefs.getInt(PREF_UPDATE_ERRORS, 0)).apply();
	}

	private void notifyUIListeners() {
		for (ContentUpdateListener listener : listeners) {
			listener.onContentUIChange();
		}
	}

	@Override
	public void onSessionChange() {
		if (!UserSessionProvider.getInstance(context).isLoggedIn()) {
			cancelUpdate();
		}
	}

	@Override
	public void onCourseChange() {
		// not interested
	}
}
