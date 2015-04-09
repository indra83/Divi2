package co.in.divi.db.model;

import android.content.ContentValues;
import android.database.Cursor;
import co.in.divi.db.UserDBContract.Commands;

public class Command {
	public static final int	COMMAND_CATEGORY_UNLOCK		= 1;
    public static final int	COMMAND_CATEGORY_DIARY		= 2;

	public static final int	COMMAND_UNLOCK_ITEM_CATEGORY_QUIZ	= 1;
	public static final int	COMMAND_UNLOCK_ITEM_CATEGORY_TEST	= 2;

    public static final int	COMMAND_DIARY_ITEM_CATEGORY_HOMEWORK	= 101;
    public static final int	COMMAND_DIARY_ITEM_CATEGORY_ANNOUNCEMENT= 102;

	public static final int	COMMAND_STATUS_ACTIVE		= 1;
	public static final int	COMMAND_STATUS_DELETE		= 2;

	public String			id;
	public String			uid;
	public String			classRoomId;
	public String			teacherId;
	public String			courseId;
	public String			bookId;
	public String			itemCode;
	public int				category;
	public int				itemCategory;
	public int				status;

	public String			data;
	public long				createdAt;
	public long				appliedAt;
	public long				endsAt;

	public long				updatedAt;

	public ContentValues toCV(String uid) {
		ContentValues values = new ContentValues();
		values.put(Commands.ID, id);
		values.put(Commands.UID, uid);
		values.put(Commands.COURSE_ID, courseId);
		values.put(Commands.BOOK_ID, bookId);
		values.put(Commands.ITEM_ID, itemCode);
		values.put(Commands.TEACHER_ID, teacherId);
		values.put(Commands.CLASS_ID, classRoomId);
		values.put(Commands.TYPE, category);
		values.put(Commands.ITEM_TYPE, itemCategory);
		values.put(Commands.STATUS, category);
		values.put(Commands.DATA, data);
		values.put(Commands.CREATE_TIMESTAMP, createdAt);
		values.put(Commands.APPLY_TIMESTAMP, appliedAt);
		values.put(Commands.END_TIMESTAMP, endsAt);
		values.put(Commands.LAST_UPDATED, updatedAt);

		return values;
	}

	public static Command fromCursor(Cursor cursor) {
		int id_index = cursor.getColumnIndex(Commands.ID);
		int uid_index = cursor.getColumnIndex(Commands.UID);
		int classId_index = cursor.getColumnIndex(Commands.CLASS_ID);
		int teacherId_index = cursor.getColumnIndex(Commands.TEACHER_ID);
		int courseId_index = cursor.getColumnIndex(Commands.COURSE_ID);
		int bookId_index = cursor.getColumnIndex(Commands.BOOK_ID);
		int itemId_index = cursor.getColumnIndex(Commands.ITEM_ID);
		int data_index = cursor.getColumnIndex(Commands.DATA);
		int type_index = cursor.getColumnIndex(Commands.TYPE);
		int itemType_index = cursor.getColumnIndex(Commands.ITEM_TYPE);
		int status_index = cursor.getColumnIndex(Commands.STATUS);
		int createTimestamp_index = cursor.getColumnIndex(Commands.CREATE_TIMESTAMP);
		int applyTimestamp_index = cursor.getColumnIndex(Commands.APPLY_TIMESTAMP);
		int endTimestamp_index = cursor.getColumnIndex(Commands.END_TIMESTAMP);
		int lastUpdated_index = cursor.getColumnIndex(Commands.LAST_UPDATED);

		Command c = new Command();
		c.id = cursor.getString(id_index);
		c.uid = cursor.getString(uid_index);
		c.classRoomId = cursor.getString(classId_index);
		c.teacherId = cursor.getString(teacherId_index);
		c.courseId = cursor.getString(courseId_index);
		c.bookId = cursor.getString(bookId_index);
		c.itemCode = cursor.getString(itemId_index);
		c.category = cursor.getInt(type_index);
		c.itemCategory = cursor.getInt(itemType_index);
		c.status = cursor.getInt(status_index);
		c.data = cursor.getString(data_index);
		c.createdAt = cursor.getLong(createTimestamp_index);
		c.appliedAt = cursor.getLong(applyTimestamp_index);
		c.endsAt = cursor.getLong(endTimestamp_index);
		c.updatedAt = cursor.getLong(lastUpdated_index);
		return c;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("id:").append(id).append(", uid:").append(uid).append(". classId:").append(classRoomId).append(", teacherId:")
				.append(teacherId).append(",courseId:").append(courseId).append(",bookId:").append(bookId).append(",itemCode:")
				.append(itemCode).append(", category:").append(category).append(",itemCategory:").append(itemCategory).append(", status:")
				.append(status).append(",appAt").append(appliedAt).append(",endsAt:").append(endsAt);
		return sb.toString();
	}
}
