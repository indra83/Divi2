package co.in.divi.db;

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
import co.in.divi.db.UserDBContract.Attempts;
import co.in.divi.db.UserDBContract.Commands;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Util;

public class UserDBProvider extends ContentProvider {
	private static final String			TAG				= UserDBProvider.class.getSimpleName();

	private static final int			ATTEMPT_LIST	= 1;
	private static final int			ATTEMPT_ID		= 2;

	private static final int			COMMAND_LIST	= 3;
	private static final int			COMMAND_ID		= 4;

	private static final UriMatcher		URI_MATCHER;
	// prepare the UriMatcher
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		// attempts
		URI_MATCHER.addURI(UserDBContract.AUTHORITY, Attempts.CONTENT_NAME, ATTEMPT_LIST);
		URI_MATCHER.addURI(UserDBContract.AUTHORITY, Attempts.CONTENT_NAME + "/#", ATTEMPT_ID);

		// commands
		URI_MATCHER.addURI(UserDBContract.AUTHORITY, Commands.CONTENT_NAME, COMMAND_LIST);
		URI_MATCHER.addURI(UserDBContract.AUTHORITY, Commands.CONTENT_NAME + "/#", COMMAND_ID);
	}

	UserDBOpenHelper					dbHelper		= null;
	private final ThreadLocal<Boolean>	mIsInBatchMode	= new ThreadLocal<Boolean>();

	@Override
	public boolean onCreate() {
		if (LogConfig.DEBUG_USERDB)
			Log.d(TAG, "oncreate");
		dbHelper = new UserDBOpenHelper(getContext());
		return true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (LogConfig.DEBUG_USERDB)
			Log.d(TAG, "delete - " + uri + ", " + selection);

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int delCount = 0;
		switch (URI_MATCHER.match(uri)) {
		case ATTEMPT_LIST:
			delCount = db.delete(UserDBSchema.TBL_ATTEMPTS, selection, selectionArgs);
			break;
		case ATTEMPT_ID:
			String idStr = uri.getLastPathSegment();
			String where = Attempts._ID + " = " + idStr;
			if (!TextUtils.isEmpty(selection)) {
				where += " AND " + selection;
			}
			delCount = db.delete(UserDBSchema.TBL_ATTEMPTS, where, selectionArgs);
			break;
		case COMMAND_LIST:
			delCount = db.delete(UserDBSchema.TBL_COMMANDS, selection, selectionArgs);
			break;
		case COMMAND_ID:
			String cmd_idStr = uri.getLastPathSegment();
			String cmd_where = Commands._ID + " = " + cmd_idStr;
			if (!TextUtils.isEmpty(selection)) {
				cmd_where += " AND " + selection;
			}
			delCount = db.delete(UserDBSchema.TBL_COMMANDS, cmd_where, selectionArgs);
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
		case ATTEMPT_LIST:
			return Attempts.CONTENT_TYPE;
		case ATTEMPT_ID:
			return Attempts.CONTENT_ITEM_TYPE;
		case COMMAND_LIST:
			return Commands.CONTENT_TYPE;
		case COMMAND_ID:
			return Commands.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (LogConfig.DEBUG_USERDB)
			Log.d(TAG, "insert - " + uri + ", " + values.size());
		if (URI_MATCHER.match(uri) != ATTEMPT_LIST && URI_MATCHER.match(uri) != COMMAND_LIST) {
			throw new IllegalArgumentException("Unsupported URI for insertion: " + uri);
		}
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		if (URI_MATCHER.match(uri) == ATTEMPT_LIST) {
			if (!values.containsKey(Attempts.SYNC_STATUS))
				values.put(Attempts.SYNC_STATUS, Attempts.SYNC_TO_SYNC);
			if (!values.containsKey(Attempts.LAST_UPDATED))// ignore if coming from SyncDown
				values.put(Attempts.LAST_UPDATED, Util.getTimestampMillis());
			long id = db.insert(UserDBSchema.TBL_ATTEMPTS, null, values);
			return getUriForId(id, uri);
		} else if (URI_MATCHER.match(uri) == COMMAND_LIST) {
			if (!values.containsKey(Commands.LAST_UPDATED))// ignore if coming from SyncDown
				values.put(Commands.LAST_UPDATED, Util.getTimestampMillis());
			long id = db.insert(UserDBSchema.TBL_COMMANDS, null, values);
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
		if (LogConfig.DEBUG_USERDB)
			Log.d(TAG, "query - " + uri + ", " + selection);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		boolean useAuthorityUri = false;
		switch (URI_MATCHER.match(uri)) {
		case ATTEMPT_LIST:
			builder.setTables(UserDBSchema.TBL_ATTEMPTS);
			if (TextUtils.isEmpty(sortOrder)) {
				sortOrder = Attempts.SORT_ORDER_DEFAULT;
			}
			break;
		case ATTEMPT_ID:
			builder.setTables(UserDBSchema.TBL_ATTEMPTS);
			// limit query to one row at most:
			builder.appendWhere(Attempts._ID + " = " + uri.getLastPathSegment());
			break;
		case COMMAND_LIST:
			builder.setTables(UserDBSchema.TBL_COMMANDS);
			if (TextUtils.isEmpty(sortOrder)) {
				sortOrder = Commands.SORT_ORDER_DEFAULT;
			}
			break;
		case COMMAND_ID:
			builder.setTables(UserDBSchema.TBL_COMMANDS);
			// limit query to one row at most:
			builder.appendWhere(Commands._ID + " = " + uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}

		Cursor cursor = builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		// if we want to be notified of any changes:
		if (useAuthorityUri) {
			cursor.setNotificationUri(getContext().getContentResolver(), UserDBContract.CONTENT_URI);
		} else {
			cursor.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if (LogConfig.DEBUG_USERDB)
			Log.d(TAG, "update - " + uri + ", " + selection);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int updateCount = 0;
		switch (URI_MATCHER.match(uri)) {
		case ATTEMPT_LIST:
			updateCount = db.update(UserDBSchema.TBL_ATTEMPTS, values, selection, selectionArgs);
			break;
		case ATTEMPT_ID:
			String idStr = uri.getLastPathSegment();
			String where = Attempts._ID + " = " + idStr;
			if (!TextUtils.isEmpty(selection)) {
				where += " AND " + selection;
			}
			updateCount = db.update(UserDBSchema.TBL_ATTEMPTS, values, where, selectionArgs);
			break;
		case COMMAND_LIST:
			updateCount = db.update(UserDBSchema.TBL_COMMANDS, values, selection, selectionArgs);
			break;
		case COMMAND_ID:
			String cmd_idStr = uri.getLastPathSegment();
			String cmd_where = Commands._ID + " = " + cmd_idStr;
			if (!TextUtils.isEmpty(selection)) {
				cmd_where += " AND " + selection;
			}
			updateCount = db.update(UserDBSchema.TBL_COMMANDS, values, cmd_where, selectionArgs);
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
			getContext().getContentResolver().notifyChange(UserDBContract.CONTENT_URI, null);
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
