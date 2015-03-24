package co.in.divi.util;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import java.io.File;

import co.in.divi.content.AllowedAppsProvider;

/**
 * Created by Indra on 3/24/2015.
 */
public class InstallAppService extends IntentService {
    private static final String TAG = InstallAppService.class.getSimpleName();

    public static final String INTENT_EXTRA_PACKAGE = "INTENT_EXTRA_PACKAGE ";
    private Handler handler;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
    public InstallAppService() {
        super(InstallAppService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            String pkgName = intent.getStringExtra(INTENT_EXTRA_PACKAGE);
            Log.d(TAG, "launching installer for package - " + pkgName);

            String selectionClause = AllowedAppsProvider.Apps.COLUMN_PACKAGE + " = ? ";
            Cursor appDetailsCursor = getContentResolver().query(AllowedAppsProvider.Apps.CONTENT_URI, null, selectionClause, new String[]{pkgName}, null);
            Log.d(TAG, "found pkg details - " + appDetailsCursor.getCount());
            if (appDetailsCursor.moveToFirst()) {
                String apkPath = appDetailsCursor.getString(appDetailsCursor
                        .getColumnIndex(AllowedAppsProvider.Apps.COLUMN_APK_PATH));
                Log.d(TAG, "apk at " + apkPath);

                Intent installerIntent = new Intent(Intent.ACTION_VIEW);
                installerIntent.setDataAndType(Uri.fromFile(new File(apkPath)), "application/vnd.android.package-archive");
                installerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(installerIntent);
            }
        } catch (Exception e) {
            Log.w(TAG, "error launching install prompt", e);
        }
    }
}
