package co.in.divi;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class DiviService extends Service {

	private static final String	WIFI_LOCK_TAG		= "WIFI_LOCK_TAG";
	private static final String	WAKE_LOCK_TAG		= "WAKE_LOCK_TAG";
	private static final long	WAKELOCK_TIMEOUT	= 60 * 1000;		// ms

	private WifiLock			wifilock			= null;
	private WakeLock			wakelock			= null;

	/*
	 * [Threadsafe] Acquires CPU & WiFi locks for WAKELOCK_TIMEOUT
	 * 
	 * Call this before releasing the lock by WakefulBroadcastReceiver.
	 */
	protected synchronized void acquireLocks() {
		if (wifilock == null && wakelock == null) {
			WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			wifilock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, WIFI_LOCK_TAG);
			wifilock.setReferenceCounted(false);
			wifilock.acquire();

			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
			wakelock.setReferenceCounted(false);
			wakelock.acquire(WAKELOCK_TIMEOUT);
		}
	}

	/*
	 * [Threadsafe] Releases any acquired locks
	 */
	protected synchronized void releaseLocks() {
		if (wifilock != null && wifilock.isHeld())
			wifilock.release();
		if (wakelock != null && wakelock.isHeld())
			wakelock.release();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
