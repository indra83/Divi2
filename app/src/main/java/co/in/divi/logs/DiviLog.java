package co.in.divi.logs;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import co.in.divi.LocationManager;
import co.in.divi.LocationManager.LOCATION_SUBTYPE;
import co.in.divi.content.DiviReference;
import co.in.divi.logs.LogsDBContract.Logs;
import co.in.divi.util.Util;

public class DiviLog implements Parcelable {

	public static final int					LOG_TYPE_TIMESPENT	= 0;
	public static final int					LOG_TYPE_NAVIGATION	= 1;

	public int								id;						// generated in DB

	public String							uid;
	public String							token;
	public String							uri;
	public int								type;

	// new properties
	public LocationManager.LOCATION_SUBTYPE	resourceType;

	public long								openedAt;
	public long								duration;

	public long								lastUpdatedAt;				// populated before db

	DiviLog() {
	}

	public DiviLog(String uid, String token, String uri, int type, LocationManager.LOCATION_SUBTYPE locType) {
		this.uid = uid;
		this.token = token;
		this.uri = uri;
		this.type = type;

		this.openedAt = Util.getTimestampMillis();

		this.resourceType = locType;
	}

	public void updateDuration() {
		this.duration = Util.getTimestampMillis() - this.openedAt;
	}

	public ContentValues toCV() {
		ContentValues values = new ContentValues();
		values.put(Logs.UID, uid);
		values.put(Logs.TOKEN, token);
		values.put(Logs.URI, uri);
		values.put(Logs.RESOURCE_TYPE, resourceType.toString());
		values.put(Logs.TYPE, type);
		values.put(Logs.OPENED_AT, openedAt);
		values.put(Logs.DURATION, duration);
		return values;
	}

	public static DiviLog fromCursor(Cursor cursor) {
		int uid_index = cursor.getColumnIndex(Logs.UID);
		int token_index = cursor.getColumnIndex(Logs.TOKEN);
		int uri_index = cursor.getColumnIndex(Logs.URI);
		int resourceType_index = cursor.getColumnIndex(Logs.RESOURCE_TYPE);
		int type_index = cursor.getColumnIndex(Logs.TYPE);
		int openedAt_index = cursor.getColumnIndex(Logs.OPENED_AT);
		int duration_index = cursor.getColumnIndex(Logs.DURATION);
		int lastUpdated_index = cursor.getColumnIndex(Logs.LAST_UPDATED);

		DiviLog log = new DiviLog();
		log.id = cursor.getInt(cursor.getColumnIndex(Logs._ID));
		log.uid = cursor.getString(uid_index);
		log.token = cursor.getString(token_index);
		log.uri = cursor.getString(uri_index);
		log.resourceType = LOCATION_SUBTYPE.valueOf(cursor.getString(resourceType_index));
		log.type = cursor.getInt(type_index);
		log.openedAt = cursor.getLong(openedAt_index);
		log.duration = cursor.getLong(duration_index);
		log.lastUpdatedAt = cursor.getLong(lastUpdated_index);

		return log;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		return sb.append(id).append(":::").append(uid).append(", ").append(uri).append(", ").append(resourceType).append(" - ")
				.append(duration).toString();
	}

	// Parcelable part

	private DiviLog(Parcel in) {
		String strs[] = new String[4];
		in.readStringArray(strs);
		uid = strs[0];
		token = strs[1];
		uri = strs[2];
		resourceType = LOCATION_SUBTYPE.valueOf(strs[3]);
		type = in.readInt();
		long longs[] = new long[2];
		in.readLongArray(longs);
		openedAt = longs[0];
		duration = longs[1];
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeStringArray(new String[] { uid, token, uri, resourceType.toString() });
		dest.writeInt(type);
		dest.writeLongArray(new long[] { openedAt, duration });
	}

	public static final Parcelable.Creator	CREATOR	= new Parcelable.Creator() {
														public DiviLog createFromParcel(Parcel in) {
															return new DiviLog(in);
														}

														public DiviLog[] newArray(int size) {
															return new DiviLog[size];
														}
													};
}
