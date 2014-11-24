package co.in.divi.fragment.questions;

import java.io.File;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.activity.AssessmentActivity;
import co.in.divi.activity.AssessmentActivity.AssessmentMode;
import co.in.divi.activity.AssessmentActivity.AttemptsChangedListener;
import co.in.divi.content.AssessmentFileModel.Question;
import co.in.divi.db.model.Attempt;

import com.google.gson.Gson;

public abstract class BaseQuestionFragment extends Fragment implements AttemptsChangedListener {
	protected static final String	TAG				= "BaseQuestionFragment";
	protected static final int		TICK_HIDE_DELAY	= 3000;

	protected Html.ImageGetter		ImageGetter;
	protected static int			SCREEN_WIDTH	= 1000;

	public static final String		QUESTION_NO		= "TOPIC_NO";
	public static final String		ASSESSMENT_ID	= "ASSESSMENT_ID";
	public static final String		BOOK_ID			= "BOOK_ID";
	public static final String		COURSE_ID		= "COURSE_ID";

	private String					courseId, bookId, assessmentId;
	protected int					questionIndex;
	protected AssessmentActivity	activity;
	protected Question				question;
	protected File					questionXmlFile;
	private LoadQuestionTask		loadTask		= null;

	// flags to synchronize loading UI & attempts.
	protected Gson					gson;
	private boolean					uiReady;
	private boolean					attemptReady;

	// correct/wrong feedback
	protected boolean				isSolved;
	protected ImageView				tick;
	protected TextView				attemptsTV, pointsTV, qnoTV;

	// scoring
	int								wrongAttempts	= 0;
	int								correctAttempts	= 0;

	protected Handler				handler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handler = new Handler();
		gson = new Gson();
		questionIndex = getArguments().getInt(QUESTION_NO);
		courseId = getArguments().getString(COURSE_ID);
		bookId = getArguments().getString(BOOK_ID);
		assessmentId = getArguments().getString(ASSESSMENT_ID);
		ImageGetter = new Html.ImageGetter() {
			@Override
			public Drawable getDrawable(String source) {
				try {
					if (source.startsWith("data:")) {
						source = source.split("base64,", 2)[1];
						byte[] data = Base64.decode(source, Base64.DEFAULT);
						Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
						BitmapDrawable bd = new BitmapDrawable(getResources(), bitmap);
						bd.setBounds(0, 0, bd.getIntrinsicWidth(), bd.getIntrinsicHeight());
						return bd;
					} else {
						// Log.d(TAG, "source:" + source);
						File imgFile = new File(questionXmlFile.getParent(), source);
						Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
						BitmapDrawable bd = new BitmapDrawable(getResources(), bitmap);
						bd.setBounds(0, 0, bd.getIntrinsicWidth(), bd.getIntrinsicHeight());
						return bd;
					}
				} catch (Exception e) {
					Log.w(TAG, "error getting image from base64", e);
					return null;
				}
			}
		};
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = (AssessmentActivity) activity;
		WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		SCREEN_WIDTH = size.x;
		Log.d(TAG, "SCREEN_WIDTH::" + SCREEN_WIDTH);
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!UserSessionProvider.getInstance(activity).isLoggedIn())
			return;
		if (getClass().equals(Error_QuestionFragment.class))
			return;
		qnoTV.setText("Question " + (questionIndex + 1));
		activity.addAttemptsChangedListener(this);
		onAttemptsChanged();// ensure we load initial attempt!
		question = this.activity.currentAssessment.questions[questionIndex];
		questionXmlFile = new File(activity.assessmentBaseDir, question.id + "/question.xml");
		loadTask = new LoadQuestionTask();
		loadTask.execute((Void) null);
	}

	@Override
	public void onStop() {
		super.onStop();
		if (getClass().equals(Error_QuestionFragment.class))
			return;
		activity.removeAttemptsChangedListener(this);
		if (loadTask != null)
			loadTask.cancel(false);
	}

	protected void saveAttempt(int subquestions, int correctAttempts, int wrongAttempts, String data) {
		activity.saveAttempt(courseId, bookId, assessmentId, question.id, question.points, subquestions, correctAttempts, wrongAttempts,
				data);
	}

	@Override
	public void onAttemptsChanged() {
		Log.d(TAG, "onAttemptsChanged");
		attemptReady = true;
		// load the attempt only if UI is already ready.
		if (uiReady) {
			sendLoadAttempt();
		}
	}

	private void sendLoadAttempt() {
		Attempt attempt = activity.attempts.get(question.id);
		if (attempt != null) {
			wrongAttempts = attempt.wrongAttempts;
			correctAttempts = attempt.correctAttempts;
		}
		try {
			loadAttempt(attempt);
		} catch (Exception e) {
			Log.e(TAG, "error loading attempt", e);
			Toast.makeText(getActivity(), "Error loading attempt", Toast.LENGTH_LONG).show();
		}
		// Test related UI
		if (getMode() == AssessmentMode.TEST_DURING) {
			pointsTV.setVisibility(View.INVISIBLE);
			attemptsTV.setVisibility(View.INVISIBLE);
		}
	}

	private class LoadQuestionTask extends AsyncTask<Void, Void, Integer> {

		Object	questionData;

		@Override
		protected Integer doInBackground(Void... params) {
			try {
				questionData = getQuestionData();
				return 0;
			} catch (Exception e) {
				Log.w(TAG, "Error loading question - " + questionXmlFile, e);
				return 1;
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result == 0) {
				try {
					fillUI(questionData);
					uiReady = true;
					// setup Test related UI
					if (getMode() == AssessmentMode.TEST_DURING) {
						pointsTV.setVisibility(View.INVISIBLE);
						attemptsTV.setVisibility(View.INVISIBLE);
					}
					// check if attempt has already arrived asynchronously and load it.
					if (attemptReady) {
						sendLoadAttempt();
					}
				} catch (Exception e) {
					Log.w(TAG, "error loading question", e);
					Toast.makeText(activity, "Error loading question", Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(activity, "Error loading question", Toast.LENGTH_LONG).show();
			}
		}
	}

	protected void showTick(boolean solved) {
		if (getMode() == AssessmentMode.TEST_DURING) {
			tick.setVisibility(View.GONE);
			return;
		}
		tick.setVisibility(View.VISIBLE);
		if (solved) {
			tick.setBackgroundResource(R.drawable.correct_big);
		} else {
			tick.setBackgroundResource(R.drawable.wrong_big);
			Runnable r = new Runnable() {
				@Override
				public void run() {
					if (!isSolved)
						tick.setVisibility(View.GONE);
				}
			};
			handler.postDelayed(r, TICK_HIDE_DELAY);
		}
	}

	protected AssessmentMode getMode() {
		return activity.mode;
	}

	/*
	 * Loads the user's previous attempt at this question. Safe to assume UI is already loaded.
	 */
	abstract void loadAttempt(Attempt attempt);

	abstract Object getQuestionData();

	abstract void fillUI(Object questionData);

	// abstract boolean isSolved(Attempt attempt);

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
		super.onSaveInstanceState(outState);
	}
}
