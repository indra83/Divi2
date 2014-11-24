package co.in.divi.vms.common;

import android.database.Cursor;

public class ChallengeAttempt {
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

	public static ChallengeAttempt fromCursor(Cursor cursor) {
		int courseId_index = cursor.getColumnIndex(DiviConstants.Attempt.COURSE_ID);
		int bookId_index = cursor.getColumnIndex(DiviConstants.Attempt.BOOK_ID);
		int assessmentId_index = cursor.getColumnIndex(DiviConstants.Attempt.ASSESSMENT_ID);
		int questionId_index = cursor.getColumnIndex(DiviConstants.Attempt.QUESTION_ID);
		int points_index = cursor.getColumnIndex(DiviConstants.Attempt.TOTAL_POINTS);
		int subquestions_index = cursor.getColumnIndex(DiviConstants.Attempt.SUBQUESTIONS);
		int correct_attempts_index = cursor.getColumnIndex(DiviConstants.Attempt.CORRECT_ATTEMPTS);
		int wrong_attempts_index = cursor.getColumnIndex(DiviConstants.Attempt.WRONG_ATTEMPTS);
		int data_index = cursor.getColumnIndex(DiviConstants.Attempt.DATA);
		int lastUpdated_index = cursor.getColumnIndex(DiviConstants.Attempt.LAST_UPDATED);
		int solvedAt_index = cursor.getColumnIndex(DiviConstants.Attempt.SOLVED_AT);

		ChallengeAttempt attempt = new ChallengeAttempt();
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(courseId).append(":").append(bookId).append(":").append(assessmentId).append(":").append(questionId);
		sb.append("---").append(totalPoints).append("/").append(subquestions).append("/").append(correctAttempts).append("/")
				.append(wrongAttempts);
		sb.append("  [").append(data).append("] ");
		return sb.toString();
	}
}
