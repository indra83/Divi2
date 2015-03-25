package co.in.divi.lecture;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request.Method;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import co.in.divi.AlarmAlertWakeLock;
import co.in.divi.DiviApplication;
import co.in.divi.LectureSessionProvider;
import co.in.divi.LectureSessionProvider.ConnectionStatus;
import co.in.divi.LectureSessionProvider.LocationHolder;
import co.in.divi.LocationManager;
import co.in.divi.LocationManager.Breadcrumb;
import co.in.divi.LocationManager.DiviLocationChangeListener;
import co.in.divi.LocationManager.LOCATION_TYPE;
import co.in.divi.R;
import co.in.divi.SyncManager;
import co.in.divi.SyncManager.SyncStatus;
import co.in.divi.UserSessionProvider;
import co.in.divi.activity.BlackoutActivity;
import co.in.divi.activity.InstructionNotificationActivity;
import co.in.divi.content.DiviReference;
import co.in.divi.db.UserDBContract.Attempts;
import co.in.divi.db.sync.SyncDownService;
import co.in.divi.model.ClassMembers;
import co.in.divi.model.Instruction;
import co.in.divi.model.LectureInstruction;
import co.in.divi.ui.TeacherPanel;
import co.in.divi.util.Config;
import co.in.divi.util.InstallAppService;
import co.in.divi.util.LogConfig;
import co.in.divi.util.ServerConfig;
import co.in.divi.util.Util;

/*
 * This service performs the following functions:
 * 
 * 1. Subscribe to the channel for the lecture and listen for instructions
 * 2. Get new instructions
 * 3. Post current location to teacher channel - throttled.
 * 4. (If teacher) listen to locations and presence of lecture members
 * 5. Sync user data (attempts) - throttled. 
 */
public class LiveLectureService extends Service implements DiviLocationChangeListener {

	static final String					TAG							= LiveLectureService.class.getSimpleName();
    private static int                  FOREGROUND_ID               = 1338;

	public static final String			INTENT_EXTRA_STOP_SERVICE	= "INTENT_EXTRA_STOP_SERVICE";
	public static final String			INTENT_EXTRA_CHANNEL		= "INTENT_EXTRA_CHANNEL";

	private Pubnub						pubnub;
	private InstructionSubscribeThread	instructionSubscribeThread	= null;
	private String						channel;
	private Handler						handler;
	private LectureSessionProvider		lectureSessionProvider;
	private UserSessionProvider			userSessionProvider;
	private LocationManager				locationManager;

	private WifiLock					wifilock					= null;
	private WakeLock					wakelock					= null;

	// Chat head
	private WindowManager				windowManager;
	private ImageView					chatHead;
	private Runnable					chatHeadAutoClick			= new Runnable() {
																		@Override
																		public void run() {
																			if (chatHead != null) {
																				chatHead.performClick();
																			}
																		}
																	};
	private SoundPool					soundPool;
	private boolean						loaded;
	private int							soundId_bell, soundId_navigate;
	private int							streamId_bell, streamId_navigate;

	boolean								isRunning;
	// location stuff
	Timer								locationPostTimer			= new Timer();
	private TimerTask					postLocationRunnable		= new TimerTask() {
																		@Override
																		public void run() {
																			LocationHolder loc = new LocationHolder();
																			loc.uid = userSessionProvider.getUserData().uid;
																			loc.locationType = locationManager.getLocationType();
																			loc.locationSubType = locationManager.getLocationSubType();
																			loc.breadcrumb = locationManager.getBreadcrumb();
																			if (locationManager.getLocationType() != LOCATION_TYPE.UNKNOWN
																					&& locationManager.getLocationRef() != null)
																				loc.locationUri = locationManager.getLocationRef().getUri()
																						.toString();
																			else {
																				loc.locationUri = null;
																				try {
																					ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
																					PackageManager pm = getPackageManager();
																					List<RunningTaskInfo> tasks = am.getRunningTasks(10);
																					if (tasks.size() > 0) {
																						loc.locationUri = tasks.get(0).topActivity
																								.getPackageName();
																						loc.externalAppName = pm.getApplicationLabel(
																								pm.getApplicationInfo(loc.locationUri,
																										PackageManager.GET_META_DATA))
																								.toString();
																					}
																				} catch (Exception e) {
																					Log.w(TAG, "error fetching open activity");
																				}
																			}

																			JSONObject locationObject = null;
																			try {
																				locationObject = new JSONObject(new Gson().toJson(loc));
																			} catch (JSONException e) {
																				e.printStackTrace();
																			}
																			if (LogConfig.DEBUG_LIVE_LECTURE)
																				Log.d(TAG, "posting location:" + locationObject);

                                                                            // trying state
                                                                            pubnub.setState(channel,userSessionProvider.getUserData().uid,locationObject,new Callback() {
																				public void successCallback(String channel, Object message) {
																					if (LogConfig.DEBUG_LIVE_LECTURE)
																						Log.d(TAG, "location posting success");
																				}

																				public void errorCallback(String channel, Object message) {
																					// TODO: retry?
																					Log.w(TAG, "error posting location! - " + message);
																				}
																			});
																		}
																	};
	// sync user attempts
	private SyncManager					syncManager;
	private Runnable					startSyncRunnable			= new Runnable() {
																		@Override
																		public void run() {
																			startService(syncManager.getSyncUpIntent());
																		}
																	};
	private MyContentObserver			attemptsObserver;

