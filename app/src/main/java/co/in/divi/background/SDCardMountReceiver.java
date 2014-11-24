package co.in.divi.background;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SDCardMountReceiver extends BroadcastReceiver {
	public static final String	TAG	= SDCardMountReceiver.class.getSimpleName();

	public SDCardMountReceiver() {
		super();
		Log.d(TAG, "in broadcast receiver");
	}

	public void onReceive(Context context, Intent intent) {
		try {
			Log.i(TAG, "####################receive " + intent.getAction());
			Log.i(TAG, "####################receive " + intent.getDataString());
			// Toast.makeText(context, "got intent:" + intent.toString(), Toast.LENGTH_LONG).show();
			Log.d(TAG, "intent - " + intent.getData());
		} catch (Exception e) {
			Log.e(TAG, "error!", e);
		}
	}
}