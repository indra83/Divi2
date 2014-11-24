package co.in.divi;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

/**
 * Hold a wakelock that can be acquired in the AlarmReceiver and released in the AlarmAlert activity
 */
public class AlarmAlertWakeLock {
	private static final String				TAG	= AlarmAlertWakeLock.class.getSimpleName();

	private static PowerManager.WakeLock	sCpuWakeLock;

	public static void acquireCpuWakeLock(Context context) {
		Log.d(TAG, "Acquiring cpu wake lock");
		if (sCpuWakeLock != null) {
			return;
		}
		Log.d(TAG, "acquiring new lock");
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

		sCpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
				TAG);
		sCpuWakeLock.acquire();
	}

	public static void releaseCpuLock() {
		Log.d(TAG, "Releasing cpu wake lock");
		if (sCpuWakeLock != null) {
			sCpuWakeLock.release();
			sCpuWakeLock = null;
		}
	}
}