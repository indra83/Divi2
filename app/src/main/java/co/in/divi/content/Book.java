package co.in.divi.content;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

public class Book implements Parcelable {

	public String	id;
	public String	courseId;
	public String	name;
	public int		order;
	public String	bookTags;	// !! name in db table is different!!
	public int		version;

    public String streamUrl = "";
    public String encryptPassPhrase = "";

	private Book() {
	}

	public static Book fromDBRow(Cursor cursor) {
		Book subject = new Book();
		subject.id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_BOOK_ID));
		subject.courseId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_BOOK_COURSE_ID));
		subject.name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_BOOK_NAME));
		subject.order = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_BOOK_ORDER));
		subject.bookTags = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_BOOK_ROOT_PATH));
		subject.version = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_BOOK_VERSION));

		// handle books with empty tags
		if (subject.bookTags == null || subject.bookTags.length() < 2) {
			subject.bookTags = "[]";// empty json array;
		}
		return subject;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(id);
		dest.writeString(courseId);
		dest.writeString(name);
		dest.writeInt(order);
		dest.writeString(bookTags);
		dest.writeInt(version);
	}

	private Book(Parcel in) {
		readFromParcel(in);
	}

	private void readFromParcel(Parcel in) {
		id = in.readString();
		courseId = in.readString();
		name = in.readString();
		order = in.readInt();
		bookTags = in.readString();
		version = in.readInt();
	}

	public static final Parcelable.Creator	CREATOR	= new Parcelable.Creator() {
														public Book createFromParcel(Parcel in) {
															return new Book(in);
														}

														public Book[] newArray(int size) {
															return new Book[size];
														}
													};
}
