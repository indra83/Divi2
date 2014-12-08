package co.in.divi.fragment;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import co.in.divi.LectureSessionProvider;
import co.in.divi.LectureSessionProvider.LectureStatusChangeListener;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.UserSessionProvider.UserSessionChangeListener;
import co.in.divi.activity.HomeActivity;
import co.in.divi.model.LectureDetails;
import co.in.divi.model.UserData;
import co.in.divi.ui.LecturesPopupHolder;
import co.in.divi.ui.UserDataPopup;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Util;

public class HeaderFragment extends Fragment implements UserSessionChangeListener, LectureStatusChangeListener {

	private static final String		TAG					= HeaderFragment.class.getName();

	private static final String		USERDATA_DIALOG_TAG	= "fragment_user_data";

	private LectureSessionProvider	lectureSessionProvider;
	private UserSessionProvider		userSessionProvider;

	private View					classroomLayout, popupAnchor;
	private TextView				usernameText, lectureDetail;
	private ImageView				lectureStatus, headerLogo, backButton, diaryButton, searchButton;
	private LecturesPopupHolder		lecturePopup;

	private boolean					showing;

    private RelativeLayout.LayoutParams lps;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		lectureSessionProvider = LectureSessionProvider.getInstance(getActivity());
		userSessionProvider = UserSessionProvider.getInstance(getActivity());

