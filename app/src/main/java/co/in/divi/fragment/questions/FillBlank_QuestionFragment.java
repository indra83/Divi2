package co.in.divi.fragment.questions;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import co.in.divi.R;
import co.in.divi.activity.AssessmentActivity.AssessmentMode;
import co.in.divi.content.questions.FillBlank_Question;
import co.in.divi.content.questions.FillBlank_QuestionXmlParser;
import co.in.divi.db.model.Attempt;
import co.in.divi.fragment.questions.state.FillBlank_AttemptData;
import co.in.divi.ui.FillBlank_LinearLayout;
import co.in.divi.ui.FillBlank_LinearLayout.FillBlankAttemptInterface;
import co.in.divi.util.TextUtil;

public class FillBlank_QuestionFragment extends BaseQuestionFragment {

	TextView							questionTV;
	LinearLayout						blanksLayout;

	ArrayList<FillBlank_LinearLayout>	blanksArray;

	FillBlank_Question					fb_question;
	boolean								latestAttempt	= false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		blanksArray = new ArrayList<FillBlank_LinearLayout>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.q_fragment_fillblank, container, false);
		questionTV = (TextView) rootView.findViewById(R.id.question);
		blanksLayout = (LinearLayout) rootView.findViewById(R.id.blanks);
		pointsTV = (TextView) rootView.findViewById(R.id.points_text);
		qnoTV = (TextView) rootView.findViewById(R.id.qno_text);
		attemptsTV = (TextView) rootView.findViewById(R.id.attempts_text);
		tick = (ImageView) rootView.findViewById(R.id.tick);

		return rootView;
	}

	@Override
	Object getQuestionData() {
		return new FillBlank_QuestionXmlParser().getQuestionFromXml(questionXmlFile, ImageGetter);
	}

	@Override
	void fillUI(Object questionData) {
		fb_question = (FillBlank_Question) questionData;
		questionTV.setText(fb_question.questionHTML);
		blanksLayout.removeAllViews();
		for (String subanswer : fb_question.blanks) {
			FillBlank_LinearLayout blank = (FillBlank_LinearLayout) ((LayoutInflater) activity
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.q_item_fillblank_word, null);
			blank.init(subanswer.trim(), 0, attemptInterface, getMode());
			blanksLayout.addView(blank);
			blanksArray.add(blank);
		}
	}

	@Override
	void loadAttempt(Attempt attempt) {
		if (attempt != null) {
			FillBlank_AttemptData data = gson.fromJson(attempt.data, FillBlank_AttemptData.class);
			if (data.attempts == null)
				data.attempts = new String[data.solved.length];
			isSolved = true;
			for (int i = 0; i < blanksArray.size(); i++) {
				isSolved = isSolved && data.solved[i];
				if (data.solved[i])
					blanksArray.get(i).setSolved();
				else {
					blanksArray.get(i).setAttempt(data.attempts[i]);
				}
			}
			if (isSolved) {
				pointsTV.setText(TextUtil.getPointsText(attempt.totalPoints, attempt.totalPoints));
			} else {
				int proratedPoints = (attempt.totalPoints * attempt.correctAttempts) / (attempt.subquestions);
				pointsTV.setText("Points  " + proratedPoints + "/" + attempt.totalPoints);
			}
			attemptsTV.setText(TextUtil.getAttemptsText(attempt.correctAttempts, attempt.wrongAttempts));
		} else {
			pointsTV.setText("Points  0/" + question.points);
			attemptsTV.setText(TextUtil.getPointsText(0, 0));
		}

		// Handle Test Mode
		if (getMode() != AssessmentMode.TEST_DURING) {
			if (isSolved)
				showTick(true);
			else {
				if (wrongAttempts > 0 && !latestAttempt)
					showTick(false);
			}
		}
	}

	FillBlankAttemptInterface	attemptInterface	= new FillBlankAttemptInterface() {
														@Override
														public void onAttempt(boolean isSolved) {
															FillBlank_AttemptData data = new FillBlank_AttemptData();
															data.solved = new boolean[blanksArray.size()];
															data.attempts = new String[blanksArray.size()];
															int solvedCount = 0;
															int attemptCount = 0;
															for (int i = 0; i < blanksArray.size(); i++) {
																data.solved[i] = blanksArray.get(i).isSolved();
																data.attempts[i] = blanksArray.get(i).getAttempt();
																if (data.solved[i])
																	solvedCount++;
																if (data.attempts[i] != null && data.attempts[i].length() > 0)
																	attemptCount++;
															}
															latestAttempt = isSolved;
															switch (getMode()) {
															case DISCOVER_ANSWER_DURING:
															case TEACHER:
																if (isSolved)
																	saveAttempt(blanksArray.size(), solvedCount, wrongAttempts,
																			gson.toJson(data));
																else
																	saveAttempt(blanksArray.size(), solvedCount, wrongAttempts + 1,
																			gson.toJson(data));
																break;
															case TEST_AFTER:
																Toast.makeText(activity, "Can't submit answer now!", Toast.LENGTH_SHORT)
																		.show();
																break;
															case TEST_DURING:
																saveAttempt(blanksArray.size(), solvedCount, attemptCount - solvedCount,
																		gson.toJson(data));
																break;
															default:
																break;

															}
														}
													};
}