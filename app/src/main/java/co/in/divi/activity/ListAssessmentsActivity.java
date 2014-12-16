package co.in.divi.activity;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import co.in.divi.BaseActivity;
import co.in.divi.DiviApplication;
import co.in.divi.LocationManager.Breadcrumb;
import co.in.divi.LocationManager.LOCATION_SUBTYPE;
import co.in.divi.LocationManager.LOCATION_TYPE;
import co.in.divi.LocationManager.ProtectedResourceMetadata;
import co.in.divi.R;
import co.in.divi.content.AssessmentFileModel;
import co.in.divi.content.Book;
import co.in.divi.content.DatabaseHelper;
import co.in.divi.content.DiviReference;
import co.in.divi.content.Node;
import co.in.divi.db.UserDBContract.Attempts;
import co.in.divi.db.UserDBContract.Commands;
import co.in.divi.db.model.Attempt;
import co.in.divi.db.model.Command;
import co.in.divi.fragment.AssessmentClassReportFragment;
import co.in.divi.fragment.WiFiSettingsFragment;
import co.in.divi.model.UserData.ClassRoom;
import co.in.divi.ui.CountDownTimerView;
import co.in.divi.ui.CountDownTimerView.CountDownTimerViewListener;
import co.in.divi.ui.ProgressGrid;
import co.in.divi.ui.ProgressGrid.ProgressState;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Util;

public class ListAssessmentsActivity extends BaseActivity implements LoaderCallbacks<Cursor>, CountDownTimerViewListener {
	private static final String					TAG							= ListAssessmentsActivity.class.getSimpleName();
	private static final String					BUNDLE_EXTRA_ASSESSMENT_ID	= "BUNDLE_EXTRA_ASSESSMENT_ID";

	private static final int					LOADER_ASSESSMENTS			= 1;
	private static final int					LOADER_COMMANDS				= 2;

	private DatabaseHelper						dbHelper;
	private AssessmentsAdapter					adapter;
	private LoadDataTask						loadDataTask;

	private ExpandableListView					assessmentsList;
	private TextView							title, titleStatus, noq, difficulty, time, progressPoints, progressAccuracy;
	private Button								startAssessment;
	private LinearLayout						reportButtonsContainer;
	private ImageView							titleStatusIcon, lockImage;
	private CountDownTimerView					timerView;
	private ProgressGrid						progressGrid;

	public Node[]								chapters;																		// !
																																// accessible
																																// to
																																// fragments
	private HashMap<String, ArrayList<Command>>	commands;

	private String								displayedAssessmentId;
	private Node								displayedAssessment;
	private Book								currentBook;
	private File								bookBaseDir;
	private DiviReference						uriToLoad;																		// could
																																// be
																																// null

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new AssessmentsAdapter();
		dbHelper = DatabaseHelper.getInstance(this);
		commands = new HashMap<String, ArrayList<Command>>();

