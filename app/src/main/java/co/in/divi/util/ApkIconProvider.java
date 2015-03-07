package co.in.divi.util;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Refer: AppIconProvider
 * Created by Indra on 3/8/2015.
 */
public class ApkIconProvider extends ContentProvider {
    private static final String TAG = ApkIconProvider.class.getSimpleName();

    public static final String APP_ICON_PROVIDER_PREFIX = "content://co.in.divi.util.ApkIconProvider/";

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
            String path = AppUtil.getApkPath(uri);
            Log.d(TAG, "path: " + path);
            File outputDir = getContext().getCacheDir(); // context being the Activity pointer
            File file = File.createTempFile(UUID.randomUUID().toString(), "jpg", outputDir);
            try {

                FileOutputStream out = new FileOutputStream(file);
                try {
                    AppUtil.drawableToBitmap(AppUtil.getApkIcon(path, getContext()))
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
