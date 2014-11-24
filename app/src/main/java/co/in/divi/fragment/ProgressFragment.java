package co.in.divi.fragment;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.MultipleCategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.content.AssessmentFileModel;
import co.in.divi.content.Book;
import co.in.divi.content.DatabaseHelper;
import co.in.divi.content.Node;
import co.in.divi.db.UserDBContract;
import co.in.divi.db.UserDBContract.Attempts;
import co.in.divi.db.UserDBContract.Commands;
import co.in.divi.db.model.Attempt;
import co.in.divi.db.model.Command;
import co.in.divi.progress.AnalyticsManager;
import co.in.divi.progress.AssessmentSummary;
import co.in.divi.progress.WeeklyTimeReport;
import co.in.divi.progress.WeeklyTimeReport.DayTime;
import co.in.divi.util.DiviCalendar;
import co.in.divi.util.TextUtil;
import co.in.divi.util.Util;
import co.in.divi.util.Week;

import com.commonsware.cwac.merge.MergeAdapter;
import com.google.gson.Gson;

public class ProgressFragment extends Fragment {
	private static final String				TAG							= ProgressFragment.class.getSimpleName();

	public static final String				INTENT_EXTRA_WEEKTIMESTAMP	= "INTENT_EXTRA_WEEKTIMESTAMP";

	private static int[]					PIE_COLORS					= new int[] { Color.rgb(218, 143, 74), Color.rgb(74, 120, 218),
			Color.rgb(74, 218, 111), Color.rgb(74, 205, 218), Color.rgb(132, 74, 218) };
	private static int[]					BAR_COLORS					= new int[] { Color.rgb(217, 147, 79), Color.rgb(183, 217, 79),
			Color.rgb(79, 192, 217)									};
	private static String[]					daysOfWeek					= new String[] { "Sunday", "Monday", "Tuesday", "Wednesday",
			"Thursday", "Friday", "Saturday"							};
	private static int						BGColor						= Color.parseColor("#DDF0FF");

	private static Typeface					bold;

	private ListView						listView;
	private LinearLayout					loadingLayout;
	private ViewGroup						root;

	private Week							week;
	private CategorySeries					pieSeries					= new CategorySeries("");
	private DefaultRenderer					pieRenderer					= new DefaultRenderer();
	private MergeAdapter					adapter;

	private DatabaseHelper					dbHelper;
	private UserSessionProvider				userSessionProvider;
	private ArrayList<AssessmentSummary>	assessmentSummaries;
	private HashMap<String, Book>			mBooks;
	private WeeklyTimeReport				weeklyTimeReport;

	private LoadTimeAnalysisTask			loadTimeAnalysisTask;
	private LoadScoresTask					loadScoresTask;
	private boolean							timeReady, scoresReady;

	public ProgressFragment() {
		super();
	}

