package co.in.divi.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.UserSessionProvider.LoginStatus;
import co.in.divi.UserSessionProvider.UserSessionChangeListener;
import co.in.divi.db.sync.SyncDownService;

public class SyncDownActivity extends Activity implements UserSessionChangeListener {
	private static final String	TAG	= SyncDownActivity.class.getSimpleName();

	private Button				cancelButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_syncdown);

		cancelButton = (Button) (findViewById(R.id.cancelButton));
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent stopSyncIntent = new Intent(SyncDownActivity.this, SyncDownService.class);
				stopSyncIntent.putExtra(SyncDownService.INTENT_EXTRA_STOP_SYNC, true);
				startService(stopSyncIntent);
				UserSessionProvider.getInstance(SyncDownActivity.this).logout();
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		UserSessionProvider.getInstance(this).addListener(this);
		if (UserSessionProvider.getInstance(this).getLoginStatus() != LoginStatus.SYNCING) {
			finish();
			return;
		}
		// ensure service is up
		Intent startService = new Intent(this, SyncDownService.class);
		startService(startService);
	}

	@Override
	protected void onResume() {
		super.onResume();
		hideBars();
	}

	@Override
	public void onSessionChange() {
		if (UserSessionProvider.getInstance(this).getLoginStatus() != LoginStatus.SYNCING)
			finish();
	}

	@Override
	public void onCourseChange() {
	}

	private void hideBars() {
		if (Build.VERSION.SDK_INT < 16) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			int newUiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
			newUiOptions ^= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN;
			if (Build.VERSION.SDK_INT >= 18) {
				newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
			}
			getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
		}
	}

}
