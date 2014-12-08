package co.in.divi.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request.Method;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;

import co.in.divi.DiviApplication;
import co.in.divi.LectureSessionProvider;
import co.in.divi.LocationManager;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.content.DiviReference;
import co.in.divi.fragment.DashboardDialogFragment;
import co.in.divi.fragment.PostInstructionDialogFragment;
import co.in.divi.model.Instruction;
import co.in.divi.model.LectureDetails;
import co.in.divi.model.UserData;
import co.in.divi.model.UserData.ClassRoom;
import co.in.divi.util.Config;
import co.in.divi.util.LogConfig;
import co.in.divi.util.ServerConfig;
import co.in.divi.util.Util;

public class LecturesPopupHolder implements OnDismissListener {
	private static final String	TAG	= LecturesPopupHolder.class.getSimpleName();

	Activity					activity;
	PopupWindow					popup;
	View						anchor;

	LayoutInflater				layoutInflater;
	ProgressBar					popupProgress;
	ViewGroup					lecturesContainer, popupRoot, teacherPanel, lastPanel, lastClickable;
	TextView					title, noLectures, lastName;
	ImageView					lastIcon;
	Button						joinLeaveButton;

	LectureSessionProvider		lectureSessionProvider;
	UserSessionProvider			userSessionProvider;
	LocationManager				locationManager;

