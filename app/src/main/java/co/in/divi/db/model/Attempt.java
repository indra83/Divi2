package co.in.divi.db.model;

import android.database.Cursor;
import co.in.divi.db.UserDBContract.Attempts;

public class Attempt {
	public String	courseId;
	public String	bookId;
	public String	assessmentId;
	public String	questionId;
	public int		totalPoints;
	public int		subquestions;
	public int		correctAttempts;
	public int		wrongAttempts;
	public String	data;
	public long		lastUpdatedAt;
	public long		solvedAt;

	public boolean isSolved() {
		return subquestions == correctAttempts;
	}

	public boolean isAttempted() {
		return (correctAttempts + wrongAttempts > 0);
	}

	public int getCurrentPoints() {
		if (subquestions > 0) {
			return ((totalPoints * correctAttempts) / subquestions);
		}
		return 0;
	}

	public double getCurrentAccuracy() {
		if (correctAttempts + wrongAttempts > 0) {
			return (100.0 * correctAttempts) / (correctAttempts + wrongAttempts);
		}
		return 0;
	}

	public static Attempt fromCursor(Cursor cursor) {
		int courseId_index = cursor.getColumnIndex(Attempts.COURSE_ID);
		int bookId_index = cursor.getColumnIndex(Attempts.BOOK_ID);
		int assessmentId_index = cursor.getColumnIndex(Attempts.ASSESSMENT_ID);
		int questionId_index = cursor.getColumnIndex(Attempts.QUESTION_ID);
		int points_index = cursor.getColumnIndex(Attempts.TOTAL_POINTS);
		int subquestions_index = cursor.getColumnIndex(Attempts.SUBQUESTIONS);
		int correct_attempts_index = cursor.getColumnIndex(Attempts.CORRECT_ATTEMPTS);
		int wrong_attempts_index = cursor.getColumnIndex(Attempts.WRONG_ATTEMPTS);
		int data_index = cursor.getColumnIndex(Attempts.DATA);
		int lastUpdated_index = cursor.getColumnIndex(Attempts.LAST_UPDATED);
		int solvedAt_index = cursor.getColumnIndex(Attempts.SOLVED_AT);

		Attempt attempt = new Attempt();
		attempt.courseId = cursor.getString(courseId_index);
		attempt.bookId = cursor.getString(bookId_index);
		attempt.assessmentId = cursor.getString(assessmentId_index);
		attempt.questionId = cursor.getString(questionId_index);
		attempt.totalPoints = cursor.getInt(points_index);
		attempt.subquestions = cursor.getInt(subquestions_index);
		attempt.correctAttempts = cursor.getInt(correct_attempts_index);
		attempt.wrongAttempts = cursor.getInt(wrong_attempts_index);
		attempt.data = cursor.getString(data_index);
		attempt.lastUpdatedAt = cursor.getLong(lastUpdated_index);
		attempt.solvedAt = cursor.getLong(solvedAt_index);

		return attempt;
	}
}