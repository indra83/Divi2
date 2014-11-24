package co.in.divi.fragment.questions;

import co.in.divi.activity.AssessmentActivity.AssessmentMode;
import android.os.Bundle;
import android.util.Log;

public final class QuestionFragmentFactory {
	private static final String	TAG						= "QuestionFragmentFactory";

	public static final String	QUESTION_TYPE_MCQ		= "mcq";
	public static final String	QUESTION_TYPE_TORF		= "torf";
	public static final String	QUESTION_TYPE_LABEL		= "label";
	public static final String	QUESTION_TYPE_FILLBLANK	= "fill_blank";
	public static final String	QUESTION_TYPE_MATCH		= "match";
	public static final String	QUESTION_TYPE_VOCAB		= "vocab";

	public static BaseQuestionFragment getQuestionFragment(AssessmentMode mode, String courseId, String bookId, String assessmentId,
			String questionType, int questionIndex) {
		BaseQuestionFragment frag = getQuestionFragment(mode, questionType);
		Bundle bundle = new Bundle(2);
		bundle.putInt(BaseQuestionFragment.QUESTION_NO, questionIndex);
		bundle.putString(BaseQuestionFragment.ASSESSMENT_ID, assessmentId);
		bundle.putString(BaseQuestionFragment.BOOK_ID, bookId);
		bundle.putString(BaseQuestionFragment.COURSE_ID, courseId);
		frag.setArguments(bundle);
		return frag;
	}

	private static BaseQuestionFragment getQuestionFragment(AssessmentMode mode, String questionType) {
		try {
			if (QUESTION_TYPE_MCQ.equals(questionType) || QUESTION_TYPE_TORF.equals(questionType)) {
				return new MCQ_QuestionFragment();
			} else if (QUESTION_TYPE_LABEL.equals(questionType)) {
				switch (mode) {
				case DISCOVER_ANSWER_BEFORE:
				case TEST_BEFORE:
					throw new RuntimeException("Not allowed?");
				case DISCOVER_ANSWER_DURING:
				case TEACHER:
					return new Label_QuestionFragment();
				case TEST_AFTER:
				case TEST_DURING:
					return new Label_testQuestionFragment();
				default:
					throw new RuntimeException("Never happen");
				}
			} else if (QUESTION_TYPE_FILLBLANK.equals(questionType)) {
				return new FillBlank_QuestionFragment();
			} else if (QUESTION_TYPE_MATCH.equals(questionType)) {
				switch (mode) {
				case DISCOVER_ANSWER_BEFORE:
				case TEST_BEFORE:
					throw new RuntimeException("Not allowed?");
				case DISCOVER_ANSWER_DURING:
				case TEACHER:
					return new Match_QuestionFragment();
				case TEST_AFTER:
				case TEST_DURING:
					return new Match_testQuestionFragment();
				default:
					throw new RuntimeException("Never happen");
				}
			} else if (QUESTION_TYPE_VOCAB.equals(questionType)) {
				return new Vocab_QuestionFragment();
			} else {
				Log.w(TAG, "unknown question type:" + questionType);
				return new Error_QuestionFragment();
			}
		} catch (Exception e) {
			Log.e(TAG, "error creating question fragment", e);
			return new Error_QuestionFragment();
		}
	}
}
