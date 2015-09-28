package co.in.divi.apps;

import android.util.Log;

/**
 * Created by indra83 on 9/28/15.
 */
public class TopAppProvider {
    private static final String TAG = TopAppProvider.class.getSimpleName();

    private boolean isActive;
    private String topAppName;
    private String topAppPackage;
    private long timestamp;

    public String getTopAppName() {
        return topAppName;
    }

    public String getTopAppPackage() {
        return topAppPackage;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public void setTopApp(String appName, String appPackage) {
        Log.d(TAG, "opened app: " + appPackage + " / " + appName);
        this.topAppName = appName;
        this.topAppPackage = appPackage;
        timestamp = System.currentTimeMillis();
    }

    // singleton
    private static TopAppProvider instance;

    private TopAppProvider() {

    }

    public static TopAppProvider getInstance() {
        if(instance==null)
            instance = new TopAppProvider();
        return instance;
    }
}
