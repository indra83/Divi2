package co.in.divi.background;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import co.in.divi.ContentUpdateManager;
import co.in.divi.SyncManager;
import co.in.divi.UserSessionProvider;
import co.in.divi.db.sync.SyncDownService;
import co.in.divi.logs.LogsSyncService;
import co.in.divi.progress.AnalyticsFetcherService;
import co.in.divi.progress.AnalyticsManager;
import co.in.divi.util.Config;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Util;

public class UniversalSyncCheckReceiver extends WakefulBroadcastReceiver {
	private static final String	TAG				= UniversalSyncCheckReceiver.class.getSimpleName();

	private static final int	INITIAL_DELAY	= 5 * 1000;										// 5 seconds

	public static void scheduleAlarms(Context ctxt) {
		AlarmManager mgr = (AlarmManager) ctxt.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(ctxt, UniversalSyncCheckReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(ctxt, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
		mgr.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + INITIAL_DELAY,
				Config.INTERVAL_ALARM_SECONDS * 1000, pi);
	}

	public static void syncNow(Context context, boolean force) {
		// we run background tasks only if logged in.
		if (!UserSessionProvider.getInstance(context).isLoggedIn())
			return;
		// check network. If off, enable the network listener.
		/* instead use same receiver for all network events as well.. */
		// ComponentName receiver = new ComponentName(context, NetworkStateReceiver.class);
		// PackageManager pm = context.getPackageManager();
		if (!Util.isNetworkOn(context)) {
			// pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
			// PackageManager.DONT_KILL_APP);
			return;
		}

		// 1. Check and start content update.
		if (force || ContentUpdateManager.getInstance(context).isUpdateRequired()) {
			Intent service = ContentUpdateManager.getInstance(context).getContentUpdateIntent();
			if (service != null) {
				if (LogConfig.DEBUG_SYNC)
					Log.d(TAG, "Starting wakeful content update");
				startWakefulService(context, service);
			}
		}

		// 2. Check and start User data Sync Up(Attempts)
		if (force || SyncManager.getInstance(context).isUpdateRequired()) {
			Intent service = SyncManager.getInstance(context).getSyncUpIntent();
			if (LogConfig.DEBUG_SYNC)
				Log.d(TAG, "Starting wakeful sync  up (attempt) update");
			startWakefulService(context, service);
		}

		// 3. Check and start Sync Down (Commands)
		long delta = Util.getTimestampMillis()
				- UserSessionProvider.getInstance(context).getTimestamp(UserSessionProvider.LAST_SYNC_COMMANDS_TIMESTAMP);
		if (force || delta > Config.INTERVAL_COMMANDS_UPDATE) {
			Intent startSyncDownService = new Intent(context, SyncDownService.class);
			startSyncDownService.putExtra(SyncDownService.INTENT_EXTRA_ONLY_COMMAND, true);
			startWakefulService(context, startSyncDownService);
		}

		// 4. Check and start Usage Logs Sync
		long delta2 = Util.getTimestampMillis()
				- UserSessionProvider.getInstance(context).getTimestamp(UserSessionProvider.LAST_SYNC_LOGS_TIMESTAMP);
		if (force || delta2 > Config.INTERVAL_LOGS_UPDATE) {
			Intent launchLogsSync = new Intent(context, LogsSyncService.class);
			startWakefulService(context, launchLogsSync);
		}

		// 5. Check and start Analytics Report gen.
		long delta3 = Util.getTimestampMillis() - AnalyticsManager.getInstance(context).getLastSyncTime();
		if (force || delta3 > Config.INTERVAL_REPORTS_UPDATE) {
			Intent launchReportsSync = new Intent(context, AnalyticsFetcherService.class);
			startWakefulService(context, launchReportsSync);
		}

		// 6. Send Heartbeat
		long delta4 = Util.getTimestampMillis()
				- UserSessionProvider.getInstance(context).getTimestamp(UserSessionProvider.LAST_SYNC_HEARTBEAT_TIMESTAMP);
		if (force || delta4 > Config.INTERVAL_HEARTBEAT_UPDATE) {
			Intent launchHeartbeatSync = new Intent(context, HeartbeatService.class);
			startWakefulService(context, launchHeartbeatSync);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (LogConfig.DEBUG_SYNC) {
			Log.d(TAG, "Begin sync check...");
			debugIntent(intent, TAG);
		}

		syncNow(context, false);
	}

	private void debugIntent(Intent intent, String tag) {
		Log.v(tag, "action: " + intent.getAction());
		Log.v(tag, "component: " + intent.getComponent());
		Bundle extras = intent.getExtras();
		if (extras != null) {
			for (String key : extras.keySet()) {
				Log.v(tag, "key [" + key + "]: " + extras.get(key));
			}
		} else {
			Log.v(tag, "no extras");
		}
	}
}
