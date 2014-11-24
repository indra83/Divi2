package co.in.divi.fragment;

import java.io.File;
import java.net.URI;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import co.in.divi.R;
import co.in.divi.activity.LearnMediaController;

public class AudioPlayerFragment extends Fragment {
	static final String				TAG						= "AudioPlayerFragment";

	private static final int		UPDATE_FREQUENCY		= 500;

	private final Handler			handler					= new Handler();
	private String					currentUrl				= "";
	private String					displayName				= "";
	private TextView				selelctedFile			= null;
	private TextView				durationView			= null;
	private SeekBar					seekbar					= null;
	private MediaPlayer				player					= null;
	private ImageButton				playButton				= null;
	private ImageButton				closeButton				= null;
	private boolean					isStarted				= true;
	private boolean					isMoveingSeekBar		= false;
	private TextView				CurrentDuration			= null;

	private LearnMediaController	lmc;

	private final Runnable			updatePositionRunnable	= new Runnable() {
																public void run() {
																	updatePosition();
																}
															};

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View playerView = inflater.inflate(R.layout.fragment_audio_playbar, container, false);
		selelctedFile = (TextView) playerView.findViewById(R.id.selectedfile);
		durationView = (TextView) playerView.findViewById(R.id.duration);
		seekbar = (SeekBar) playerView.findViewById(R.id.seekbar);
		playButton = (ImageButton) playerView.findViewById(R.id.play);
		closeButton = (ImageButton) playerView.findViewById(R.id.close);
		CurrentDuration = (TextView) playerView.findViewById(R.id.runningdurtion);
		player = new MediaPlayer();
		player.setOnCompletionListener(onCompletion);
		player.setOnErrorListener(onError);
		seekbar.setOnSeekBarChangeListener(seekBarChanged);
		playButton.setOnClickListener(onButtonClick);
		closeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				lmc.closeAll();
			}
		});

		return playerView;
	}

	private MediaPlayer.OnCompletionListener	onCompletion	= new MediaPlayer.OnCompletionListener() {
																	public void onCompletion(MediaPlayer mp) {
																		PlayerReset();
																	}
																};

	private MediaPlayer.OnErrorListener			onError			= new MediaPlayer.OnErrorListener() {
																	public boolean onError(MediaPlayer mp, int what, int extra) {
																		return false;
																	}
																};

	private SeekBar.OnSeekBarChangeListener		seekBarChanged	= new SeekBar.OnSeekBarChangeListener() {
																	public void onStopTrackingTouch(SeekBar seekBar) {
																		isMoveingSeekBar = false;
																	}

																	public void onStartTrackingTouch(SeekBar seekBar) {
																		isMoveingSeekBar = true;
																	}

																	public void onProgressChanged(SeekBar seekBar, int progress,
																			boolean fromUser) {
																		if (isMoveingSeekBar) {
																			player.seekTo(progress);
																		}
																	}
																};

	private View.OnClickListener				onButtonClick	= new View.OnClickListener() {
																	public void onClick(View v) {
																		switch (v.getId()) {
																		case R.id.play: {
																			if (player.isPlaying()) {
																				handler.removeCallbacks(updatePositionRunnable);
																				player.pause();
																				playButton
																						.setImageResource(android.R.drawable.ic_media_play);
																			} else {
																				if (isStarted) {
																					player.start();
																					playButton
																							.setImageResource(android.R.drawable.ic_media_pause);
																					updatePosition();
																				} else {
																					startPlay(lmc, currentUrl, displayName);
																				}
																			}
																			break;
																		}
																		}
																	}
																};

	public void startPlay(LearnMediaController lmc, String url, String dispalyname) {
		this.lmc = lmc;
		// this.set
		this.displayName = dispalyname;
		selelctedFile.setText(dispalyname);
		this.currentUrl = url;
		seekbar.setProgress(0);
		if (player.isPlaying())
			player.stop();
		player.reset();
		this.CurrentDuration.setText("");
		this.durationView.setText("");
		try {
			Log.d(TAG, "setting audio url:" + url);
			File audioFile = new File(URI.create(url));
			// Log.d(TAG,"f:"+audioFile.toString()+":::"+audioFile.exists());
			player.setDataSource(audioFile.getAbsolutePath());
			player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				public void onPrepared(MediaPlayer mp) {
					Log.d(TAG, "onPrepared, starting playback");
					player.start();
					long duration = player.getDuration() / 1000;
					durationView.setText(String.format("%d:%02d", duration / 60, duration % 60));
					seekbar.setMax(player.getDuration());
					playButton.setImageResource(android.R.drawable.ic_media_pause);
					updatePosition();
					isStarted = true;
				}
			});
			player.prepareAsync();
			// player.prepare();
		} catch (Exception e) {
			Log.w(TAG, "error playing audio:" + url);
		}
	}

	public void stopPlay(boolean hideUI) {
		if (player.isPlaying())
			player.stop();
		if (hideUI && (!isHidden())) {
			FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
			ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
			ft.hide(AudioPlayerFragment.this);
			ft.commitAllowingStateLoss(); // to fix illegal state exception..
		}
	}

	private void updatePosition() {
		handler.removeCallbacks(updatePositionRunnable);
		seekbar.setProgress(player.getCurrentPosition());
		long currentDuration = player.getCurrentPosition() / 1000;
		String curDuration = String.format("%d:%02d", currentDuration / 60, currentDuration % 60);
		this.CurrentDuration.setText(curDuration);
		handler.postDelayed(updatePositionRunnable, UPDATE_FREQUENCY);
	}

	private void PlayerReset() {
		player.stop();
		player.reset();
		playButton.setImageResource(android.R.drawable.ic_media_play);
		handler.removeCallbacks(updatePositionRunnable);
		seekbar.setProgress(0);
		this.CurrentDuration.setText("");
		this.durationView.setText("");
		isStarted = false;
	}

	@Override
	public void onPause() {
		super.onPause();
		PlayerReset();
	}
}