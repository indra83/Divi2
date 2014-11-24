package co.in.divi.ui;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import co.in.divi.R;
import co.in.divi.activity.AssessmentActivity.AssessmentMode;

/*
 * Instantiated through q_item_fillblank_word.xml
 */
public class FillBlank_LinearLayout extends LinearLayout {
	private static final String	TAG	= FillBlank_LinearLayout.class.getSimpleName();

	public interface FillBlankAttemptInterface {
		public void onAttempt(boolean isSolved);
	}

	private AssessmentMode		assMode;
	private GridLayout			lettersGrid, answerGrid;
	private ImageView			feedback;
	private View				lettersContainer;
	private ImageButton			resetButton;

	private String				word;
	private int					wordIndex;
	private boolean				isSolved;
	private String				attempt;

	// datamodel
	private String[]			answerModel, lettersModel;
	FillBlankAttemptInterface	attemptInterface;

	public FillBlank_LinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void init(String word, int wordIndex, FillBlankAttemptInterface attemptInterface, AssessmentMode mode) {
		this.word = word;
		this.wordIndex = wordIndex;
		this.attemptInterface = attemptInterface;
		this.assMode = mode;

		lettersGrid = (GridLayout) findViewById(R.id.letters);
		answerGrid = (GridLayout) findViewById(R.id.answer);
		feedback = (ImageView) findViewById(R.id.feedback);
		resetButton = (ImageButton) findViewById(R.id.reset);
		lettersContainer = findViewById(R.id.lettersContainer);
		feedback.setVisibility(View.GONE);

		answerModel = new String[word.length()];
		lettersModel = new String[word.length()];

		resetButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				resetBlank();
				FillBlank_LinearLayout.this.attemptInterface.onAttempt(false);
			}
		});
		resetBlank();
		setupUI();
	}

	public boolean isSolved() {
		return isSolved;
	}

	public String getAttempt() {
		return attempt;
	}

	public void setSolved() {
		setAttempt(word);
	}

	public void setAttempt(String att) {
		if (att == null || word.length() != att.length()) {
			resetBlank();
		} else {
			if (word.equalsIgnoreCase(att))
				isSolved = true;
			this.attempt = att;
			for (int i = 0; i < att.length(); i++) {
				answerModel[i] = att.substring(i, i + 1).toUpperCase();
				lettersModel[i] = null;
			}
		}
		setupUI();
	}

	private void resetBlank() {
		ArrayList<Character> chars = new ArrayList<Character>(word.length());
		for (char c : word.toCharArray()) {
			chars.add(c);
		}
		Collections.shuffle(chars);
		char[] shuffled = new char[chars.size()];
		for (int i = 0; i < shuffled.length; i++) {
			shuffled[i] = chars.get(i);
		}
		String shuffledWord = new String(shuffled);

		for (int i = 0; i < word.length(); i++) {
			answerModel[i] = null;
			lettersModel[i] = shuffledWord.substring(i, i + 1).toUpperCase();
		}
		attempt = "";
		isSolved = false;
	}

	private void setupUI() {
		answerGrid.removeAllViews();
		lettersGrid.removeAllViews();
		feedback.setVisibility(View.INVISIBLE);
		resetButton.setVisibility(View.INVISIBLE);

		View.OnClickListener ansClickListener = null;
		View.OnClickListener letClickListener = null;
		switch (assMode) {
		case DISCOVER_ANSWER_BEFORE:
		case TEST_BEFORE:
			Log.w(TAG, "?? shouldn't enter here!");
			break;
		case TEACHER:
		case DISCOVER_ANSWER_DURING:
			if (isSolved) {
				resetButton.setVisibility(View.INVISIBLE);
			} else {
				ansClickListener = answerClickListener;
				letClickListener = letterClickListener;
				resetButton.setVisibility(View.VISIBLE);
			}
			break;
		case TEST_DURING:
			resetButton.setVisibility(View.VISIBLE);
			ansClickListener = answerClickListener;
			letClickListener = letterClickListener;
			break;
		case TEST_AFTER:
			resetButton.setVisibility(View.INVISIBLE);
			feedback.setVisibility(View.VISIBLE);
			if (isSolved)
				feedback.setBackgroundResource(R.drawable.ic_correct_small);
			else
				feedback.setBackgroundResource(R.drawable.ic_wrong_small);
			break;
		default:
			break;

		}

		for (int i = 0; i < answerModel.length; i++) {
			TextView letterView = (TextView) ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
					R.layout.q_item_letter, null);
			letterView.setBackgroundResource(R.drawable.q_bg_fill_blank_answer);
			if (answerModel[i] != null) {
				letterView.setText(answerModel[i]);
				letterView.setOnClickListener(ansClickListener);
			} else {
				letterView.setText(" ");
			}
			letterView.setTag(i);
			answerGrid.addView(letterView);
		}

		for (int i = 0; i < lettersModel.length; i++) {
			TextView letterView = (TextView) ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
					R.layout.q_item_letter, null);
			letterView.setBackgroundResource(R.drawable.q_bg_fill_blank_letter);
			if (lettersModel[i] != null) {
				letterView.setText(lettersModel[i]);
				letterView.setOnClickListener(letClickListener);
			} else {
				letterView.setText(" ");
				letterView.setVisibility(View.INVISIBLE);
			}
			letterView.setTag(i);
			lettersGrid.addView(letterView);
		}
	}

	View.OnClickListener	letterClickListener	= new View.OnClickListener() {
													@Override
													public void onClick(View v) {
														int index = (Integer) v.getTag();
														String letter = lettersModel[index];
														lettersModel[index] = null;
														for (int i = 0; i < answerModel.length; i++) {
															if (answerModel[i] == null) {
																answerModel[i] = letter;
																break;
															}
														}
														// check if we solved
														boolean filled = true;
														for (int i = 0; i < lettersModel.length; i++) {
															if (lettersModel[i] != null) {
																filled = false;
																break;
															}
														}
														if (filled) {
															StringBuilder sb = new StringBuilder();
															for (int i = 0; i < answerModel.length; i++) {
																sb.append(answerModel[i]);
															}
															if (sb.toString().equalsIgnoreCase(word)) {
																isSolved = true;
															} else {
																isSolved = false;
															}
															attempt = sb.toString();
															attemptInterface.onAttempt(isSolved);
														}
														setupUI();
													}
												};

	View.OnClickListener	answerClickListener	= new View.OnClickListener() {
													@Override
													public void onClick(View v) {
														int index = (Integer) v.getTag();
														String letter = answerModel[index];
														answerModel[index] = null;
														for (int i = 0; i < lettersModel.length; i++) {
															if (lettersModel[i] == null) {
																lettersModel[i] = letter;
																break;
															}
														}
														attempt = null;
														setupUI();
													}
												};
}