	private class MyContentObserver extends ContentObserver {
		Handler	handler;

		public MyContentObserver(Handler handler) {
			super(handler);
			this.handler = handler;
		}

		@Override
		public void onChange(boolean selfChange) {
			long timeSinceLastSync = Util.getTimestampMillis() - syncManager.getLastSyncTime();
			if (LogConfig.DEBUG_LIVE_LECTURE)
				Log.d(TAG, "content observer change - " + timeSinceLastSync + ", " + syncManager.getSyncStatus());
			if (syncManager.getSyncStatus() == SyncStatus.SYNCING || timeSinceLastSync < Config.LECTURE_SYNC_THROTTLE_TIME) {
				handler.removeCallbacks(startSyncRunnable);
				handler.postDelayed(startSyncRunnable, Config.LECTURE_SYNC_THROTTLE_TIME);
				if (LogConfig.DEBUG_LIVE_LECTURE)
					Log.d(TAG, "sync task enqued");
			} else {
				handler.removeCallbacks(startSyncRunnable);
				startSyncRunnable.run();
			}
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		lectureSessionProvider = LectureSessionProvider.getInstance(this);
		userSessionProvider = UserSessionProvider.getInstance(this);
		locationManager = LocationManager.getInstance(this);
		syncManager = SyncManager.getInstance(this);
		handler = new Handler(Looper.getMainLooper());
		attemptsObserver = new MyContentObserver(handler);
		pubnub = new Pubnub(ServerConfig.PUBNUB_PUBLISH_KEY, ServerConfig.PUBNUB_SUBSCRIBE_KEY, false);
		pubnub.setUUID(userSessionProvider.getUserData().uid);
		pubnub.setMaxRetries(8);
		pubnub.setRetryInterval(3*1000);
//		pubnub.setSubscribeTimeout(55 * 1000);// default is 310 secs, do we need to reduce?
		pubnub.setNonSubscribeTimeout(10000);// timeout for publish/herenow etc.
		pubnub.setResumeOnReconnect(false);

		// heartbeat
		pubnub.setHeartbeat(22, new Callback() {
            int errorCount=0;
			@Override
			public void successCallback(String channel, Object arg1) {
                errorCount = 0;
				if (LogConfig.DEBUG_LIVE_LECTURE)
					Log.d(TAG, "heartbeat success!! - " + channel);
			}

			@Override
			public void errorCallback(String channel, PubnubError error) {
				Log.w(TAG, "heartbeat error! - " + error);
                errorCount++;
                if(errorCount>1) {
                    disconnect();
                }
			}
		});

		// locks
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifilock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "DiviPubnub");
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

