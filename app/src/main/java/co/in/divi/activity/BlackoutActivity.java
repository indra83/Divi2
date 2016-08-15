package co.in.divi.activity;

import android.os.Bundle;

import co.in.divi.BaseActivity;
import co.in.divi.LectureSessionProvider.LectureStatusChangeListener;
import co.in.divi.Location;
import co.in.divi.LocationManager;
import co.in.divi.R;
import co.in.divi.apps.AppLauncher;
import co.in.divi.fragment.HeaderFragment;

public class BlackoutActivity extends BaseActivity implements LectureStatusChangeListener {
	private static final String	TAG	= BlackoutActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_blackout);
		((HeaderFragment) getFragmentManager().findFragmentById(R.id.header)).disableBack();
	}

	@Override
	protected void onStart() {
		super.onStart();
		AppLauncher.clearAllApps(getApplicationContext());
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!lectureSessionProvider.isBlackout()) {
			finish();
			return;
		}
		LocationManager.getInstance(this).setNewLocation(Location.LOCATION_TYPE.BLACKOUT, null, null, null, null);
	}

	@Override
	public void onCourseChange() {
		// do nothing
	}

	@Override
	public void onLectureJoinLeave() {
		if (!lectureSessionProvider.isBlackout()) {
			finish();
		}
	}

	@Override
	public void onConnectionStatusChange() {
		if (!lectureSessionProvider.isBlackout()) {
			finish();
		}
	}

	@Override
	public void onReceivedNewInstruction() {
		// this should be taken care already?!
	}
}
