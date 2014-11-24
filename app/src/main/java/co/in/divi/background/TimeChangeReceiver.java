package co.in.divi.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import co.in.divi.DiviApplication;
import co.in.divi.UserSessionProvider;

public class TimeChangeReceiver extends BroadcastReceiver {
	private static final String	TAG	= TimeChangeReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.w(TAG, "Time changed! logout?");
		long newDiff = System.currentTimeMillis() - SystemClock.elapsedRealtime();
		Log.d(TAG, "new diff:" + newDiff);
		long oldDiff = ((DiviApplication) context.getApplicationContext()).getTimeDiff();
		Log.d(TAG, "old diff:" + oldDiff);

		long diffDiff = Math.abs(newDiff - oldDiff);
		Log.d(TAG, "diffdiff:" + diffDiff);
		if (diffDiff > 61 * 1000) {
			Log.w(TAG, "logging out!");
			UserSessionProvider.getInstance(context).logout();
		} else {
			((DiviApplication) context.getApplicationContext()).setTimeDiff(newDiff);
		}
	}
}
