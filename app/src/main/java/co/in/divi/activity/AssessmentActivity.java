package co.in.divi.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import co.in.divi.BaseActivity;
import co.in.divi.DiviApplication;
import co.in.divi.LocationManager.Breadcrumb;
import co.in.divi.LocationManager.LOCATION_SUBTYPE;
import co.in.divi.LocationManager.LOCATION_TYPE;
import co.in.divi.LocationManager.ProtectedResourceMetadata;
import co.in.divi.R;
import co.in.divi.content.AssessmentFileModel;
import co.in.divi.content.AssessmentFileModel.Question;
import co.in.divi.content.Book;
import co.in.divi.content.DatabaseHelper;
import co.in.divi.content.DiviReference;
import co.in.divi.content.Node;
import co.in.divi.db.UserDBContract;
import co.in.divi.db.UserDBContract.Attempts;
import co.in.divi.db.UserDBContract.Commands;
import co.in.divi.db.model.Attempt;
import co.in.divi.db.model.Command;
import co.in.divi.fragment.HeaderFragment;
import co.in.divi.fragment.questions.QuestionFragmentFactory;
import co.in.divi.ui.CountDownTimerView;
import co.in.divi.ui.CountDownTimerView.CountDownTimerViewListener;
import co.in.divi.ui.SaundProgressBar;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Util;

public class AssessmentActivity extends BaseActivity implements LoaderCallbacks<Cursor>, CountDownTimerViewListener {
	private static final String	TAG							= AssessmentActivity.class.getSimpleName();
	private static final String	BUNDLE_EXTRA_QUESTION_ID	= "BUNDLE_EXTRA_QUESTION_ID";
	public static final String	INTENT_EXTRA_ASSESSMENT_ID	= "INTENT_EXTRA_ASSESSMENT_ID";

	private static final int	ASSESSMENTS_LOADER			= 1;

	public enum AssessmentMode {
		TEACHER, DISCOVER_ANSWER_BEFORE, DISCOVER_ANSWER_DURING, TEST_BEFORE, TEST_DURING, TEST_AFTER
	}

	ProgressDialog						pd;
	ViewPager							viewPager;
	ListView							listView;
	DrawerLayout						drawer;
	TextView							pointsText, accuracyText, asTitle;
	SaundProgressBar					progressBar;
	View								lockView;
	CountDownTimerView					countDownTimerView, testTimerView;

	DatabaseHelper						dbHelper;
	QuestionsPagerAdapter				questionsPagerAdapter;
	QuestionsListAdapter				questionsListAdapter;
	LoadDataTask						loadDataTask;

	public AssessmentFileModel			currentAssessment;
	public AssessmentMode				mode;
	private Command						command;
	private String						currentCourseId;
	private Book						currentBook;
	private Node						currentAssessmentNode;
	private ProtectedResourceMetadata	protectedResourceMetadata;
	public File							assessmentBaseDir;
	private int							currentQuestionIndex	= -1;
	DiviReference						uriToLoad;							// could
																			// be
																			// null
	// to use before we get currentAssessment
	private String						assessmentId;
	private String						displayedQuestionId;

	boolean								questionsLoaded, attemptsLoaded;

	// for attempts
	public interface AttemptsChangedListener {
		public void onAttemptsChanged();
	}

	public ArrayList<AttemptsChangedListener>	attemptsChangedListeners;
	public HashMap<String, Attempt>				attempts;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		attempts = new HashMap<String, Attempt>();
		attemptsChangedListeners = new ArrayList<AssessmentActivity.AttemptsChangedListener>();
		questionsListAdapter = new QuestionsListAdapter();
		questionsPagerAdapter = new QuestionsPagerAdapter(getFragmentManager());
		dbHelper = DatabaseHelper.getInstance(this);

