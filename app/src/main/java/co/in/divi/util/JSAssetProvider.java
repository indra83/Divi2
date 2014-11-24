package co.in.divi.util;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class JSAssetProvider extends ContentProvider {

	static final String	TAG	= "JSAssetProvider";

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
	public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
		// Log.d(TAG, "Uri: " + uri + ",path:" + uri.getPath());
		// Log.d(TAG, "mode: " + mode);
		try {
			// hack to get around the media file only restriction in assets..
			String fileName = uri.getPath().substring(1);
			if (!fileName.endsWith(".png"))
				fileName = fileName + ".png";
			return getContext().getAssets().openFd(fileName);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "ERROR: " + e);
			throw new FileNotFoundException(e.getMessage());
		}
	}

}
