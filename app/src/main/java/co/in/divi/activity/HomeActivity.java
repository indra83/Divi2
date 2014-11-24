package co.in.divi.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.app.DownloadManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import co.in.divi.BaseActivity;
import co.in.divi.ContentUpdateManager;
import co.in.divi.ContentUpdateManager.ContentUpdateListener;
import co.in.divi.ContentUpdateManager.UpdateStatus;
import co.in.divi.LocationManager;
import co.in.divi.LocationManager.LOCATION_TYPE;
import co.in.divi.R;
import co.in.divi.content.Book;
import co.in.divi.content.DatabaseHelper;
import co.in.divi.fragment.AppsFragment;
import co.in.divi.fragment.HeaderFragment;
import co.in.divi.fragment.ProgressFragment;
import co.in.divi.progress.AnalyticsFetcherService;
import co.in.divi.progress.AnalyticsManager;
import co.in.divi.util.Config;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Util;
import co.in.divi.util.Week;

public class HomeActivity extends BaseActivity implements ContentUpdateListener {
	static final String				TAG					= HomeActivity.class.getSimpleName();

	private static final int		SECTION_LEARN		= 0;
	private static final int		SECTION_PRACTICE	= 1;
	private static final int		SECTION_PROGRESS	= 2;
	private static final int		SECTION_APPS		= 3;

	private int						currentSection		= -1;
	private boolean					progressInitialized	= false;

	private View					learnButton, practiceButton, progressButton, appsButton;
	private ImageView				titleIcon, batteryIcon, syncIcon;
	private TextView				titleText, updatesText, batteryText, timeText, progressPagerEmpty;
	private GridView				bookButtonsGrid;
	private ViewPager				progressPager;
	private AppsFragment			appsFragment;

	private ContentUpdateManager	contentUpdateManager;
	private DatabaseHelper			dbHelper;
	private BooksAdapter			adapter;
	private FetchBooksTask			fetchBooksTask;

	private Timer					timer				= new Timer();
	private Timer					timeDisplayTimer	= new Timer();
	private Handler					handler				= new Handler();

	private BroadcastReceiver		batteryReceiver		= new BroadcastReceiver() {
															int	scale	= -1;
															int	level	= -1;
															int	voltage	= -1;
															int	temp	= -1;

															@Override
															public void onReceive(Context context, Intent intent) {
																level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
																scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
																temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
																voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
																if (LogConfig.DEBUG_ACTIVITIES)
																	Log.d("BatteryManager", "level is " + level + "/" + scale
																			+ ", temp is " + temp + ", voltage is " + voltage);
																int percent = (int) (level * 100.0 / scale);
																batteryText.setText("  " + percent + " %");
																if (percent < 15) {
																	batteryIcon.setImageResource(R.drawable.ic_battery1);
																} else if (percent < 30) {
																	batteryIcon.setImageResource(R.drawable.ic_battery2);
																} else if (percent < 50) {
																	batteryIcon.setImageResource(R.drawable.ic_battery3);
																} else if (percent < 80) {
																	batteryIcon.setImageResource(R.drawable.ic_battery4);
																} else {
																	batteryIcon.setImageResource(R.drawable.ic_battery5);
																}

															}
														};
	private IntentFilter			filter				= new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		contentUpdateManager = ContentUpdateManager.getInstance(this);
		dbHelper = DatabaseHelper.getInstance(this);
		adapter = new BooksAdapter();
		setContentView(R.layout.activity_home);
		learnButton = findViewById(R.id.home_learn);
		practiceButton = findViewById(R.id.home_practice);
		progressButton = findViewById(R.id.home_progress);
		appsButton = findViewById(R.id.home_apps);
		titleIcon = (ImageView) findViewById(R.id.home_title_icon);
		batteryIcon = (ImageView) findViewById(R.id.battery_icon);
		syncIcon = (ImageView) findViewById(R.id.sync_icon);
		titleText = (TextView) findViewById(R.id.home_title_text);
		updatesText = (TextView) findViewById(R.id.update_text);
		batteryText = (TextView) findViewById(R.id.battery_text);
		progressPagerEmpty = (TextView) findViewById(R.id.progressPagerEmpty);
		timeText = (TextView) findViewById(R.id.time_text);
		bookButtonsGrid = (GridView) findViewById(R.id.books);
		progressPager = (ViewPager) findViewById(R.id.progressPager);
		appsFragment = (AppsFragment) getFragmentManager().findFragmentById(R.id.apps);
		getFragmentManager().beginTransaction().hide(appsFragment).commit();

