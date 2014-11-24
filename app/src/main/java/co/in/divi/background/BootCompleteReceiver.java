package co.in.divi.background;

import co.in.divi.DiviApplication;
import android.content.BroadcastReceiver;
import android.os.SystemClock;
import android.util.Log;

public class BootCompleteReceiver extends BroadcastReceiver {
	private static final String	TAG	= BootCompleteReceiver.class.getSimpleName();

	public void onReceive(android.content.Context context, android.content.Intent intent) {
		Log.d(TAG, "Boot complete ");
		Log.d(TAG, "elapsed: " + SystemClock.elapsedRealtime());
		Log.d(TAG, "current: " + System.currentTimeMillis());
		long diff = System.currentTimeMillis() - SystemClock.elapsedRealtime();
		Log.d(TAG, "setting diff:" + diff);
		((DiviApplication) context.getApplicationContext()).setTimeDiff(diff);
	}

}