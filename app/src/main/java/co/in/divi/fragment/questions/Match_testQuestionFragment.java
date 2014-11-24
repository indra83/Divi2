package co.in.divi.fragment.questions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import co.in.divi.R;
import co.in.divi.activity.AssessmentActivity.AssessmentMode;
import co.in.divi.content.questions.Match_Question;
import co.in.divi.content.questions.Match_QuestionXmlParser;
import co.in.divi.db.model.Attempt;
import co.in.divi.fragment.questions.state.Match_AttemptData;
import co.in.divi.util.TextUtil;

public class Match_testQuestionFragment extends BaseQuestionFragment {

	private TextView							title;
	private LinearLayout						leftContainer, rightContainer, solvedContainer;
	private int									selectedLeft, selectedRight;
	private LinearLayout.LayoutParams			lp;
	private ArrayList<View>						leftViews, rightViews;

	private Match_Question						matchQuestion;

	private int									subquestions	= 0;

	private ArrayList<Match_AttemptData.Match>	matches;
	private boolean								latestAttempt	= false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		matches = new ArrayList<Match_AttemptData.Match>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.q_fragment_match, container, false);
		title = (TextView) rootView.findViewById(R.id.title);
		leftContainer = (LinearLayout) rootView.findViewById(R.id.leftContainer);
		rightContainer = (LinearLayout) rootView.findViewById(R.id.rightContainer);
		solvedContainer = (LinearLayout) rootView.findViewById(R.id.solved);
		pointsTV = (TextView) rootView.findViewById(R.id.points_text);
		attemptsTV = (TextView) rootView.findViewById(R.id.attempts_text);
		qnoTV = (TextView) rootView.findViewById(R.id.qno_text);
		tick = (ImageView) rootView.findViewById(R.id.tick);
		lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		lp.setMargins(0, 10, 0, 10);
		return rootView;
	}

	@Override
	void loadAttempt(Attempt attempt) {
		if (attempt != null) {
			Log.d(TAG, "att:" + attempt.data);
			Match_AttemptData data = gson.fromJson(attempt.data, Match_AttemptData.class);
			matches.clear();
			if (data.attemptedMatches != null) {
				matches.addAll(Arrays.asList(data.attemptedMatches));
			}
			isSolved = checkSolved();
			if (isSolved)
				showTick(true);
			else {
				if (wrongAttempts > 0 && !latestAttempt)
					showTick(false);
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
		updateUI();
	}

	@Override
	Object getQuestionData() {
		return new Match_QuestionXmlParser().getQuestionFromXml(questionXmlFile, ImageGetter);
	}

	@Override
	void fillUI(Object questionData) {
		matchQuestion = (Match_Question) questionData;
		subquestions = matchQuestion.matches.size();
		// reset
		leftContainer.removeAllViews();
		rightContainer.removeAllViews();
		selectedLeft = -1;
		selectedRight = -1;
		// setup UI
		title.setText(matchQuestion.questionHTML);
		leftViews = new ArrayList<View>();
		rightViews = new ArrayList<View>();
		for (int i = 0; i < matchQuestion.matches.size(); i++) {
			TextView tv = new TextView(activity);
			tv.setText(matchQuestion.matches.get(i).leftHTML);
			tv.setTag(i);
			tv.setOnClickListener(leftClickListener);
			tv.setTextSize(20);
			tv.setTextColor(Color.parseColor("#444444"));
			tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
			tv.setBackgroundResource(R.drawable.q_bg_left_jigsaw_n);
			tv.setPadding(15, 0, 60, 0);
			leftViews.add(tv);
			tv = new TextView(activity);
			tv.setText(matchQuestion.matches.get(i).rightHTML);
			tv.setTag(i);
			tv.setOnClickListener(rightClickListener);
			tv.setTextSize(20);
			tv.setTextColor(Color.parseColor("#444444"));
			tv.setGravity(Gravity.CENTER_VERTICAL);
			tv.setBackgroundResource(R.drawable.q_bg_right_jigsaw_n);
			tv.setPadding(50, 0, 15, 0);
			rightViews.add(tv);
		}
		Collections.shuffle(leftViews);
		Collections.shuffle(rightViews);
		for (View v : leftViews)
			leftContainer.addView(v, lp);
		for (View v : rightViews)
			rightContainer.addView(v, lp);
		leftContainer.requestLayout();
		rightContainer.requestLayout();
	}

	private boolean checkSolved() {
		return false;
	}

	private void saveAttempt() {
		Match_AttemptData data = new Match_AttemptData();
		data.attemptedMatches = matches.toArray(new Match_AttemptData.Match[0]);
		int correct = 0;
		for (Match_AttemptData.Match match : matches) {
			if (match.left == match.right)
				correct++;
		}
		saveAttempt(subquestions, correct, matches.size() - correct, gson.toJson(data));
	}

	private void updateUI() {
		Log.d(TAG, "sel:" + selectedLeft + "," + selectedRight);
		// show solved questions
		solvedContainer.removeAllViews();
		leftContainer.removeAllViews();
		rightContainer.removeAllViews();
		if (getMode() == AssessmentMode.TEST_AFTER) {
			for (int i = 0; i < matchQuestion.matches.size(); i++) {
				boolean solved = false;
				for (Match_AttemptData.Match match : matches) {
					if (match.left == i && match.right == i)
						solved = true;
				}
				LinearLayout solvedView = (LinearLayout) ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(R.layout.q_item_match_solved, null);
				((TextView) solvedView.findViewById(R.id.left_match)).setText(matchQuestion.matches.get(i).leftHTML);
				((TextView) solvedView.findViewById(R.id.right_match)).setText(matchQuestion.matches.get(i).rightHTML);
				ImageView feedbackView = (ImageView) solvedView.findViewById(R.id.feedback);
				feedbackView.setVisibility(View.VISIBLE);
				if (solved)
					feedbackView.setBackgroundResource(R.drawable.ic_correct_small);
				else
					feedbackView.setBackgroundResource(R.drawable.ic_wrong_small);
				solvedContainer.addView(solvedView, lp);
			}
		} else {
			// check if we have a match
			if (selectedLeft >= 0 && selectedRight >= 0) {
				Match_AttemptData.Match match = new Match_AttemptData.Match();
				match.left = selectedLeft;
				match.right = selectedRight;
				matches.add(match);
				selectedLeft = selectedRight = -1;

				// save
				saveAttempt();
				return; // UI refresh will happen on load attempt...
			}

			for (int i = 0; i < matches.size(); i++) {
				Match_AttemptData.Match match = matches.get(i);
				LinearLayout solvedView = (LinearLayout) ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(R.layout.q_item_match_solved, null);
				((TextView) solvedView.findViewById(R.id.left_match)).setText(matchQuestion.matches.get(match.left).leftHTML);
				((TextView) solvedView.findViewById(R.id.right_match)).setText(matchQuestion.matches.get(match.right).rightHTML);
				solvedView.setClickable(true);
				solvedView.setOnClickListener(cancelClickListener);
				solvedView.setTag(i);
				solvedContainer.addView(solvedView, lp);
			}

			// updates UI after selection change
			for (int i = 0; i < leftViews.size(); i++) {
				if (containsLeftMatch((Integer) leftViews.get(i).getTag())) {
					continue;
				} else {
					View leftView = leftViews.get(i);
					if ((Integer) leftView.getTag() == selectedLeft) {
						leftView.setBackgroundResource(R.drawable.q_bg_left_jigsaw_s);
					} else {
						leftView.setBackgroundResource(R.drawable.q_bg_left_jigsaw_n);
					}
					leftView.setPadding(15, 0, 60, 0);
					leftContainer.addView(leftView, lp);
				}
			}
			for (int i = 0; i < rightViews.size(); i++) {
				if (containsRightMatch((Integer) rightViews.get(i).getTag())) {
					continue;
				} else {
					View rightView = rightViews.get(i);
					if ((Integer) rightView.getTag() == selectedRight) {
						rightView.setBackgroundResource(R.drawable.q_bg_right_jigsaw_s);
					} else {
						rightView.setBackgroundResource(R.drawable.q_bg_right_jigsaw_n);
					}
					rightView.setPadding(50, 0, 15, 0);
					rightContainer.addView(rightView, lp);
				}
			}
		}
	}

	private boolean containsLeftMatch(int i) {
		for (Match_AttemptData.Match match : matches) {
			if (match.left == i)
				return true;
		}
		return false;
	}

	private boolean containsRightMatch(int i) {
		for (Match_AttemptData.Match match : matches) {
			if (match.right == i)
				return true;
		}
		return false;
	}

	View.OnClickListener	leftClickListener	= new View.OnClickListener() {
													@Override
													public void onClick(View v) {
														if (selectedLeft == (Integer) v.getTag())
															selectedLeft = -1;
														else
															selectedLeft = (Integer) v.getTag();
														updateUI();
													}
												};
	View.OnClickListener	rightClickListener	= new View.OnClickListener() {
													@Override
													public void onClick(View v) {
														if (selectedRight == (Integer) v.getTag())
															selectedRight = -1;
														else
															selectedRight = (Integer) v.getTag();
														updateUI();
													}
												};

	View.OnClickListener	cancelClickListener	= new View.OnClickListener() {
													@Override
													public void onClick(View v) {
														matches.remove((int) (Integer) v.getTag());
														saveAttempt();
													}
												};
}