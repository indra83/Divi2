package co.in.divi.lecture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import co.in.divi.DiviApplication;
import co.in.divi.LectureSessionProvider;
import co.in.divi.LectureSessionProvider.DashboardChangeListener;
import co.in.divi.LectureSessionProvider.LectureStatusChangeListener;
import co.in.divi.LectureSessionProvider.LocationHolder;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.content.AssessmentFileModel;
import co.in.divi.content.AssessmentFileModel.Question;
import co.in.divi.content.Book;
import co.in.divi.content.DatabaseHelper;
import co.in.divi.content.DiviReference;
import co.in.divi.content.Node;
import co.in.divi.content.Topic;
import co.in.divi.db.model.Attempt;
import co.in.divi.model.ClassMembers.ClassMember;
import co.in.divi.model.DashboardData;
import co.in.divi.model.DashboardData.QuestionScore;
import co.in.divi.model.DashboardData.StudentScore;
import co.in.divi.model.LectureInstruction.Instruction;
import co.in.divi.model.StudentScores;
import co.in.divi.ui.TagsScorecard;
import co.in.divi.ui.TagsScorecard.TagsScorecardModel;
import co.in.divi.util.Config;
import co.in.divi.util.ServerConfig;
import co.in.divi.util.TextUtil;
import co.in.divi.util.Util;
import co.in.divi.vms.common.Challenge;
import co.in.divi.vms.common.ChallengeXmlParser;
import co.in.divi.vms.common.VMChallenges;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request.Method;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.FadeInNetworkImageView;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;

public class DashboardDialog extends Dialog implements DashboardChangeListener, LectureStatusChangeListener {

	private static final String				TAG									= DashboardDialog.class.getSimpleName();

	public static final long				DASHBOARD_PRESENCE_REFRESH_INTERVAL	= 2 * 1000;

	private Context							context;
	private UserSessionProvider				userSessionProvider;
	private LectureSessionProvider			lectureSessionProvider;
	private DatabaseHelper					dbHelper;

	View									dashboard;
	private ProgressBar						pb;
	private TextView						onlineBar, offlineBar;
	private ListView						onlineGrid, offlineGrid, questionsGrid;
	private Switch							questionStudentSwitch;
	private View							close, studentContainer;
	private TagsScorecard					tagsScorecard;

	private StudentBadgeAdapter				onlineAdapter, offlineAdapter;
	private QuestionsAdapter				questionsAdapter;
	private boolean							showingOnline;
	private ArrayList<ClassMember>			onlineMembers, offlineMembers;
	private HashMap<String, LocationHolder>	locations;
	private HashMap<String, Integer>		studentPoints, studentAccuracies, questionPoints, questionAccuracies;
//	private VMChallenges					sharedVMChallenges;
//	private Topic.VM						sharedVMDef;
	private AssessmentFileModel				assessmentModel;
	private Book							assessmentBook;
	private DiviReference					assessmentRef;
	private int								maxPoints;
	private String							assessmentTitle;
	private co.in.divi.model.Instruction	lastAssessmentInstruction, latestInstruction;

	private Timer							timer;
	private Handler							handler;
	private boolean							refreshPending;
	private LoadAssessmentDataTask			loadAssessmentDataTask;
//	private LoadVMDataTask					loadVMDataTask;
	private TagsScorecardModel				tagsScorecardModel;
	private Runnable						refreshUIRunnable					= new Runnable() {
																					@Override
																					public void run() {
																						refreshPending = false;
																						refreshUI();
																					}
																				};

	public DashboardDialog(Context context) {
		super(context);
		this.context = context;
		getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		userSessionProvider = UserSessionProvider.getInstance(context);
		lectureSessionProvider = LectureSessionProvider.getInstance(context);
		dbHelper = DatabaseHelper.getInstance(context);
		// setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
		handler = new Handler();
		timer = new Timer();
		onlineMembers = new ArrayList<ClassMember>();
		offlineMembers = new ArrayList<ClassMember>();
		locations = new HashMap<String, LocationHolder>();
		studentPoints = new HashMap<String, Integer>();
		studentAccuracies = new HashMap<String, Integer>();
		questionPoints = new HashMap<String, Integer>();
		questionAccuracies = new HashMap<String, Integer>();
		lastAssessmentInstruction = null;
		latestInstruction = null;
	}