		setContentView(R.layout.activity_listassessments);
		title = (TextView) findViewById(R.id.title_text);
		titleStatus = (TextView) findViewById(R.id.title_status_text);
		lockImage = (ImageView) findViewById(R.id.lock);
		noq = (TextView) findViewById(R.id.noq);
		difficulty = (TextView) findViewById(R.id.difficulty);
		time = (TextView) findViewById(R.id.time);
		startAssessment = (Button) findViewById(R.id.start);
		reportButtonsContainer = (LinearLayout) findViewById(R.id.reportButtons);
		timerView = (CountDownTimerView) findViewById(R.id.timer);
		// questions = (TextView) findViewById(R.id.title_text);
		titleStatusIcon = (ImageView) findViewById(R.id.title_icon);
		assessmentsList = (ExpandableListView) findViewById(R.id.assessments);
		progressPoints = (TextView) findViewById(R.id.progress_points);
		progressAccuracy = (TextView) findViewById(R.id.progress_accuracy);
		progressGrid = (ProgressGrid) findViewById(R.id.progressGrid);
		assessmentsList.setAdapter(adapter);
		assessmentsList.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				showAssessment(chapters[groupPosition].getChildren().get(childPosition));
				return true;
			}
		});
		assessmentsList.setOnGroupClickListener(new OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				Node chapterNode = (Node) adapter.getGroup(groupPosition);
				if (displayedAssessment != null && displayedAssessment.parentId.equals(chapterNode.id)) {
					return true;
				}
				return false;
			}
		});

		startAssessment.setOnClickListener(new View.OnClickListener() {
			@SuppressLint("NewApi")
			@Override
			public void onClick(View v) {
				if (displayedAssessment != null) {
					Intent launchAssessment = new Intent(ListAssessmentsActivity.this, AssessmentActivity.class);
					launchAssessment.putExtra(Util.INTENT_EXTRA_BOOK, currentBook);
					launchAssessment.putExtra(AssessmentActivity.INTENT_EXTRA_ASSESSMENT_ID, displayedAssessment.id);
					Bundle b = null;
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						b = ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight()).toBundle();
                        startActivity(launchAssessment, b);
                    }else {
                        startActivity(launchAssessment);
                    }
				} else {
					Toast.makeText(ListAssessmentsActivity.this, "Select assessment", Toast.LENGTH_LONG).show();
				}
			}
		});
		if (savedInstanceState != null)
			displayedAssessmentId = savedInstanceState.getString(BUNDLE_EXTRA_ASSESSMENT_ID);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.d(TAG, "on new intent" + intent.getStringExtra(Util.INTENT_EXTRA_URI));
		displayedAssessmentId = null; // clear previous selection
		setIntent(intent);
		// loadData();
		// !! in our scenario onStart is always called when this happens?
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		loadData();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (loadDataTask != null && loadDataTask.getStatus() != LoadDataTask.Status.FINISHED) {
			loadDataTask.cancel(false);
		}
		timerView.stop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (displayedAssessmentId != null) {
			outState.putString(BUNDLE_EXTRA_ASSESSMENT_ID, displayedAssessmentId);
		}
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(0, R.anim.zoom_out);
	}

	private void loadData() {
		uriToLoad = null;
		// TODO: add courseId check
		try {
			if (getIntent().hasExtra(Util.INTENT_EXTRA_BOOK)) {
				this.currentBook = (Book) getIntent().getExtras().get(Util.INTENT_EXTRA_BOOK);
			} else {
				uriToLoad = new DiviReference(Uri.parse(getIntent().getExtras().getString(Util.INTENT_EXTRA_URI)));
				ArrayList<Book> books = DatabaseHelper.getInstance(this).getBooks(uriToLoad.courseId);
				for (Book book : books) {
					if (book.id.equals(uriToLoad.bookId)) {
						this.currentBook = book;
						break;
					}
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "error loading book", e);
			Toast.makeText(this, "Error loading book!", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		if (this.currentBook == null) {
			Toast.makeText(this, "Book not found. Please run update.", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		// TODO:ensure user has access to current book

		bookBaseDir = new File(((DiviApplication) getApplication()).getBooksBaseDir(currentBook.courseId), currentBook.id);
		if (loadDataTask != null && loadDataTask.getStatus() != LoadDataTask.Status.FINISHED) {
			loadDataTask.cancel(false);
		}
		loadDataTask = new LoadDataTask();
		loadDataTask.execute(new Void[0]);
	}

	private void refreshUI(Node[] newData) {
		// remove chapters without assessments
		ArrayList<Node> chaptersWithAssessments = new ArrayList<Node>();
		for (Node n : newData) {
			if (n.getChildren().size() > 0)
				chaptersWithAssessments.add(n);
		}
		if (chaptersWithAssessments.size() == 0) {
			Toast.makeText(this, "No assessments found for this book.", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		this.chapters = chaptersWithAssessments.toArray(new Node[0]);
		adapter.notifyDataSetChanged();
		if (displayedAssessmentId != null) {
			for (Node ch : chapters) {
				for (Node ass : ch.getChildren()) {
					if (ass.id.equals(displayedAssessmentId)) {
						showAssessment(ass);
						return;
					}
				}
			}
		}
		Log.d(TAG, "Uri to load:" + uriToLoad);
		if (uriToLoad != null) {
			for (Node ch : chapters) {
				for (Node ass : ch.getChildren()) {
					if (ass.id.equals(uriToLoad.itemId)) {
						showAssessment(ass);
						return;
					}
				}
			}
		}
		showAssessment(chapters[0].getChildren().get(0));
	}

	private void showAssessment(Node assessmentNode) {
		long now = Util.getTimestampMillis();
		displayedAssessmentId = assessmentNode.id;
		displayedAssessment = assessmentNode;
		adapter.notifyDataSetChanged();
		for (int i = 0; i < this.chapters.length; i++) {
			if (displayedAssessment.parentId.equals(chapters[i].id)) {
				assessmentsList.expandGroup(i, true);
			} else {
				assessmentsList.collapseGroup(i);
			}
		}
		// draw test details
		AssessmentFileModel assessment = (AssessmentFileModel) displayedAssessment.tag;
		String prefix = "";
		if (AssessmentFileModel.TYPE_ASSIGNMENT.equalsIgnoreCase(assessment.type))
			prefix = "[Exercise] ";
		else if (AssessmentFileModel.TYPE_QUIZ.equalsIgnoreCase(assessment.type))
			prefix = "[Quiz] ";
		else if (AssessmentFileModel.TYPE_TEST.equalsIgnoreCase(assessment.type))
			prefix = "[Test] ";
		title.setText(prefix + assessment.name);
		noq.setText("" + assessment.questions.length);
		difficulty.setText(assessment.difficulty.substring(0, 1).toUpperCase() + assessment.difficulty.substring(1));
		int mins = 10;
		try {
			mins = Integer.parseInt(assessment.time);
		} catch (Exception e) {
			Log.w(TAG, "error parsing time");
		}

		time.setText(String.format("%02d:%02d", mins / 60, mins % 60));
		if (userSessionProvider.isLoggedIn() && !userSessionProvider.getUserData().isTeacher()) {
			assessment.shuffleQuestions();
		}
		progressGrid.initQuestions(assessment.questions);
		getLoaderManager().restartLoader(LOADER_ASSESSMENTS, null, this);
		// update location
		ProtectedResourceMetadata prm = null;
		if (userSessionProvider.getUserData().isTeacher()) {
			if (AssessmentFileModel.TYPE_QUIZ.equalsIgnoreCase(assessment.type))
				prm = new ProtectedResourceMetadata(assessment.name, Command.COMMAND_UNLOCK_ITEM_CATEGORY_QUIZ, mins * 60 * 1000, null);
			else if (AssessmentFileModel.TYPE_TEST.equalsIgnoreCase(assessment.type))
				prm = new ProtectedResourceMetadata(assessment.name, Command.COMMAND_UNLOCK_ITEM_CATEGORY_TEST, mins * 60 * 1000, null);
		}
		locationManager
				.setNewLocation(LOCATION_TYPE.ASSESSMENT, LOCATION_SUBTYPE.ASSESSMENT_QUIZ, new DiviReference(currentBook.courseId,
						currentBook.id, DiviReference.REFERENCE_TYPE_ASSESSMENT, displayedAssessment.id, null), Breadcrumb.get(
						userSessionProvider.getCourseName(), currentBook.name, displayedAssessment.parentName, displayedAssessment.name,
						null), prm);
		// report buttons
		reportButtonsContainer.removeAllViews();

		// locks & protected content
		timerView.stop();
		if (userSessionProvider.getUserData().isTeacher()) {
			lockImage.setVisibility(View.GONE);
			startAssessment.setVisibility(View.VISIBLE);
			if (commands.containsKey(assessmentNode.id)) {
				final DiviReference assRef = locationManager.getLocationRef();
				final String chapterName = displayedAssessment.parentName;
				for (Command c : commands.get(assessmentNode.id)) {
					Button reportLink = new Button(this);
					String className = null;
					for (ClassRoom cr : userSessionProvider.getUserData().classRooms) {
						if (cr.classId.equals(c.classRoomId)) {
							className = cr.toString();
						}
					}
					if (className != null) {
						final String classId = c.classRoomId;
						reportLink.setText("View scorecard of " + className);
						reportLink.setTextSize(18);
						reportLink.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								if (!Util.isNetworkOn(ListAssessmentsActivity.this)) {
									Toast.makeText(ListAssessmentsActivity.this, "Please connect to network.", Toast.LENGTH_LONG).show();
									DialogFragment wifiFragment = new WiFiSettingsFragment();
									FragmentManager fm = getFragmentManager();
									wifiFragment.show(fm, "WiFi");
								} else {
									AssessmentClassReportFragment f = new AssessmentClassReportFragment(classId, assRef, chapterName);
									f.show(getFragmentManager(), "as_class_fragment");
								}
							}
						});
						reportButtonsContainer.addView(reportLink);
					}
				}
			}
		} else {
			lockImage.setVisibility(View.VISIBLE);
			startAssessment.setVisibility(View.GONE);
			ArrayList<Command> cmds = commands.get(assessmentNode.id);
			if (assessment.type.equalsIgnoreCase(AssessmentFileModel.TYPE_ASSIGNMENT)) {
				lockImage.setVisibility(View.GONE);
				startAssessment.setVisibility(View.VISIBLE);
			} else if (assessment.type.equalsIgnoreCase(AssessmentFileModel.TYPE_QUIZ)) {
				if (cmds != null && cmds.size() > 0) {
					Command oldestAppliedCommand = cmds.get(0);
					for (Command c : cmds)
						if (c.appliedAt < oldestAppliedCommand.appliedAt) {
							oldestAppliedCommand = c;
						}
					if (oldestAppliedCommand.appliedAt > now) {
						timerView.start(oldestAppliedCommand.appliedAt, "Unlocks in :", this);
					} else {
						lockImage.setVisibility(View.GONE);
						startAssessment.setVisibility(View.VISIBLE);
					}
					// startAssessment.setEnabled(true);
				}
			} else if (assessment.type.equalsIgnoreCase(AssessmentFileModel.TYPE_TEST)) {
				if (cmds != null && cmds.size() > 0) {
					Command oldestAppliedCommand = cmds.get(0);
					for (Command c : cmds)
						if (c.appliedAt < oldestAppliedCommand.appliedAt) {
							oldestAppliedCommand = c;
						}
					if (oldestAppliedCommand.appliedAt > now) {
						timerView.start(oldestAppliedCommand.appliedAt, "Unlocks in :", this);
					} else {
						lockImage.setVisibility(View.INVISIBLE);
						startAssessment.setVisibility(View.VISIBLE);
						if (oldestAppliedCommand.endsAt > now)
							timerView.start(oldestAppliedCommand.endsAt, "Test in progress!\n Time left :", this);
					}
				}
			}
		}
	}

	class LoadDataTask extends AsyncTask<Void, Void, Integer> {

		Node[]	newChapters;
		String	courseId;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			courseId = userSessionProvider.getCourseId();
		}

		@Override
		protected Integer doInBackground(Void... params) {
			try {
				this.newChapters = dbHelper.getChapterNodes(courseId, currentBook.id, Node.NODE_TYPE_ASSESSMENT);
			} catch (Exception e) {
				Log.e(TAG, "error loading assessments", e);
				return 1;
			}
			return 0;
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result == 0) {
				refreshUI(this.newChapters);
				getLoaderManager().restartLoader(LOADER_COMMANDS, null, ListAssessmentsActivity.this);
			} else {
				Toast.makeText(ListAssessmentsActivity.this, "Error loading assessments.", Toast.LENGTH_LONG).show();
				finish();
			}
		}
	}

	class AssessmentsAdapter extends BaseExpandableListAdapter {

		LayoutInflater	inflater;
		int				normalTextColor, selectedTextColor;

		AssessmentsAdapter() {
			inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			normalTextColor = getResources().getColor(R.color.text_topic_normal);
			selectedTextColor = getResources().getColor(R.color.text_topic_selected);
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return chapters[groupPosition].getChildren().get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.item_assessment, parent, false);
			}
			TextView title = (TextView) convertView.findViewById(R.id.title);
			ImageView lock = (ImageView) convertView.findViewById(R.id.lock);

			Node assNode = (Node) getChild(groupPosition, childPosition);
			int textColor;
			if (displayedAssessment != null && assNode.id.equals(displayedAssessment.id)) {
				convertView.setBackgroundResource(R.drawable.bg_topic_selected);
				textColor = selectedTextColor;
			} else {
				convertView.setBackgroundResource(R.drawable.bg_topic_normal);
				textColor = normalTextColor;
			}
			AssessmentFileModel assessment = (AssessmentFileModel) assNode.tag;
			if (assessment.type.equals(AssessmentFileModel.TYPE_QUIZ) || assessment.type.equals(AssessmentFileModel.TYPE_TEST)) {
				lock.setVisibility(View.VISIBLE);
				if (commands.containsKey(assNode.id)) {
					lock.setVisibility(View.GONE);
				}
			} else {
				lock.setVisibility(View.GONE);
			}
			title.setText(assNode.name);
			title.setTextColor(textColor);
			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return chapters[groupPosition].getChildren().size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return chapters[groupPosition];
		}

		@Override
		public int getGroupCount() {
			if (chapters == null) {
				return 0;
			}
			return chapters.length;
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.item_chapter, parent, false);
			}
			Node chapterNode = (Node) getGroup(groupPosition);

			if (displayedAssessment != null && displayedAssessment.parentId.equals(chapterNode.id)) {
				convertView.setBackgroundResource(R.drawable.bg_home_section_selected);
			} else {
				convertView.setBackgroundColor(Color.parseColor("#33393D"));
			}
			TextView chapterTitle = (TextView) convertView.findViewById(R.id.chapter_title);
			TextView chapterName = (TextView) convertView.findViewById(R.id.chapter_name);
			chapterTitle.setText("Chapter " + (1 + groupPosition));
			chapterName.setText(chapterNode.name);
			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

	}

	@Override
	public void onCourseChange() {
		finish();
	}

	@Override
	public void onLectureJoinLeave() {
		super.onLectureJoinLeave();
		loadData();
	}

	/* Attempts/Commands Related */
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String mSelectionClause;
		String[] mSelectionArgs;
		switch (id) {
		case LOADER_ASSESSMENTS:
			mSelectionClause = Attempts.UID + " = ? AND " + Attempts.COURSE_ID + " = ? AND " + Attempts.BOOK_ID + " = ? AND "
					+ Attempts.ASSESSMENT_ID + " = ? ";
			mSelectionArgs = new String[] { userSessionProvider.getUserData().uid, userSessionProvider.getCourseId(), currentBook.id,
					displayedAssessment.id };
			CursorLoader loader = new CursorLoader(this, Attempts.CONTENT_URI, Attempts.PROJECTION_ALL, mSelectionClause, mSelectionArgs,
					null);
			return loader;
		case LOADER_COMMANDS:
			if (lectureSessionProvider.isCurrentUserTeacher()) {
				mSelectionClause = Commands.STATUS + " = ? AND " + Commands.TYPE + " = ? AND " + Commands.UID + " = ? AND "
						+ Commands.COURSE_ID + " = ? AND " + Commands.BOOK_ID + " = ? AND " + Commands.CLASS_ID + " = ? ";
				mSelectionArgs = new String[] { "" + Command.COMMAND_STATUS_ACTIVE, "" + Command.COMMAND_CATEGORY_UNLOCK,
						userSessionProvider.getUserData().uid, userSessionProvider.getCourseId(), currentBook.id,
						lectureSessionProvider.getCurrentLecture().classRoomId };
			} else {
				mSelectionClause = Commands.STATUS + " = ? AND " + Commands.TYPE + " = ? AND " + Commands.UID + " = ? AND "
						+ Commands.COURSE_ID + " = ? AND " + Commands.BOOK_ID + " = ? ";
				mSelectionArgs = new String[] { "" + Command.COMMAND_STATUS_ACTIVE, "" + Command.COMMAND_CATEGORY_UNLOCK,
						userSessionProvider.getUserData().uid, userSessionProvider.getCourseId(), currentBook.id };
			}
			loader = new CursorLoader(this, Commands.CONTENT_URI, Commands.PROJECTION_ALL, mSelectionClause, mSelectionArgs, null);
			return loader;
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "onLoadFinished - " + loader.getId());
		switch (loader.getId()) {
		case LOADER_ASSESSMENTS:
			HashMap<String, ProgressState> attemptStates = new HashMap<String, ProgressState>();
			progressPoints.setText("--");
			progressAccuracy.setText("--");
			if (cursor != null && cursor.getCount() > 0) {
				HashMap<String, Attempt> attempts = new HashMap<String, Attempt>();
				while (cursor.moveToNext()) {
					if (LogConfig.DEBUG_ACTIVITIES) {
						// Log.d(TAG, "cursor::" + cursor.getColumnName(2) + "," + cursor.getColumnName(3) + "," +
						// cursor.getColumnName(4));
						Log.d(TAG, "attempt values::" + cursor.getString(2) + "," + cursor.getString(3) + "," + cursor.getString(4) + ","
								+ cursor.getString(5) + "," + cursor.getString(6));
					}
					Attempt a = Attempt.fromCursor(cursor);
					attempts.put(a.questionId, a);
					if (a.isSolved())
						attemptStates.put(a.questionId, ProgressState.SOLVED);
					else if (a.isAttempted())
						attemptStates.put(a.questionId, ProgressState.ATTEMPTED);
				}
				// ignore during test
				ArrayList<Command> cmds = commands.get(displayedAssessmentId);
				Command oldestAppliedCommand = null;
				if (cmds != null) {
					oldestAppliedCommand = cmds.get(0);
					for (Command c : cmds)
						if (c.appliedAt < oldestAppliedCommand.appliedAt) {
							oldestAppliedCommand = c;
						}
				}
				AssessmentFileModel assessment = (AssessmentFileModel) displayedAssessment.tag;
				if (assessment.type.equalsIgnoreCase(AssessmentFileModel.TYPE_TEST)) {
					long now = Util.getTimestampMillis();
					if (oldestAppliedCommand == null || oldestAppliedCommand.endsAt > now) {
						// test in progress, clear progress grid
						for (Entry<String, Attempt> a : attempts.entrySet()) {
							if (a.getValue().isSolved()) {
								attemptStates.put(a.getValue().questionId, ProgressState.ATTEMPTED);
							}
						}
						// set attempt state
						progressGrid.setattemptStates(attemptStates);
						break;
					}
				}
				AssessmentFileModel.AssessmentScorecard scorecard = assessment.getScorecard(attempts);
				progressPoints.setText("" + scorecard.userPoints);
				progressAccuracy.setText(String.format("%.2f", scorecard.avgAccuracy) + "%");
				progressGrid.setattemptStates(attemptStates);
			}
			break;
		case LOADER_COMMANDS:
			if (LogConfig.DEBUG_ACTIVITIES)
				Log.d(TAG, "commands");
			commands.clear();
			if (cursor != null && cursor.getCount() > 0) {
				while (cursor.moveToNext()) {
					Command c = Command.fromCursor(cursor);
					if (!commands.containsKey(c.itemCode))
						commands.put(c.itemCode, new ArrayList<Command>());
					commands.get(c.itemCode).add(c);
					if (LogConfig.DEBUG_ACTIVITIES)
						Log.d(TAG, "found command:" + c.toString());
				}
				if (chapters != null)
					refreshUI(chapters);
			}
			break;
		default:
			break;
		}
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "onLoadFinished - finished");
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "onLoaderReset");
	}

	@Override
	public void timerEvent() {
		loadData();
	}
}