        lps = new RelativeLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.help_ok_width), getResources().getDimensionPixelSize(R.dimen.help_ok_height));
        lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        int margin = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        lps.setMargins(margin, margin, margin, margin);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_header, container, false);
		lectureStatus = (ImageView) rootView.findViewById(R.id.header_classroom_status);
		headerLogo = (ImageView) rootView.findViewById(R.id.header_logo);
		backButton = (ImageView) rootView.findViewById(R.id.header_back);
		diaryButton = (ImageView) rootView.findViewById(R.id.header_bookmark);
		searchButton = (ImageView) rootView.findViewById(R.id.header_search);
		lectureDetail = (TextView) rootView.findViewById(R.id.header_classroom_text);
		usernameText = (TextView) rootView.findViewById(R.id.header_username_text);
		classroomLayout = rootView.findViewById(R.id.header_classroom);
		popupAnchor = rootView.findViewById(R.id.header_separator1);
		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().onBackPressed();
				// getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
			}
		});
		headerLogo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getActivity(), HomeActivity.class);
				getActivity().startActivity(i);
				getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
			}
		});
		diaryButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(getActivity(), "'Diary' feature coming soon...", Toast.LENGTH_SHORT).show();
				// ((BaseActivity) getActivity()).showDiary();
			}
		});
		searchButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(getActivity(), "Search feature coming soon...", Toast.LENGTH_SHORT).show();
			}
		});

		showing = false;
		return rootView;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onStart() {
		super.onStart();
		userSessionProvider.addListener(this);
		lectureSessionProvider.addListener(this);
		classroomLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showClassroomPopup();
			}
		});
		usernameText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showUserDataPopup();
			}
		});

		lecturePopup = new LecturesPopupHolder(getActivity(), popupAnchor);

		updateUserDetails();
		updateLectureDetails();
	}

	@Override
	public void onResume() {
		super.onResume();
		showing = true;
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "showing");

        // Help screens
        if(userSessionProvider.isLoggedIn() && getActivity() instanceof HomeActivity){
            new ShowcaseView.Builder(getActivity())
                    .setContentTitle("User Settings")
                    .setContentText("Access to classroom management, course and general settings.")
                    .setTarget(new ViewTarget(usernameText))
                    .setStyle(R.style.CustomShowcaseTheme)
                    .singleShot(1)
                    .setShowcaseEventListener(new OnShowcaseEventListener() {
                        @Override
                        public void onShowcaseViewHide(ShowcaseView showcaseView) {
//                            showcaseView.hide();
                            new ShowcaseView.Builder(getActivity())
                                    .setContentTitle("Live Lectures")
                                    .setContentText("Create and join Live Lectures.")
                                    .setTarget(new ViewTarget(lectureDetail))
                                    .setStyle(R.style.CustomShowcaseTheme)
                                    .singleShot(2)
                                    .build()
                                    .setButtonPosition(lps);
                        }

                        @Override
                        public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        }

                        @Override
                        public void onShowcaseViewShow(ShowcaseView showcaseView) {
                        }
                    })
                    .build()
                    .setButtonPosition(lps);
        }
	}

	@Override
	public void onPause() {
		super.onPause();
		showing = false;
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "hidden?");
	}

	@Override
	public void onStop() {
		super.onStop();
		userSessionProvider.removeListener(this);
		lectureSessionProvider.removeListener(this);
		if (lecturePopup != null && lecturePopup.isShowing())
			lecturePopup.dismiss();
	}

	public void disableBack() {
		backButton.setVisibility(View.INVISIBLE);
		backButton.setEnabled(false);
		backButton.setOnClickListener(null);
	}

	public boolean isShowing() {
		return showing;
	}

	private void updateUserDetails() {
		if (userSessionProvider.hasUserData()) {
			UserData userData = userSessionProvider.getUserData();
			usernameText.setText(Html.fromHtml(userData.name + "<br/><b>" + userSessionProvider.getCourseName() + "</b>"));
		}
	}

	private void updateLectureDetails() {
		switch (lectureSessionProvider.getStatus()) {
		case CONNECTING:
			lectureStatus.setImageResource(R.drawable.ic_header_connecting);
			// lectureDetail.setText("connecting");
			break;
		case NOTCONNECTED:
			lectureStatus.setImageResource(R.drawable.ic_header_disconnected);
			lectureDetail.setText("Not Connected");
			break;
		case CONNECTED:
			lectureStatus.setImageResource(R.drawable.ic_header_connected);
			// lectureDetail.setText("Connected!");
			break;
		default:
			break;
		}
		if (lectureSessionProvider.isLectureJoined()) {
			LectureDetails curLecture = lectureSessionProvider.getCurrentLecture();
			lectureDetail.setText(Html.fromHtml("Connected to<br/><b>" + curLecture.name + "</b>"));
		}
	}

	/* User data popup code */
	private void showUserDataPopup() {
		FragmentManager fm = getFragmentManager();
		UserDataPopup userDataDialog = new UserDataPopup();
		userDataDialog.show(fm, USERDATA_DIALOG_TAG);
	}

	/* Classroom popup code */
	private void showClassroomPopup() {
		if (lecturePopup.isShowing()) {
			lecturePopup.dismiss();
		} else {
			// ensure wifi
			if (!Util.isNetworkOn(getActivity())) {
				Toast.makeText(getActivity(), "Please connect to network.", Toast.LENGTH_LONG).show();
				DialogFragment wifiFragment = new WiFiSettingsFragment();
				FragmentManager fm = getFragmentManager();
				wifiFragment.show(fm, "WiFi");
			} else {
				lecturePopup.show();
			}
		}
	}

	/* Listeners */
	@Override
	public void onSessionChange() {
		updateUserDetails();
		Fragment userDataPopup = getFragmentManager().findFragmentByTag(USERDATA_DIALOG_TAG);
		if (userDataPopup != null && userDataPopup.isVisible())
			((UserDataPopup) userDataPopup).refresh();
	}

	@Override
	public void onCourseChange() {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "onCourseChange");
		updateUserDetails();
		Fragment userDataPopup = getFragmentManager().findFragmentByTag(USERDATA_DIALOG_TAG);
		if (userDataPopup != null && userDataPopup.isVisible())
			((UserDataPopup) userDataPopup).refresh();
	}

	@Override
	public void onConnectionStatusChange() {
		updateLectureDetails();
	}

	@Override
	public void onReceivedNewInstruction() {
		if (lecturePopup.isShowing())
			lecturePopup.refresh();
	}

	@Override
	public void onLectureJoinLeave() {
		updateLectureDetails();
		if (lecturePopup.isShowing()) {
			if (lectureSessionProvider.isLectureJoined()) {
				lecturePopup.refresh();
			} else {
				lecturePopup.dismiss();
			}
		}
	}
}