	public LecturesPopupHolder(Activity context, View anchor) {
		this.activity = context;
		this.anchor = anchor;
		lectureSessionProvider = LectureSessionProvider.getInstance(context);
		userSessionProvider = UserSessionProvider.getInstance(context);
		locationManager = LocationManager.getInstance(context);
		layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		popupRoot = (ViewGroup) layoutInflater.inflate(R.layout.popup_lecture, null);
		lecturesContainer = (ViewGroup) popupRoot.findViewById(R.id.contents);
		teacherPanel = (ViewGroup) popupRoot.findViewById(R.id.teacher_panel);
		lastPanel = (ViewGroup) popupRoot.findViewById(R.id.last_panel);
		lastClickable = (ViewGroup) popupRoot.findViewById(R.id.instruction);
		popupProgress = (ProgressBar) popupRoot.findViewById(R.id.progress);
		noLectures = (TextView) popupRoot.findViewById(R.id.no_lectures);
		title = (TextView) popupRoot.findViewById(R.id.title);
		lastName = (TextView) popupRoot.findViewById(R.id.last_name);
		lastIcon = (ImageView) popupRoot.findViewById(R.id.last_icon);

		popupRoot.findViewById(R.id.btn_dashboard).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
				DashboardDialogFragment dashboardFragment = new DashboardDialogFragment();
				FragmentManager fm = activity.getFragmentManager();
				dashboardFragment.show(fm, "dialog_fragment");
			}
		});
		popupRoot.findViewById(R.id.btn_share).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (locationManager.getLocationRef() == null) {
					Toast.makeText(activity, "Open the resource you want to share", Toast.LENGTH_LONG).show();
					return;
				}
				FragmentManager fm = activity.getFragmentManager();
				PostInstructionDialogFragment postInstruction = new PostInstructionDialogFragment();
				postInstruction.show(fm, "POST_INSTRUCTION_DIALOG");
			}
		});
		popupRoot.findViewById(R.id.btn_blackout).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentManager fm = activity.getFragmentManager();
				PostInstructionDialogFragment postInstruction = new PostInstructionDialogFragment();
				Bundle args = new Bundle();
				args.putBoolean(PostInstructionDialogFragment.EXTRA_IS_BLACKOUT, true);
				postInstruction.setArguments(args);
				postInstruction.show(fm, "POST_INSTRUCTION_DIALOG");
			}
		});

		popup = new PopupWindow(popupRoot, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		popup.setOutsideTouchable(true);
		popup.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.bg_class_popup));
		popup.setOnDismissListener(this);
	}

	public void refresh() {
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "refresh");
		hideAll();
		if (!lectureSessionProvider.isLectureJoined()) {
			showProgressBar();
			// start request for live lectures
			try {
				JSONObject jsonRequest = new JSONObject();
				jsonRequest.put("uid", userSessionProvider.getUserData().uid);
				jsonRequest.put("token", userSessionProvider.getUserData().token);
				String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_GETLECTURES;
				if (LogConfig.DEBUG_ACTIVITIES)
					Log.d(TAG, jsonRequest.toString());
				JsonArrayRequest liveLectureRequest = new JsonArrayRequest(Method.POST, url, jsonRequest, new Listener<JSONArray>() {
					@Override
					public void onResponse(JSONArray response) {
						if (LogConfig.DEBUG_ACTIVITIES)
							Log.d(TAG, "got response:\n" + response.toString());
						// validate response
						Gson gson = new Gson();
						LectureDetails[] lectures = gson.fromJson(response.toString(), LectureDetails[].class);
						showAvailableLectures(lectures);
					}
				}, new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.w(TAG, "error:" + error);
						Toast.makeText(activity, "Error fetching live lectures.", Toast.LENGTH_LONG).show();
						dismiss();
					}
				});
				liveLectureRequest.setShouldCache(false);
				DiviApplication.get().getRequestQueue().add(liveLectureRequest).setTag(this);
			} catch (Exception e) {
				Log.e(TAG, "Error fetching lectures", e);
				Toast.makeText(activity, "Error fetching live lectures.", Toast.LENGTH_LONG).show();
			}
		} else {
			// display details of current lecture.
			showLectureDetails();
		}
	}

	private void showProgressBar() {
		hideAll();
		popupProgress.setVisibility(View.VISIBLE);
	}

	private void showLectureDetails() {
		lecturesContainer.setVisibility(View.VISIBLE);
		lecturesContainer.removeAllViews();
		LectureDetails curLecture = lectureSessionProvider.getCurrentLecture();
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "showing lecture details:" + curLecture);
		View lectureItem = layoutInflater.inflate(R.layout.item_lecture, null);
		((TextView) lectureItem.findViewById(R.id.lecture_name)).setText(curLecture.name);
		((TextView) lectureItem.findViewById(R.id.lecture_teacher)).setText(curLecture.teacherName);
		joinLeaveButton = ((Button) lectureItem.findViewById(R.id.lecture_join));
		if (lectureSessionProvider.isCurrentUserTeacher()) {
			teacherPanel.setVisibility(View.VISIBLE);
			joinLeaveButton.setText("End Lecture");
			joinLeaveButton.setBackgroundResource(R.drawable.bg_red_button);
			joinLeaveButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View button) {
					joinLeaveButton.setText("Ending...");
					try {
						JSONObject instructionData = new JSONObject();
						instructionData.put("type", 2);
						instructionData.put("location", "get lost");

						JSONObject jsonRequest = new JSONObject();
						jsonRequest.put("uid", userSessionProvider.getUserData().uid);
						jsonRequest.put("token", userSessionProvider.getUserData().token);
						jsonRequest.put("lectureId", lectureSessionProvider.getCurrentLecture().id);
						jsonRequest.put("instruction", instructionData.toString());
						if (LogConfig.DEBUG_ACTIVITIES)
							Log.d(TAG, "endLecture request:\n" + jsonRequest.toString());
						String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_ENDLECTURE;
						JsonObjectRequest endLectureRequest = new JsonObjectRequest(Method.POST, url, jsonRequest,
								new Listener<JSONObject>() {
									@Override
									public void onResponse(JSONObject response) {
										if (LogConfig.DEBUG_ACTIVITIES)
											Log.d(TAG, "endLecture got response:\n" + response.toString());
										// TODO: validate response
										lectureSessionProvider.leaveLecture();
										refresh();
									}
								}, new ErrorListener() {
									@Override
									public void onErrorResponse(VolleyError error) {
										Log.w(TAG, "error:" + error);
										Toast.makeText(activity, "Error ending lecture.", Toast.LENGTH_LONG).show();
										dismiss();
									}
								});
						endLectureRequest.setShouldCache(false);
						DiviApplication.get().getRequestQueue().add(endLectureRequest).setTag(this);

					} catch (Exception e) {
						Log.e(TAG, "Error fetching lectures", e);
						Toast.makeText(activity, "Error fetching live lectures.", Toast.LENGTH_LONG).show();
					}
					popup.dismiss();
				}
			});
		} else {
			joinLeaveButton.setText("Leave");
			joinLeaveButton.setBackgroundResource(R.drawable.bg_red_button);
			joinLeaveButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View button) {
					lectureSessionProvider.leaveLecture();
					popup.dismiss();
				}
			});
		}
		lecturesContainer.addView(lectureItem);
		// fill last instruction
		if (lectureSessionProvider.getInstructions() != null && lectureSessionProvider.getInstructions().instructions.length > 0) {
			if (LogConfig.DEBUG_ACTIVITIES)
				Log.d(TAG, " showing instruction");
			try {
				Instruction instruction = new Gson().fromJson(lectureSessionProvider.getInstructions().instructions[0].data,
						Instruction.class);
				lastPanel.setVisibility(View.VISIBLE);
				lastClickable.setOnClickListener(null);
				if (instruction.type == Instruction.INSTRUCTION_TYPE_NAVIGATE) {
					final DiviReference ref = new DiviReference(Uri.parse(instruction.location));
					if (ref.type == DiviReference.REFERENCE_TYPE_TOPIC) {
						lastIcon.setImageResource(R.drawable.ic_home_learn_grey);
						lastName.setText(instruction.breadcrumb[2]);
						lastClickable.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								Util.openInstruction(activity, ref.getUri());
							}
						});
					} else if (ref.type == DiviReference.REFERENCE_TYPE_ASSESSMENT) {
						lastIcon.setImageResource(R.drawable.ic_home_practice_grey);
						lastName.setText(instruction.breadcrumb[2]);
						lastClickable.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								Util.openInstruction(activity, ref.getUri());
							}
						});
					}
				} else if (instruction.type == Instruction.INSTRUCTION_TYPE_BLACKOUT) {
					lastIcon.setImageResource(R.drawable.ic_blackout_n);
					lastName.setText("Blackout");
				} else if (instruction.type == Instruction.INSTRUCTION_TYPE_NAVIGATE_EXTERNAL) {
					PackageManager pm = activity.getPackageManager();
					lastIcon.setImageDrawable(pm.getApplicationIcon(instruction.location));
					lastName.setText(instruction.breadcrumb[0]);
					final String pkgName = instruction.location;
					lastClickable.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = activity.getPackageManager().getLaunchIntentForPackage(pkgName);
							if (intent != null) {
								activity.startActivity(intent);
							} else {
								Toast.makeText(activity, "Shared app could not be opened...", Toast.LENGTH_SHORT).show();
							}
						}
					});
				} else {
					Log.w(TAG, "unknown instruction type? - " + instruction);
				}
			} catch (Exception e) {
				Log.w(TAG, "error parsing instruction", e);
			}
		}
	}

	private void showAvailableLectures(LectureDetails[] lectures) {
		hideAll();
		UserData userData = userSessionProvider.getUserData();
		if (lectures.length == 0 && !userData.isTeacher()) {
			noLectures.setVisibility(View.VISIBLE);
		} else {
			lecturesContainer.setVisibility(View.VISIBLE);
			lecturesContainer.removeAllViews();
			// auto join if teacher is same
			for (LectureDetails lecture : lectures) {
				if (lecture.teacherId.equals(userSessionProvider.getUserData().uid)) {
					if (LogConfig.DEBUG_ACTIVITIES)
						Log.d(TAG, "auto joining own lecture");
					lectureSessionProvider.joinLecture(lecture);
					return;
				}
			}
			HashSet<String> lectureClassIds = new HashSet<String>();
			for (final LectureDetails lecture : lectures) {
				if (LogConfig.DEBUG_ACTIVITIES)
					Log.d(TAG, "adding lecture: " + lecture.name);
				lectureClassIds.add(lecture.classRoomId);
				View lectureItem = layoutInflater.inflate(R.layout.item_lecture, null);
				((TextView) lectureItem.findViewById(R.id.lecture_name)).setText(lecture.name);
				((TextView) lectureItem.findViewById(R.id.lecture_teacher)).setText(lecture.teacherName);
				((Button) lectureItem.findViewById(R.id.lecture_join)).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View arg0) {
						lectureSessionProvider.joinLecture(lecture);
					}
				});
				lecturesContainer.addView(lectureItem);
			}
			// teacher stuff (to start new lecture)
			if (userData.isTeacher()) {
				for (final ClassRoom classRoom : userData.classRooms) {
					if (lectureClassIds.contains(classRoom.classId))
						continue;// cannot create lecture if another teacher already created one...
                    if(Config.IS_PLAYSTORE_APP && classRoom.classId.equalsIgnoreCase(Config.IGNORE_CLASS_ID))
                        continue;// Don't allow lecture creation for default classroom.
					View lectureItem = layoutInflater.inflate(R.layout.item_lecture, null);
					((TextView) lectureItem.findViewById(R.id.lecture_name)).setText(classRoom.toString());
					// ((TextView) lectureItem.findViewById(R.id.lecture_teacher)).setText(userData.name);
					final Button lectureJoin = ((Button) lectureItem.findViewById(R.id.lecture_join));
					lectureJoin.setText("Start lecture");
					lectureJoin.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View button) {
							lectureJoin.setText("Starting...");
							try {
								JSONObject jsonRequest = new JSONObject();
								jsonRequest.put("uid", userSessionProvider.getUserData().uid);
								jsonRequest.put("token", userSessionProvider.getUserData().token);
								jsonRequest.put("classRoomId", classRoom.classId);
								jsonRequest.put("name", classRoom.toString());
								jsonRequest.put("startTime", Util.getTimestampMillis());
								if (LogConfig.DEBUG_ACTIVITIES)
									Log.d(TAG, "request:\n" + jsonRequest.toString());
								String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_CREATELECTURE;
								JsonObjectRequest liveLectureRequest = new JsonObjectRequest(Method.POST, url, jsonRequest,
										new Listener<JSONObject>() {
											@Override
											public void onResponse(JSONObject response) {
												if (LogConfig.DEBUG_ACTIVITIES)
													Log.d(TAG, "got response:\n" + response.toString());
												// validate response
												Gson gson = new Gson();
												LectureDetails lecture = gson.fromJson(response.toString(), LectureDetails.class);
												if (lecture == null || lecture.id == null) {
													Toast.makeText(activity, "Error creating lecture.", Toast.LENGTH_LONG).show();
													dismiss();
													return;
												}
												lectureSessionProvider.joinLecture(lecture);
												showLectureDetails();
											}
										}, new ErrorListener() {
											@Override
											public void onErrorResponse(VolleyError error) {
												Log.w(TAG, "error:" + error);
												Toast.makeText(activity, "Error creating lecture.", Toast.LENGTH_LONG).show();
												dismiss();
											}
										});
								liveLectureRequest.setShouldCache(false);
								DiviApplication.get().getRequestQueue().add(liveLectureRequest).setTag(this);

							} catch (Exception e) {
								Log.e(TAG, "Error fetching lectures", e);
								Toast.makeText(activity, "Error fetching live lectures.", Toast.LENGTH_LONG).show();
							}
						}
					});
					lecturesContainer.addView(lectureItem);
				}
			}
		}
	}

	private void hideAll() {
		teacherPanel.setVisibility(View.GONE);
		popupProgress.setVisibility(View.INVISIBLE);
		noLectures.setVisibility(View.INVISIBLE);
		lecturesContainer.setVisibility(View.GONE);
		lastPanel.setVisibility(View.GONE);
	}

	// proxy popup functions
	public boolean isShowing() {
		return popup.isShowing();
	}

	public void dismiss() {
		popup.dismiss();
	}

	public void show() {
		popup.showAsDropDown(anchor, -25, 0);
		refresh();
	}

	@Override
	public void onDismiss() {
		DiviApplication.get().getRequestQueue().cancelAll(this);
	}
}
