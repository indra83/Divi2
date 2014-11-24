package co.in.divi.content.importer;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import co.in.divi.ContentUpdateManager;
import co.in.divi.util.LogConfig;

public class DownloadCompleteReceiver extends BroadcastReceiver {
	static final String		TAG	= DownloadCompleteReceiver.class.getSimpleName();

	ContentUpdateManager	contentUpdateManager;
	private long			enqueue;
	DownloadManager			dm;

	public DownloadCompleteReceiver() {
		super();
		if (LogConfig.DEBUG_CONTENT_IMPORT)
			Log.d(TAG, "in DownloadCompleteReceiver");
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		contentUpdateManager = ContentUpdateManager.getInstance(context);
		enqueue = contentUpdateManager.getCurrentDownloadId();
		if (LogConfig.DEBUG_CONTENT_IMPORT)
			Log.d(TAG, "waiting for download:" + enqueue);
		if (enqueue < 0) {
			if (LogConfig.DEBUG_CONTENT_IMPORT)
				Log.d(TAG, "no download enqued");
			return;
		}
		dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		String action = intent.getAction();
		if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
			long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
			if (downloadId != enqueue) {
				Log.i(TAG, "not our download, ignoring");
				return;
			}
			Query query = new Query();
			query.setFilterById(enqueue);
			Cursor c = dm.query(query);
			if (c.moveToFirst()) {
				int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
				if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
					if (LogConfig.DEBUG_CONTENT_IMPORT)
						Log.d(TAG, "download successful! begin import");
					contentUpdateManager.downloadCompleted(Uri.parse(c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))));
				} else if (DownloadManager.STATUS_FAILED == c.getInt(columnIndex)) {
					if (LogConfig.DEBUG_CONTENT_IMPORT)
						Log.d(TAG, "download failed, removing id:" + enqueue);
					int errorCode = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
					dm.remove(enqueue);
					contentUpdateManager.downloadFailed(errorCode);
				}
			}
		}
	}
}
