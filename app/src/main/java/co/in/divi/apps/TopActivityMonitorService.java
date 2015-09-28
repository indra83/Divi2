package co.in.divi.apps;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * Created by indra83 on 9/28/15.
 */
public class TopActivityMonitorService extends AccessibilityService {
    private static final String TAG = TopActivityMonitorService.class.getSimpleName();


    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        //Configure these here for compatibility with API 13 and below.
        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;

        if (Build.VERSION.SDK_INT >= 16)
            //Just in case this helps
            config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

        setServiceInfo(config);

        TopAppProvider.getInstance().setActive(true);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                ComponentName componentName = new ComponentName(
                        event.getPackageName().toString(),
                        event.getClassName().toString()
                );
//                if(componentName.getPackageName().equals(getPackageName())) {
//                    Log.d(TAG,"ignoring Divi");
//                    return;
//                }
                ActivityInfo activityInfo = tryGetActivity(componentName);
                boolean isActivity = activityInfo != null;
                if (isActivity) {
                    Log.i(TAG, componentName.flattenToShortString());
                    PackageManager pm = getPackageManager();
                    TopAppProvider.getInstance().setTopApp(componentName.getPackageName(),
                            pm.getApplicationLabel(activityInfo.applicationInfo).toString());
                } else {
                    Log.w(TAG, "whats this? " + componentName);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "exception in accessibility", e);
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "onInterrupt");
    }

    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
