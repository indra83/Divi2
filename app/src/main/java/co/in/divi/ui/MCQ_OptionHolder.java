package co.in.divi.ui;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import co.in.divi.R;
import co.in.divi.activity.AssessmentActivity.AssessmentMode;
import co.in.divi.content.questions.MCQ_Question.Option;

public class MCQ_OptionHolder {
	private static final String	TAG	= MCQ_OptionHolder.class.getSimpleName();

	private View				optionView;
	private CheckBox			checkBox;
	private TextView			optionTextView;
	private ImageView			feedback;

	private String				id;
	private boolean				isAnswer;

	public MCQ_OptionHolder(Context context, Option option, AssessmentMode mode) {
		id = option.id;
		isAnswer = option.isAnswer;
		optionView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.q_item_mcq_option, null);
		checkBox = (CheckBox) optionView.findViewById(R.id.checkbox);
		optionTextView = (TextView) optionView.findViewById(R.id.optionText);
		optionView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setChecked(!checkBox.isChecked());
			}
		});

		optionTextView.setText(option.optionHTML);

		// feedback
		feedback = (ImageView) optionView.findViewById(R.id.feedback);
		if (isAnswer) {
			feedback.setImageResource(R.drawable.ic_correct_small);
		} else {
			feedback.setImageResource(R.drawable.ic_wrong_small);
		}
		switch (mode) {
		case TEST_AFTER:
			feedback.setVisibility(View.VISIBLE);
			break;
		default:
			feedback.setVisibility(View.GONE);
		}
	}

	public View getView() {
		return optionView;
	}

	public boolean isOptionCorrectlyMarked() {
		Log.d(TAG, "id-" + id + ", isanswer-" + isAnswer);
		return isAnswer == checkBox.isChecked();
	}

	public boolean isMarked() {
		return checkBox.isChecked();
	}

	public String getId() {
		return id;
	}

	public void setChecked(boolean checked) {
		if (!checked) {
			checkBox.setChecked(false);
			optionView.setBackgroundResource(R.drawable.q_bg_mcq_option_n);
			optionTextView.setTextColor(Color.DKGRAY);
			optionTextView.setText(optionTextView.getText());
		} else {
			checkBox.setChecked(true);
			optionView.setBackgroundResource(R.drawable.q_bg_mcq_option_s);
			optionTextView.setTextColor(Color.WHITE);
			optionTextView.setText(optionTextView.getText());
		}
	}
}