	public static ProgressFragment newInstance(long weekBeingTimestamp) {
		Bundle b = new Bundle();
		b.putLong(INTENT_EXTRA_WEEKTIMESTAMP, weekBeingTimestamp);
		ProgressFragment f = new ProgressFragment();
		f.setArguments(b);
		return f;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		root = (ViewGroup) inflater.inflate(R.layout.fragment_progress, container, false);
		listView = (ListView) root.findViewById(R.id.list);
		listView.setEmptyView(root.findViewById(android.R.id.empty));
		loadingLayout = (LinearLayout) root.findViewById(R.id.loading);
		return root;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (bold == null)
			bold = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Roboto-Bold.ttf");
		userSessionProvider = UserSessionProvider.getInstance(getActivity());
		dbHelper = DatabaseHelper.getInstance(getActivity());
		mBooks = new HashMap<String, Book>();

		DiviCalendar cal = DiviCalendar.get();
		cal.setTimeInMillis(getArguments().getLong(INTENT_EXTRA_WEEKTIMESTAMP));
		week = Week.getWeek(cal);

		// start (re)loading data
		loadingLayout.setVisibility(View.VISIBLE);
		listView.setVisibility(View.GONE);
		timeReady = false;
		loadTimeAnalysisTask = new LoadTimeAnalysisTask();
		loadTimeAnalysisTask.execute(new Void[0]);

		scoresReady = false;
		loadScoresTask = new LoadScoresTask();
		loadScoresTask.execute(new Void[0]);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onStop() {
		super.onStop();
		if (loadTimeAnalysisTask != null)
			loadTimeAnalysisTask.cancel(false);
		if (loadScoresTask != null)
			loadScoresTask.cancel(false);
	}

	private void setupPie(LinearLayout pieChartLayout, LinearLayout pieChartLegend) {
		if (weeklyTimeReport == null)
			return;// wait for report to load
		pieRenderer.setStartAngle(90);
		// mRenderer.setDisplayValues(true);

		String[] titles = new String[weeklyTimeReport.bookTimes.length];
		double[] values = new double[weeklyTimeReport.bookTimes.length];
		pieSeries.clear();
		pieChartLegend.removeAllViews();
		for (int i = 0; i < weeklyTimeReport.bookTimes.length; i++) {
			pieSeries.add(weeklyTimeReport.bookTimes[i].bookId, weeklyTimeReport.bookTimes[i].timeSpent);
			String bookId = weeklyTimeReport.bookTimes[i].bookId;
			if (mBooks.containsKey(bookId))
				titles[i] = mBooks.get(bookId).name;
			else
				titles[i] = "#" + bookId;
			values[i] = (weeklyTimeReport.bookTimes[i].timeSpent / (3600 * 1000.0));
		}

		// Instantiating a renderer for the Pie Chart
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		pieRenderer.removeAllRenderers();
		for (int i = 0; i < weeklyTimeReport.bookTimes.length; i++) {
			SimpleSeriesRenderer seriesRenderer = new SimpleSeriesRenderer();
			seriesRenderer.setColor(PIE_COLORS[i % PIE_COLORS.length]);
			seriesRenderer.setDisplayChartValues(true);
			seriesRenderer.setShowLegendItem(false);
			// Adding a renderer for a slice
			pieRenderer.addSeriesRenderer(seriesRenderer);
			// setup legend
			TextView tv = new TextView(getActivity());
			tv.setTextSize(18);
			tv.setText("█  " + titles[i]);
			tv.setTextColor(PIE_COLORS[i % PIE_COLORS.length]);
			pieChartLegend.addView(tv, lp);
		}

		pieRenderer.setLabelsTextSize(20);
		pieRenderer.setLabelsColor(Color.parseColor("#4f4f4f"));
		pieRenderer.setZoomButtonsVisible(false);
		pieRenderer.setZoomEnabled(false);
		pieRenderer.setPanEnabled(false);
		pieRenderer.setDisplayValues(true);
		pieRenderer.setShowLegend(false);
		pieRenderer.setBackgroundColor(BGColor);

		MultipleCategorySeries donutCategorySeries = new MultipleCategorySeries("");
		donutCategorySeries.add(titles, values);

		GraphicalView mChartView = ChartFactory.getDoughnutChartView(getActivity(), donutCategorySeries, pieRenderer);
		// ChartFactory.getPieChartView(getActivity(),pieSeries,pieRenderer);
		pieChartLayout.removeAllViews();
		pieChartLayout.addView(mChartView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mChartView.repaint();
	}

	private void setupBar(ViewGroup barChartLayout) {
		if (weeklyTimeReport == null)
			return;// wait for report to load
		// Creating an XYSeries for Income
		XYSeries learnTimeSeries = new XYSeries("Learn   ");
		XYSeries assessmentTimeSeries = new XYSeries("Assessment   ");
		XYSeries videoTimeSeries = new XYSeries("Video   ");
		// Adding data to Series
		for (int i = 0; i < weeklyTimeReport.dayTimes.length; i++) {
			DayTime dt = weeklyTimeReport.dayTimes[i];
			int index = getDayIndex(dt.dayOfWeek);
			learnTimeSeries.add(index, dt.learnTime / (3600 * 1000.0));
			assessmentTimeSeries.add(index, (dt.learnTime + dt.assessmentTime) / (3600 * 1000.0));
			videoTimeSeries.add(index, (dt.learnTime + dt.assessmentTime + dt.videoTime) / (3600 * 1000.0));
		}

		// Creating a dataset to hold each series
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		dataset.addSeries(videoTimeSeries);
		dataset.addSeries(assessmentTimeSeries);
		dataset.addSeries(learnTimeSeries);

		// Creating XYSeriesRenderer to customize incomeSeries
		XYSeriesRenderer learnRenderer = new XYSeriesRenderer();
		learnRenderer.setColor(BAR_COLORS[0]);
		learnRenderer.setFillPoints(true);
		learnRenderer.setLineWidth(2);
		// learnRenderer.setDisplayChartValues(true);

		// Creating XYSeriesRenderer to customize expenseSeries
		XYSeriesRenderer assRenderer = new XYSeriesRenderer();
		assRenderer.setColor(BAR_COLORS[1]);
		assRenderer.setFillPoints(true);
		assRenderer.setLineWidth(2);
		// assRenderer.setDisplayChartValues(true);

		// Creating XYSeriesRenderer to customize expenseSeries
		XYSeriesRenderer videoRenderer = new XYSeriesRenderer();
		videoRenderer.setColor(BAR_COLORS[2]);
		videoRenderer.setFillPoints(true);
		videoRenderer.setLineWidth(2);
		// videoRenderer.setDisplayChartValues(true);

		// Creating a XYMultipleSeriesRenderer to customize the whole chart
		XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();

		multiRenderer.setZoomButtonsVisible(false);
		multiRenderer.setPanEnabled(false, false);
		multiRenderer.setZoomEnabled(false, false);

		multiRenderer.setApplyBackgroundColor(true);
		multiRenderer.setBackgroundColor(BGColor);
		multiRenderer.setMarginsColor(BGColor);

		// multiRenderer.setBarSpacing(5);
		multiRenderer.setBarWidth(30);
		multiRenderer.setLabelsTextSize(20);
		// multiRenderer.setAxisTitleTextSize(20);

		// multiRenderer.setLegendHeight(70);
		// multiRenderer.setLegendTextSize(25);
		multiRenderer.setShowLegend(false);

		multiRenderer.clearXTextLabels();
		multiRenderer.setXLabels(0);
		multiRenderer.setYLabels(3);
		multiRenderer.setXLabelsAngle(-30);
		multiRenderer.setXLabelsAlign(Align.RIGHT);
		multiRenderer.setXLabelsPadding(-5);
		// multiRenderer.setXLabelsPadding(5);

		multiRenderer.setShowAxes(false);
		multiRenderer.setLabelFormat(new DecimalFormat("0.0 'Hr'"));
		// multiRenderer.setYLabelsPadding(15);
		multiRenderer.setYLabelsAlign(Align.RIGHT);
		multiRenderer.setGridColor(Color.BLACK);
		multiRenderer.setLabelsColor(Color.BLACK);

		multiRenderer.setShowGridX(true);
		// multiRenderer.setYTitle("Time spent");
		for (int i = 0; i < weeklyTimeReport.dayTimes.length; i++) {
			multiRenderer.addXTextLabel(getDayIndex(i + 1), daysOfWeek[i]);
		}

		// Note: The order of adding dataseries to detest and renderers to multipleRenderer
		// should be same
		multiRenderer.addSeriesRenderer(videoRenderer);
		multiRenderer.addSeriesRenderer(assRenderer);
		multiRenderer.addSeriesRenderer(learnRenderer);

		int[] margins = multiRenderer.getMargins();
		// Log.d(TAG, "margins:" + margins[0] + "," + margins[1] + "," + margins[2] + "," + margins[3]);
		multiRenderer.setMargins(new int[] { margins[0] + 15, margins[1] + 30, margins[2] + 45, margins[3] + 15 });

		GraphicalView mChartView = ChartFactory.getBarChartView(getActivity(), dataset, multiRenderer, Type.STACKED);
		barChartLayout.removeAllViews();
		barChartLayout.addView(mChartView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mChartView.repaint();
	}

	private int getDayIndex(int dayOfWeek) {
		return (dayOfWeek - DiviCalendar.FIRST_DAY_OF_WEEK + 7) % 7;
	}

	private void drawReport() {
		loadingLayout.setVisibility(View.GONE);
		listView.setVisibility(View.VISIBLE);
		adapter = new MergeAdapter();
		LayoutInflater inflater = LayoutInflater.from(getActivity());
		LinearLayout progressTop = (LinearLayout) inflater.inflate(R.layout.item_progress_top, null);
		ImageView needle = (ImageView) progressTop.findViewById(R.id.score_needle);
		TextView pieTitle = (TextView) progressTop.findViewById(R.id.pieTitle);
		TextView barTitle = (TextView) progressTop.findViewById(R.id.barTitle);
		LinearLayout pieChart = (LinearLayout) progressTop.findViewById(R.id.pieChart);
		LinearLayout pieLegend = (LinearLayout) progressTop.findViewById(R.id.pieLegend);
		LinearLayout barChart = (LinearLayout) progressTop.findViewById(R.id.barChart);
		if (timeReady) {
			((TextView) progressTop.findViewById(R.id.timeText)).setText(TextUtil.getTimeText(weeklyTimeReport.totalTime));
			pieTitle.setVisibility(View.VISIBLE);
			pieChart.setVisibility(View.VISIBLE);
			barTitle.setVisibility(View.VISIBLE);
			barChart.setVisibility(View.VISIBLE);
			setupPie(pieChart, pieLegend);
			setupBar(barChart);
		}
		if (timeReady || scoresReady)
			adapter.addView(progressTop);
		if (scoresReady) {
			SparseArray<ArrayList<AssessmentSummary>> summariesByDay = new SparseArray<ArrayList<AssessmentSummary>>();
			int totalScore = 0;
			for (final AssessmentSummary summary : assessmentSummaries) {
				totalScore += summary.diviScore;
				DiviCalendar cal = DiviCalendar.get();
				cal.setTimeInMillis(summary.unlockCommand.appliedAt);
				cal.getTimeInMillis();
				int dayOfWeek = cal.get(DiviCalendar.DAY_OF_WEEK);
				Log.d(TAG, "adding summary:" + summary.assessmentNode.name + ", " + dayOfWeek);
				if (summariesByDay.get(dayOfWeek) == null)
					summariesByDay.put(dayOfWeek, new ArrayList<AssessmentSummary>());
				summariesByDay.get(dayOfWeek).add(summary);
			}
			if (totalScore > 0)
				totalScore /= assessmentSummaries.size();
			int angle = ((totalScore - 50) * 9) / 5;

			((TextView) progressTop.findViewById(R.id.scoreText)).setText("" + totalScore);
			needle.setImageResource(R.drawable.score_widget_needle);
			Animation anim = new RotateAnimation(-90, angle, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.6617f);
			anim.setFillAfter(true);
			anim.setDuration(1500);
			needle.setAnimation(anim);

			ListView.LayoutParams lp = new ListView.LayoutParams(ListView.LayoutParams.WRAP_CONTENT, ListView.LayoutParams.WRAP_CONTENT);
			for (int i = 0; i < 7; i++) {
				// start from first day of week
				int dayOfWeek = ((i + DiviCalendar.FIRST_DAY_OF_WEEK - 1) % 7) + 1;
				ArrayList<AssessmentSummary> summaries = summariesByDay.get(dayOfWeek);
				String dayName = daysOfWeek[dayOfWeek - 1];
				Log.d(TAG, "reports for " + dayName);
				if (summaries != null && summaries.size() > 0) {
					Log.d(TAG, "adding summaries:" + summaries.size());
					TextView dayNameTV = new TextView(getActivity());
					dayNameTV.setText("  " + dayName.toUpperCase() + "  ");
					dayNameTV.setTextSize(18);
					dayNameTV.setTextColor(Color.WHITE);
					dayNameTV.setBackgroundResource(R.drawable.bg_week);
					dayNameTV.setLayoutParams(lp);
					adapter.addView(dayNameTV, false);
					// group summaries by book
					HashMap<String, ArrayList<AssessmentSummary>> summariesByBook = new HashMap<String, ArrayList<AssessmentSummary>>();
					for (AssessmentSummary as : summaries) {
						if (!summariesByBook.containsKey(as.book.id))
							summariesByBook.put(as.book.id, new ArrayList<AssessmentSummary>());
						summariesByBook.get(as.book.id).add(as);
					}
					ArrayList<AssessmentSummary[]> summariesByWeekByBook = new ArrayList<AssessmentSummary[]>();
					for (Entry<String, ArrayList<AssessmentSummary>> bookSummaries : summariesByBook.entrySet()) {
						summariesByWeekByBook.add(bookSummaries.getValue().toArray(new AssessmentSummary[bookSummaries.getValue().size()]));
					}
					adapter.addAdapter(new AssessmentAdapter(getActivity(), summariesByWeekByBook));
				}
			}
		}
		listView.setAdapter(adapter);
	}

	private void showAssessmentDetails(AssessmentSummary summary) {
		AssessmentStudentReportFragment f = new AssessmentStudentReportFragment(summary);
		FragmentManager fm = getActivity().getFragmentManager();
		f.show(fm, "as_fragment");
	}

	class LoadTimeAnalysisTask extends AsyncTask<Void, Void, Integer> {
		String				uid;
		WeeklyTimeReport	report;
		File				reportFile;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			uid = userSessionProvider.getUserData().uid;
			reportFile = AnalyticsManager.getInstance(getActivity()).getWeekReportFile(uid, week);
		}

		@Override
		protected Integer doInBackground(Void... params) {
			try {
				Log.d(TAG, "reading report file - " + reportFile);
				if (reportFile.exists()) {
					report = new Gson().fromJson(Util.openJSONFile(reportFile), WeeklyTimeReport.class);
					return 0;
				} else
					return 1;
			} catch (Exception e) {
				Log.w(TAG, "Error loading progress - ", e);
				return 2;
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result == 0) {
				Log.d(TAG, "time report ready!");
				weeklyTimeReport = report;
				timeReady = true;
				drawReport();
			} else if (result == 1) {
				Toast.makeText(getActivity(), "Time report not ready", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getActivity(), "Error loading progress", Toast.LENGTH_LONG).show();
			}
		}
	}

	class LoadScoresTask extends AsyncTask<Void, Void, Integer> {
		String								uid;
		HashMap<String, Book>				books;
		HashMap<String, AssessmentSummary>	summaries;
		String[]							allCourseIds;
		Date[]								bounds;

		String								commandSelectionClause, attemptSelectionClause;
		String[]							commandSelectionArgs;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			uid = userSessionProvider.getUserData().uid;
			bounds = week.getBounds();
			books = new HashMap<String, Book>();
			summaries = new HashMap<String, AssessmentSummary>();
			allCourseIds = userSessionProvider.getAllCourseIds();

			commandSelectionClause = Commands.STATUS + " = ? AND " + Commands.UID + " = ? AND " + Commands.APPLY_TIMESTAMP + " >= ? AND "
					+ Commands.APPLY_TIMESTAMP + " <= ? AND " + Commands.TYPE + " = ? ";

			commandSelectionArgs = new String[] { "" + Command.COMMAND_STATUS_ACTIVE, uid, "" + bounds[0].getTime(),
					"" + bounds[1].getTime(), "" + Command.COMMAND_CATEGORY_UNLOCK };

			attemptSelectionClause = Attempts.UID + " = ? AND " + Attempts.COURSE_ID + " = ? AND " + Attempts.BOOK_ID + " = ? AND "
					+ Attempts.ASSESSMENT_ID + " = ? ";
		}

		@Override
		protected Integer doInBackground(Void... params) {
			try {
				// 1. get all books details

				for (String courseId : allCourseIds)
					for (Book book : dbHelper.getBooks(courseId))
						books.put(book.id, book);
				// 2. get all commands in timeframe of the week

				Cursor cursor = getActivity().getContentResolver().query(UserDBContract.Commands.CONTENT_URI,
						UserDBContract.Commands.PROJECTION_ALL, commandSelectionClause, commandSelectionArgs, Commands.SORT_ORDER_DEFAULT);
				if (cursor != null && cursor.moveToFirst()) {
					do {
						if (isCancelled())
							break;
						Command c = Command.fromCursor(cursor);
						if (c.endsAt < System.currentTimeMillis()) {
							AssessmentSummary summary = new AssessmentSummary();
							summary.unlockCommand = c;
							summary.book = books.get(summary.unlockCommand.bookId);
							summaries.put(c.itemCode, summary);
						}
					} while (cursor.moveToNext());
					cursor.close();
				}
				// 3. get all question details of the quizzes/tests

				ArrayList<String> toRemove = new ArrayList<String>();
				for (Entry<String, AssessmentSummary> summary : summaries.entrySet()) {
					Node n = dbHelper.getNode(summary.getValue().unlockCommand.itemCode, summary.getValue().unlockCommand.bookId,
							summary.getValue().unlockCommand.courseId);
					if (n == null)
						toRemove.add(summary.getKey());
					summary.getValue().assessmentNode = n;
				}
				for (String removeId : toRemove)
					summaries.remove(removeId);
				// 4. get attempts of all quizzes/tests unlocked in the week

				for (Entry<String, AssessmentSummary> summary : summaries.entrySet()) {
					Node assNode = summary.getValue().assessmentNode;
					String[] mSelectionArgs = new String[] { uid, assNode.courseId, assNode.bookId, assNode.id };
					Cursor attemptCursor = getActivity().getContentResolver().query(Attempts.CONTENT_URI, Attempts.PROJECTION_ALL,
							attemptSelectionClause, mSelectionArgs, Attempts.SORT_ORDER_DEFAULT);
					if (attemptCursor != null && attemptCursor.moveToFirst()) {
						do {
							if (isCancelled())
								return null;
							Attempt a = Attempt.fromCursor(attemptCursor);
							summary.getValue().attempts.put(a.questionId, a);
						} while (attemptCursor.moveToNext());
					}
					if (attemptCursor != null)
						attemptCursor.close();
				}
				// 5. Compute the scores
				if (isCancelled())
					return null;

				for (Entry<String, AssessmentSummary> summary : summaries.entrySet()) {
					Node assNode = summary.getValue().assessmentNode;

					AssessmentFileModel.AssessmentScorecard scorecard = ((AssessmentFileModel) assNode.getTag()).getScorecard(summary
							.getValue().attempts);

					summary.getValue().totalPoints = scorecard.userPoints;
					summary.getValue().maxPoints = scorecard.maxPoints;
					summary.getValue().avgAccuracy = scorecard.avgAccuracy;// only attempted questions
					summary.getValue().diviScore = scorecard.diviScore;
				}
				return 0;
			} catch (Exception e) {
				Log.w(TAG, "error calculating assessments summary", e);
				return 1;
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result == 0) {
				mBooks = books;
				ProgressFragment.this.assessmentSummaries = new ArrayList<AssessmentSummary>();
				for (Entry<String, AssessmentSummary> entry : summaries.entrySet()) {
					ProgressFragment.this.assessmentSummaries.add(entry.getValue());
				}
				Collections.sort(ProgressFragment.this.assessmentSummaries);

				scoresReady = true;
				drawReport();
			} else {
				Toast.makeText(getActivity(), "Error fetching Assessments summary", Toast.LENGTH_LONG).show();
			}
		}
	}

	class AssessmentAdapter extends ArrayAdapter<AssessmentSummary[]> {

		LinearLayout.LayoutParams	lp;

		public AssessmentAdapter(Context context, ArrayList<AssessmentSummary[]> summariesCollection) {
			super(context, R.layout.item_progress_assessment, summariesCollection);
			lp = new LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
					android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final AssessmentSummary[] summaries = getItem(position);
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_progress_assessment, parent, false);
			}
			// setup book box
			TextView buttonText1 = (TextView) convertView.findViewById(R.id.book_button_text1);
			TextView buttonText2 = (TextView) convertView.findViewById(R.id.book_button_text2);
			buttonText1.setTypeface(bold);
			buttonText1.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
			String bookName = summaries[0].book.name;
			LinearLayout container = (LinearLayout) convertView.findViewById(R.id.assessmentsList);
			container.removeAllViews();
			buttonText1.setText(bookName.substring(0, 1).toUpperCase() + bookName.substring(1, 2).toLowerCase());
			buttonText2.setText(bookName.toUpperCase());
			for (final AssessmentSummary summary : summaries) {
				View convertView2 = LayoutInflater.from(getContext()).inflate(R.layout.item_progress_assessment2, (ViewGroup) convertView,
						false);
				((TextView) convertView2.findViewById(R.id.chapName)).setText(summary.assessmentNode.parentName);
				((TextView) convertView2.findViewById(R.id.assessmentName)).setText(summary.assessmentNode.name);
				((TextView) convertView2.findViewById(R.id.assessmentScore)).setText("" + summary.diviScore);
				convertView2.setClickable(true);
				convertView2.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showAssessmentDetails(summary);
					}
				});
				container.addView(convertView2, lp);
			}
			return convertView;
		}

		@Override
		public boolean isEnabled(int position) {
			return false;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
		super.onSaveInstanceState(outState);
	}
}