		bookButtonsGrid.setAdapter(adapter);
		bookButtonsGrid.setOnItemClickListener(new OnItemClickListener() {
			@SuppressLint("NewApi")
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				Book book = (Book) adapter.getItem(position);
				Intent launchBook;
				if (currentSection == SECTION_LEARN) {
					launchBook = new Intent(HomeActivity.this, LearnActivity.class);
				} else {
					launchBook = new Intent(HomeActivity.this, ListAssessmentsActivity.class);
				}
				launchBook.putExtra(Util.INTENT_EXTRA_BOOK, book);
				Bundle b = null;
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					b = ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight()).toBundle();
				}
				startActivity(launchBook, b);
			}
		});

		learnButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				setSection(SECTION_LEARN);
			}
		});
		practiceButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				setSection(SECTION_PRACTICE);
			}
		});
		progressButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				setSection(SECTION_PROGRESS);
			}
		});
		appsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				setSection(SECTION_APPS);
			}
		});

		// Debug
		Button debugButton = (Button) findViewById(R.id.debug_button);
		if (!Config.SHOW_DEBUG_BUTTON)
			debugButton.setVisibility(View.GONE);
		debugButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent launchSync = new Intent(HomeActivity.this, AnalyticsFetcherService.class);
				startService(launchSync);
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		registerReceiver(batteryReceiver, filter);
		contentUpdateManager.addListener(this);
		if (currentSection < 0)
			setSection(SECTION_LEARN);
		loadBooks();
		refreshUpdatesText();
		((HeaderFragment) getFragmentManager().findFragmentById(R.id.header)).disableBack();
		timeDisplayTimer.cancel();
		timeDisplayTimer = new Timer();
		timeDisplayTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						Calendar c = Calendar.getInstance();
						SimpleDateFormat df = new SimpleDateFormat("HH:mm");
						String formattedDate = df.format(c.getTime());
						timeText.setText("" + formattedDate);
					}
				});
			}
		}, 0, 1000);

		String versionName = "--";
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			Log.w(TAG, "error getting version name", e);
		}
		((TextView) findViewById(R.id.version)).setText(versionName);
		refreshUpdatesText();
	}

	@Override
	protected void onResume() {
		super.onResume();
		LocationManager.getInstance(this).setNewLocation(LOCATION_TYPE.HOME, null, null, null, null);
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(batteryReceiver);
		timer.cancel();
		timeDisplayTimer.cancel();
		contentUpdateManager.removeListener(this);
		if (fetchBooksTask != null)
			fetchBooksTask.cancel(false);
	}

	private void refreshUpdatesText() {
		updatesText.setText(contentUpdateManager.getStatusString());
		if (contentUpdateManager.getUpdateStatus() == UpdateStatus.DOWNLOADING_UPDATE) {
			timer.cancel();
			timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					Cursor c = null;
					try {
						DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
						c = dm.query(new DownloadManager.Query().setFilterById(contentUpdateManager.getCurrentDownloadId()));

						if (c == null || c.getCount() == 0) {
							if (LogConfig.DEBUG_ACTIVITIES)
								Log.d(TAG, "download went missing...");
							handler.post(new Runnable() {
								@Override
								public void run() {
									if (contentUpdateManager.getUpdateStatus() != UpdateStatus.DOWNLOADING_UPDATE)
										timer.cancel();
									Toast.makeText(HomeActivity.this, "Download not found!", Toast.LENGTH_LONG).show();
								}
							});
						} else {
							c.moveToFirst();
							final long downloadedBytes = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
							if (LogConfig.DEBUG_ACTIVITIES) {
								Log.d(TAG, "COLUMN_ID: " + c.getLong(c.getColumnIndex(DownloadManager.COLUMN_ID)));
								Log.d(TAG, "downloaded : " + downloadedBytes);
								Log.d(TAG, "total: " + c.getInt(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)));
								Log.d(TAG, "local uri: " + c.getInt(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
								Log.d(TAG, "local file: " + c.getInt(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME)));
								Log.d(TAG, "COLUMN_STATUS: " + c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)));
								Log.d(TAG, "COLUMN_REASON: " + c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON)));
							}

							handler.post(new Runnable() {
								@Override
								public void run() {
									String percentDownload = " (" + String.format("%.2f", ((downloadedBytes / 10000) * 0.01)) + " MB)";
									updatesText.setText(contentUpdateManager.getStatusString() + percentDownload);
								}
							});
						}
					} catch (Exception e) {
						Log.w(TAG, "error fetching download state", e);
					} finally {
						if (c != null)
							c.close();
					}
				}
			}, 0, 5000);
		} else {
			timer.cancel();
		}
		if (contentUpdateManager.getUpdateStatus() == UpdateStatus.NONE) {
			boolean showWarn = (Util.getTimestampMillis() - contentUpdateManager.getLastUpdateTime()) > Config.CONTENT_UPDATE_WARN;
			if (showWarn)
				syncIcon.setImageResource(R.drawable.ic_update_fail);
			else
				syncIcon.setImageResource(R.drawable.ic_update_ok);
		} else {
			syncIcon.setImageResource(R.drawable.ic_update_loading);
		}
	}

	private void loadBooks() {
		if (userSessionProvider.getCourseId() != null) {
			fetchBooksTask = new FetchBooksTask();
			fetchBooksTask.execute(new Void[0]);
		} else {
			Toast.makeText(this, "No courses available", Toast.LENGTH_LONG).show();
		}
	}

	private void setSection(int section) {
		if (currentSection == section)
			return;
		currentSection = section;
		learnButton.setBackgroundResource(0);
		practiceButton.setBackgroundResource(0);
		progressButton.setBackgroundResource(0);
		appsButton.setBackgroundResource(0);
		bookButtonsGrid.setVisibility(View.VISIBLE);
		if (appsFragment.isAdded() && appsFragment.isVisible())
			getFragmentManager().beginTransaction().hide(appsFragment).commit();
		switch (section) {
		case SECTION_LEARN:
			learnButton.setBackgroundResource(R.drawable.bg_home_section_selected);
			titleIcon.setImageResource(R.drawable.ic_home_learn_grey);
			titleText.setText("Start learning by selecting a subject");
			hideProgress();
			break;
		case SECTION_PRACTICE:
			practiceButton.setBackgroundResource(R.drawable.bg_home_section_selected);
			titleIcon.setImageResource(R.drawable.ic_home_practice_grey);
			titleText.setText("Start practicing by selecting a subject");
			hideProgress();
			break;
		case SECTION_PROGRESS:
			progressButton.setBackgroundResource(R.drawable.bg_home_section_selected);
			titleIcon.setImageResource(R.drawable.ic_home_progress_grey);
			titleText.setText("View progress");
			bookButtonsGrid.setVisibility(View.GONE);
			showProgress();
			break;
		case SECTION_APPS:
			appsButton.setBackgroundResource(R.drawable.bg_home_section_selected);
			titleIcon.setImageResource(R.drawable.ic_home_progress_grey);
			titleText.setText("Apps");
			bookButtonsGrid.setVisibility(View.GONE);
			hideProgress();
			getFragmentManager().beginTransaction().show(appsFragment).commit();
			break;
		}

		refreshContent();
	}

	private void refreshContent() {

	}

	private void showProgress() {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "showProgress");
		progressPager.setVisibility(View.VISIBLE);
		if (!progressInitialized) {
			ProgressPagerAdapter adapter = new ProgressPagerAdapter(getFragmentManager(), AnalyticsManager.getInstance(this).getAllWeeks());
			progressPager.setAdapter(adapter);
			progressPager.setCurrentItem(adapter.getCount() - 1);
			if (adapter.getCount() == 0)
				progressPagerEmpty.setVisibility(View.VISIBLE);
		}
	}

	private void hideProgress() {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "hideProgress");
		progressPager.setVisibility(View.GONE);
		progressPagerEmpty.setVisibility(View.GONE);
	}

	class BooksAdapter extends BaseAdapter {
		ArrayList<Book>	books	= new ArrayList<Book>();
		LayoutInflater	inflater;

		Typeface		bold;

		public BooksAdapter() {
			inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			bold = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf");
		}

		public void setBooks(ArrayList<Book> newBooks) {
			this.books = newBooks;
			notifyDataSetChanged();
		}

		public int getCount() {
			return books.size();
		}

		public Object getItem(int position) {
			return books.get(position);
		}

		public long getItemId(int position) {
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.widget_book_button, parent, false);
			}
			TextView buttonText1 = (TextView) convertView.findViewById(R.id.book_button_text1);
			TextView buttonText2 = (TextView) convertView.findViewById(R.id.book_button_text2);
			buttonText1.setTypeface(bold);
			buttonText1.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
			Book book = (Book) getItem(position);

			if (book.name == null || book.name.length() < 2) {
				return convertView;
			}
			buttonText1.setText(book.name.substring(0, 1).toUpperCase() + book.name.substring(1, 2).toLowerCase());
			buttonText2.setText(book.name.toUpperCase());
			return convertView;
		}
	}

	class FetchBooksTask extends AsyncTask<Void, Void, Integer> {

		ArrayList<Book>	books;
		String			courseId;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			courseId = userSessionProvider.getCourseId();
		}

		@Override
		protected Integer doInBackground(Void... arg0) {
			try {
				books = dbHelper.getBooks(courseId);
				return books.size();
			} catch (Exception e) {
				Log.e(TAG, "error fetching books!", e);
				return -1;
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result > 0) {
				adapter.setBooks(books);
			} else if (result == 0) {
				adapter.setBooks(new ArrayList<Book>());
				Toast.makeText(HomeActivity.this, "No books Available!", Toast.LENGTH_LONG).show();
			} else {
				adapter.setBooks(new ArrayList<Book>());
				Toast.makeText(HomeActivity.this, "Error fetching books. Please try restarting the tablet!", Toast.LENGTH_LONG).show();
			}
		}
	}

	class ProgressPagerAdapter extends FragmentStatePagerAdapter {
		ArrayList<Week>	weeks;

		public ProgressPagerAdapter(FragmentManager fm, ArrayList<Week> weeks) {
			super(fm);
			this.weeks = weeks;
			if (LogConfig.DEBUG_ACTIVITIES)
				Log.d(TAG, "got weeks:" + weeks.size());
		}

		@Override
		public Fragment getItem(int position) {
			if (LogConfig.DEBUG_ACTIVITIES)
				Log.d(TAG, "returning frag:" + position);
			ProgressFragment f = ProgressFragment.newInstance(weeks.get(position).weekBeginTimestamp);
			return f;
		}

		@Override
		public int getCount() {
			return weeks.size();
		}

		@Override
		public int getItemPosition(Object object) {
			// force refresh on notify dataset change.
			return POSITION_NONE;
		}

		@Override
		public void setPrimaryItem(ViewGroup container, int position, Object object) {
			super.setPrimaryItem(container, position, object);
			// Log.d(TAG, "setting primary item, position:	" + position);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return weeks.get(position).getDisplayString();
		}
	}

	@Override
	public void onCourseChange() {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "reloading books");
		loadBooks();
	}

	@Override
	public void onBookUpdating(String bookId) {
	}

	@Override
	public void onContentUIChange() {
		refreshUpdatesText();
		loadBooks();
	}
}