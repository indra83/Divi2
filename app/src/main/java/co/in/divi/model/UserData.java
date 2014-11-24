package co.in.divi.model;

import android.util.Log;

import com.google.gson.Gson;

public class UserData {
	public String		uid;
	public String		token;
	public String		name;
	public String		profilePic;
	public String		role;
	public long			time;
	public long			reportStartsAt;

	// error
	public Error		error;

	// place for metadata
	public String		metadata;

	public String		schoolName;

	public ClassRoom[]	classRooms;

	public static class ClassRoom {
		public String	classId;
		public String	className;
		public String	section;
		public Course[]	courses;

		@Override
		public String toString() {
			return className + " - " + section;
		}
	}

	public static class Course {
		public String	id;
		public String	name;
	}

	public static class Error {
		public String	message;
		public int		code;
	}

	public static class Metadata {
		public String		dropboxContentUrl;
		public SkipBook[]	skipBooks;

		public static class SkipBook {
			public String	courseId;
			public String	bookId;
		}
	}

	// try parse metadata
	public Metadata getMetadata() {
		try {
			Log.d("skip", "metadata:" + metadata);
			if (metadata != null && metadata.length() > 1) {
				Metadata m = new Gson().fromJson(metadata, Metadata.class);
				return m;
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	// helper function for display
	public String getSecondLine() {
		if (role.equalsIgnoreCase("student")) {
			String className = "";
			if (classRooms.length > 0) {
				className = classRooms[0].className + "   " + classRooms[0].section;
			}
			return className;
		} else
			return role;
	}

	public boolean isTeacher() {
		return "teacher".equals(role);
	}
}
