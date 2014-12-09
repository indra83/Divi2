package co.in.divi.fragment;

import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import co.in.divi.LectureSessionProvider;
import co.in.divi.LectureSessionProvider.LectureStatusChangeListener;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.content.AllowedAppsProvider.Apps;
import co.in.divi.util.Config;
import co.in.divi.util.LogConfig;

public class AppsFragment extends Fragment implements LoaderCallbacks<Cursor>, LectureStatusChangeListener {
	private static final String	TAG					= AppsFragment.class.getSimpleName();

	private static final int	ALLOWED_APPS_LOADER	= 1;

	private ListView			listView;
	private AppsListAdapter		appsAdapter;

	private ArrayList<App>		allowedApps			= new ArrayList<App>();

	BroadcastReceiver			br					= new BroadcastReceiver() {
														@Override
														public void onReceive(Context context, Intent intent) {
															Log.d(TAG, "apps changed - " + intent.getAction());
															getLoaderManager().restartLoader(ALLOWED_APPS_LOADER, null, AppsFragment.this);
														}
													};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		appsAdapter = new AppsListAdapter();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_apps, container);
		listView = (ListView) rootView.findViewById(R.id.list);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listView.setAdapter(appsAdapter);
		// getLoaderManager().initLoader(ALLOWED_APPS_LOADER, null, this);
	}

	@Override
	public void onStart() {
		super.onStart();
		getLoaderManager().restartLoader(ALLOWED_APPS_LOADER, null, AppsFragment.this);
		LectureSessionProvider.getInstance(getActivity()).addListener(this);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				App appToOpen = (App) appsAdapter.getItem(position);
				if (appToOpen.appVersionCode > appToOpen.versionCodeInTab) {
                    if (Config.IS_PLAYSTORE_APP) {
                        String appUrl = "market://details?id=" + appToOpen.appPackage;
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(appUrl));
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(new File(appToOpen.apkPath)), "application/vnd.android.package-archive");
                        startActivity(intent);
                    }
                } else {
					Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(appToOpen.appPackage);
					intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
					Log.d(TAG, "got intent:" + intent.toUri(0));
					startActivity(intent);
				}
			}
		});
		// listen for broadcasts and refresh list
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
		intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
		intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		intentFilter.addDataScheme("package");
		getActivity().registerReceiver(br, intentFilter);
	}

	@Override
	public void onStop() {
		super.onStop();
		LectureSessionProvider.getInstance(getActivity()).removeListener(this);
		getActivity().unregisterReceiver(br);
	}

	private class AppsListAdapter extends BaseAdapter {
		LayoutInflater	inflater;

		public AppsListAdapter() {
			inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return allowedApps.size();
		}

		@Override
		public Object getItem(int position) {
			return allowedApps.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.item_app_home2, parent, false);
			}
			TextView appNameView = (TextView) convertView.findViewById(R.id.name);
			ImageView appIconView = (ImageView) convertView.findViewById(R.id.icon);

			App app = (App) getItem(position);
			String appName = app.appName;

			Drawable icon = null;
			if (app.packageInfo != null)
				icon = app.packageInfo.applicationInfo.loadIcon(getActivity().getPackageManager());
			if (app.versionCodeInTab > 0) {
				if (app.versionCodeInTab < app.appVersionCode)
					appName = appName + " (update)";
			} else {
				appName = appName + " (install)";
			}

			appNameView.setText(appName);
			appIconView.setImageDrawable(icon);
			return convertView;
		}

	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "onCreateLoader - ");
		if (UserSessionProvider.getInstance(getActivity()).isLoggedIn()) {
			String[] courseIds = UserSessionProvider.getInstance(getActivity()).getAllCourseIds();
			ArrayList<String> queryParams = new ArrayList<String>();
			queryParams.addAll(Arrays.asList(courseIds));
			queryParams.add("0");
			if (courseIds.length > 0) {
				StringBuilder sb = new StringBuilder(courseIds.length * 2 - 1);
				sb.append("?");
				for (int i = 1; i < courseIds.length; i++) {
					sb.append(",?");
				}
				String mSelectionClause = Apps.COLUMN_COURSE_ID + " IN (" + sb.toString() + ") AND " + Apps.COLUMN_SHOW_IN_APPS + " > ? ";
				CursorLoader loader = new CursorLoader(getActivity(), Apps.CONTENT_URI, null, mSelectionClause,
						queryParams.toArray(new String[0]), null);
				return loader;
			}
		}
        // no apps
        String mSelectionClause = Apps.COLUMN_COURSE_ID + " = ? ";
        return new CursorLoader(getActivity(), Apps.CONTENT_URI, null, mSelectionClause, new String[] { "blah" }, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "onLoadFinished - " + cursor);
		allowedApps.clear();
		HashSet<String> uniqueIdentifier = new HashSet<String>();
		HashMap<String, PackageInfo> installedApps = new HashMap<String, PackageInfo>();
		for (PackageInfo pi : getActivity().getPackageManager().getInstalledPackages(0)) {
			// Log.d(TAG, "package - " + pi.packageName);
			installedApps.put(pi.packageName, pi);
		}
		if (cursor != null && cursor.getCount() > 0) {
			int packageIndex = cursor.getColumnIndex(Apps.COLUMN_PACKAGE);
			int versionCodeIndex = cursor.getColumnIndex(Apps.COLUMN_VERSION_CODE);
			int titleIndex = cursor.getColumnIndex(Apps.COLUMN_NAME);
			int apkPathIndex = cursor.getColumnIndex(Apps.COLUMN_APK_PATH);
			while (cursor.moveToNext()) {
				App app = new App();
				app.appName = cursor.getString(titleIndex);
				app.appPackage = cursor.getString(packageIndex);
				app.appVersionCode = cursor.getInt(versionCodeIndex);
				app.apkPath = cursor.getString(apkPathIndex);
				if (installedApps.containsKey(app.appPackage)) {
					app.packageInfo = installedApps.get(app.appPackage);
					app.versionCodeInTab = installedApps.get(app.appPackage).versionCode;
				}
				if (uniqueIdentifier.contains(app.appPackage))
					continue;
				uniqueIdentifier.add(app.appPackage);
				allowedApps.add(app);
				if (LogConfig.DEBUG_ACTIVITIES)
					Log.d(TAG, "got app: " + app.appName + ", show? " + cursor.getInt(cursor.getColumnIndex(Apps.COLUMN_SHOW_IN_APPS))
							+ " apk:" + app.apkPath);
			}
		}
		appsAdapter.notifyDataSetChanged();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	static class App {
		public String		appName;
		public int			appVersionCode;
		public String		appPackage;
		public String		apkPath;

		public int			versionCodeInTab	= -1;
		public PackageInfo	packageInfo;
	}

	@Override
	public void onLectureJoinLeave() {
		// getLoaderManager().restartLoader(ALLOWED_APPS_LOADER, null, AppsFragment.this);
	}

	@Override
	public void onConnectionStatusChange() {
		// getLoaderManager().restartLoader(ALLOWED_APPS_LOADER, null, AppsFragment.this);
	}

	@Override
	public void onReceivedNewInstruction() {
		// getLoaderManager().restartLoader(ALLOWED_APPS_LOADER, null, AppsFragment.this);
	}
}