		setContentView(R.layout.activity_assessment);
		header = (HeaderFragment) getFragmentManager().findFragmentById(R.id.header);
		headerShadow = findViewById(R.id.header_shadow);
		viewPager = (ViewPager) findViewById(R.id.pager);
		listView = (ListView) findViewById(R.id.questions);
		ViewGroup listHeader = (ViewGroup) ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
				R.layout.item_header_assessment, listView, false);
		lockView = findViewById(R.id.lock_screen);
		countDownTimerView = (CountDownTimerView) findViewById(R.id.timer);
		testTimerView = (CountDownTimerView) findViewById(R.id.test_timer);
		pointsText = (TextView) listHeader.findViewById(R.id.progress_points);
		accuracyText = (TextView) listHeader.findViewById(R.id.progress_accuracy);
		asTitle = (TextView) listHeader.findViewById(R.id.title);
		progressBar = (SaundProgressBar) listHeader.findViewById(R.id.progress_progressbar);
		listView.addHeaderView(listHeader, null, false);
		drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.setDrawerListener(new DrawerListener() {
			@Override
			public void onDrawerStateChanged(int arg0) {
			}

			@Override
			public void onDrawerSlide(View drawerView, float slideOffset) {
				if (slideOffset > 0.1)
					showHeader();
				else
					hideHeader();
			}

			@Override
			public void onDrawerOpened(View arg0) {
			}

			@Override
			public void onDrawerClosed(View arg0) {
				hideHeader();
			}
		});

		findViewById(R.id.slide_toc_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (LogConfig.DEBUG_ACTIVITIES)
					Log.d(TAG, "on onClick - slide_toc_button");
				drawer.openDrawer(Gravity.LEFT);
			}
		});

		viewPager.setAdapter(questionsPagerAdapter);
		viewPager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				setDisplayedQuestion(position);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});

		listView.setAdapter(questionsListAdapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				showQuestion(position - 1);// correct position to take into account header
			}
		});
		if (savedInstanceState != null)
			displayedQuestionId = savedInstanceState.getString(BUNDLE_EXTRA_QUESTION_ID);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "onNewIntent");
		displayedQuestionId = null;
		setIntent(intent);
		// loadData();
		// !! in our scenario onStart is always called when this happens?
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "onStart");
		// loadData();
	}

	@Override
	protected void onResume() {
		super.onResume();
		currentQuestionIndex = -1;
		loadData();
	}

	@Override
	protected void onStop() {
		super.onStop();
		countDownTimerView.stop();
		testTimerView.stop();
		if (pd != null)
			pd.cancel();
		if (loadDataTask != null && loadDataTask.getStatus() != LoadDataTask.Status.FINISHED) {
			loadDataTask.cancel(false);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (displayedQuestionId != null) {
			outState.putString(BUNDLE_EXTRA_QUESTION_ID, displayedQuestionId);
		}
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(0, R.anim.zoom_out);
	}

	private void updateLocation() {
		if (currentQuestionIndex >= 0 && currentAssessmentNode != null && currentAssessment != null) {
			locationManager.setNewLocation(LOCATION_TYPE.ASSESSMENT, LOCATION_SUBTYPE.ASSESSMENT_QUIZ, new DiviReference(
					currentBook.courseId, currentBook.id, DiviReference.REFERENCE_TYPE_ASSESSMENT, currentAssessmentNode.id,
					currentAssessment.questions[currentQuestionIndex].id), Breadcrumb.get(userSessionProvider.getCourseName(),
					currentBook.name, currentAssessmentNode.parentName, currentAssessmentNode.name,
					currentAssessment.questions[currentQuestionIndex].name), protectedResourceMetadata);
		}
	}

	private void setDisplayedQuestion(int newIndex) {
		if (this.currentQuestionIndex == newIndex)
			return;// could be called twice!
		this.currentQuestionIndex = newIndex;
		displayedQuestionId = currentAssessment.questions[newIndex].id;
		questionsListAdapter.notifyDataSetChanged();

		updateLocation();
	}

	private void showQuestion(int questionIndex) {
		viewPager.setCurrentItem(questionIndex, true);
		setDisplayedQuestion(questionIndex);
	}

	private void loadData() {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "in loadData");
		uriToLoad = null;
		assessmentId = null;
		currentCourseId = null;
		try {
			if (getIntent().hasExtra(Util.INTENT_EXTRA_BOOK)) {
				this.currentBook = (Book) getIntent().getExtras().get(Util.INTENT_EXTRA_BOOK);
				assessmentId = getIntent().getStringExtra(INTENT_EXTRA_ASSESSMENT_ID);
				currentCourseId = currentBook.courseId;
				if (assessmentId == null)
					throw new IllegalArgumentException();
			} else {
				uriToLoad = new DiviReference(Uri.parse(getIntent().getExtras().getString(Util.INTENT_EXTRA_URI)));
				ArrayList<Book> books = DatabaseHelper.getInstance(this).getBooks(uriToLoad.courseId);
				for (Book book : books) {
					if (book.id.equals(uriToLoad.bookId)) {
						this.currentBook = book;
						break;
					}
				}
				assessmentId = uriToLoad.itemId;
				currentCourseId = uriToLoad.courseId;
				command = null;
				// questionId = uriToLoad.subItemId;
				if (this.currentBook == null) {
					Toast.makeText(this, "Book not found. Please run update.", Toast.LENGTH_LONG).show();
					finish();
					return;
				}
				if (this.assessmentId == null) {
					Toast.makeText(this, "Assessment not found. Please update content.", Toast.LENGTH_LONG).show();
					finish();
					return;
				}
			}

			if (!currentCourseId.equals(userSessionProvider.getCourseId())) {
				Toast.makeText(this, "Change course!", Toast.LENGTH_LONG).show();
				finish();
				return;
			}
		} catch (Exception e) {
			Log.e(TAG, "error loading book", e);
			Toast.makeText(this, "Error loading book!", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		// TODO:ensure user has access to current book
		if (pd != null)
			pd.cancel();// TODO: fix lifecycle, temp fix
		pd = ProgressDialog.show(this, "Loading", "Please wait while we load your previous attempts...");
		if (loadDataTask != null && loadDataTask.getStatus() != LoadDataTask.Status.FINISHED) {
			loadDataTask.cancel(false);
		}
		loadDataTask = new LoadDataTask();
		loadDataTask.execute(new String[] { assessmentId });
		// now fetch attempts (asynchronously)
		getLoaderManager().restartLoader(ASSESSMENTS_LOADER, null, this);
	}

	private void refreshUI(Node assessmentNode) {
		lockView.setVisibility(View.GONE);
		countDownTimerView.stop();
		testTimerView.stop();
		testTimerView.setVisibility(View.GONE);
		if (mode == AssessmentMode.DISCOVER_ANSWER_BEFORE || mode == AssessmentMode.TEST_BEFORE) {
			lockView.setVisibility(View.VISIBLE);
			countDownTimerView.start(command.appliedAt, "Starts in:", this);
		} else if (mode == AssessmentMode.TEST_DURING) {
			testTimerView.setVisibility(View.VISIBLE);
			testTimerView.start(command.endsAt, "Time left :", this);
		}
		currentAssessmentNode = assessmentNode;
		File bookBaseDir = new File(((DiviApplication) getApplication()).getBooksBaseDir(currentBook.courseId), currentBook.id);
		assessmentBaseDir = new File(new File(bookBaseDir, assessmentNode.parentId), assessmentNode.id);
		currentAssessment = (AssessmentFileModel) assessmentNode.tag;
		int mins = 10;
		try {
			mins = Integer.parseInt(currentAssessment.time);
		} catch (Exception e) {
			Log.w(TAG, "error parsing time");
		}
		if (AssessmentFileModel.TYPE_QUIZ.equalsIgnoreCase(currentAssessment.type)) {
			if (userSessionProvider.isLoggedIn() && !userSessionProvider.getUserData().isTeacher()) {
				currentAssessment.shuffleQuestions();
			}
			protectedResourceMetadata = new ProtectedResourceMetadata(currentAssessment.name, Command.COMMAND_UNLOCK_ITEM_CATEGORY_QUIZ,
					mins * 60 * 1000, null);
		} else if (AssessmentFileModel.TYPE_TEST.equalsIgnoreCase(currentAssessment.type)) {
			if (userSessionProvider.isLoggedIn() && !userSessionProvider.getUserData().isTeacher()) {
				currentAssessment.shuffleQuestions();
			}
			protectedResourceMetadata = new ProtectedResourceMetadata(currentAssessment.name, Command.COMMAND_UNLOCK_ITEM_CATEGORY_TEST,
					mins * 60 * 1000, null);
		} else {
			protectedResourceMetadata = null;
		}

		questionsListAdapter.notifyDataSetChanged();
		questionsPagerAdapter.notifyDataSetChanged();
		drawer.openDrawer(Gravity.LEFT);
		asTitle.setText(currentAssessmentNode.name);
		questionsLoaded = true;
		updateProgress();
		String idToOpen = displayedQuestionId;
		if (idToOpen == null) {
			if (uriToLoad != null && uriToLoad.subItemId != null)
				idToOpen = uriToLoad.subItemId;
		}
		if (idToOpen != null) {
			int qIndex = 0;
			for (Question q : currentAssessment.questions) {
				if (q.id.equals(idToOpen)) {
					showQuestion(qIndex);
					return;
				}
				qIndex++;
			}
		}

		if (currentAssessment.questions.length > 0) {
			showQuestion(0);
		}
	}

	/*
	 * Attempts related
	 */
	public void saveAttempt(String courseId, String bookId, String assessmentId, String questionId, int points, int subquestions,
			int correctAttempts, int wrongAttempts, String data) {
		if (!(mode == AssessmentMode.TEACHER || mode == AssessmentMode.DISCOVER_ANSWER_DURING || mode == AssessmentMode.TEST_DURING)) {
			Toast.makeText(this, "Oops, time is up!", Toast.LENGTH_SHORT).show();
			return;
		}
		// validate we are saving to correct quiz!!
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "c:" + courseId + ", b:" + bookId + ", a:" + assessmentId);
		if (!(courseId.equals(currentCourseId) && bookId.equals(currentBook.id) && assessmentId.equals(currentAssessmentNode.id))) {
			Toast.makeText(this, "Oops! Unexpected state, please open again...", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		ContentValues values = new ContentValues();
		values.put(Attempts.UID, userSessionProvider.getUserData().uid);
		values.put(Attempts.COURSE_ID, userSessionProvider.getCourseId());
		values.put(Attempts.BOOK_ID, currentBook.id);
		values.put(Attempts.ASSESSMENT_ID, currentAssessmentNode.id);
		values.put(Attempts.QUESTION_ID, questionId);
		values.put(Attempts.TOTAL_POINTS, points);
		values.put(Attempts.SUBQUESTIONS, subquestions);
		values.put(Attempts.CORRECT_ATTEMPTS, correctAttempts);
		values.put(Attempts.WRONG_ATTEMPTS, wrongAttempts);
		values.put(Attempts.DATA, data);
		if (correctAttempts == subquestions)
			values.put(Attempts.SOLVED_AT, Util.getTimestampMillis());
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "Saving (" + questionId + ") - " + points + "/" + subquestions + " , " + correctAttempts + "/" + wrongAttempts);
		getContentResolver().insert(Attempts.CONTENT_URI, values);
	}

	class LoadDataTask extends AsyncTask<String, Void, Integer> {
		private Node		assessmentNode;
		private String		courseId;
		private String		uid;
		private String		mSelectionClause;
		private String[]	mSelectionArgs;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			courseId = userSessionProvider.getCourseId();
			uid = userSessionProvider.getUserData().uid;
		}

		@Override
		protected Integer doInBackground(String... params) {
			try {
				assessmentNode = dbHelper.getNode(params[0], currentBook.id, courseId);
				mSelectionClause = Commands.UID + " = ? AND " + Commands.COURSE_ID + " = ? AND " + Commands.BOOK_ID + " = ? AND "
						+ Commands.ITEM_ID + " = ? AND " + Commands.TYPE + " = ? AND " + Commands.STATUS + " = ?";
				mSelectionArgs = new String[] { uid, courseId, currentBook.id, params[0], "" + Command.COMMAND_CATEGORY_UNLOCK,
						"" + Command.COMMAND_STATUS_ACTIVE };
				Cursor cursor = getContentResolver().query(UserDBContract.Commands.CONTENT_URI, UserDBContract.Commands.PROJECTION_ALL,
						mSelectionClause, mSelectionArgs, Commands.SORT_ORDER_LATEST_FIRST);
				if (cursor.moveToFirst())
					command = Command.fromCursor(cursor);
			} catch (Exception e) {
				Log.e(TAG, "error loading assessments", e);
				return 2;
			}
			return 0;
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result == 0) {
				// check if assessment is protected (quiz/test)
				long now = Util.getTimestampMillis();
				AssessmentFileModel afm = (AssessmentFileModel) assessmentNode.tag;
				if (userSessionProvider.getUserData().isTeacher()) {
					mode = AssessmentMode.TEACHER;
				} else if (AssessmentFileModel.TYPE_QUIZ.equalsIgnoreCase(afm.type)) {
					if (command == null) {
						Toast.makeText(AssessmentActivity.this, "Quiz needs to be unlocked.", Toast.LENGTH_LONG).show();
						finish();
						return;
					} else {
						if (now < command.appliedAt) {
							mode = AssessmentMode.DISCOVER_ANSWER_BEFORE;
						} else {
							mode = AssessmentMode.DISCOVER_ANSWER_DURING;
						}
					}
				} else if (AssessmentFileModel.TYPE_TEST.equalsIgnoreCase(afm.type)) {
					if (command == null) {
						Toast.makeText(AssessmentActivity.this, "Test needs to be unlocked.", Toast.LENGTH_LONG).show();
						finish();
						return;
					} else {
						if (now < command.appliedAt) {
							mode = AssessmentMode.TEST_BEFORE;
						} else if (now <= command.endsAt) {
							mode = AssessmentMode.TEST_DURING;
						} else {
							mode = AssessmentMode.TEST_AFTER;
						}
					}
				} else {
					mode = AssessmentMode.DISCOVER_ANSWER_DURING;// default for exercise
				}
				refreshUI(assessmentNode);
			} else {
				Toast.makeText(AssessmentActivity.this, "Error loading the assessment.", Toast.LENGTH_LONG).show();
				finish();
			}
		}
	}

	class QuestionsListAdapter extends BaseAdapter {

		LayoutInflater	inflater;

		public QuestionsListAdapter() {
			inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			if (currentAssessment == null) {
				return 0;
			}
			return currentAssessment.questions.length;
		}

		@Override
		public Object getItem(int position) {
			return currentAssessment.questions[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.item_question, parent, false);
			}
			TextView title = (TextView) convertView.findViewById(R.id.question_title);
			ImageView status = (ImageView) convertView.findViewById(R.id.question_status);

			// Log.d(TAG, "text::    " + currentAssessment.questions[position].text);
			// Log.d(TAG, "metadata::  " + new Gson().toJson(currentAssessment.questions[position].metadata));

			if (attempts.containsKey(currentAssessment.questions[position].id)) {
				Attempt a = attempts.get(currentAssessment.questions[position].id);
				status.setVisibility(View.VISIBLE);
				if (mode == AssessmentMode.TEST_DURING) {
					if (a.correctAttempts + a.wrongAttempts == 0)
						status.setVisibility(View.GONE);
					else
						status.setImageResource(R.drawable.yellow_circle);
				} else {
					if (a.isSolved())
						status.setImageResource(R.drawable.green_circle);
					else {
						if (a.correctAttempts + a.wrongAttempts == 0)
							status.setVisibility(View.GONE);
						else
							status.setImageResource(R.drawable.red_circle);
					}
				}
			} else {
				status.setVisibility(View.GONE);
			}

			if (position == currentQuestionIndex) {
				convertView.setBackgroundResource(R.drawable.bg_home_section_selected);
			} else {
				convertView.setBackgroundColor(Color.parseColor("#33393D"));
			}

			title.setText("  Q " + (position + 1));
			// title.setText(currentAssessment.questions[position].id);

			return convertView;
		}
	}

	class QuestionsPagerAdapter extends FragmentStatePagerAdapter {

		public QuestionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return QuestionFragmentFactory.getQuestionFragment(mode, currentCourseId, currentBook.id, currentAssessmentNode.id,
					currentAssessment.questions[position].type, position);
		}

		@Override
		public int getCount() {
			if (currentAssessment == null) {
				return 0;
			}
			return currentAssessment.questions.length;
		}

		@Override
		public int getItemPosition(Object object) {
			// force refresh on notify dataset change.
			return POSITION_NONE;
		}
	}

	/*
	 * implemented methods
	 */
	@Override
	public void onCourseChange() {
		finish();
	}

	/*
	 * Attempts related code
	 */
	public void addAttemptsChangedListener(AttemptsChangedListener listener) {
		if (!this.attemptsChangedListeners.contains(listener))
			this.attemptsChangedListeners.add(listener);
	}

	public void removeAttemptsChangedListener(AttemptsChangedListener listener) {
		this.attemptsChangedListeners.remove(listener);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "onCreateLoader - " + assessmentId);
		String mSelectionClause = Attempts.UID + " = ? AND " + Attempts.COURSE_ID + " = ? AND " + Attempts.BOOK_ID + " = ? AND "
				+ Attempts.ASSESSMENT_ID + " = ? ";
		String[] mSelectionArgs = new String[] { userSessionProvider.getUserData().uid, userSessionProvider.getCourseId(), currentBook.id,
				assessmentId };
		CursorLoader loader = new CursorLoader(this, Attempts.CONTENT_URI, Attempts.PROJECTION_BASIC, mSelectionClause, mSelectionArgs,
				null);
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "onLoadFinished - " + loader.dataToString(cursor));
		pd.cancel();
		attempts.clear();
		if (cursor != null && cursor.getCount() > 0) {
			int questionId_index = cursor.getColumnIndex(Attempts.QUESTION_ID);
			int points_index = cursor.getColumnIndex(Attempts.TOTAL_POINTS);
			int subquestions_index = cursor.getColumnIndex(Attempts.SUBQUESTIONS);
			int correct_attempts_index = cursor.getColumnIndex(Attempts.CORRECT_ATTEMPTS);
			int wrong_attempts_index = cursor.getColumnIndex(Attempts.WRONG_ATTEMPTS);
			int data_index = cursor.getColumnIndex(Attempts.DATA);

			while (cursor.moveToNext()) {
				Attempt a = new Attempt();
				a.questionId = cursor.getString(questionId_index);
				a.totalPoints = cursor.getInt(points_index);
				a.subquestions = cursor.getInt(subquestions_index);
				a.correctAttempts = cursor.getInt(correct_attempts_index);
				a.wrongAttempts = cursor.getInt(wrong_attempts_index);
				a.data = cursor.getString(data_index);
				attempts.put(a.questionId, a);
				if (LogConfig.DEBUG_ACTIVITIES)
					Log.d(TAG, "got attempt:" + a.questionId + " - " + a.totalPoints + "/" + a.subquestions + " , " + a.correctAttempts
							+ "/" + a.wrongAttempts);
			}
		}
		attemptsLoaded = true;
		updateProgress();
		questionsListAdapter.notifyDataSetChanged();
		for (AttemptsChangedListener listener : attemptsChangedListeners)
			listener.onAttemptsChanged();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "onLoaderReset");
		pd.cancel();
	}

	private void updateProgress() {
		pointsText.setVisibility(View.VISIBLE);
		accuracyText.setVisibility(View.VISIBLE);
		progressBar.setVisibility(View.VISIBLE);
		if (questionsLoaded && attemptsLoaded) {
			AssessmentFileModel.AssessmentScorecard scorecard = currentAssessment.getScorecard(attempts);
			pointsText.setText("" + scorecard.userPoints + "/" + scorecard.maxPoints);
			accuracyText.setText(String.format("%.2f", scorecard.avgAccuracy));
			progressBar.setProgress((scorecard.userPoints * 100) / scorecard.maxPoints);
		}
		if (mode == AssessmentMode.TEST_DURING) {
			pointsText.setVisibility(View.INVISIBLE);
			accuracyText.setVisibility(View.INVISIBLE);
			progressBar.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public void timerEvent() {
		loadData();
	}
}