package co.in.divi.logs;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class LogsDBOpenHelper extends SQLiteOpenHelper {

	private static final String	NAME	= LogsDBSchema.DB_NAME;
	private static final int	VERSION	= 3;

	public LogsDBOpenHelper(Context context) {
		super(context, NAME, null, VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(LogsDBSchema.DDL_CREATE_TBL_LOGS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// throw new RuntimeException("Database updgrade not yet supported!");
		db.execSQL(LogsDBSchema.DDL_DROP_TBL_LOGS);
		onCreate(db);
		// onUpgrade(db, oldVersion, newVersion);
	}
}
