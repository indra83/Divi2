package co.in.divi.activity;

import java.io.File;

import uk.co.senab.photoview.PhotoViewAttacher;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import co.in.divi.LocationManager.LOCATION_SUBTYPE;
import co.in.divi.content.Topic.Audio;
import co.in.divi.content.Topic.Image;
import co.in.divi.content.Topic.ImageSet;
import co.in.divi.content.Topic.Video;
import co.in.divi.fragment.AudioPlayerFragment;
import co.in.divi.ui.TextureVideoView;
import co.in.divi.ui.VideoControllerView;
import co.in.divi.ui.VideoControllerView.MediaPlayerControl;
import co.in.divi.util.LogConfig;
import co.in.divi.util.ServerConfig;
import co.in.divi.util.image.ImageResizer;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.ErrorReason;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerFragment;

public class LearnMediaController implements OnInitializedListener {
	private static final String	TAG						= LearnMediaController.class.getSimpleName();

	private static final int	IMAGE_WIDTH_PIXELS		= 1280;
	private static final int	IMAGE_HEIGHT_PIXELS		= 800;

	private static final String	VIDEO_MODE_SEPARATOR	= ":::";
	private static final String	VIDEO_MODE_PLAY			= "play";
	private static final String	VIDEO_MODE_PAUSE		= "pause";

	private enum MODE {
		NONE, AUDIO, IMAGE, IMAGESET, VIDEO, YOUTUBE
	}

	private LearnActivity		activity;
	private MODE				curMode;

	YouTubePlayerFragment		youtubeFragment;
	ViewGroup					youtubeRoot;
	ViewPager					imagePager;
	AudioPlayerFragment			audioPlayerFragment;
	TextureVideoView			videoView;
	View						videoRoot, imageRoot;
	ImageView					imageView, rightArrow, leftArrow;
	HeaderHidingMediaController	mediaController;
	private PhotoViewAttacher	mAttacher;
	private YouTubePlayer		youtubePlayer;

	private Video				video;
	private ImageSet			imageSet;

	private boolean				videoReady, youtubeVideoReady;
	private boolean				youtubePlayerReady;

	public LearnMediaController(LearnActivity activity) {
		this.activity = activity;
	}