	public void init() {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		dashboard = inflater.inflate(R.layout.fragment_dashboard, null, false);
		tagsScorecard = (TagsScorecard) dashboard.findViewById(R.id.scorecard);
		setContentView(dashboard);
	}

	@Override
	public void show() {
		super.show();
		lectureSessionProvider.addDashboardListener(this);
		lectureSessionProvider.addListener(this);
		onReceivedNewInstruction();// populate latestInstruction

		close = dashboard.findViewById(R.id.close);
		studentContainer = dashboard.findViewById(R.id.students_container);
		pb = (ProgressBar) dashboard.findViewById(R.id.progressBar);
		onlineBar = (TextView) dashboard.findViewById(R.id.online_bar);
		offlineBar = (TextView) dashboard.findViewById(R.id.offline_bar);
		onlineGrid = (ListView) dashboard.findViewById(R.id.online_grid);
		offlineGrid = (ListView) dashboard.findViewById(R.id.offline_grid);
		questionsGrid = (ListView) dashboard.findViewById(R.id.questions_grid);
		questionStudentSwitch = (Switch) dashboard.findViewById(R.id.student_question_switch);
		close.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				dismiss();
			}
		});
		tagsScorecard.setVisibility(View.GONE);
		questionStudentSwitch.setChecked(false);
		showStudents();
		questionStudentSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					showQuestions();
				} else {
					showStudents();
				}
			}
		});
		dashboard.findViewById(R.id.online_bar_holder).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!showingOnline) {
					showingOnline = true;
					refreshUI();
				}
			}
		});
		dashboard.findViewById(R.id.offline_bar_holder).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (showingOnline) {
					showingOnline = false;
					refreshUI();
				}
			}
		});

		showingOnline = true;
		if (onlineAdapter == null) {
			onlineAdapter = new StudentBadgeAdapter(true);
			onlineGrid.setAdapter(onlineAdapter);
		}
		if (offlineAdapter == null) {
			offlineAdapter = new StudentBadgeAdapter(false);
			offlineGrid.setAdapter(offlineAdapter);
		}
		onlineGrid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				if (assessmentModel != null && assessmentRef != null) {
					if (assessmentBook == null) {
						Toast.makeText(context, "Please ensure you have the content!", Toast.LENGTH_SHORT).show();
						return;
					}
					cancelStudentScorecardRequests();
					ClassMember student = (ClassMember) onlineAdapter.getItem(position);
					tagsScorecardModel = new TagsScorecardModel();
					tagsScorecardModel.userName = student.name;
					tagsScorecardModel.profilePicUrl = student.profilePic;
					tagsScorecardModel.assessmentModel = assessmentModel;
					tagsScorecardModel.book = assessmentBook;
					// fill attempts async.
					fetchStudentScores(student.uid, assessmentRef.courseId, assessmentRef.bookId, assessmentModel.assessmentId);
//				} else if (sharedVMChallenges != null && assessmentRef != null) {
//					Toast.makeText(context, "ToDo", Toast.LENGTH_SHORT).show();
				}
			}
		});
		offlineGrid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				if (assessmentModel != null && assessmentRef != null) {
					if (assessmentBook == null) {
						Toast.makeText(context, "Please ensure you have the content!", Toast.LENGTH_SHORT).show();
						return;
					}
					cancelStudentScorecardRequests();
					ClassMember student = (ClassMember) offlineAdapter.getItem(position);
					tagsScorecardModel = new TagsScorecardModel();
					tagsScorecardModel.userName = student.name;
					tagsScorecardModel.profilePicUrl = student.profilePic;
					tagsScorecardModel.assessmentModel = assessmentModel;
					tagsScorecardModel.book = assessmentBook;
					// fill attempts async.
					fetchStudentScores(student.uid, assessmentRef.courseId, assessmentRef.bookId, assessmentModel.assessmentId);
//				} else if (sharedVMChallenges != null && assessmentRef != null) {
//					Toast.makeText(context, "ToDo", Toast.LENGTH_SHORT).show();
				}
			}
		});
		if (questionsAdapter == null) {
			questionsAdapter = new QuestionsAdapter();
			questionsGrid.setAdapter(questionsAdapter);
			questionsGrid.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
					if (assessmentModel != null && assessmentModel.questions.length > position) {
						String qId = assessmentModel.questions[position].id;
						DiviReference ref = new DiviReference(assessmentRef.courseId, assessmentRef.bookId,
								DiviReference.REFERENCE_TYPE_ASSESSMENT, assessmentRef.itemId, qId);
						dismiss();
						Util.openInstruction(context, ref.getUri());
//					} else if (sharedVMChallenges != null && assessmentRef != null) {
//						Toast.makeText(context, "ToDo", Toast.LENGTH_SHORT).show();
					}
				}
			});
		}

		refreshUI();
		timer.cancel();
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						Log.d(TAG, "fetch dashboard data");
						lastAssessmentInstruction = null;
