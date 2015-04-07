package co.in.divi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

import co.in.divi.logs.DiviLog;
import co.in.divi.logs.LogsRecorderService;

/**
 * Created by Indra on 3/4/2015.
 */
public class DiviAppUsageReceiver extends BroadcastReceiver {
    private static final String TAG = DiviAppUsageReceiver.class.getSimpleName();

    public static final String INTENT_EXTRA_APP_PACKAGE = "INTENT_EXTRA_APP_PACKAGE";
    public static final String INTENT_EXTRA_OPENED_AT = "INTENT_EXTRA_OPENED_AT";
    public static final String INTENT_EXTRA_DURATION = "INTENT_EXTRA_DURATION";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Got app usage broadcast");
        UserSessionProvider userSessionProvider = UserSessionProvider.getInstance(context);
        if (userSessionProvider.isLoggedIn()) {
            DiviLog diviLog = new DiviLog(userSessionProvider.getUserData().uid, userSessionProvider.getUserData().token,
                    intent.getStringExtra(INTENT_EXTRA_APP_PACKAGE), DiviLog.LOG_TYPE_TIMESPENT, Location.LOCATION_SUBTYPE.APP);
            diviLog.duration = intent.getLongExtra(INTENT_EXTRA_DURATION, 0L);
            diviLog.openedAt = intent.getLongExtra(INTENT_EXTRA_OPENED_AT, 0L);
            ArrayList<DiviLog> logs = new ArrayList<DiviLog>();
            logs.add(diviLog);
            Intent recordLogsIntent = new Intent(context, LogsRecorderService.class);
            recordLogsIntent.putParcelableArrayListExtra(LogsRecorderService.INTENT_EXTRA_LOGS, logs);
            context.startService(recordLogsIntent);
        }
    }
}
