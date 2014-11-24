package co.in.divi.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class UserDBOpenHelper extends SQLiteOpenHelper {

	private static final String	NAME	= UserDBSchema.DB_NAME;
	private static final int	VERSION	= 11;

	public UserDBOpenHelper(Context context) {
		super(context, NAME, null, VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(UserDBSchema.DDL_CREATE_TBL_ATTEMPTS);
		db.execSQL(UserDBSchema.DDL_CREATE_TBL_COMMANDS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// throw new RuntimeException("Database updgrade not yet supported!");
		db.execSQL(UserDBSchema.DDL_DROP_TBL_ATTEMPTS);
		db.execSQL(UserDBSchema.DDL_DROP_TBL_COMMANDS);
		onCreate(db);
		// onUpgrade(db, oldVersion, newVersion);
	}

}
