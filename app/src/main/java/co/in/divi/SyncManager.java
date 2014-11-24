package co.in.divi;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import co.in.divi.db.sync.SyncUpService;
import co.in.divi.util.Config;
import co.in.divi.util.Util;

/**
 * Sync for user data (bookmarks, answers etc.)
 * 
 * @author indraneel
 * 
 */
public class SyncManager {
	private static final String				TAG	= SyncManager.class.getSimpleName();

	private Context							context;
	private UserSessionProvider				userSessionProvider;
	private ArrayList<SyncStatusListener>	listeners;
	private SyncStatus						syncStatus;

	private Long							syncStartTime;

	public interface SyncStatusListener {
		public void onSyncStateChange();
	}

	private static SyncManager	instance	= null;

	public static SyncManager getInstance(Context context) {
		if (instance == null) {
			instance = new SyncManager(context);
		}
		return instance;
	}

	public static enum SyncStatus {
		NONE, SYNCING
	}

	private SyncManager(Context context) {
		this.context = context;
		userSessionProvider = UserSessionProvider.getInstance(context);
		listeners = new ArrayList<SyncStatusListener>();
		syncStatus = SyncStatus.NONE;
	}

	public void addListener(SyncStatusListener listener) {
		if (!this.listeners.contains(listener))
			listeners.add(listener);
	}

	public void removeListener(SyncStatusListener listener) {
		listeners.remove(listener);
	}

	public Intent getSyncUpIntent() {
		return new Intent(context, SyncUpService.class);
	}

	public long getLastSyncTime() {
		return userSessionProvider.getTimestamp(UserSessionProvider.LAST_SYNC_ATTEMTPS_TIMESTAMP);
	}

	public void setLastSyncTime(long syncTime) {
		userSessionProvider.setTimestamp(UserSessionProvider.LAST_SYNC_ATTEMTPS_TIMESTAMP, Util.getTimestampMillis());
	}

	public boolean isUpdateRequired() {
		long timeElapsed = (Util.getTimestampMillis() - getLastSyncTime());
		return (timeElapsed > Config.INTERVAL_ATTEMPTS_UPDATE) ? true : false;
	}

	public SyncStatus getSyncStatus() {
		return syncStatus;
	}

	public void setSyncStatus(SyncStatus newStatus) {
		this.syncStatus = newStatus;
		notifyListeners();
	}

	private void notifyListeners() {
		for (SyncStatusListener l : listeners)
			l.onSyncStateChange();
	}
}