	public void init() {
		// setup video & audio
		mediaController = new HeaderHidingMediaController(activity);
		videoView.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				closeAll();
			}
		});
		videoView.setOnErrorListener(new OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				Log.w(TAG, "Error occured playing video:" + what + ",extra:" + extra);
				return false;
			}
		});
		// setup youtube
		youtubeFragment.initialize(ServerConfig.YOUTUBE_DEVELOPER_KEY, this);
		youtubeFragment.getView().setVisibility(View.GONE);
		// setup image
		curMode = MODE.NONE;
	}

	public void processFollowMe(String subItemId, String fragment) {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "curr mode:" + curMode + ", frag:" + fragment);
		if (curMode == MODE.IMAGESET) {
			if (fragment != null) {
				try {
					int pageNo = Integer.parseInt(fragment);
					setImageSetItem(pageNo);
					return;
				} catch (Exception e) {
					Log.w(TAG, "error parsing stream fragment..", e);
				}
			}
		} else if (curMode == MODE.VIDEO) {
			if (fragment != null && videoReady) {
				try {
					String segs[] = fragment.split(VIDEO_MODE_SEPARATOR);
					int pos = Integer.parseInt(segs[1]);
					if (segs[0].equalsIgnoreCase(VIDEO_MODE_PLAY)) {
						videoView.seekTo(pos);
						videoView.start();
					} else if (segs[0].equalsIgnoreCase(VIDEO_MODE_PAUSE)) {
						if (videoView.isPlaying()) {
							videoView.seekTo(pos);
							videoView.pause();
						}// else ignore?
					}
				} catch (NumberFormatException nfe) {
					Log.e(TAG, "error seeking video:", nfe);
				} catch (Exception e) {
					Log.w(TAG, "error parsing stream fragment..", e);
				}
			} else {
				Log.w(TAG, "video stream received without video ready!!!");
			}
		} else if (curMode == MODE.YOUTUBE) {
			if (fragment != null && youtubeVideoReady) {
				try {
					String segs[] = fragment.split(VIDEO_MODE_SEPARATOR);
					int pos = Integer.parseInt(segs[1]);
					if (segs[0].equalsIgnoreCase(VIDEO_MODE_PLAY)) {
						youtubePlayer.seekToMillis(pos);
						youtubePlayer.play();
					} else if (segs[0].equalsIgnoreCase(VIDEO_MODE_PAUSE)) {
						if (youtubePlayer.isPlaying()) {
							youtubePlayer.seekToMillis(pos);
							youtubePlayer.pause();
						}// else ignore?
					}
				} catch (NumberFormatException nfe) {
					Log.e(TAG, "error seeking video:", nfe);
				} catch (Exception e) {
					Log.w(TAG, "error parsing stream fragment..", e);
				}
			} else {
				Log.w(TAG, "video stream received without video ready!!!");
			}
		}
	}

	public boolean handleBackPress() {
		if (curMode != MODE.NONE) {
			closeAll();
			return true;
		} else
			return false;
	}

	public void closeAll() {
		// Log.d(TAG, "BEGIN closeAll()");
		if (imageLoadTask != null)
			imageLoadTask.cancel(false);
		video = null;
		imageSet = null;
		videoReady = false;
		youtubeVideoReady = false;
		closeVideo();
		closeAudio(true);
		closeImage();
		closeImageSet();
		closeYoutube();

		curMode = MODE.NONE;

		// reset location
		activity.setTopicLocation();
		// Log.d(TAG, "END closeAll()");
	}

	void playVideo(File baseDir, Video vid, final String fragment) {
		closeAll();
		videoRoot.setVisibility(View.VISIBLE);
		this.video = vid;
		videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			public void onPrepared(MediaPlayer mp) {
				videoView.setOnTouchListener(new View.OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						mediaController.show();
						return false;
					}
				});
				final MediaPlayer player = mp;
				VideoControllerView.MediaPlayerControl mpc = new MediaPlayerControl() {
					@Override
					public boolean canPause() {
						return true;
					}

					@Override
					public boolean canSeekBackward() {
						return true;
					}

					@Override
					public boolean canSeekForward() {
						return true;
					}

					@Override
					public int getBufferPercentage() {
						return 0;
					}

					@Override
					public int getCurrentPosition() {
						return player.getCurrentPosition();
					}

					@Override
					public int getDuration() {
						return player.getDuration();
					}

					@Override
					public boolean isPlaying() {
						return player.isPlaying();
					}

					@Override
					public void pause() {
						player.pause();
						activity.setSubItemInLocation(video.id, video.title, LOCATION_SUBTYPE.TOPIC_VIDEO, buildFragment(VIDEO_MODE_PAUSE));
					}

					@Override
					public void seekTo(int i) {
						player.seekTo(i);
						if (isPlaying())
							activity.setSubItemInLocation(video.id, video.title, LOCATION_SUBTYPE.TOPIC_VIDEO,
									buildFragment(VIDEO_MODE_PLAY));
						else
							activity.setSubItemInLocation(video.id, video.title, LOCATION_SUBTYPE.TOPIC_VIDEO,
									buildFragment(VIDEO_MODE_PAUSE));
					}

					@Override
					public void start() {
						player.start();
						if (LogConfig.DEBUG_ACTIVITIES)
							Log.d(TAG, "video:::start - ");
						activity.setSubItemInLocation(video.id, video.title, LOCATION_SUBTYPE.TOPIC_VIDEO, buildFragment(VIDEO_MODE_PLAY));
					}

					@Override
					public boolean isFullScreen() {
						return true;
					}

					@Override
					public void toggleFullScreen() {
					}

					private String buildFragment(String mode) {
						return mode + VIDEO_MODE_SEPARATOR + getCurrentPosition();
					}
				};
				mediaController.setMediaPlayer(mpc);
				mediaController.setAnchorView((ViewGroup) videoRoot);
				activity.setSubItemInLocation(video.id, video.title, LOCATION_SUBTYPE.TOPIC_VIDEO, null);
				if (fragment != null) {
					try {
						String segs[] = fragment.split(VIDEO_MODE_SEPARATOR);
						int pos = Integer.parseInt(segs[1]);
						if (segs[0].equalsIgnoreCase(VIDEO_MODE_PLAY)) {
							mpc.seekTo(pos);
							mpc.start();
							videoReady = true;
						} else if (segs[0].equalsIgnoreCase(VIDEO_MODE_PAUSE)) {
							mpc.seekTo(pos);
							// mpc.start();
							// mpc.pause();
							videoReady = true;
						}
					} catch (NumberFormatException nfe) {
						Log.e(TAG, "error seeking video:", nfe);
					}
				} else {
					mpc.start();
					videoReady = true;
				}
			}
		});
		File videoFile = new File(baseDir, video.src);
		videoView.setVideoPath(videoFile.getAbsolutePath());
		videoView.requestFocus();
		curMode = MODE.VIDEO;
	}

	void playAudio(File baseDir, Audio audio, final String fragment) {
		closeAll();
		File audioFile = new File(baseDir, audio.src);
		if (audioPlayerFragment.isHidden()) {
			FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
			ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
			ft.show(audioPlayerFragment);
			ft.commit();
		}
		audioPlayerFragment.startPlay(this, audioFile.toURI().toString(), audio.title);
		activity.setSubItemInLocation(audio.id, audio.title, LOCATION_SUBTYPE.TOPIC_AUDIO, null);
		curMode = MODE.AUDIO;
	}

	void showImage(File baseDir, Image image, final String fragment) {
		closeAll();
		File imageFile = new File(baseDir, image.src);
		activity.setSubItemInLocation(image.id, image.title, LOCATION_SUBTYPE.TOPIC_IMAGE, null);

		if (imageLoadTask != null)
			imageLoadTask.cancel(false);
		imageLoadTask = new ImageLoadTask();
		imageLoadTask.execute(new File[] { imageFile });
		curMode = MODE.IMAGE;
	}

	void showImageSet(File baseDir, final ImageSet imageSet, final String fragment) {
		closeAll();
		this.imageSet = imageSet;
		imageRoot.setVisibility(View.VISIBLE);
		imagePager.setVisibility(View.VISIBLE);
		imagePager.setAdapter(new ImagePagerAdapter(activity.getFragmentManager(), imageSet, baseDir));
		final int totalImages = imageSet.imagesItems.length;
		imagePager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageSelected(int page) {
				Toast.makeText(activity, (page + 1) + "/" + totalImages, Toast.LENGTH_SHORT).show();
				activity.setSubItemInLocation(imageSet.id, imageSet.title, LOCATION_SUBTYPE.TOPIC_IMAGESET, "" + page);
				drawPaginationArrows(page);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {

			}

			@Override
			public void onPageScrollStateChanged(int arg0) {

			}
		});
		curMode = MODE.IMAGESET;
		activity.showHeader();
		if (fragment != null) {
			try {
				setImageSetItem(Integer.parseInt(fragment));
				return;
			} catch (Exception e) {
				Log.w(TAG, "error parsing stream fragment..", e);
			}
		}
		setImageSetItem(0);
	}

	void playYoutubeVideo(Video youtubeVideo, final String fragment) {
		closeAll();
		video = youtubeVideo;
		activity.showHeader();
		youtubeRoot.setVisibility(View.VISIBLE);
		activity.getFragmentManager().beginTransaction().show(youtubeFragment).commit();
		youtubePlayer.setFullscreen(false);
		youtubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.CHROMELESS);
		youtubePlayer.setPlayerStateChangeListener(new YouTubePlayer.PlayerStateChangeListener() {
			@Override
			public void onVideoStarted() {
			}

			@Override
			public void onVideoEnded() {
			}

			@Override
			public void onLoading() {
			}

			@Override
			public void onLoaded(String videoId) {
				activity.hideHeader();
				youtubeFragment.getView().setOnTouchListener(new View.OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						mediaController.show();
						return true;
					}
				});
				VideoControllerView.MediaPlayerControl mpc = new MediaPlayerControl() {
					@Override
					public boolean canPause() {
						return true;
					}

					@Override
					public boolean canSeekBackward() {
						return true;
					}

					@Override
					public boolean canSeekForward() {
						return true;
					}

					@Override
					public int getBufferPercentage() {
						return 0;
					}

					@Override
					public int getCurrentPosition() {
						return youtubePlayer.getCurrentTimeMillis();
					}

					@Override
					public int getDuration() {
						return youtubePlayer.getDurationMillis();
					}

					@Override
					public boolean isPlaying() {
						return youtubePlayer.isPlaying();
					}

					@Override
					public void pause() {
						youtubePlayer.pause();
						activity.setSubItemInLocation(video.id, video.title, LOCATION_SUBTYPE.TOPIC_VIDEO, buildFragment(VIDEO_MODE_PAUSE));
					}

					@Override
					public void seekTo(int i) {
						youtubePlayer.seekToMillis(i);
						if (isPlaying())
							activity.setSubItemInLocation(video.id, video.title, LOCATION_SUBTYPE.TOPIC_VIDEO,
									buildFragment(VIDEO_MODE_PLAY));
						else
							activity.setSubItemInLocation(video.id, video.title, LOCATION_SUBTYPE.TOPIC_VIDEO,
									buildFragment(VIDEO_MODE_PAUSE));
					}

					@Override
					public void start() {
						youtubePlayer.play();
						if (LogConfig.DEBUG_ACTIVITIES)
							Log.d(TAG, "video:::start - ");
						activity.setSubItemInLocation(video.id, video.title, LOCATION_SUBTYPE.TOPIC_VIDEO, buildFragment(VIDEO_MODE_PLAY));
					}

					@Override
					public boolean isFullScreen() {
						return true;
					}

					@Override
					public void toggleFullScreen() {
					}

					private String buildFragment(String mode) {
						return mode + VIDEO_MODE_SEPARATOR + getCurrentPosition();
					}
				};
				mediaController.setMediaPlayer(mpc);
				mediaController.setAnchorView(youtubeRoot);
				youtubePlayer.play();
				activity.setSubItemInLocation(video.id, video.title, LOCATION_SUBTYPE.TOPIC_VIDEO, null);
				if (fragment != null) {
					try {
						String segs[] = fragment.split(VIDEO_MODE_SEPARATOR);
						int pos = Integer.parseInt(segs[1]);
						if (segs[0].equalsIgnoreCase(VIDEO_MODE_PLAY)) {
							mpc.seekTo(pos);
							mpc.start();
							youtubeVideoReady = true;
						} else if (segs[0].equalsIgnoreCase(VIDEO_MODE_PAUSE)) {
							mpc.seekTo(pos);
							mpc.start();
							mpc.pause();
							youtubeVideoReady = true;
						}
					} catch (NumberFormatException nfe) {
						Log.e(TAG, "error seeking video:", nfe);
					}
				} else {
					mpc.start();
					youtubeVideoReady = true;
				}

			}

			@Override
			public void onError(ErrorReason error) {
				Log.w(TAG, "Error loading video:" + error);
			}

			@Override
			public void onAdStarted() {
			}
		});
		youtubePlayer.cueVideo(youtubeVideo.youtubeId);
		curMode = MODE.YOUTUBE;
	}

	private void setImageSetItem(int page) {
		imagePager.setCurrentItem(page, true);
		if (page == 0)
			// listener not called for page 0
			activity.setSubItemInLocation(imageSet.id, imageSet.title, LOCATION_SUBTYPE.TOPIC_IMAGESET, "0");

		drawPaginationArrows(page);
	}

	private void drawPaginationArrows(int page) {
		// pagination indicator
		leftArrow.setVisibility(View.VISIBLE);
		rightArrow.setVisibility(View.VISIBLE);
		if (page == 0) {
			leftArrow.setVisibility(View.GONE);
		} else if (page == imageSet.imagesItems.length - 1) {
			rightArrow.setVisibility(View.GONE);
		}
		leftArrow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setImageSetItem(imagePager.getCurrentItem() - 1);
			}
		});
		rightArrow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setImageSetItem(imagePager.getCurrentItem() + 1);
			}
		});
	}

	void closeVideo() {
		if (videoRoot.isShown()) {
			if (LogConfig.DEBUG_ACTIVITIES)
				Log.d(TAG, "stopping video playback");
			mediaController.hide();
			videoView.setOnPreparedListener(null);
			videoView.stopPlayback();
			videoRoot.setVisibility(View.GONE);
		}
	}

	void closeAudio(boolean hideUI) {
		// if (audioPlayerFragment.isVisible()) {
		audioPlayerFragment.stopPlay(hideUI);
		// }
	}

	void closeImage() {
		imageRoot.setVisibility(View.GONE);
		imageView.setVisibility(View.GONE);
	}

	void closeImageSet() {
		imageRoot.setVisibility(View.GONE);
		imagePager.setVisibility(View.GONE);
		leftArrow.setVisibility(View.GONE);
		rightArrow.setVisibility(View.GONE);
		imagePager.setAdapter(null);
	}

	void closeYoutube() {
		Log.d(TAG, "closing youtube - " + curMode);
		if (curMode == MODE.YOUTUBE) {
			// youtubePlayerReady = false;
			// youtubePlayer.release();
			// youtubePlayer = null;
			Log.d(TAG, "pausing youtube");
			youtubePlayer.pause();
		}
		activity.getFragmentManager().beginTransaction().hide(youtubeFragment).commit();
		youtubeRoot.setVisibility(View.GONE);
	}

	class HeaderHidingMediaController extends VideoControllerView {

		public HeaderHidingMediaController(Context context) {
			super(context);
		}

		@Override
		public void hide() {
			super.hide();
			activity.hideHeader();
		}

		@Override
		public void show() {
			super.show();
			activity.showHeader();
		}
	}

	class ImagePagerAdapter extends FragmentStatePagerAdapter {

		ImageSet	imageSet;
		File		baseDir;

		public ImagePagerAdapter(FragmentManager fm, ImageSet imageSet, File baseDir) {
			super(fm);
			this.imageSet = imageSet;
			this.baseDir = baseDir;
		}

		@Override
		public Fragment getItem(int position) {
			ImageFragment f = ImageFragment.newInstance(new File(baseDir, imageSet.imagesItems[position].src));
			return f;
		}

		@Override
		public int getCount() {
			return imageSet.imagesItems.length;
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
	}

	public static class ImageFragment extends Fragment {
		private static final String	IMAGE_FILE	= "IMAGE_FILE";

		private File				imageFile;
		private ImageView			imageView;
		private PhotoViewAttacher	mAttacher;

		private ImageLoadTask		loadTask;

		public ImageFragment() {
			super();
		}

		public static ImageFragment newInstance(File imageFile) {
			Bundle b = new Bundle();
			b.putString(IMAGE_FILE, imageFile.getAbsolutePath());
			ImageFragment f = new ImageFragment();
			f.setArguments(b);
			return f;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			imageFile = new File(getArguments().getString(IMAGE_FILE));
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			imageView = new ImageView(getActivity());
			return imageView;
		}

		@Override
		public void onStart() {
			super.onStart();
			mAttacher = new PhotoViewAttacher(imageView);
			if (loadTask != null)
				loadTask.cancel(false);
			loadTask = new ImageLoadTask();
			loadTask.execute(new Void[0]);
		}

		@Override
		public void onStop() {
			super.onStop();
			if (loadTask != null)
				loadTask.cancel(false);
			mAttacher.cleanup();
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
			outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
			super.onSaveInstanceState(outState);
		}

		private class ImageLoadTask extends AsyncTask<Void, Void, Integer> {
			private Bitmap	bm	= null;

			@Override
			protected Integer doInBackground(Void... params) {
				try {
					final BitmapFactory.Options options = new BitmapFactory.Options();
					options.inJustDecodeBounds = true;
					BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
					options.inSampleSize = ImageResizer.calculateInSampleSize(options, IMAGE_WIDTH_PIXELS, IMAGE_HEIGHT_PIXELS);
					options.inJustDecodeBounds = false;
					bm = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
					return 0;
				} catch (Exception e) {
					Log.w(TAG, "error loading image", e);
					return 1;
				}
			}

			@Override
			protected void onPostExecute(Integer result) {
				if (result == 0) {
					mAttacher = new PhotoViewAttacher(imageView);
					imageView.setImageBitmap(bm);
					mAttacher.update();
				} else {
					Toast.makeText(getActivity(), "Error loading slide", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	// Image load task
	private ImageLoadTask	imageLoadTask	= null;

	private class ImageLoadTask extends AsyncTask<File, Void, Integer> {
		private Bitmap	bm	= null;

		@Override
		protected Integer doInBackground(File... params) {
			try {
				final BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(params[0].getAbsolutePath(), options);
				options.inSampleSize = ImageResizer.calculateInSampleSize(options, IMAGE_WIDTH_PIXELS, IMAGE_HEIGHT_PIXELS);
				options.inJustDecodeBounds = false;
				bm = BitmapFactory.decodeFile(params[0].getAbsolutePath(), options);
				return 0;
			} catch (Exception e) {
				Log.w(TAG, "error loading image", e);
				return 1;
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result == 0) {
				mAttacher = new PhotoViewAttacher(imageView);
				imageView.setImageBitmap(bm);
				mAttacher.update();
				imageRoot.setVisibility(View.VISIBLE);
				imageView.setVisibility(View.VISIBLE);
				activity.showHeader();
			} else {
				Toast.makeText(activity, "Error loading image", Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Override
	public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult error) {
		Toast.makeText(activity, "Error initializing YouTube player..", Toast.LENGTH_SHORT).show();
		Log.w(TAG, "YouTube init failed! - " + error.toString());
	}

	@Override
	public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
		youtubePlayerReady = true;
		youtubePlayer = player;
	}
}