		// Load the sound
		soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
		soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
				loaded = true;
			}
		});
		soundId_bell = soundPool.load(this, R.raw.bell, 1);
		soundId_navigate = soundPool.load(this, R.raw.navigate, 1);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (LogConfig.DEBUG_LIVE_LECTURE)
			Log.d(TAG, "got intent:" + intent);

		if (!lectureSessionProvider.isLectureJoined()) {
			// this is a crash? just end
			stopSelf();
			return START_NOT_STICKY;
		}

		isRunning = true;
		if (intent.hasExtra(INTENT_EXTRA_STOP_SERVICE)) {
			disconnect();
		} else {
			if (instructionSubscribeThread != null && instructionSubscribeThread.isAlive()) {
				if (LogConfig.DEBUG_LIVE_LECTURE)
					Log.d(TAG, "thread exists, unsubscribing...");
				disconnect();
			}
			if (wifilock != null && !wifilock.isHeld()) {
				wifilock.acquire();
				if (LogConfig.DEBUG_LIVE_LECTURE)
					Log.d(TAG, "got wifilock:" + wifilock.isHeld());
			}
			if (wakelock != null && !wakelock.isHeld()) {
				wakelock.acquire();
				if (LogConfig.DEBUG_LIVE_LECTURE)
					Log.d(TAG, "got wake lock:" + wakelock.isHeld());
			}
			channel = intent.getStringExtra(INTENT_EXTRA_CHANNEL);

			String[] channels;
			if (lectureSessionProvider.isCurrentUserTeacher()) {
				channels = new String[] { channel};
				fetchStudents(false);
			} else {
				channels = new String[] { channel };
			}
			instructionSubscribeThread = new InstructionSubscribeThread(channels);
			instructionSubscribeThread.start();

			// ensure we have latest commands
			startCommandSync();

			// sync user data and location
			handler.removeCallbacks(startSyncRunnable);
			handler.post(startSyncRunnable);
			handler.removeCallbacks(postLocationRunnable);
			handler.post(postLocationRunnable);

            startForeground(FOREGROUND_ID, new NotificationCompat.Builder(this)
                    .setOngoing(true)
                    .setContentTitle("Divi ClassControl")
                    .setContentText("Joined in lecture '" + lectureSessionProvider.getCurrentLecture().name+"'")
                    .setSmallIcon(R.drawable.ic_header_connected).build());

			// listen for changes to user data.
			getContentResolver().registerContentObserver(Attempts.CONTENT_URI, true, attemptsObserver);
		}

		return START_REDELIVER_INTENT;// if we get killed, restart(?)
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (LogConfig.DEBUG_LIVE_LECTURE)
			Log.d(TAG, "onDestroy");
		locationManager.removeListener(LiveLectureService.this);
		AlarmAlertWakeLock.releaseCpuLock();
		removeChatHeads();
		soundPool.release();
		soundPool = null;
		try {
			pubnub.shutdown();
		} catch (Exception e) {
			Log.w(TAG, "error shutdow!", e);
		}
		locationPostTimer.cancel();
		getContentResolver().unregisterContentObserver(attemptsObserver);
		DiviApplication.get().getRequestQueue().cancelAll(this);
		if (wifilock != null && wifilock.isHeld())
			wifilock.release();
		if (wakelock != null && wakelock.isHeld())
			wakelock.release();
		lectureSessionProvider.resetLecture();

		removeTeacherPanel();
	}

	private void disconnect() {
		// thread safe
		handler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(LiveLectureService.this, "Disconnected from lecture", Toast.LENGTH_LONG).show();
				locationManager.removeListener(LiveLectureService.this);
				AlarmAlertWakeLock.releaseCpuLock();
				removeChatHeads();
				locationPostTimer.cancel();
				getContentResolver().unregisterContentObserver(attemptsObserver);
				DiviApplication.get().getRequestQueue().cancelAll(LiveLectureService.this);
				if (wifilock != null && wifilock.isHeld())
					wifilock.release();
				if (wakelock != null && wakelock.isHeld())
					wakelock.release();
				try {
					pubnub.unsubscribeAll();
				} catch (Exception e) {
					Log.e(TAG, "error unsubscribing", e);
				}
				if (instructionSubscribeThread != null) {
					instructionSubscribeThread.interrupt();
					try {
						instructionSubscribeThread.join(500);
						// TODO: update state
					} catch (InterruptedException e) {
						Log.e(TAG, "worker thread did not join", e);
					}
				}
				stopSelf();
			}
		});
	}

	class InstructionSubscribeThread extends Thread {
		String[]	channels;

		public InstructionSubscribeThread(String[] channels) {
			super();
			this.channels = channels;
		}

		@Override
		public void run() {
			handler.post(new Runnable() {
				@Override
				public void run() {
					lectureSessionProvider.setStatus(ConnectionStatus.CONNECTING);
				}
			});
			if (LogConfig.DEBUG_LIVE_LECTURE)
				Log.d(TAG, "starting subscribe to " + channel);
			try {
				pubnub.subscribe(channels, new Callback() {
					@Override
					public void connectCallback(String channel, Object arg1) {
						if (LogConfig.DEBUG_LIVE_LECTURE)
							Log.d(TAG, "connectCallback - " + channel);
						handler.post(new Runnable() {
							@Override
							public void run() {
								fetchInstructions(true);
								locationManager.addListener(LiveLectureService.this);
								lectureSessionProvider.setStatus(ConnectionStatus.CONNECTED);
								// if teacher, subscribe for presence & show panel
								if (lectureSessionProvider.isCurrentUserTeacher()) {
									addTeacherPanel();
									subscribeForPresence();
								} else {
									locationPostTimer.cancel();
									locationPostTimer = new Timer();
									locationPostTimer.scheduleAtFixedRate(postLocationRunnable, 0,
											Config.LECTURE_LOCATION_POST_THROTTLE_TIME);
								}
							}
						});
					}

					@Override
					public void reconnectCallback(String channel, Object arg1) {
						if (LogConfig.DEBUG_LIVE_LECTURE)
							Log.d(TAG, "reconnectCallback - " + channel);
						handler.post(new Runnable() {
							@Override
							public void run() {
								lectureSessionProvider.setStatus(ConnectionStatus.CONNECTED);
                                fetchInstructions(true);
							}
						});
					}

					@Override
					public void disconnectCallback(String channel, Object arg1) {
						if (LogConfig.DEBUG_LIVE_LECTURE)
							Log.d(TAG, "disconnectCallback - " + channel+", "+arg1);
                        disconnect();
					}

					@Override
					public void successCallback(String channel, Object message) {
						if (LogConfig.DEBUG_LIVE_LECTURE)
							Log.d(TAG, "channel - " + channel + ", message - " + message);
						// check if stream command
						final Object msg = message;

						if (channel.equals(LiveLectureService.this.channel)) {
							handler.post(new Runnable() {
								@Override
								public void run() {
									try {
										Instruction newInstruction = new Gson().fromJson(msg.toString(), Instruction.class);
										if (newInstruction.type == Instruction.INSTRUCTION_TYPE_FOLLOW_ME) {
											if (LogConfig.DEBUG_LIVE_LECTURE)
												Log.d(TAG, "got follow instruction! - " + newInstruction.location);
											if (!lectureSessionProvider.isCurrentUserTeacher()) { // avoid infinite loop
												KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
												PowerManager powermanager = (PowerManager) LiveLectureService.this
														.getSystemService(Context.POWER_SERVICE);
												if (powermanager.isScreenOn() && !keyguardManager.inKeyguardRestrictedInputMode()) {
													lectureSessionProvider.setFollowMeInstruction(Uri.parse(newInstruction.location));
												} else {
													AlarmAlertWakeLock.acquireCpuWakeLock(LiveLectureService.this);
													// show notif activity
													Intent intent = new Intent(LiveLectureService.this,
															InstructionNotificationActivity.class);
													intent.setData(Uri.parse(newInstruction.location));
													String name = newInstruction.breadcrumb[0] + " > " + newInstruction.breadcrumb[1]
															+ " > " + newInstruction.breadcrumb[2] + " > " + newInstruction.breadcrumb[3];
													intent.putExtra(InstructionNotificationActivity.INTENT_EXTRA_NAME, name);
													intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
													startActivity(intent);
												}
											}
											return;
										}
									} catch (Exception e) {
										Log.w(TAG, "error in stream follow?", e);
									}
									fetchInstructions(false);
								}
							});
						}
					}

					@Override
					public void errorCallback(String channel, Object message) {
						if (LogConfig.DEBUG_LIVE_LECTURE)
							Log.d(TAG, "channel - " + channel + ", error - " + message);
						final String errorString = message.toString();
						handler.post(new Runnable() {
							@Override
							public void run() {
								lectureSessionProvider.onConnectionError(errorString);
                                lectureSessionProvider.setStatus(ConnectionStatus.CONNECTING);
							}
						});
					}
				});
			} catch (PubnubException e) {
				Log.e(TAG, "pubnub exception", e);
				disconnect();
			}
		}
	}

	private void subscribeForPresence() {
		if (LogConfig.DEBUG_LIVE_LECTURE)
			Log.d(TAG, "starting presence request");
		try {
			// start listening for changes
			pubnub.presence(channel, new Callback() {

				@Override
				public void connectCallback(String channel, Object message) {
					if (LogConfig.DEBUG_LIVE_LECTURE)
						Log.d(TAG, "PRESENCE : " + channel + " : " + message.getClass() + " : " + message.toString());
					updateHereNow();
				}

				@Override
				public void reconnectCallback(String channel, Object message) {
					if (LogConfig.DEBUG_LIVE_LECTURE)
						Log.d(TAG, "PRESENCE : " + channel + " : " + message.getClass() + " : " + message.toString());
					updateHereNow();
				}

				@Override
				public void successCallback(String channel, Object message) {
					if (LogConfig.DEBUG_LIVE_LECTURE)
						Log.d(TAG, "PRESENCE : " + channel + " : " + message.getClass() + " : " + message.toString());
					JSONObject presenceObject = (JSONObject) message;
					try {
						final String uid = presenceObject.getString("uuid");
						final String action = presenceObject.getString("action");
                        if (action.equals("state-change")) {
                            final LocationHolder loc = new Gson().fromJson(presenceObject.getJSONObject("data").toString(),LocationHolder.class);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        loc.timestampInMillis = Util.getTimestampMillis();
                                        lectureSessionProvider.setLocation(loc.uid, loc);
                                    } catch (Exception e) {
                                        Log.w(TAG, "", e);
                                    }// never fail!
                                }
                            });
                        } else {
                            final int occupancy = presenceObject.getInt("occupancy");
                            final long timestamp = presenceObject.getLong("timestamp");
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if ("join".equalsIgnoreCase(action)) {
                                        lectureSessionProvider.joinHereNow(uid, timestamp);
                                    } else if ("leave".equalsIgnoreCase(action) || "timeout".equalsIgnoreCase(action)) {
                                        lectureSessionProvider.leaveHereNow(uid, timestamp);
                                    }
                                    if (lectureSessionProvider.hereNow().size() != occupancy) {
                                        // re sync occupancy
                                        updateHereNow();
                                    }
                                }
                            });
                        }
                    } catch (JSONException e) {
						Log.e(TAG, "error parsing here now response", e);
						// disconnect();
					}
				}

				@Override
				public void errorCallback(String channel, PubnubError error) {
					Log.w(TAG, "PRESENCE : ERROR on channel " + channel + " : " + error.toString());
					// just ignore
				}

				@Override
				public void disconnectCallback(String channel, Object message) {
					Log.w(TAG, "PRESENCE : disconnectCallback on channel " + channel + " : " + message);
					disconnect();
				}
			});
		} catch (Exception e) {
			Log.e(TAG, "error occured:", e);
			disconnect();
		}
	}

	private void updateHereNow() {
		try {// get current users
			if (LogConfig.DEBUG_LIVE_LECTURE)
				Log.d(TAG, "starting herenow request");
			pubnub.hereNow(channel, new Callback() {
				@Override
				public void errorCallback(String channel, Object error) {
					Log.w(TAG, "PRESENCE(herenow) : ERROR on channel " + channel + " : " + error.toString());
				}

				@Override
				public void successCallback(String channel, Object message) {
					if (LogConfig.DEBUG_LIVE_LECTURE)
						Log.d(TAG, "HERE NOW : " + message);
					JSONObject hereNowObject = (JSONObject) message;
					try {
						JSONArray uuidsArray = hereNowObject.getJSONArray("uuids");
						final ArrayList<String> uuids = new ArrayList<String>();
						for (int i = 0; i < uuidsArray.length(); i++)
							uuids.add(uuidsArray.getString(i));
						handler.post(new Runnable() {
							@Override
							public void run() {
								lectureSessionProvider.setHereNow(uuids);
							}
						});
					} catch (JSONException e) {
						Log.e(TAG, "error parsing here now response", e);
					}
				}

				@Override
				public void errorCallback(String channel, PubnubError error) {
					Log.e(TAG, "HERE NOW : " + error);
				}
			});
		} catch (Exception e) {
			Log.e(TAG, "error occured:", e);
			// end if teacher can't get presence info
			disconnect();
			Toast.makeText(this, "Online presence detection failed!", Toast.LENGTH_LONG).show();
		}
	}

	private void fetchInstructions(final boolean checkHistory) {
		// call from main thread
		try {
			JSONObject jsonRequest = new JSONObject();
			jsonRequest.put("uid", userSessionProvider.getUserData().uid);
			jsonRequest.put("token", userSessionProvider.getUserData().token);
			jsonRequest.put("lectureId", lectureSessionProvider.getCurrentLecture().id);
			jsonRequest.put("since", 0);
			String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_GETINSTRUCTIONS;
			if (LogConfig.DEBUG_LIVE_LECTURE)
				Log.d(TAG, "request:\n" + jsonRequest.toString());
			JsonObjectRequest getInstructionsRequest = new JsonObjectRequest(Method.POST, url, jsonRequest, new Listener<JSONObject>() {
				@Override
				public void onResponse(JSONObject response) {
					if (LogConfig.DEBUG_LIVE_LECTURE)
						Log.d(TAG, "got response:\n" + response.toString());
					Gson gson = new Gson();
					LectureInstruction lectureInstruction = gson.fromJson(response.toString(), LectureInstruction.class);
					if (!lectureInstruction.lectureStatus.equals("live")) {
						Toast.makeText(LiveLectureService.this, "Lecture ended, ignoring teacher instruction.", Toast.LENGTH_LONG).show();
						disconnect();
					}
					// ensure its not a repeat instruction
					// if (lectureSessionProvider.getInstructions() == null
					// || !lectureSessionProvider.getInstructions().instructions[0].id
					// .equalsIgnoreCase(lectureInstruction.instructions[0].id)) {
					if (LogConfig.DEBUG_LIVE_LECTURE)
						Log.d(TAG, "setting new instructions");
					lectureSessionProvider.setInstructions(lectureInstruction);
					executeLatestInstruction(lectureInstruction);
					// }
					// Check for last stream command
					if (checkHistory && lectureInstruction.instructions.length > 0) {
						if (LogConfig.DEBUG_LIVE_LECTURE)
							Log.d(TAG, "starting history request");
						pubnub.history(channel, 1, false, new Callback() {
							@Override
							public void successCallback(String channel, Object message) {
								if (LogConfig.DEBUG_LIVE_LECTURE)
									Log.d(TAG, "history:::msg - " + message);
								try {
									JSONArray historyArray = new JSONArray(message.toString());
									Instruction newInstruction = new Gson().fromJson(historyArray.getJSONArray(0).getJSONObject(0)
											.toString(), Instruction.class);
									if (newInstruction.type == Instruction.INSTRUCTION_TYPE_FOLLOW_ME) {
										if (LogConfig.DEBUG_LIVE_LECTURE)
											Log.d(TAG, "got follow instruction! - " + newInstruction.location);
										if (!lectureSessionProvider.isCurrentUserTeacher()) { // avoid infinite loop
											removeChatHeads();
											Uri locUri = Uri.parse(newInstruction.location);
											DiviReference diviRef = new DiviReference(locUri);
											if (!userSessionProvider.getCourseId().equals(diviRef.courseId))
												Toast.makeText(LiveLectureService.this, "Please change course!", Toast.LENGTH_SHORT).show();
											Util.openInstruction(LiveLectureService.this, locUri);
										}
									}
								} catch (Exception e) {
									Log.w(TAG, "error processing history for stream instructions", e);
								}
							}
						});
					}
				}
			}, new ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					Log.w(TAG, "error:" + error);
					Toast.makeText(LiveLectureService.this, "Error fetching instructions.", Toast.LENGTH_LONG).show();
					// TODO: retry
				}
			});
			getInstructionsRequest.setShouldCache(false);
			DiviApplication.get().getRequestQueue().add(getInstructionsRequest).setTag(this);
		} catch (Exception e) {
			Log.e(TAG, "Error fetching lectures", e);
			Toast.makeText(LiveLectureService.this, "Error fetching live lectures.", Toast.LENGTH_LONG).show();
		}
	}

	private void fetchStudents(final boolean isRetry) {
		if (LogConfig.DEBUG_LIVE_LECTURE)
			Log.d(TAG, "fetching student list");
		try {
			JSONObject jsonRequest = new JSONObject();
			jsonRequest.put("uid", userSessionProvider.getUserData().uid);
			jsonRequest.put("token", userSessionProvider.getUserData().token);
			jsonRequest.put("lectureId", lectureSessionProvider.getCurrentLecture().id);
			String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_GETLECTUREMEMBERS;
			if (LogConfig.DEBUG_LIVE_LECTURE)
				Log.d(TAG, "request:\n" + jsonRequest.toString());
			JsonObjectRequest getInstructionsRequest = new JsonObjectRequest(Method.POST, url, jsonRequest, new Listener<JSONObject>() {
				@Override
				public void onResponse(JSONObject response) {
					if (LogConfig.DEBUG_LIVE_LECTURE)
						Log.d(TAG, "got response:\n" + response.toString());
					Gson gson = new Gson();
					ClassMembers classMembers = gson.fromJson(response.toString(), ClassMembers.class);
					lectureSessionProvider.setClassMembers(classMembers.members);
				}
			}, new ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					Log.w(TAG, "error:" + error);
					if (isRetry) {
						Toast.makeText(LiveLectureService.this, "Error fetching students.", Toast.LENGTH_LONG).show();
						disconnect();// without student list don't stay connected.
					} else
						fetchStudents(true);
				}
			});
			getInstructionsRequest.setShouldCache(false);
			DiviApplication.get().getRequestQueue().add(getInstructionsRequest).setTag(this);
		} catch (Exception e) {
			Log.w(TAG, "Error fetching students", e);
			if (isRetry) {
				Toast.makeText(LiveLectureService.this, "Error fetching students.", Toast.LENGTH_LONG).show();
				disconnect();// without student list don't stay connected.
			} else
				fetchStudents(true);
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	private void executeLatestInstruction(LectureInstruction instructions) {
		if (LogConfig.DEBUG_LIVE_LECTURE)
			Log.d(TAG, "executing latest instruction");
		if (instructions.instructions.length > 0) {
			if (LogConfig.DEBUG_LIVE_LECTURE)
				Log.d(TAG, "instruction - " + instructions.instructions[0].id);
            if(instructions.instructions[0].id.equals(lectureSessionProvider.getLastExecutedInstructionId())) {
                Log.d(TAG,"ignoring instruction, already applied");
                return;
            }else {
                lectureSessionProvider.setLastExecutedInstructionId(instructions.instructions[0].id);
            }
			try {
				// play a notification sound
				AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
				float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
				float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
				float volume = actualVolume / maxVolume;
				soundPool.stop(streamId_bell);
				streamId_bell = soundPool.play(soundId_bell, volume, volume, 1, 0, 1f);
				Log.d(TAG, "parsing - " + instructions.instructions[0].data);
				Instruction instruction = new Gson().fromJson(instructions.instructions[0].data, Instruction.class);
				if (instruction.syncCommand) {
					startCommandSync();
				}
				if (instruction.type == Instruction.INSTRUCTION_TYPE_NAVIGATE
						|| instruction.type == Instruction.INSTRUCTION_TYPE_NAVIGATE_EXTERNAL) {
					if (lectureSessionProvider.isCurrentUserTeacher()) {
						Toast.makeText(this, "Instruction posted.", Toast.LENGTH_LONG).show();
						return;
					}

					KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Activity.KEYGUARD_SERVICE);
					PowerManager powermanager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
					if (powermanager.isScreenOn() && !keyguardManager.inKeyguardRestrictedInputMode()) {
						// show chathead
						if (instruction.type == Instruction.INSTRUCTION_TYPE_NAVIGATE)
							showChatHead(Uri.parse(instruction.location), null);
						else
							showChatHead(null, instruction.location);
					} else {
						AlarmAlertWakeLock.acquireCpuWakeLock(this);
						// show notif activity
						if (instruction.type == Instruction.INSTRUCTION_TYPE_NAVIGATE) {
							Intent intent = new Intent(this, InstructionNotificationActivity.class);
							intent.setData(Uri.parse(instruction.location));
							String name = instruction.breadcrumb[0] + " > " + instruction.breadcrumb[1] + " > " + instruction.breadcrumb[2]
									+ " > " + instruction.breadcrumb[3];
							intent.putExtra(InstructionNotificationActivity.INTENT_EXTRA_NAME, name);
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
							startActivity(intent);
						} else {// instruction is external app
							Intent intent = new Intent(this, InstructionNotificationActivity.class);
							intent.putExtra(InstructionNotificationActivity.INTENT_EXTRA_EXTERNAL_APP, instruction.location);
							String name = instruction.breadcrumb[0];
							intent.putExtra(InstructionNotificationActivity.INTENT_EXTRA_NAME, name);
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
							startActivity(intent);
						}
					}
				} else if (instruction.type == Instruction.INSTRUCTION_TYPE_BLACKOUT) {
					if (lectureSessionProvider.isCurrentUserTeacher()) {
						Toast.makeText(this, "Blackout posted.", Toast.LENGTH_LONG).show();
						return;
					}
					Intent intent = new Intent(this, BlackoutActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
					startActivity(intent);

				} else if (instruction.type == Instruction.INSTRUCTION_TYPE_END_LECTURE) {
					lectureSessionProvider.leaveLecture();
				}
			} catch (Exception e) {
				Log.w(TAG, "error parsing instruction", e);
				return;
			}
		}
		// for (String uid : hereNow) {
		// Log.d(TAG, "here now : " + uid);
		// }
	}

	// sync commands
	private void startCommandSync() {
		Intent startSyncDownService = new Intent(this, SyncDownService.class);
		startSyncDownService.putExtra(SyncDownService.INTENT_EXTRA_ONLY_COMMAND, true);
		startService(startSyncDownService);
	}

	// chat-head
	private void showChatHead(final Uri uri, final String externalAppPackageName) {
		removeChatHeads();
		chatHead = new ImageView(this);
		chatHead.setImageResource(R.drawable.ic_bell);

		WindowManager.LayoutParams params = new WindowManager.LayoutParams(100, 100, WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

		params.gravity = Gravity.TOP | Gravity.RIGHT;
		params.x = 15;
		params.y = 150;

		chatHead.setClickable(true);
		chatHead.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(LiveLectureService.this, "Opening instruction...", Toast.LENGTH_SHORT).show();
				AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
				float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
				float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
				float volume = actualVolume / maxVolume;
				soundPool.stop(streamId_navigate);
				streamId_navigate = soundPool.play(soundId_navigate, volume, volume, 1, 0, 1f);
				removeChatHeads();
				if (uri != null)
					Util.openInstruction(LiveLectureService.this, uri);
				else {
					try {
						Intent intent = getPackageManager().getLaunchIntentForPackage(externalAppPackageName);
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                            startActivity(intent);
                        } else {
                            Toast.makeText(LiveLectureService.this, "Shared app could not be opened...", Toast.LENGTH_SHORT).show();
                            Intent installerIntent = new Intent(LiveLectureService.this, InstallAppService.class);
                            installerIntent.putExtra(InstallAppService.INTENT_EXTRA_PACKAGE, externalAppPackageName);
                            startService(installerIntent);
                        }
                    } catch (Exception e) {
                        Toast.makeText(LiveLectureService.this, "Shared app not found on your tablet!", Toast.LENGTH_LONG).show();
                        Intent installerIntent = new Intent(LiveLectureService.this, InstallAppService.class);
                        installerIntent.putExtra(InstallAppService.INTENT_EXTRA_PACKAGE, externalAppPackageName);
                        startService(installerIntent);
                    }
				}
			}
		});
		windowManager.addView(chatHead, params);
		handler.postDelayed(chatHeadAutoClick, Config.DELAY_CHATHEAD_AUTO_OPEN);
		Toast.makeText(this, "Received an instruction from teacher", Toast.LENGTH_SHORT).show();
	}

	private void removeChatHeads() {
		handler.removeCallbacks(chatHeadAutoClick);
		if (chatHead != null)
			windowManager.removeView(chatHead);
		chatHead = null;
	}

	// Teacher Panel stuff
	private TeacherPanel	teacherPanel;

	private void addTeacherPanel() {
		removeTeacherPanel();
		teacherPanel = (TeacherPanel) ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.panel_teacher,
				null, false);
		WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

		params.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
		((WindowManager) getSystemService(Context.WINDOW_SERVICE)).addView(teacherPanel, params);
		teacherPanel.initialize();
	}

	private void removeTeacherPanel() {
		if (teacherPanel != null) {
			teacherPanel.stopAndRemove();
			teacherPanel = null;
		}
	}

	/*
	 * If stream is on, keep publishing new locations.
	 */
	@Override
	public void onLocationChange(DiviReference newRef, Breadcrumb breadcrumb) {
		try {
			if (lectureSessionProvider.isFollowMe()) {
				if (lectureSessionProvider.getInstructions() != null && lectureSessionProvider.getInstructions().instructions.length > 0) {
					Instruction instruction = new Gson().fromJson(lectureSessionProvider.getInstructions().instructions[0].data,
							Instruction.class);
					if (instruction.followMe) {
						DiviReference instructionLoc = new DiviReference(Uri.parse(instruction.location));
						if (instructionLoc.isSameResourceAs(newRef) && newRef.fragment != null) {
							Instruction streamInstruction = new Instruction();
							streamInstruction.type = Instruction.INSTRUCTION_TYPE_FOLLOW_ME;
							streamInstruction.location = newRef.getUri().toString();
							streamInstruction.breadcrumb = breadcrumb.getBreadcrumbArray();
							JSONObject instructionObject = new JSONObject(new Gson().toJson(streamInstruction));
							pubnub.publish(channel, instructionObject, new Callback() {
								@Override
								public void successCallback(String arg0, Object arg1) {
									if (LogConfig.DEBUG_LIVE_LECTURE)
										Log.d(TAG, "stream follow instruction posted!");
								}

								@Override
								public void errorCallback(String arg0, PubnubError arg1) {
									Log.w(TAG, "stream follow post failed" + arg1);
									Toast.makeText(LiveLectureService.this, "Error fetching students.", Toast.LENGTH_LONG).show();
								}
							});
						} else {
							if (LogConfig.DEBUG_LIVE_LECTURE)
								Log.d(TAG, "stopping follow!");
							lectureSessionProvider.setFollowMe(false);
						}
					}
				}
			}
		} catch (Exception e) {// never fail because of this...
			Log.w(TAG, "error in stream?", e);
		}
	}
}