//						sharedVMDef = null;
//						sharedVMChallenges = null;
						// get the latest quiz from instructions
						if (lectureSessionProvider.getInstructions() == null) {
							Log.d(TAG, "no instructions yet!");
							return;
						}
						Instruction[] instructions = lectureSessionProvider.getInstructions().instructions;
						if (instructions != null) {
							for (Instruction instruction : instructions) {
								try {
									Log.d(TAG, "got instruction:" + instruction.data);
									co.in.divi.model.Instruction i = new Gson().fromJson(instruction.data,
											co.in.divi.model.Instruction.class);
									if (i.location == null)
										continue;
									if (i.type == co.in.divi.model.Instruction.INSTRUCTION_TYPE_NAVIGATE) {
										DiviReference ref = new DiviReference(Uri.parse(i.location));
										if (i.isVM) {
											lastAssessmentInstruction = i;
//											sharedVMChallenges = null;
//											sharedVMDef = null;
											assessmentModel = null;
											// fetch VM model
//											if (loadVMDataTask != null)
//												loadVMDataTask.cancel(false);
//											loadVMDataTask = new LoadVMDataTask();
//											loadVMDataTask.execute(new String[] { ref.courseId, ref.bookId, ref.itemId, ref.subItemId });
											fetchDashboardData(ref.courseId, ref.bookId, ref.itemId + "_" + ref.subItemId);
											return;
										} else {
											if (ref.type == DiviReference.REFERENCE_TYPE_ASSESSMENT) {
												lastAssessmentInstruction = i;
//												sharedVMChallenges = null;
												// fetch questions if we havent
												if (assessmentModel == null || !assessmentModel.assessmentId.equals(ref.itemId)) {
													assessmentModel = null;
													assessmentRef = ref;
													assessmentTitle = "Assessment :" + lastAssessmentInstruction.breadcrumb[2] + " -> "
															+ lastAssessmentInstruction.breadcrumb[3];
													if (loadAssessmentDataTask != null)
														loadAssessmentDataTask.cancel(false);
													loadAssessmentDataTask = new LoadAssessmentDataTask();
													loadAssessmentDataTask.execute(new String[] { ref.courseId, ref.bookId, ref.itemId });
												}
												fetchDashboardData(ref.courseId, ref.bookId, ref.itemId);
												return;
											}
										}
									}
								} catch (Exception e) {
									Log.e(TAG, "error parsing instructions", e);
								}
							}
						}
						Log.d(TAG, "no assessment instruction found");
					}
				});
			}

		}, 0, Config.DASHBOARD_SCORES_REFRESH_INTERVAL);
	}

	private void stop() {
		if (loadAssessmentDataTask != null)
			loadAssessmentDataTask.cancel(false);
//		if (loadVMDataTask != null)
//			loadVMDataTask.cancel(false);
		timer.cancel();
		lectureSessionProvider.removeDashboardListener(this);
		lectureSessionProvider.removeListener(this);
		handler.removeCallbacks(refreshUIRunnable);
		DiviApplication.get().getRequestQueue().cancelAll(this);
		cancelStudentScorecardRequests();
	}

	@Override
	public void dismiss() {
		stop();
		super.dismiss();
	}

	private void showStudents() {
		studentContainer.setVisibility(View.VISIBLE);
		questionsGrid.setVisibility(View.GONE);
		pb.setVisibility(View.GONE);
	}

	private void showQuestions() {
		studentContainer.setVisibility(View.GONE);
		questionsGrid.setVisibility(View.VISIBLE);
		pb.setVisibility(View.GONE);
	}

	private void refreshUI() {
		Log.d(TAG, "herenow:" + lectureSessionProvider.hereNow().size() + ", class:" + lectureSessionProvider.getClassMembers().size());
		onlineMembers.clear();
		offlineMembers.clear();
		locations.clear();
		locations.putAll((lectureSessionProvider.getAllLocations()));
		for (ClassMember member : lectureSessionProvider.getClassMembers()) {
			if (member.role.equalsIgnoreCase(ClassMember.ROLE_TEACHER) || member.role.equalsIgnoreCase(ClassMember.ROLE_TESTER))
				continue;
			if (lectureSessionProvider.hereNow().contains(member.uid)) {
				onlineMembers.add(member);
			} else {
				offlineMembers.add(member);
			}
		}
		Comparator<ClassMember> classMemberComparator = new Comparator<ClassMember>() {

			@Override
			public int compare(ClassMember left, ClassMember right) {
				int l = studentAccuracies.containsKey(left.uid) ? studentAccuracies.get(left.uid) : -1;
				int r = studentAccuracies.containsKey(right.uid) ? studentAccuracies.get(right.uid) : -1;
				return new Integer(l).compareTo(new Integer(r));
			}
		};
		Collections.sort(onlineMembers, classMemberComparator);
		Collections.sort(offlineMembers, classMemberComparator);
		String assessmentText = "";
		if (assessmentModel != null)
			assessmentText = assessmentTitle;
//		else if (sharedVMDef != null)
//			assessmentText = sharedVMDef.title;
		onlineBar.setText("  Online - " + onlineMembers.size());
		offlineBar.setText("  Offline - " + offlineMembers.size());
		if (showingOnline) {
			onlineGrid.setVisibility(View.VISIBLE);
			offlineGrid.setVisibility(View.GONE);
			onlineAdapter.notifyDataSetChanged();
		} else {
			onlineGrid.setVisibility(View.GONE);
			offlineGrid.setVisibility(View.VISIBLE);
			offlineAdapter.notifyDataSetChanged();
		}
		questionsAdapter.notifyDataSetChanged();
	}

	private void cancelStudentScorecardRequests() {
		DiviApplication.get().getRequestQueue().cancelAll(tagsScorecard);
	}

	private class StudentBadgeAdapter extends BaseAdapter {
		boolean			isOnlineGrid;
		LayoutInflater	inflater;

		public StudentBadgeAdapter(boolean isOnlineGrid) {
			this.isOnlineGrid = isOnlineGrid;
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			if (isOnlineGrid) {
				return onlineMembers.size();
			} else {
				return offlineMembers.size();
			}
		}

		@Override
		public Object getItem(int position) {
			if (isOnlineGrid) {
				return onlineMembers.get(position);
			} else {
				return offlineMembers.get(position);
			}
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ClassMember member = (ClassMember) getItem(position);
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.item_dashboard, parent, false);
			}
			TextView name = (TextView) convertView.findViewById(R.id.name);
			TextView loc = (TextView) convertView.findViewById(R.id.location);
			TextView progressText = (TextView) convertView.findViewById(R.id.progress_text);
			View progressbar2 = convertView.findViewById(R.id.progress_progressbar2);
			View blueBar = convertView.findViewById(R.id.blue_bar);
			View blackBar = convertView.findViewById(R.id.black_bar);
			TextView accuracy = (TextView) convertView.findViewById(R.id.accuracy);
			if (assessmentModel != null && studentPoints.containsKey(member.uid)) {
				progressbar2.setVisibility(View.VISIBLE);
				accuracy.setVisibility(View.VISIBLE);
				accuracy.setText(studentAccuracies.get(member.uid) + " %");
				accuracy.setBackgroundResource(TextUtil.getAccuracyBg(studentAccuracies.get(member.uid)));

				progressText.setText("" + studentPoints.get(member.uid) + " / " + maxPoints);
				LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
				LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);

				lp1.weight = studentPoints.get(member.uid) * 1.0f / maxPoints;
				lp2.weight = 1.0f - lp1.weight;
				blueBar.setLayoutParams(lp1);
				blackBar.setLayoutParams(lp2);
                /*
			} else if (sharedVMDef != null && studentPoints.containsKey(member.uid)) {
				progressbar2.setVisibility(View.VISIBLE);
				accuracy.setVisibility(View.VISIBLE);
				accuracy.setText(studentAccuracies.get(member.uid) + " %");
				accuracy.setBackgroundResource(TextUtil.getAccuracyBg(studentAccuracies.get(member.uid)));

				progressText.setText("" + studentPoints.get(member.uid) + " / " + maxPoints);
				LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
				LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);

				lp1.weight = studentPoints.get(member.uid) * 1.0f / maxPoints;
				lp2.weight = 1.0f - lp1.weight;
				blueBar.setLayoutParams(lp1);
				blackBar.setLayoutParams(lp2);
				*/
			} else {
				progressbar2.setVisibility(View.INVISIBLE);
				accuracy.setVisibility(View.INVISIBLE);
			}
			name.setText(member.name);
			FadeInNetworkImageView profilePic = (FadeInNetworkImageView) convertView.findViewById(R.id.profile_pic);
			profilePic.setDefaultImageResId(R.drawable.ic_profile);
			profilePic.setErrorImageResId(R.drawable.ic_profile);
			profilePic.setImageUrl(null, DiviApplication.get().getImageLoader());
			if (member.profilePic != null) {
				Uri picUri = Uri.parse(member.profilePic);
				if (picUri != null && picUri.getHost() != null)
					profilePic.setImageUrl(member.profilePic, DiviApplication.get().getImageLoader());
			}

			if (locations.get(member.uid) != null) {
				loc.setText(TextUtil.getLocationText(latestInstruction, locations.get(member.uid)));
			} else {
				loc.setText(" --- ");
			}
			return convertView;
		}
	}

	private class QuestionsAdapter extends BaseAdapter {
		LayoutInflater	inflater;

		public QuestionsAdapter() {
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {

			if (assessmentModel != null)
				return assessmentModel.questions.length;
//			if (sharedVMDef != null && sharedVMChallenges != null)
//				return sharedVMChallenges.challenges.length;
			return 0;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.item_dashboard, parent, false);
			}
			TextView name = (TextView) convertView.findViewById(R.id.name);
			TextView loc = (TextView) convertView.findViewById(R.id.location);
			TextView progressText = (TextView) convertView.findViewById(R.id.progress_text);
			View progressbar2 = convertView.findViewById(R.id.progress_progressbar2);
			View blueBar = convertView.findViewById(R.id.blue_bar);
			View blackBar = convertView.findViewById(R.id.black_bar);

			TextView accuracy = (TextView) convertView.findViewById(R.id.accuracy);
			if (assessmentModel != null) {
				String qId = assessmentModel.questions[position].id;
				name.setText("  Q  " + (1 + position));
				loc.setText(assessmentModel.questions[position].text);
				if (questionPoints.containsKey(qId)) {
					progressbar2.setVisibility(View.VISIBLE);
					accuracy.setVisibility(View.VISIBLE);
					accuracy.setText(questionAccuracies.get(qId) + " %");
					accuracy.setBackgroundResource(TextUtil.getAccuracyBg(questionAccuracies.get(qId)));

					progressText.setText(""
							+ ((questionPoints.get(qId) * 100) / (maxPoints * (onlineMembers.size() + offlineMembers.size()))) + " %");
					LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
					LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);

					lp1.weight = ((questionPoints.get(qId) * 1.0f) / maxPoints);// studentPoints.get(member.uid) * 1.0f
																				// /
																				// maxPoints;
					lp2.weight = 1.0f - lp1.weight;
					blueBar.setLayoutParams(lp1);
					blackBar.setLayoutParams(lp2);
				} else {
					progressbar2.setVisibility(View.INVISIBLE);
					accuracy.setVisibility(View.INVISIBLE);
				}
                /*
			} else if (sharedVMChallenges != null) {
				String challengeId = sharedVMChallenges.challenges[position].id;
				name.setText(sharedVMChallenges.challenges[position].title);
				loc.setText(sharedVMChallenges.challenges[position].description);
				*/
			}
			convertView.findViewById(R.id.profile_pic).setVisibility(View.GONE);

			return convertView;
		}
	}

	@Override
	public void onDashboardChange() {
		if (!refreshPending) {
			refreshPending = true;
			handler.postDelayed(refreshUIRunnable, DASHBOARD_PRESENCE_REFRESH_INTERVAL);
		}
	}

	@Override
	public void onLectureJoinLeave() {
		dismiss();
	}

	@Override
	public void onConnectionStatusChange() {
	}

	@Override
	public void onReceivedNewInstruction() {
		if (lectureSessionProvider.getInstructions() != null && lectureSessionProvider.getInstructions().instructions.length > 0)
			latestInstruction = new Gson().fromJson(lectureSessionProvider.getInstructions().instructions[0].data,
					co.in.divi.model.Instruction.class);
	}

	private void fetchDashboardData(String courseId, String bookId, String assessmentId) {
		try {
			JSONObject jsonRequest = new JSONObject();

			jsonRequest.put("uid", userSessionProvider.getUserData().uid);
			jsonRequest.put("token", userSessionProvider.getUserData().token);
			jsonRequest.put("classId", lectureSessionProvider.getCurrentLecture().classRoomId);
			jsonRequest.put("courseId", courseId);
			jsonRequest.put("bookId", bookId);
			jsonRequest.put("assessmentId", assessmentId);

			Log.d(TAG, "fetching dashboard" + jsonRequest.toString());
			String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_GETDASHBOARDDATA;
			/****** Temp ******/
			// url = "http://divi-service.herokuapp.com/temp/dashboard.json";
			/****** End Temp ******/
			JsonObjectRequest requestDashboardData = new JsonObjectRequest(Method.POST, url, jsonRequest, new Listener<JSONObject>() {
				@Override
				public void onResponse(JSONObject response) {
					Log.d(TAG, "got response:\n" + response.toString());
					// validate response
					DashboardData data = new Gson().fromJson(response.toString(), DashboardData.class);
					studentAccuracies.clear();
					studentPoints.clear();
					for (StudentScore score : data.scoresByStudent) {
						studentPoints.put(score.userId, score.points);
						studentAccuracies.put(score.userId, score.accuracy);
					}
					questionPoints.clear();
					questionAccuracies.clear();
					for (QuestionScore score : data.scoresByQuestion) {
						questionPoints.put(score.questionId, score.points);
						questionAccuracies.put(score.questionId, score.accuracy);
					}
					refreshUI();
				}
			}, new ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					Log.w(TAG, "error:" + error);
					Toast.makeText(context, "Error getting dashboard.", Toast.LENGTH_LONG).show();
				}
			});
			requestDashboardData.setShouldCache(false);
			requestDashboardData.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
					DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
			DiviApplication.get().getRequestQueue().add(requestDashboardData).setTag(this);
		} catch (Exception e) {
			Log.e(TAG, "Error sending instruction", e);
			Toast.makeText(context, "Error in dashboard", Toast.LENGTH_LONG).show();
		}
	}

	class LoadAssessmentDataTask extends AsyncTask<String, Void, Integer> {

		Node	assessmentNode;
		Book	book	= null;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Integer doInBackground(String... params) {
			try {
				String courseId = params[0];
				String bookId = params[1];
				String assessmentId = params[2];
				assessmentNode = dbHelper.getNode(assessmentId, bookId, courseId);
				ArrayList<Book> allBooks = dbHelper.getBooks(courseId);
				for (Book b : allBooks)
					if (b.id.equals(bookId))
						book = b;
			} catch (Exception e) {
				Log.e(TAG, "error loading assessments", e);
				return 1;
			}
			if (assessmentNode == null)
				return 1;
			return 0;
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result == 0) {
				assessmentBook = book;
				assessmentModel = (AssessmentFileModel) assessmentNode.tag;
				assessmentModel.assessmentId = assessmentNode.id;
				maxPoints = 0;
				for (Question q : assessmentModel.questions) {
					maxPoints += q.points;
				}
			} else {
				Toast.makeText(context, "Error loading the assessment.", Toast.LENGTH_LONG).show();
			}
		}
	}
