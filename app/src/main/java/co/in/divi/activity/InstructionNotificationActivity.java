package co.in.divi.activity;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import co.in.divi.AlarmAlertWakeLock;
import co.in.divi.Location;
import co.in.divi.LocationManager;
import co.in.divi.R;
import co.in.divi.util.Config;
import co.in.divi.util.InstallAppService;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Util;

public class InstructionNotificationActivity extends Activity {
	static final String			TAG							= InstructionNotificationActivity.class.getSimpleName();

	public static final String	INTENT_EXTRA_NAME			= "INTENT_EXTRA_NAME";
	public static final String	INTENT_EXTRA_EXTERNAL_APP	= "INTENT_EXTRA_EXTERNAL_APP";

	private SoundPool			soundPool;
	private boolean				loaded;
	private int					soundId;
	private int					streamId;

	private TextView			instruction;
	private Button				start;
	private Handler				handler;

	private Typeface			thinFont;

	Timer						timer;

	// private PowerManager.WakeLock wl;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);

		final Window win = getWindow();
		win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		setContentView(R.layout.activity_inotification);
		thinFont = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Thin.ttf");
		TextView title = ((TextView) findViewById(R.id.title));
		title.setTypeface(thinFont);
		start = (Button) findViewById(R.id.button);
		start.setTypeface(thinFont);
		start.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		instruction = (TextView) findViewById(R.id.instruction);

		handler = new Handler();

		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		// Load the sound
		// soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
		// soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
		// @Override
		// public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
		// loaded = true;
		// }
		// });
		// soundId = soundPool.load(this, R.raw.hydra, 1);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	protected void onResume() {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "onResume");
		super.onResume();
		String name = getIntent().getExtras().getString(INTENT_EXTRA_NAME);
		instruction.setText(name);
		// handler.postDelayed(new Runnable() {
		// @Override
		// public void run() {
		// AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		// float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		// float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		// float volume = actualVolume / maxVolume;
		// soundPool.stop(streamId);
		// streamId = soundPool.play(soundId, volume, volume, 1, -1, 1f);
		// handler.postDelayed(new Runnable() {
		// @Override
		// public void run() {
		// soundPool.stop(streamId);
		// }
		// }, 20000);
		// }
		// }, 5000);
		//
		// KeyguardManager keyguardManager = (KeyguardManager)
		// getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
		// KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
		// keyguardLock.disableKeyguard();

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
				// instruction.invalidate();
			}
		}, 300);

		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						start.performClick();
					}
				});
			}
		}, Config.DELAY_INSTRUCTION_AUTO_OPEN);

        // set location to home
        LocationManager.getInstance(this).setNewLocation(Location.LOCATION_TYPE.HOME, null, null, null, null);
	}

	@Override
	protected void onPause() {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "onPause");
		super.onPause();
		timer.cancel();
	}

	@Override
	protected void onStop() {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "onStop");
		super.onStop();
		if (!isFinishing()) {
			// finish();
		} else {
			AlarmAlertWakeLock.releaseCpuLock();
			if (getIntent().hasExtra(INTENT_EXTRA_EXTERNAL_APP)) {
				try {
					Intent intent = getPackageManager().getLaunchIntentForPackage(getIntent().getStringExtra(INTENT_EXTRA_EXTERNAL_APP));
					if (intent != null) {
						intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK
								| Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
						startActivity(intent);
					} else {
						Toast.makeText(this, "Shared app could not be opened...", Toast.LENGTH_SHORT).show();
                        Intent installerIntent = new Intent(InstructionNotificationActivity.this, InstallAppService.class);
                        installerIntent.putExtra(InstallAppService.INTENT_EXTRA_PACKAGE, getIntent().getStringExtra(INTENT_EXTRA_EXTERNAL_APP));
                        startService(installerIntent);
					}
				} catch (Exception e) {
					Toast.makeText(this, "Shared app not found on your tablet!", Toast.LENGTH_LONG).show();
				}
			} else {
				Util.openInstruction(InstructionNotificationActivity.this, getIntent().getData());
			}
			// handler.removeCallbacksAndMessages(null);
			// soundPool.release();
			// soundPool = null;
		}
	}
}
