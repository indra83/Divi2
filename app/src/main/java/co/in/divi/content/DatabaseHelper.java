package co.in.divi.content;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONException;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import co.in.divi.content.importer.BookDefinition;
import co.in.divi.content.importer.BookDefinition.ChapterDefinition;
import co.in.divi.content.importer.BookDefinition.TopicNode;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Util;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String		TAG						= DatabaseHelper.class.getSimpleName();

	// The Android's default system path of your application database.
	private static String			DB_PATH					= "/data/data/co.in.divi/databases/";

	private static final String		DATABASE_NAME			= "content_metadata.db";
	private static final int		DATABASE_VERSION		= 1;

	public static final String		TABLE_BOOKS				= "books";
	public static final String		KEY_BOOK_ID				= "_id";
	public static final String		KEY_BOOK_COURSE_ID		= "_course_id";
	public static final String		KEY_BOOK_NAME			= "name";
	public static final String		KEY_BOOK_ROOT_PATH		= "book_root_path";
	public static final String		KEY_BOOK_ORDER			= "order_no";
	public static final String		KEY_BOOK_SYNC			= "last_sync";
	public static final String		KEY_BOOK_VERSION		= "version";

	public static final String		TABLE_NODES				= "nodes2";
	public static final String		KEY_NODE_ID				= "_id";
	public static final String		KEY_NODE_COURSE_ID		= "_course_id";
	public static final String		KEY_NODE_BOOK_ID		= "_book_id";
	public static final String		KEY_NODE_PARENT_ID		= "_parent_id";
	public static final String		KEY_NODE_ORDER			= "order_no";
	public static final String		KEY_NODE_TYPE			= "type";
	public static final String		KEY_NODE_NAME			= "name";
	public static final String		KEY_NODE_PARENT_NAME	= "_parent_name";
	public static final String		KEY_NODE_CONTENT		= "content";
	public static final String		KEY_NODE_SYNC			= "last_sync";

	private static DatabaseHelper	instance				= null;

	public static DatabaseHelper getInstance(Context context) {
		if (instance == null) {
			if (LogConfig.DEBUG_DBHELPER)
				Log.d(TAG, "instantiating database helper");
			instance = new DatabaseHelper(context);

			boolean dbExist = checkDataBase();
			// check if we need to load initial db.
			if (!dbExist) {
				Log.i(TAG, "DB not found, creating(copying) db");
				instance.getReadableDatabase();
				instance.close();
				try {
					Log.d(TAG, "flushing database and loading initdata...");
					instance.loadInitData(context);
				} catch (IOException e) {
					Log.w(TAG, "loading initdata failed!");
					e.printStackTrace();
				}
				// load db from new file.
				instance = new DatabaseHelper(context);
			}
		}
		return instance;
	}

	private DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// runSQLScript(db, "database/create_db.sql");
		// data can't contain ';' as we split by it!
		// runSQLScript(db, "database/init_db.sql");
		// called only once so we don't need context any more
	}

	/**
	 * Copies your database from your local assets-folder to the just created empty database in the system folder, from
	 * where it can be accessed and handled. This is done by transfering bytestream.
	 * */
	private void loadInitData(Context context) throws IOException {
		// Open your local db as the input stream
		InputStream myInput = context.getAssets().open("initdb/" + DATABASE_NAME);
		// Path to the just created empty db
		String outFileName = DB_PATH + DATABASE_NAME;
		Util.copy(myInput, new File(outFileName));
	}

	/**
	 * Check if the database already exist to avoid re-copying the file each time you open the application.
	 * 
	 * @return true if it exists, false if it doesn't
	 */
	private static boolean checkDataBase() {
		// return false;
		SQLiteDatabase checkDB = null;
		try {
			String myPath = DB_PATH + DATABASE_NAME;
			checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
		} catch (SQLiteException e) {
			// database does't exist yet.
		}

		if (checkDB != null) {
			checkDB.close();
		}
		return checkDB != null ? true : false;
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	public ArrayList<Book> getBooks(String courseId) {
		if (LogConfig.DEBUG_DBHELPER)
			Log.d(TAG, "getting subjects for course - " + courseId);

		SQLiteDatabase db = getReadableDatabase();
		String selection = KEY_BOOK_COURSE_ID + "= ?";
		Cursor cursor = db.query(TABLE_BOOKS, null, selection, new String[] { courseId }, null, null, null);

		ArrayList<Book> subjects = new ArrayList<Book>();
		try {
			if (cursor.moveToFirst()) {
				do {
					subjects.add(Book.fromDBRow(cursor));
				} while (cursor.moveToNext());
			}

		} catch (IllegalArgumentException iae) {
			Log.e(TAG, "error getting subjects!", iae);
		}
		cursor.close();
		if (LogConfig.DEBUG_DBHELPER)
			Log.d(TAG, "found subjects - " + subjects.size());

		return subjects;
	}

	public Node[] getChapterNodes(String courseId, String bookId, int childType) {
		if (LogConfig.DEBUG_DBHELPER)
			Log.d(TAG, "getting chapters for - " + bookId);
		// TODO: validation and escaping
		SQLiteDatabase db = getReadableDatabase();
		String chapterSelection = KEY_NODE_COURSE_ID + "= ? AND " + KEY_NODE_BOOK_ID + "= ? AND type = ?";
		Cursor cursor = db.query(TABLE_NODES, null, chapterSelection, new String[] { courseId, bookId, "chapter" }, null, null, "order_no");

		ArrayList<Node> chapters = new ArrayList<Node>();
		HashMap<String, Node> chaptersDict = new HashMap<String, Node>();
		try {
			if (cursor.moveToFirst()) {
				do {
					Node node = Node.fromDBRow(cursor);
					chapters.add(node);
					chaptersDict.put(node.id, node);
				} while (cursor.moveToNext());
			}
		} catch (IllegalArgumentException iae) {
			Log.e(TAG, "error getting chapters", iae);
			throw new RuntimeException("Book is corrupt");
		} catch (JSONException jsone) {
			Log.e(TAG, "error getting chapters", jsone);
			throw new RuntimeException("Book is corrupt");
		} finally {
			cursor.close();
		}

		// fill up children in chapters (topics or exercises)
		String topicSelection = KEY_NODE_COURSE_ID + "= ? AND " + KEY_NODE_BOOK_ID + "= ? AND type = ?";
		cursor = db.query(TABLE_NODES, null, topicSelection, new String[] { courseId, bookId, Node.getNodeTypeName(childType) }, null,
				null, "order_no");
		HashMap<String, Node> level2Dict = new HashMap<String, Node>();
		try {
			if (cursor.moveToFirst()) {
				do {
					Node node = Node.fromDBRow(cursor);
					if (LogConfig.DEBUG_DBHELPER)
						Log.d(TAG, "found child=" + node.id + ",adding to parent=" + node.parentId);
					node.setParent(chaptersDict.get(node.parentId));
					node.getParent().addChild(node);
					level2Dict.put(node.id, node);
				} while (cursor.moveToNext());
			}
		} catch (IllegalArgumentException iae) {
			Log.e(TAG, "error getting chapters", iae);
			throw new RuntimeException("Book is corrupt");
		} catch (JSONException jsone) {
			Log.e(TAG, "error getting chapters", jsone);
			throw new RuntimeException("Book is corrupt");
		} finally {
			cursor.close();
		}

		// if topics, fill resources
		// if (childType == Node.NODE_TYPE_TOPIC) {
		// String resourceSelection = KEY_NODE_COURSE_ID + "= ? AND " + KEY_NODE_BOOK_ID + "= ? AND type = ?";
		// cursor = db.query(TABLE_NODES, null, resourceSelection,
		// new String[] { courseId, bookId, Node.getNodeTypeName(Node.NODE_TYPE_RESOURCE) }, null, null, "order_no");
		// try {
		// if (cursor.moveToFirst()) {
		// do {
		// Node node = Node.fromDBRow(cursor);
		// Log.d(TAG, "found child=" + node.id + ",adding to parent=" + node.parentId);
		// node.setParent(level2Dict.get(node.parentId));
		// node.getParent().addChild(node);
		// } while (cursor.moveToNext());
		// }
		// } catch (IllegalArgumentException iae) {
		// Log.e(TAG, "error getting chapters", iae);
		// throw new RuntimeException("Book is corrupt");
		// } catch (JSONException jsone) {
		// Log.e(TAG, "error getting chapters", jsone);
		// throw new RuntimeException("Book is corrupt");
		// } finally {
		// cursor.close();
		// }
		// }

		if (LogConfig.DEBUG_DBHELPER)
			Log.d(TAG, "found chapters - " + chapters.size());
		return chapters.toArray(new Node[chapters.size()]);
	}

	public Node getNode(String nodeId, String bookId, String courseId) {
		// parent & children are not populated
		if (LogConfig.DEBUG_DBHELPER)
			Log.d(TAG, "getting node - " + nodeId + ",b:" + bookId + ",c:" + courseId);
		// TODO: validation and escaping
		SQLiteDatabase db = getReadableDatabase();
		String selection = KEY_NODE_ID + "= ? AND " + KEY_NODE_BOOK_ID + "= ? AND " + KEY_NODE_COURSE_ID + "= ?";
		Cursor cursor = db.query(TABLE_NODES, null, selection, new String[] { nodeId, bookId, courseId }, null, null, null);

		Node node = null;
		try {
			if (cursor.moveToFirst()) {
				node = Node.fromDBRow(cursor);
			}
		} catch (IllegalArgumentException iae) {
			Log.e(TAG, "error getting chapters", iae);
			throw new RuntimeException("Book is corrupt");
		} catch (JSONException jsone) {
			Log.e(TAG, "error getting chapters", jsone);
			throw new RuntimeException("Book is corrupt");
		} finally {
			cursor.close();
		}

		if (LogConfig.DEBUG_DBHELPER && node != null)
			Log.d(TAG, "found node - " + node.name);
		return node;
	}

	// public Exercise getExercise(String exerciseId) {
	// if (Config.DEBUG)
	// Log.d(TAG, "getting exercise - " + exerciseId);
	// // TODO: validation and escaping
	// SQLiteDatabase db = getReadableDatabase();
	// String selection = KEY_EXERCISE_ID + "= ?";
	// Cursor cursor = db.query(TABLE_EXERCISES, null, selection,
	// new String[] { exerciseId }, null, null, null);
	//
	// Exercise exercise = null;
	// try {
	// if (cursor.moveToFirst()) {
	// exercise = Exercise.fromDBRow(cursor);
	// // exercise.questions = getQuestions(exercise);
	// }
	// } catch (IllegalArgumentException iae) {
	// Log.e(TAG, "error getting Exercise", iae);
	// }
	// cursor.close();
	//
	// if (Config.DEBUG && exercise != null)
	// Log.d(TAG, "found exercise - " + exercise.name);
	// return exercise;
	// }

	// public ArrayList<Question> getQuestions(Node exercise) {
	// String exerciseId = exercise.id;
	// if (Config.DEBUG)
	// Log.d(TAG, "getting questions for exercise - " + exerciseId);
	//
	// SQLiteDatabase db = getReadableDatabase();
	// String selection = KEY_QUESTION_EXERCISE_ID + "= ?";
	// Cursor cursor = db.query(TABLE_QUESTIONS, null, selection, new String[] { exerciseId }, null, null, null);
	//
	// ArrayList<Question> questions = new ArrayList<Question>();
	// try {
	// if (cursor.moveToFirst()) {
	// do {
	// questions.add(Question.fromDBRow(cursor));
	// } while (cursor.moveToNext());
	// }
	//
	// } catch (IllegalArgumentException iae) {
	// Log.e(TAG, "error getting questions!", iae);
	// }
	// cursor.close();
	// if (Config.DEBUG)
	// Log.d(TAG, "found questions - " + questions.size());
	//
	// return questions;
	// }

	/*
	 * importing data into db..
	 */
	public boolean resetBook(BookDefinition bookDef) {
		if (LogConfig.DEBUG_DBHELPER)
			Log.d(TAG, "reseting book from db:" + bookDef.name);
		SQLiteDatabase db = null;
		try {
			db = getWritableDatabase();
			db.beginTransaction();
			try {
				int count = db.delete(TABLE_BOOKS, KEY_BOOK_ID + " = ? AND " + KEY_BOOK_COURSE_ID + " = ?", new String[] { bookDef.bookId,
						bookDef.courseId });
				Log.d(TAG, "deleting book:" + count);

				count = db.delete(TABLE_NODES, KEY_NODE_BOOK_ID + " = ? AND " + KEY_NODE_COURSE_ID + " = ?", new String[] { bookDef.bookId,
						bookDef.courseId });
				Log.d(TAG, "deleting nodes:" + count);

				Log.d(TAG, "book reset successful");
				db.setTransactionSuccessful();
				return true;
			} catch (Exception e) {
				Log.w(TAG, "error reseting data", e);
			} finally {
				if (LogConfig.DEBUG_DBHELPER)
					Log.d(TAG, "finishing transaction");
				db.endTransaction();
			}
		} catch (Exception e) {
			Log.w(TAG, "reseting book failed", e);
		} finally {
			if (LogConfig.DEBUG_DBHELPER)
				Log.d(TAG, "closing writable db");
			if (db != null)
				db.close();
		}
		return false;
	}

	@SuppressLint("SimpleDateFormat")
	public boolean insertBook(BookDefinition bookDef, String tagsJSON) {
		if (LogConfig.DEBUG_DBHELPER)
			Log.d(TAG, "inserting book into db:" + bookDef.name);
		SQLiteDatabase db = null;
		try {
			db = getWritableDatabase();
			db.beginTransaction();
			try {
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date date = new Date();
				// insert the book details
				ContentValues bookEntry = new ContentValues();
				bookEntry.put(KEY_BOOK_ID, bookDef.bookId);
				bookEntry.put(KEY_BOOK_COURSE_ID, bookDef.courseId);
				bookEntry.put(KEY_BOOK_NAME, bookDef.name);
				bookEntry.put(KEY_BOOK_ORDER, bookDef.orderNo);
				bookEntry.put(KEY_BOOK_ROOT_PATH, tagsJSON);// !! name mismatch !!
				bookEntry.put(KEY_BOOK_VERSION, bookDef.version);
				bookEntry.put(KEY_BOOK_SYNC, dateFormat.format(date));

				db.replace(TABLE_BOOKS, null, bookEntry);

				// insert the nodes (chapters, topics and exercises)
				for (int i = 0; i < bookDef.chapters.length; i++) {
					ChapterDefinition chapterNode = bookDef.chapters[i];
					if (LogConfig.DEBUG_DBHELPER)
						Log.d(TAG, "      inserting chapter:" + chapterNode.name);
					ContentValues chapterEntry = new ContentValues();
					chapterEntry.put(KEY_NODE_ID, chapterNode.id);
					chapterEntry.put(KEY_NODE_BOOK_ID, bookDef.bookId);
					// chapterEntry.put(KEY_NODE_PARENT_ID, null);
					chapterEntry.put(KEY_NODE_ORDER, i);
					chapterEntry.put(KEY_NODE_TYPE, Node.getNodeTypeName(Node.NODE_TYPE_CHAPTER));
					chapterEntry.put(KEY_NODE_NAME, chapterNode.name);
					chapterEntry.put(KEY_NODE_CONTENT, "blah");// not used
					chapterEntry.put(KEY_NODE_SYNC, dateFormat.format(date));
					chapterEntry.put(KEY_NODE_COURSE_ID, bookDef.courseId);

					db.replace(TABLE_NODES, null, chapterEntry);

					for (int j = 0; j < chapterNode.topicNodes.size(); j++) {
						TopicNode topicNode = chapterNode.topicNodes.get(j);
						if (LogConfig.DEBUG_DBHELPER) {
							Log.d(TAG, "             inserting topic:" + topicNode.name);
							Log.d(TAG, "                -" + topicNode.content);
						}
						ContentValues topicEntry = new ContentValues();
						topicEntry.put(KEY_NODE_ID, topicNode.id);
						topicEntry.put(KEY_NODE_BOOK_ID, bookDef.bookId);
						topicEntry.put(KEY_NODE_PARENT_ID, chapterNode.id);
						topicEntry.put(KEY_NODE_ORDER, j);
						topicEntry.put(KEY_NODE_TYPE, Node.getNodeTypeName(Node.NODE_TYPE_TOPIC));
						topicEntry.put(KEY_NODE_NAME, topicNode.name);
						topicEntry.put(KEY_NODE_PARENT_NAME, chapterNode.name);
						topicEntry.put(KEY_NODE_CONTENT, topicNode.content);
						topicEntry.put(KEY_NODE_SYNC, dateFormat.format(date));
						topicEntry.put(KEY_NODE_COURSE_ID, bookDef.courseId);

						db.replace(TABLE_NODES, null, topicEntry);
					}

					// insert exercises
					for (int j = 0; j < chapterNode.assessmentNodes.size(); j++) {
						AssessmentFileModel assessment = chapterNode.assessmentNodes.get(j);
						if (LogConfig.DEBUG_DBHELPER)
							Log.d(TAG, "             inserting assessment:" + assessment.name);
						ContentValues exerciseEntry = new ContentValues();
						exerciseEntry.put(KEY_NODE_ID, assessment.assessmentId);
						exerciseEntry.put(KEY_NODE_BOOK_ID, bookDef.bookId);
						exerciseEntry.put(KEY_NODE_PARENT_ID, chapterNode.id);
						exerciseEntry.put(KEY_NODE_ORDER, j);
						exerciseEntry.put(KEY_NODE_TYPE, Node.getNodeTypeName(Node.NODE_TYPE_ASSESSMENT));
						exerciseEntry.put(KEY_NODE_NAME, assessment.name);
						exerciseEntry.put(KEY_NODE_PARENT_NAME, chapterNode.name);
						exerciseEntry.put(KEY_NODE_CONTENT, assessment.content);
						exerciseEntry.put(KEY_NODE_SYNC, dateFormat.format(date));
						exerciseEntry.put(KEY_NODE_COURSE_ID, bookDef.courseId);

						db.replace(TABLE_NODES, null, exerciseEntry);

						// for (int k = 0; k < assessment.questions.length; k++) {
						// AssessmentFileModel.Question question = assessment.questions[k];
						// if (Config.DEBUG)
						// Log.d(TAG, "             inserting question:" + question.id);
						// ContentValues questionEntry = new ContentValues();
						// questionEntry.put(KEY_QUESTION_ID, question.id);
						// questionEntry.put(KEY_QUESTION_EXERCISE_ID, assessment.id);
						// questionEntry.put(KEY_QUESTION_BOOK_ID, bookDef.bookId);
						// questionEntry.put(KEY_QUESTION_CONTENT, question.content);
						// questionEntry.put(KEY_QUESTION_ANSWER, question.answer);
						// questionEntry.put(KEY_QUESTION_SYNC, dateFormat.format(date));
						// questionEntry.put(KEY_QUESTION_ORDER, k);
						// questionEntry.put(KEY_QUESTION_COURSE_ID, bookDef.courseId);
						//
						// db.replace(TABLE_QUESTIONS, null, questionEntry);
						// }
					}
				}
				if (LogConfig.DEBUG_DBHELPER)
					Log.d(TAG, "book insert successful");
				db.setTransactionSuccessful();
				return true;
			} catch (Exception e) {
				Log.w(TAG, "error inserting data", e);
			} finally {
				if (LogConfig.DEBUG_DBHELPER)
					Log.d(TAG, "finishing transaction");
				db.endTransaction();
			}
		} catch (Exception e) {
			Log.w(TAG, "importing book failed", e);
		} finally {
			if (LogConfig.DEBUG_DBHELPER)
				Log.d(TAG, "closing writable db");
			if (db != null)
				db.close();
		}
		return false;
	}

	/*
	 * Clean the database (helper for testing).
	 */
	public boolean cleanDatabase() {
		if (LogConfig.DEBUG_DBHELPER)
			Log.d(TAG, "CLEANING DB!!");
		SQLiteDatabase db = null;
		try {
			db = getWritableDatabase();
			db.beginTransaction();
			try {
				int count = db.delete(TABLE_BOOKS, null, null);
				Log.d(TAG, "deleted books count: " + count);

				count = db.delete(TABLE_NODES, null, null);
				Log.d(TAG, "deleted nodes count: " + count);

				Log.d(TAG, "clean successful");
				db.setTransactionSuccessful();
				return true;
			} catch (Exception e) {
				Log.w(TAG, "error cleaning db", e);
			} finally {
				if (LogConfig.DEBUG_DBHELPER)
					Log.d(TAG, "finishing transaction");
				db.endTransaction();
			}
		} catch (Exception e) {
			Log.w(TAG, "cleaning db failed", e);
		} finally {
			if (LogConfig.DEBUG_DBHELPER)
				Log.d(TAG, "closing writable db");
			if (db != null)
				db.close();
		}
		return false;
	}
}