/*
	class LoadVMDataTask extends AsyncTask<String, Void, Integer> {

		VMChallenges	vmc;
		Topic.VM		vmDef;
		Book			book	= null;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Integer doInBackground(String... params) {
			try {
				String courseId = params[0];
				String bookId = params[1];
				String topicId = params[2];
				String vmContentId = params[3];
				Node vmTopicNode = dbHelper.getNode(topicId, bookId, courseId);
				ArrayList<Book> allBooks = dbHelper.getBooks(courseId);
				for (Book b : allBooks)
					if (b.id.equals(bookId))
						book = b;
				for (Topic.VM vmDef : ((Topic) vmTopicNode.tag).vms) {
					if (vmDef.id.equals(vmContentId)) {
						this.vmDef = vmDef;
					}
				}
				// load challenges
				Resources vmRes = getContext().getPackageManager().getResourcesForApplication(vmDef.appPackage);
				vmc = new ChallengeXmlParser().getVMsFromXml(vmRes.getXml(vmRes.getIdentifier("challenges", "xml", vmDef.appPackage))).get(
						vmDef.appActivityName);
			} catch (Exception e) {
				Log.e(TAG, "error loading assessments", e);
				return 1;
			}
			if (vmDef == null)
				return 1;
			return 0;
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result == 0) {
				assessmentBook = book;
				sharedVMDef = vmDef;
				sharedVMChallenges = vmc;
				maxPoints = 0;
				for (Challenge c : vmc.challenges) {
					maxPoints += c.points;
				}
			} else {
				Toast.makeText(context, "Error loading the vm details.", Toast.LENGTH_LONG).show();
			}
		}
	}
*/
	private void fetchStudentScores(String studentId, String courseId, String bookId, String assessmentId) {
		try {
			JSONObject jsonRequest = new JSONObject();

			jsonRequest.put("uid", userSessionProvider.getUserData().uid);
			jsonRequest.put("token", userSessionProvider.getUserData().token);
			jsonRequest.put("classId", studentId);
			jsonRequest.put("courseId", courseId);
			jsonRequest.put("bookId", bookId);
			jsonRequest.put("assessmentId", assessmentId);
			jsonRequest.put("studentId", studentId);

			Log.d(TAG, "fetching dashboard" + jsonRequest.toString());
			String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_GETSCORES;
			JsonObjectRequest requestDashboardData = new JsonObjectRequest(Method.POST, url, jsonRequest, new Listener<JSONObject>() {
				@Override
				public void onResponse(JSONObject response) {
					Log.d(TAG, "got response:\n" + response.toString());
					// validate response
					StudentScores data = new Gson().fromJson(response.toString(), StudentScores.class);
					tagsScorecardModel.attempts = new HashMap<String, Attempt>();
					for (Attempt a : data.attempts) {
						tagsScorecardModel.attempts.put(a.questionId, a);
					}

					tagsScorecard.setVisibility(View.VISIBLE);
					tagsScorecard.setData(tagsScorecardModel, new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							tagsScorecard.setVisibility(View.GONE);
						}
					});
					pb.setVisibility(View.GONE);
				}
			}, new ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					Log.w(TAG, "error:" + error);
					Toast.makeText(context, "Error getting student scores.", Toast.LENGTH_LONG).show();
					pb.setVisibility(View.GONE);
				}
			});
			pb.setVisibility(View.VISIBLE);
			requestDashboardData.setShouldCache(false);
			requestDashboardData.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
					DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
			DiviApplication.get().getRequestQueue().add(requestDashboardData).setTag(tagsScorecard);
		} catch (Exception e) {
			Log.e(TAG, "Error fetching student scores", e);
			Toast.makeText(context, "Error in dashboard", Toast.LENGTH_LONG).show();
		}
	}
}
