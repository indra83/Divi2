package co.in.divi.logs;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import co.in.divi.logs.LogsDBContract.Logs;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Util;

public class LogsDBProvider extends ContentProvider {
	private static final String			TAG				= LogsDBProvider.class.getSimpleName();

	private static final int			LOG_LIST		= 1;
	private static final int			LOG_ID			= 2;

	private static final UriMatcher		URI_MATCHER;
	// prepare the UriMatcher
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		// attempts
		URI_MATCHER.addURI(LogsDBContract.AUTHORITY, Logs.CONTENT_NAME, LOG_LIST);
		URI_MATCHER.addURI(LogsDBContract.AUTHORITY, Logs.CONTENT_NAME + "/#", LOG_ID);
	}

	LogsDBOpenHelper					dbHelper		= null;
	private final ThreadLocal<Boolean>	mIsInBatchMode	= new ThreadLocal<Boolean>();

	@Override
	public boolean onCreate() {
		if (LogConfig.DEBUG_LOGS)
			Log.d(TAG, "oncreate");
		dbHelper = new LogsDBOpenHelper(getContext());
		return true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (LogConfig.DEBUG_LOGS)
			Log.d(TAG, "delete - " + uri + ", " + selection);

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int delCount = 0;
		switch (URI_MATCHER.match(uri)) {
		case LOG_LIST:
			delCount = db.delete(LogsDBSchema.TBL_LOGS, selection, selectionArgs);
			break;
		case LOG_ID:
			String idStr = uri.getLastPathSegment();
			String where = Logs._ID + " = " + idStr;
			if (!TextUtils.isEmpty(selection)) {
				where += " AND " + selection;
			}
			delCount = db.delete(LogsDBSchema.TBL_LOGS, where, selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
		// notify all listeners of changes:
		if (delCount > 0 && !isInBatchMode()) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return delCount;
	}

	@Override
	public String getType(Uri uri) {
		switch (URI_MATCHER.match(uri)) {
		case LOG_LIST:
			return Logs.CONTENT_TYPE;
		case LOG_ID:
			return Logs.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (LogConfig.DEBUG_LOGS)
			Log.d(TAG, "insert - " + uri + ", " + values.size());
		if (URI_MATCHER.match(uri) != LOG_LIST) {
			throw new IllegalArgumentException("Unsupported URI for insertion: " + uri);
		}
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		if (URI_MATCHER.match(uri) == LOG_LIST) {
			if (!values.containsKey(Logs.SYNC_STATUS))
				values.put(Logs.SYNC_STATUS, Logs.SYNC_TO_SYNC);
			if (!values.containsKey(Logs.LAST_UPDATED))// ignore if coming from SyncDown
				values.put(Logs.LAST_UPDATED, Util.getTimestampMillis());
			long id = db.insert(LogsDBSchema.TBL_LOGS, null, values);
			return getUriForId(id, uri);
		} else {
			return null;
		}
	}

	private Uri getUriForId(long id, Uri uri) {
		if (id > 0) {
			Uri itemUri = ContentUris.withAppendedId(uri, id);
			if (!isInBatchMode()) {
				// notify all listeners of changes and return itemUri:
				getContext().getContentResolver().notifyChange(itemUri, null);
			}
			return itemUri;
		}
		// s.th. went wrong:
		throw new SQLException("Problem while inserting into uri: " + uri);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		if (LogConfig.DEBUG_LOGS)
			Log.d(TAG, "query - " + uri + ", " + selection);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		boolean useAuthorityUri = false;
		switch (URI_MATCHER.match(uri)) {
		case LOG_LIST:
			builder.setTables(LogsDBSchema.TBL_LOGS);
			if (TextUtils.isEmpty(sortOrder)) {
				sortOrder = Logs.SORT_ORDER_DEFAULT;
			}
			break;
		case LOG_ID:
			builder.setTables(LogsDBSchema.TBL_LOGS);
			// limit query to one row at most:
			builder.appendWhere(Logs._ID + " = " + uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}

		Cursor cursor = builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		// if we want to be notified of any changes:
		if (useAuthorityUri) {
			cursor.setNotificationUri(getContext().getContentResolver(), LogsDBContract.CONTENT_URI);
		} else {
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if (LogConfig.DEBUG_LOGS)
			Log.d(TAG, "update - " + uri + ", " + selection);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int updateCount = 0;
		switch (URI_MATCHER.match(uri)) {
		case LOG_LIST:
			updateCount = db.update(LogsDBSchema.TBL_LOGS, values, selection, selectionArgs);
			break;
		case LOG_ID:
			String idStr = uri.getLastPathSegment();
			String where = Logs._ID + " = " + idStr;
			if (!TextUtils.isEmpty(selection)) {
				where += " AND " + selection;
			}
			updateCount = db.update(LogsDBSchema.TBL_LOGS, values, where, selectionArgs);
			break;
		default:
			// no support for updating photos!
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
		// notify all listeners of changes:
		if (updateCount > 0 && !isInBatchMode()) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return updateCount;
	}

	@Override
	public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		mIsInBatchMode.set(true);
		// the next line works because SQLiteDatabase
		// uses a thread local SQLiteSession object for
		// all manipulations
		db.beginTransaction();
		try {
			final ContentProviderResult[] retResult = super.applyBatch(operations);
			db.setTransactionSuccessful();
			getContext().getContentResolver().notifyChange(LogsDBContract.CONTENT_URI, null);
			return retResult;
		} finally {
			mIsInBatchMode.remove();
			db.endTransaction();
		}
	}

	private boolean isInBatchMode() {
		return mIsInBatchMode.get() != null && mIsInBatchMode.get();
	}
}
