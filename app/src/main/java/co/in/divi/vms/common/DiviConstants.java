package co.in.divi.vms.common;

import android.net.Uri;

public final class DiviConstants {

	public static final String	LOCATION_BROADCASTLISTENER_NAME	= "";

	public static final String	LOCATION_COURSE_ID				= "COURSE_ID";
	public static final String	LOCATION_BOOK_ID				= "BOOK_ID";
	public static final String	LOCATION_TOPIC_ID				= "TOPIC_ID";
	public static final String	LOCATION_VM_ID					= "VM_ID";
	public static final String	LOCATION_BREADCRUMB				= "BREADCRUMB";

	public static final class Attempt {
		public static final String	CONTENT_NAME		= "attempts";
		public static final Uri		CONTENT_URI			= Uri.withAppendedPath(Uri.parse("content://co.in.divi.userdb"), CONTENT_NAME);

		// columns
		public static final String	UID					= "uid";
		public static final String	COURSE_ID			= "course_id";
		public static final String	BOOK_ID				= "book_id";
		public static final String	ASSESSMENT_ID		= "assessment_id";
		public static final String	QUESTION_ID			= "question_id";
		public static final String	TOTAL_POINTS		= "total_points";
		public static final String	SUBQUESTIONS		= "subquestions";
		public static final String	CORRECT_ATTEMPTS	= "correct_attempts";
		public static final String	WRONG_ATTEMPTS		= "wrong_attempts";
		public static final String	DATA				= "data";
		public static final String	SOLVED_AT			= "solved_at";
		public static final String	LAST_UPDATED		= "last_updated";
	}

}
