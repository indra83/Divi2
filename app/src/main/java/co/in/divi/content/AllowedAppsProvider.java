package co.in.divi.content;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import co.in.divi.LectureSessionProvider;
import co.in.divi.UserSessionProvider;
import co.in.divi.db.UserDBContract;
import co.in.divi.model.Instruction;
import co.in.divi.model.LectureInstruction;
import co.in.divi.util.LogConfig;

import com.google.gson.Gson;

public class AllowedAppsProvider extends ContentProvider {
	private static final String	TAG			= AllowedAppsProvider.class.getSimpleName();

	public static final String	AUTHORITY	= "co.in.divi.allowedapps";
	public static final Uri		CONTENT_URI	= Uri.parse("content://" + AUTHORITY);

	public static class Apps {
		public static final String	CONTENT_NAME		= "apps";
		public static final Uri		CONTENT_URI			= Uri.withAppendedPath(AllowedAppsProvider.CONTENT_URI, CONTENT_NAME);
		public static final String	CONTENT_TYPE		= ContentResolver.CURSOR_DIR_BASE_TYPE + "/co.in.divi.db_apps";
		public static final String	CONTENT_ITEM_TYPE	= ContentResolver.CURSOR_ITEM_BASE_TYPE + "/co.in.divi.db_apps";

		public static final String	COLUMN_ID			= "_id";
		public static final String	COLUMN_PACKAGE		= "package";
		public static final String	COLUMN_NAME			= "name";
		public static final String	COLUMN_COURSE_ID	= "courseId";
		public static final String	COLUMN_BOOK_ID		= "bookId";
		public static final String	COLUMN_VERSION_CODE	= "versionCode";
		public static final String	COLUMN_APK_PATH		= "apkPath";
		public static final String	COLUMN_SHOW_IN_APPS	= "showInApps";
	}

	static final int		APPS_LIST	= 1;
	static final int		APPS_ID		= 2;

