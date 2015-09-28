package co.in.divi.apps;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;

import co.in.divi.util.InstallAppService;
import co.in.divi.util.Util;

/**
 * Created by indra83 on 9/28/15.
 */
public class AppLauncher {
    private static final String TAG = AppLauncher.class.getSimpleName();

    private static final String LOLLIPOP_LAUNCHER = "co.in.divi.llauncher";
    private static final String LAUNCH_ACTION = "co.in.divi.llauncher.LAUNCH_APP";
    private static final String INTENT_EXTRA_PACKAGE = "INTENT_EXTRA_PACKAGE";
    private static final String INTENT_EXTRA_ACTIVITY = "INTENT_EXTRA_ACTIVITY";

    public static final void launchApp(Context context, String pkgName, int versionCode, String activityName) {
        // 1. App doesn't exist/outdated, launch install process.
        PackageManager pm = context.getPackageManager();
        if (!Util.isAppExists(pm, pkgName, versionCode)) {
            beginInstall(context, pkgName);
            return;
        }

        // 2. app exists, check if we have Lollipop_Launcher
        Intent i = new Intent();
        i.setAction(LAUNCH_ACTION);
        i.putExtra(INTENT_EXTRA_PACKAGE, pkgName);
        if (activityName != null && activityName.length() > 0)
            i.putExtra(INTENT_EXTRA_ACTIVITY, activityName);
        for (ResolveInfo resolveInfo : pm.queryBroadcastReceivers(i, PackageManager.MATCH_DEFAULT_ONLY)) {
            if (resolveInfo.activityInfo.packageName.equals(LOLLIPOP_LAUNCHER)) {
                context.sendBroadcast(i);
                return;
            }
        }

        // 3. Not lollipop OR no launcher, launch 'manually'.
        Intent intent;
        if (activityName != null && activityName.length() > 0) {
            intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName(pkgName, activityName));
        } else {
            intent = pm.getLaunchIntentForPackage(pkgName);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }

    public static final void beginInstall(Context context, String pkgName) {
        Toast.makeText(context, "Installing app...", Toast.LENGTH_SHORT).show();
        Intent installerIntent = new Intent(context, InstallAppService.class);
        installerIntent.putExtra(InstallAppService.INTENT_EXTRA_PACKAGE, pkgName);
        context.startService(installerIntent);
    }
}
