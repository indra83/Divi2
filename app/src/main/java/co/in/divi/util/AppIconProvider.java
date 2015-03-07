package co.in.divi.util;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Based on blog:
 * The reason this works is down to the way that Linux filesystems operate: directories maintain links to files, when a process opens a file a new link is created, closing a file or removing it from a directory removes a link. When there are no links to a file, the file is deleted.
 * <p/>
 * So by opening the file in our application, we create a link to it. Then ‘deleting’ the file actually unlinks it, but the file won’t really be deleted until the file descriptor is discarded (ie. the last link is removed). This will happen automatically at some point after the calling application has finished accessing the file and any associated ParcelFileDescriptor objects have become garbage.
 * <p/>
 * So it’s a very simple solution, but only if you know something about how the filesystem can be expected to work on Android devices.
 * Created by Indra on 3/8/2015.
 */
public class AppIconProvider extends ContentProvider {
    private static final String TAG = AppIconProvider.class.getSimpleName();

    public static final String APP_ICON_PROVIDER_PREFIX = "content://co.in.divi.util.AppIconProvider/";

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        Log.d(TAG, "Uri: " + uri + ",path:" + uri.getPath());
        try {
            String pkgName = uri.getPath().substring(1);
            Log.d(TAG, "pkg: " + pkgName);
            File outputDir = getContext().getCacheDir(); // context being the Activity pointer
            File file = File.createTempFile(pkgName, "jpg", outputDir);
            try {

                FileOutputStream out = new FileOutputStream(file);
                try {
                    AppUtil.drawableToBitmap(AppUtil.getAppIcon(pkgName, getContext()))
                            .compress(Bitmap.CompressFormat.PNG, 100, out);
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Failed to close file: " + file);
                    }
                }
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            } finally {
                file.delete();
            }
        } catch (Exception e) {
            Log.w(TAG, "error getting app icon", e);
        }
        return null;
    }
}
