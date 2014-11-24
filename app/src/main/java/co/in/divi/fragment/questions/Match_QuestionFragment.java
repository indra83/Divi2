package co.in.divi.fragment.questions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import co.in.divi.R;
import co.in.divi.content.questions.Match_Question;
import co.in.divi.content.questions.Match_QuestionXmlParser;
import co.in.divi.db.model.Attempt;
import co.in.divi.fragment.questions.state.Match_AttemptData;
import co.in.divi.util.TextUtil;

public class Match_QuestionFragment extends BaseQuestionFragment {

	TextView					title;
	LinearLayout				leftContainer, rightContainer, solvedContainer;
	int							selectedLeft, selectedRight;
	LinearLayout.LayoutParams	lp;

	Match_Question				matchQuestion;

	int							subquestions	= 0;

	ArrayList<Integer>			solved;
	boolean						latestAttempt	= false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		solved = new ArrayList<Integer>();
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
			solved.clear();
			solved.addAll(Arrays.asList(gson.fromJson(attempt.data, Match_AttemptData.class).solvedMatches));
			updateUI();
			isSolved = solved.size() == matchQuestion.matches.size();
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
		ArrayList<View> leftViews, rightViews;
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

	private void updateUI() {
		// check if we have a match
		if (selectedLeft >= 0 && selectedRight >= 0) {
			Match_AttemptData data = new Match_AttemptData();
			if (selectedRight == selectedLeft) {
				solved.add(selectedLeft);
				data.solvedMatches = solved.toArray(new Integer[0]);
				selectedLeft = selectedRight = -1;
				saveAttempt(subquestions, correctAttempts + 1, wrongAttempts, gson.toJson(data));
				latestAttempt = true;
			} else {
				data.solvedMatches = solved.toArray(new Integer[0]);
				selectedLeft = selectedRight = -1;
				saveAttempt(subquestions, correctAttempts, wrongAttempts + 1, gson.toJson(data));
				showTick(false);
				latestAttempt = false;
			}
		}
		// updates UI after selection change
		for (int i = 0; i < leftContainer.getChildCount(); i++) {
			if (solved.contains(((Integer) leftContainer.getChildAt(i).getTag()))) {
				leftContainer.removeViewAt(i);
				i = -1; // restart since we changed the container.
			} else {
				if (((Integer) leftContainer.getChildAt(i).getTag()) == selectedLeft) {
					leftContainer.getChildAt(i).setBackgroundResource(R.drawable.q_bg_left_jigsaw_s);
					leftContainer.getChildAt(i).setPadding(15, 0, 60, 0);
				} else {
					leftContainer.getChildAt(i).setBackgroundResource(R.drawable.q_bg_left_jigsaw_n);
					leftContainer.getChildAt(i).setPadding(15, 0, 60, 0);
				}
			}
		}
		for (int i = 0; i < rightContainer.getChildCount(); i++) {
			if (solved.contains(((Integer) rightContainer.getChildAt(i).getTag()))) {
				rightContainer.removeViewAt(i);
				i = -1; // restart since we changed the container.
			} else {
				if (((Integer) rightContainer.getChildAt(i).getTag()) == selectedRight) {
					rightContainer.getChildAt(i).setBackgroundResource(R.drawable.q_bg_right_jigsaw_s);
					rightContainer.getChildAt(i).setPadding(50, 0, 15, 0);
				} else {
					rightContainer.getChildAt(i).setBackgroundResource(R.drawable.q_bg_right_jigsaw_n);
					rightContainer.getChildAt(i).setPadding(50, 0, 15, 0);
				}
			}
		}

		// show solved questions
		solvedContainer.removeAllViews();
		for (int i = 0; i < solved.size(); i++) {
			LinearLayout solvedView = (LinearLayout) ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
					R.layout.q_item_match_solved, null);
			((TextView) solvedView.findViewById(R.id.left_match)).setText(matchQuestion.matches.get(solved.get(i)).leftHTML);
			((TextView) solvedView.findViewById(R.id.right_match)).setText(matchQuestion.matches.get(solved.get(i)).rightHTML);
			solvedContainer.addView(solvedView, lp);
		}
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
}