	static final UriMatcher	uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, "apps", APPS_LIST);
		uriMatcher.addURI(AUTHORITY, "apps/#", APPS_ID);
	}

	/**
	 * Helper class that actually creates and manages the provider's underlying data repository.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		static final String	DATABASE_NAME		= "Apps";
		static final String	APPS_TABLE_NAME		= "AllowedApps";
		static final int	DATABASE_VERSION	= 1;
		static final String	CREATE_DB_TABLE		= " CREATE TABLE " + APPS_TABLE_NAME + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
														+ " package TEXT NOT NULL, " + " name TEXT NOT NULL, "
														+ " courseId TEXT NOT NULL, " + " bookId TEXT NOT NULL, "
														+ " versionCode INTEGER, " + " showInApps INTEGER, " + " apkPath TEXT NOT NULL);";

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_DB_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + APPS_TABLE_NAME);
			onCreate(db);
		}
	}

	private SQLiteDatabase	db;
	private Handler			uiHandler;

	@Override
	public boolean onCreate() {
		Context context = getContext();
		uiHandler = new Handler();
		db = new DatabaseHelper(context).getWritableDatabase();
		return (db == null) ? false : true;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (LogConfig.DEBUG_USERDB)
			Log.d(TAG, "insert - " + uri + ", " + values.size());
		if (uriMatcher.match(uri) == APPS_LIST) {
			long rowID = db.insert(DatabaseHelper.APPS_TABLE_NAME, "", values);
			return getUriForId(rowID, uri);
		}
		throw new SQLException("Failed to add a record into " + uri);
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
		// Log.d(TAG, "thread is ui?" + (Looper.getMainLooper().getThread() == Thread.currentThread()));
		final CountDownLatch latch = new CountDownLatch(1);
		final Object allowedApps[] = new Object[] { null, null };
		final Boolean blackout[] = new Boolean[] { false };
		final ArrayList<String> courseIds = new ArrayList<String>();
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				try {
					// Log.d(TAG, "thread is ui?" + (Looper.getMainLooper().getThread() == Thread.currentThread()));
					UserSessionProvider userSessionProvider = UserSessionProvider.getInstance(getContext());
					if (!userSessionProvider.isLoggedIn())
						return;
					courseIds.addAll(Arrays.asList(userSessionProvider.getAllCourseIds()));
					LectureSessionProvider lectureSessionProvider = LectureSessionProvider.getInstance(getContext());
					if (lectureSessionProvider.isLectureJoined() && !lectureSessionProvider.isCurrentUserTeacher()) {
						if (lectureSessionProvider.isBlackout()) {
							blackout[0] = true;
						} else if (lectureSessionProvider.getInstructions() != null
								&& lectureSessionProvider.getInstructions().instructions != null) {
							int index = 0;
							for (LectureInstruction.Instruction i : lectureSessionProvider.getInstructions().instructions) {
								if (index > 1)
									break;
								Instruction instruction = new Gson().fromJson(i.data, Instruction.class);
								if (instruction.type == Instruction.INSTRUCTION_TYPE_NAVIGATE_EXTERNAL) {
									allowedApps[index] = instruction.location;
									index++;
								} else if (instruction.isVM) {
									// fetch the VM package
									try {
										DiviReference ref = new DiviReference(Uri.parse(instruction.location));
										allowedApps[index] = ref;
										index++;
									} catch (Exception e) {
										Log.w(TAG, "error fetching vm package");
									}
								}
							}
						}
					}
				} catch (Exception e) {
					Log.w(TAG, "error fetching allowed apps during live lecture", e);
				} finally {
					latch.countDown();
				}
			}
		});
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(DatabaseHelper.APPS_TABLE_NAME);
		switch (uriMatcher.match(uri)) {
		case APPS_LIST:
			break;
		case APPS_ID:
			builder.appendWhere(Apps.COLUMN_ID + " = " + uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		if (allowedApps[0] != null && allowedApps[0] instanceof DiviReference) {
			allowedApps[0] = fetchVMPackage((DiviReference) allowedApps[0]);
		}
		if (allowedApps[1] != null && allowedApps[1] instanceof DiviReference) {
			allowedApps[1] = fetchVMPackage((DiviReference) allowedApps[1]);
		}
		if (allowedApps[0] != null && allowedApps[1] != null)
			builder.appendWhere(Apps.COLUMN_PACKAGE + " IN ( '" + allowedApps[0] + "' , '" + allowedApps[1] + "')");
		else if (allowedApps[0] != null)
			builder.appendWhere(Apps.COLUMN_PACKAGE + " = '" + allowedApps[0] + "'");
		else {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			for (int i = 0; i < courseIds.size(); i++) {
				if (i > 0)
					sb.append(",");
				sb.append("'" + courseIds.get(i) + "'");
			}
			sb.append(")");
			builder.appendWhere(Apps.COLUMN_COURSE_ID + " IN " + sb.toString());
		}

		if (sortOrder == null || sortOrder == "") {
			sortOrder = Apps.COLUMN_NAME;
		}
		if (blackout[0] || courseIds.size() == 0)
			return null;
		Cursor cursor = builder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		// if we want to be notified of any changes:
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (LogConfig.DEBUG_USERDB)
			Log.d(TAG, "delete - " + uri + ", " + selection);
		int count = 0;

		switch (uriMatcher.match(uri)) {
		case APPS_LIST:
			count = db.delete(DatabaseHelper.APPS_TABLE_NAME, selection, selectionArgs);
			break;
		case APPS_ID:
			String id = uri.getPathSegments().get(1);
			count = db.delete(DatabaseHelper.APPS_TABLE_NAME, Apps.COLUMN_ID + " = " + id
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		if (count > 0 && !isInBatchMode()) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if (LogConfig.DEBUG_USERDB)
			Log.d(TAG, "update - " + uri + ", " + selection);
		// only purpose of this method is to raise notifications
		getContext().getContentResolver().notifyChange(uri, null);
		return 1;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		/**
		 * Get all apps records
		 */
		case APPS_LIST:
			return Apps.CONTENT_TYPE;
			/**
			 * Get a particular app
			 */
		case APPS_ID:
			return Apps.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	private final ThreadLocal<Boolean>	mIsInBatchMode	= new ThreadLocal<Boolean>();

	@Override
	public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
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

	private String fetchVMPackage(DiviReference ref) {
		try {
			Node vmTopicNode = co.in.divi.content.DatabaseHelper.getInstance(getContext()).getNode(ref.itemId, ref.bookId, ref.courseId);
			for (Topic.VM vmDef : ((Topic) vmTopicNode.tag).vms) {
				if (vmDef.id.equals(ref.subItemId)) {
					return vmDef.appPackage;
				}
			}
		} catch (Exception e) {
			Log.w(TAG, "error fetching vm package");
		}
		return null;
	}
}
