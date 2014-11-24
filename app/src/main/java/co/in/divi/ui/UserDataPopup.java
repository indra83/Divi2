package co.in.divi.ui;

import java.util.HashSet;
import java.util.Random;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import co.in.divi.AdminPasswordManager;
import co.in.divi.DiviApplication;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.activity.AdminSettingsActivity;
import co.in.divi.background.UniversalSyncCheckReceiver;
import co.in.divi.fragment.BaseDialogFragment;
import co.in.divi.model.UserData;
import co.in.divi.model.UserData.ClassRoom;
import co.in.divi.model.UserData.Course;
import co.in.divi.util.Config;
import co.in.divi.util.Util;

import com.android.volley.toolbox.FadeInNetworkImageView;

public class UserDataPopup extends BaseDialogFragment {
	private static final String	TAG	= UserDataPopup.class.getSimpleName();

	LayoutInflater				layoutInflater;
	TextView					name, school, syncStatus;
	Button						logout, settings, syncNow;
	FadeInNetworkImageView		profilePic;
	LinearLayout				courses;
	SeekBar						brightnessBar;

	UserSessionProvider			userSessionProvider;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_FRAME, R.style.UserDataDialog);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		userSessionProvider = UserSessionProvider.getInstance(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		layoutInflater = inflater;
		ViewGroup popupRoot = (ViewGroup) layoutInflater.inflate(R.layout.popup_userdata, null);
		name = (TextView) popupRoot.findViewById(R.id.name);
		school = (TextView) popupRoot.findViewById(R.id.school);
		profilePic = (FadeInNetworkImageView) popupRoot.findViewById(R.id.profile_pic);
		courses = (LinearLayout) popupRoot.findViewById(R.id.courses);
		settings = (Button) popupRoot.findViewById(R.id.settings);
		syncNow = (Button) popupRoot.findViewById(R.id.syncNow);
		syncStatus = (TextView) popupRoot.findViewById(R.id.syncStatus);
		brightnessBar = (SeekBar) popupRoot.findViewById(R.id.brightness);
		brightnessBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				Settings.System.putInt(getActivity().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, progress);

				WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
				lp.screenBrightness = (progress + 5) / 255.0f;
				getActivity().getWindow().setAttributes(lp);
			}
		});
		logout = (Button) popupRoot.findViewById(R.id.logout);
		logout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (!Config.ENABLE_OTP) {
					userSessionProvider.logout();
				} else {
					final int challenge = new Random(System.currentTimeMillis()).nextInt(10000);
					final EditText input = new EditText(getActivity());
					input.setInputType(InputType.TYPE_CLASS_NUMBER);
					AlertDialog ad = new AlertDialog.Builder(getActivity()).setTitle("Enter password")
							.setMessage("Enter the key for challenge: " + challenge).setView(input)
							.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									dialog.dismiss();
									boolean isAuthorized = AdminPasswordManager.getInstance().isAuthorized(challenge,
											input.getText().toString());
									if (isAuthorized) {
										userSessionProvider.logout();
									} else {
										Toast.makeText(getActivity(), "Authorization failed", Toast.LENGTH_SHORT).show();
										// resetTimer();
									}
								}
							}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									// resetTimer();
									dialog.dismiss();
								}
							}).show();
				}
			}
		});
		settings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent launchSettings = new Intent(getActivity(), AdminSettingsActivity.class);
				getActivity().startActivity(launchSettings);
			}
		});

		syncNow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!Util.isNetworkOn(getActivity())) {
					Toast.makeText(getActivity(), "Please connect to internet..", Toast.LENGTH_SHORT).show();
					return;
				}
				// clear timestamps to force perform all Syncs
				UniversalSyncCheckReceiver.syncNow(getActivity(), true);
			}
		});

		return popupRoot;
	}

	@Override
	public void onStart() {
		super.onStart();
		try {
			brightnessBar.setProgress(android.provider.Settings.System.getInt(getActivity().getContentResolver(),
					android.provider.Settings.System.SCREEN_BRIGHTNESS));
		} catch (SettingNotFoundException e) {
			Log.w(TAG, "we shouldn't be here", e);
		}
		// sync status
		long oldestSyncTime = Util.getTimestampMillis();
		oldestSyncTime = Math.min(oldestSyncTime, userSessionProvider.getTimestamp(UserSessionProvider.LAST_SYNC_ATTEMTPS_TIMESTAMP));
		oldestSyncTime = Math.min(oldestSyncTime, userSessionProvider.getTimestamp(UserSessionProvider.LAST_SYNC_COMMANDS_TIMESTAMP));
		oldestSyncTime = Math.min(oldestSyncTime, userSessionProvider.getTimestamp(UserSessionProvider.LAST_SYNC_CONTENT_TIMESTAMP));
		long diff = Util.getTimestampMillis() - oldestSyncTime;
		int minutes = (int) diff / 60000;
		if (minutes > 24 * 60)
			syncStatus.setText("Last sync " + (minutes / (24 * 60)) + " days ago.");
		else
			syncStatus.setText("Last sync " + (minutes / 60) + " hours " + (minutes % 60) + " minutes ago.");
		refresh();
	}

	public void refresh() {
		name.setText("");
		school.setText("");
		profilePic.setDefaultImageResId(R.drawable.ic_profile);
		courses.removeAllViews();
		if (userSessionProvider.hasUserData()) {
			UserData userData = userSessionProvider.getUserData();
			name.setText(userData.name);
			school.setText(userData.schoolName);
			if (userData.profilePic != null) {
				Uri picUri = Uri.parse(userData.profilePic);
				if (picUri != null && picUri.getHost() != null)
					profilePic.setImageUrl(userData.profilePic, DiviApplication.get().getImageLoader());
			}
			HashSet<String> added = new HashSet<String>();
			for (ClassRoom classroom : userData.classRooms) {
				for (Course course : classroom.courses) {
					if (added.contains(course.id))
						continue;
					added.add(course.id);
					final String courseId = course.id;
					CheckedTextView item = (CheckedTextView) layoutInflater.inflate(R.layout.item_course, null);
					item.setText(course.name);
					if (courseId.equals(userSessionProvider.getCourseId()))
						item.setChecked(true);
					item.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							userSessionProvider.setCourseId(courseId);
						}
					});
					courses.addView(item);
				}
			}
		}
	}
}
