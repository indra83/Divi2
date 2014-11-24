package co.in.divi.fragment.questions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import android.os.Bundle;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import co.in.divi.R;
import co.in.divi.activity.AssessmentActivity.AssessmentMode;
import co.in.divi.content.questions.MCQ_Question;
import co.in.divi.content.questions.MCQ_Question.Option;
import co.in.divi.content.questions.MCQ_QuestionXmlParser;
import co.in.divi.db.model.Attempt;
import co.in.divi.fragment.questions.state.MCQ_AttemptData;
import co.in.divi.ui.MCQ_OptionHolder;
import co.in.divi.util.TextUtil;

public class MCQ_QuestionFragment extends BaseQuestionFragment {
	private static final String	TAG	= "MCQ_QuestionFragment";

	private TextView			questionTV;
	private Button				submitButton;
	private ViewGroup[]			holderContainers;

	ArrayList<MCQ_OptionHolder>	optionHolders;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		optionHolders = new ArrayList<MCQ_OptionHolder>();
		holderContainers = new ViewGroup[6];
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.q_fragment_mcq, container, false);
		holderContainers[0] = (ViewGroup) rootView.findViewById(R.id.holder1);
		holderContainers[1] = (ViewGroup) rootView.findViewById(R.id.holder2);
		holderContainers[2] = (ViewGroup) rootView.findViewById(R.id.holder3);
		holderContainers[3] = (ViewGroup) rootView.findViewById(R.id.holder4);
		holderContainers[4] = (ViewGroup) rootView.findViewById(R.id.holder5);
		holderContainers[5] = (ViewGroup) rootView.findViewById(R.id.holder6);
		questionTV = (TextView) rootView.findViewById(R.id.questionText);
		pointsTV = (TextView) rootView.findViewById(R.id.points_text);
		qnoTV = (TextView) rootView.findViewById(R.id.qno_text);
		attemptsTV = (TextView) rootView.findViewById(R.id.attempts_text);
		submitButton = (Button) rootView.findViewById(R.id.submitButton);
		tick = (ImageView) rootView.findViewById(R.id.tick);
		submitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				saveAttempt();
			}
		});
		return rootView;
	}

	@Override
	Object getQuestionData() {
		return new MCQ_QuestionXmlParser().getQuestionFromXml(questionXmlFile, ImageGetter);
	}

	@Override
	void fillUI(Object questionData) {
		MCQ_Question question = (MCQ_Question) questionData;
		// shuffle options
		Collections.shuffle(question.options, new Random(System.currentTimeMillis()));
		questionTV.setText(question.questionHTML);
		int i = 0;
		int maxWidth = 0;
		try {
			for (Option option : question.options) {
				for (ImageSpan is : option.optionHTML.getSpans(0, option.optionHTML.length(), ImageSpan.class)) {
					maxWidth = Math.max(maxWidth, is.getDrawable().getIntrinsicWidth());
				}
			}
		} catch (Exception e) {
			Log.w(TAG, "error max width", e);
		}
		Log.d(TAG, "maxWidth:" + maxWidth);
		boolean useFullWidth = maxWidth > 0.30 * SCREEN_WIDTH;
		for (Option option : question.options) {
			if (useFullWidth) {
				Log.d(TAG, "applying full width");
				((LinearLayout) holderContainers[i].getParent()).setOrientation(LinearLayout.VERTICAL);
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT, 0);
				lp.setMargins(15, 10, 15, 10);
				holderContainers[i].setLayoutParams(lp);
			}
			MCQ_OptionHolder optionHolder = new MCQ_OptionHolder(activity, option, getMode());
			holderContainers[i].addView(optionHolder.getView());
			optionHolders.add(optionHolder);
			i++;
		}
	}

	@Override
	void loadAttempt(Attempt attempt) {
		Log.d(TAG, "in load attempt - ");
		if (attempt != null) {
			Log.d(TAG, "" + attempt.data);
			MCQ_AttemptData data = gson.fromJson(attempt.data, MCQ_AttemptData.class);
			isSolved = true;
			for (MCQ_OptionHolder o : optionHolders) {
				o.setChecked(false);
				for (String s : data.selectedOptions) {
					if (o.getId().equals(s)) {
						o.setChecked(true);
						break;
					}
				}
				isSolved = isSolved && o.isOptionCorrectlyMarked();
			}
			// hide loading
			if (getMode() != AssessmentMode.TEST_DURING)
				showTick(isSolved);
			if (isSolved) {
				pointsTV.setText(TextUtil.getPointsText(attempt.totalPoints, attempt.totalPoints));
				submitButton.setEnabled(false);
			} else {
				int proratedPoints = (attempt.totalPoints * attempt.correctAttempts) / (attempt.subquestions);
				pointsTV.setText("Points  " + proratedPoints + "/" + attempt.totalPoints);
				submitButton.setEnabled(true);
			}
			attemptsTV.setText(TextUtil.getAttemptsText(attempt.correctAttempts, attempt.wrongAttempts));
		} else {
			pointsTV.setText("Points  0/" + question.points);
			attemptsTV.setText(TextUtil.getPointsText(0, 0));
		}
		// Handle Test Mode
		if (getMode() == AssessmentMode.TEST_DURING) {
			submitButton.setEnabled(true);
		}
	}

	private void saveAttempt() {
		boolean attempted = false;
		boolean solved = true;
		ArrayList<String> markedOptions = new ArrayList<String>();
		for (MCQ_OptionHolder o : optionHolders) {
			attempted = attempted || o.isMarked();
			solved = solved && o.isOptionCorrectlyMarked();
			if (o.isMarked())
				markedOptions.add(o.getId());
		}

		MCQ_AttemptData data = new MCQ_AttemptData();
		data.selectedOptions = markedOptions.toArray(new String[markedOptions.size()]);
		if (getMode() == AssessmentMode.TEST_DURING) {
			if (solved)
				saveAttempt(1, 1, 0, gson.toJson(data));
			else {
				if (attempted)
					saveAttempt(1, 0, 1, gson.toJson(data));
				else
					saveAttempt(1, 0, 0, gson.toJson(data));
			}
		} else {
			if (solved)
				saveAttempt(1, 1, wrongAttempts, gson.toJson(data));
			else
				saveAttempt(1, 0, wrongAttempts + 1, gson.toJson(data));
		}
	}
}