package co.in.divi;

import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;

import co.in.divi.content.DiviReference;
import co.in.divi.logs.DiviLog;
import co.in.divi.logs.LogsRecorderService;
import co.in.divi.util.Config;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Util;

public class LocationManager {
    private static final String TAG = LocationManager.class.getSimpleName();

    public static final int LOGS_QUEUE_SIZE = 12;

    // timestamp of last logs save
    private long lastLogsRecordedTimestamp = 0;
    private Location curLocation;

    private Context context;
    private UserSessionProvider userSessionProvider;
    private ArrayList<DiviLocationChangeListener> listeners;
    private BroadcastReceiver mPowerKeyReceiver = null;

    // for logging
    private ArrayList<DiviLog> logs;
    private DiviLog curLog;

    public interface DiviLocationChangeListener {
        public void onLocationChange(Location newLocation);
    }

    private static LocationManager instance = null;

    public static LocationManager getInstance(Context context) {
        if (instance == null) {
            instance = new LocationManager(context);
        }
        return instance;
    }

    public void addListener(DiviLocationChangeListener listener) {
        if (!this.listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeListener(DiviLocationChangeListener listener) {
        listeners.remove(listener);
    }

    public Location getLocation() {
        // IF location 'unknown', fill in the 3pApp details.
        if (Config.ENABLE_EXTERNAL_APP_SHARING && curLocation.getLocationType() == Location.LOCATION_TYPE.UNKNOWN) {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            PackageManager pm = context.getPackageManager();
            List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(10);
            if (tasks.size() > 0) {
                String pkgName = tasks.get(0).topActivity.getPackageName();
                String appName = "n/a";
                try {
                    appName = pm.getApplicationLabel(pm.getApplicationInfo(pkgName, PackageManager.GET_META_DATA))
                            .toString();
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(TAG, "error sharing external app:", e);
                }
                curLocation.setAppDetails(pkgName, appName);
            } else {
                Log.w(TAG, "error getting app details");
            }
        }
        return curLocation;
    }

    public void setNewLocation(Location.LOCATION_TYPE type, Location.LOCATION_SUBTYPE subType, DiviReference newRef, Location.Breadcrumb breadcrumb,
                               Location.ProtectedResourceMetadata unlockData) {
        if (curLocation.getLocationRef() != null && newRef != null && curLocation.getLocationRef().equals(newRef))
            return;
        if (LogConfig.DEBUG_LOCATION) {
            Log.d(TAG, "type:" + type);
            // Log.d(TAG, "setting new location:" + type + ", " + subType + ", " + newRef + ", " + breadcrumb);
            if (newRef != null)
                Log.d(TAG, newRef.getUri().toString());
        }

        curLocation = Location.getLocation(type,subType,newRef,breadcrumb,unlockData);

        notifyListeners();

        // logging
        // 1. update duration and close prev log
        if (curLog != null) {
            curLog.updateDuration();
            logs.add(curLog);
            curLog = null;
        }
        // 2. start new log
        if (type == Location.LOCATION_TYPE.ASSESSMENT || type == Location.LOCATION_TYPE.TOPIC) {
            curLog = new DiviLog(userSessionProvider.getUserData().uid, userSessionProvider.getUserData().token, curLocation.getLocationRef().getUri()
                    .toString(), DiviLog.LOG_TYPE_TIMESPENT, subType);
        }
        // 3. Check if we have enough logs & insert into DB
        if (logs.size() > LOGS_QUEUE_SIZE || Util.getTimestampMillis() - lastLogsRecordedTimestamp > 30 * 1000) {
            lastLogsRecordedTimestamp = Util.getTimestampMillis();
            Intent recordLogsIntent = new Intent(context, LogsRecorderService.class);
            recordLogsIntent.putParcelableArrayListExtra(LogsRecorderService.INTENT_EXTRA_LOGS, logs);
            context.startService(recordLogsIntent);
            logs.clear();
        }
    }

    private LocationManager(Context context) {
        this.context = context;
        userSessionProvider = UserSessionProvider.getInstance(context);
        listeners = new ArrayList<LocationManager.DiviLocationChangeListener>();
        logs = new ArrayList<DiviLog>();
        curLog = null;
        curLocation = Location.getUnknownLocation();
        // register for screen off
        final IntentFilter theFilter = new IntentFilter();
        /** System Defined Broadcast */
        theFilter.addAction(Intent.ACTION_SCREEN_ON);
        theFilter.addAction(Intent.ACTION_SCREEN_OFF);

        mPowerKeyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String strAction = intent.getAction();

                if (strAction.equals(Intent.ACTION_SCREEN_OFF)) {
                    setNewLocation(Location.LOCATION_TYPE.OFF, null, null, null, null);
                } else if (strAction.equals(Intent.ACTION_SCREEN_ON) && curLocation.getLocationType() == Location.LOCATION_TYPE.OFF) {
                    setNewLocation(Location.LOCATION_TYPE.UNKNOWN, null, null, null, null);
                }
            }
        };

        context.getApplicationContext().registerReceiver(mPowerKeyReceiver, theFilter);
    }

    private void notifyListeners() {
        for (DiviLocationChangeListener listener : listeners) {
            listener.onLocationChange(curLocation);
        }
    }


}
