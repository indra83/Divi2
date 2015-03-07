package co.in.divi.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by Indra on 3/7/2015.
 */
public class AppUtil {
    private static final String TAG = AppUtil.class.getSimpleName();

    public static final String APP_ICON_PROVIDER_PREFIX = "content://co.in.divi.util.AppIconProvider/";
    public static final String APK_ICON_PROVIDER_PREFIX = "content://co.in.divi.util.ApkIconProvider/";

    public static String getAppIconUrl(String pkgName) {
        return Uri.parse(APP_ICON_PROVIDER_PREFIX).buildUpon().path(pkgName).build().toString();
    }

    public static String getApkIconUrl(String path) {
        return Uri.parse(APK_ICON_PROVIDER_PREFIX).buildUpon().encodedPath(Uri.encode(path)).build().toString();
    }

    public static String getApkPath(Uri uri) {
        return uri.getPath().substring(1);
    }

    public static boolean isPackageInstalled(String pkg, Context context) {
        PackageManager pm = context.getPackageManager();
        boolean app_installed = false;
        try {
            pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }

    public static Drawable getAppIcon(String packageName, Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, 0);
            Context otherAppCtx = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);

            int displayMetrics[] = {DisplayMetrics.DENSITY_XHIGH, DisplayMetrics.DENSITY_HIGH, DisplayMetrics.DENSITY_TV};

            for (int displayMetric : displayMetrics) {
                try {
                    Drawable d = otherAppCtx.getResources().getDrawableForDensity(pi.applicationInfo.icon, displayMetric);
                    if (d != null) {
                        return d;
                    }
                } catch (Resources.NotFoundException e) {
                    continue;
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "error loading icon", e);
        }
        return null;
    }

    public static Drawable getApkIcon(String apkPath, Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageArchiveInfo(apkPath, 0);

            // the secret are these two lines....
            pi.applicationInfo.sourceDir = apkPath;
            pi.applicationInfo.publicSourceDir = apkPath;

            return pi.applicationInfo.loadIcon(context.getPackageManager());

        } catch (Exception e) {
            Log.w(TAG, "error loading icon", e);
        }
        return null;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static InputStream bitmapToInputStream(Bitmap bitmap) {
        int size = bitmap.getHeight() * bitmap.getRowBytes();
        ByteBuffer buffer = ByteBuffer.allocate(size);
        bitmap.copyPixelsToBuffer(buffer);
        return new ByteArrayInputStream(buffer.array());
    }

    public static Bitmap getAppIcon(File apkFile, Context context) {
        return null;
    }
}
