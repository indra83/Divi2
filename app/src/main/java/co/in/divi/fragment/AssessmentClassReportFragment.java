package co.in.divi.fragment;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request.Method;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import co.in.divi.DiviApplication;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.content.AssessmentFileModel;
import co.in.divi.content.AssessmentFileModel.Question;
import co.in.divi.content.Book;
import co.in.divi.content.DatabaseHelper;
import co.in.divi.content.DiviReference;
import co.in.divi.content.Node;
import co.in.divi.db.model.Attempt;
import co.in.divi.model.ClassMembers;
import co.in.divi.model.ClassMembers.ClassMember;
import co.in.divi.model.DashboardData;
import co.in.divi.model.DashboardData.QuestionScore;
import co.in.divi.model.DashboardData.StudentScore;
import co.in.divi.model.StudentScores;
import co.in.divi.ui.TagsScorecard;
import co.in.divi.ui.TagsScorecard.TagsScorecardModel;
import co.in.divi.util.ServerConfig;
import co.in.divi.util.TextUtil;
import co.in.divi.util.Util;
import co.in.divi.util.image.FadeInNetworkImageView;

@SuppressLint("ValidFragment")
public class AssessmentClassReportFragment extends BaseDialogFragment {
	private static final String		TAG	= AssessmentClassReportFragment.class.getSimpleName();

	private UserSessionProvider		userSessionProvider;
	private DatabaseHelper			dbHelper;

	private ProgressBar				pb;
	private ListView				studentsGrid, questionsGrid;
	private Switch					questionStudentSwitch;
	private View					close;
	private TagsScorecard			tagsScorecard;

	private StudentBadgeAdapter		studentAdapter;
	private QuestionsAdapter		questionsAdapter;
	private ArrayList<ClassMember>	students;
	private HashMap<String, Integer>	studentPoints, studentAccuracies, questionPoints, questionAccuracies;

	// given
	private String						classId;
	private DiviReference				assessmentRef;
	private String						chapterName;

	// computed
	private AssessmentFileModel			assessmentModel;
	private Book						assessmentBook;
	int									maxPoints;

	private LoadDataTask				loadDataTask;
	private TagsScorecardModel			tagsScorecardModel;

	public AssessmentClassReportFragment() {
	}

	public AssessmentClassReportFragment(String classId, DiviReference assessmentRef, String chapterName) {
		this.classId = classId;
		this.assessmentRef = assessmentRef;
		this.chapterName = chapterName;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
		userSessionProvider = UserSessionProvider.getInstance(getActivity());
		dbHelper = DatabaseHelper.getInstance(getActivity());
		students = new ArrayList<ClassMember>();
		studentPoints = new HashMap<String, Integer>();
		studentAccuracies = new HashMap<String, Integer>();
		questionPoints = new HashMap<String, Integer>();
		questionAccuracies = new HashMap<String, Integer>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View dashboard = inflater.inflate(R.layout.fragment_classscorecard, container, false);
		dashboard.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				dismiss();
			}
		});
		tagsScorecard = (TagsScorecard) dashboard.findViewById(R.id.scorecard);
		return dashboard;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (classId == null || assessmentRef == null) {
			dismissAllowingStateLoss();
			return;
		}
		close = getView().findViewById(R.id.close);
		pb = (ProgressBar) getView().findViewById(R.id.progressBar);
		studentsGrid = (ListView) getView().findViewById(R.id.students_grid);
		questionsGrid = (ListView) getView().findViewById(R.id.questions_grid);
		questionStudentSwitch = (Switch) getView().findViewById(R.id.student_question_switch);
		close.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				dismiss();
			}
		});
		tagsScorecard.setVisibility(View.GONE);
		questionStudentSwitch.setChecked(false);
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
		studentAdapter = new StudentBadgeAdapter();
		studentsGrid.setAdapter(studentAdapter);
		studentsGrid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				if (assessmentModel != null && assessmentRef != null) {
					if (assessmentBook == null) {
						Toast.makeText(getActivity(), "Please ensure you have the content!", Toast.LENGTH_SHORT).show();
						return;
					}
					cancelStudentScorecardRequests();
					ClassMember student = (ClassMember) studentAdapter.getItem(position);
					tagsScorecardModel = new TagsScorecardModel();
					tagsScorecardModel.userName = student.name;
					tagsScorecardModel.profilePicUrl = student.profilePic;
					tagsScorecardModel.assessmentModel = assessmentModel;
					tagsScorecardModel.book = assessmentBook;
					// fill attempts async.
					fetchStudentScores(student.uid, assessmentRef.courseId, assessmentRef.bookId, assessmentModel.assessmentId);
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
						Util.openInstruction(getActivity(), ref.getUri());
					}
				}
			});
		}

		if (loadDataTask != null)
			loadDataTask.cancel(false);
		loadDataTask = new LoadDataTask();
		loadDataTask.execute(new String[] { assessmentRef.courseId, assessmentRef.bookId, assessmentRef.itemId });

		showStudents();
	}

	@Override
	public void onStop() {
		super.onStop();
		if (loadDataTask != null)
			loadDataTask.cancel(false);
		DiviApplication.get().getRequestQueue().cancelAll(this);
		cancelStudentScorecardRequests();
	}

	private void showStudents() {
		studentsGrid.setVisibility(View.VISIBLE);
		questionsGrid.setVisibility(View.GONE);
		pb.setVisibility(View.GONE);
	}

	private void showQuestions() {
		studentsGrid.setVisibility(View.GONE);
		questionsGrid.setVisibility(View.VISIBLE);
		pb.setVisibility(View.GONE);
	}

	private void refreshUI() {
		Comparator<ClassMember> classMemberComparator = new Comparator<ClassMember>() {
			@Override
			public int compare(ClassMember left, ClassMember right) {
				int l = studentAccuracies.containsKey(left.uid) ? studentAccuracies.get(left.uid) : -1;
				int r = studentAccuracies.containsKey(right.uid) ? studentAccuracies.get(right.uid) : -1;
				return new Integer(l).compareTo(new Integer(r));
			}
		};
		Collections.sort(students, classMemberComparator);
		studentAdapter.notifyDataSetChanged();
		questionsAdapter.notifyDataSetChanged();
	}

	private void cancelStudentScorecardRequests() {
		DiviApplication.get().getRequestQueue().cancelAll(tagsScorecard);
	}

	private class StudentBadgeAdapter extends BaseAdapter {
		LayoutInflater	inflater;

		public StudentBadgeAdapter() {
			inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return students.size();
		}

		@Override
		public Object getItem(int position) {
			return students.get(position);
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

			return convertView;
		}
	}

	private class QuestionsAdapter extends BaseAdapter {
		LayoutInflater	inflater;

		public QuestionsAdapter() {
			inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			if (assessmentModel == null)
				return 0;
			else
				return assessmentModel.questions.length;
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

			String qId = assessmentModel.questions[position].id;
			name.setText("  Q  " + (1 + position));
			loc.setText(assessmentModel.questions[position].text);
			if (assessmentModel != null && questionPoints.containsKey(qId)) {
				progressbar2.setVisibility(View.VISIBLE);
				accuracy.setVisibility(View.VISIBLE);
				accuracy.setText(questionAccuracies.get(qId) + " %");
				accuracy.setBackgroundResource(TextUtil.getAccuracyBg(questionAccuracies.get(qId)));

				progressText.setText("" + ((questionPoints.get(qId) * 100) / (maxPoints * (students.size()))) + " %");
				LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
				LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);

				lp1.weight = ((questionPoints.get(qId) * 1.0f) / maxPoints);// studentPoints.get(member.uid) * 1.0f /
																			// maxPoints;
				lp2.weight = 1.0f - lp1.weight;
				blueBar.setLayoutParams(lp1);
				blackBar.setLayoutParams(lp2);
			} else {
				progressbar2.setVisibility(View.INVISIBLE);
				accuracy.setVisibility(View.INVISIBLE);
			}
			convertView.findViewById(R.id.profile_pic).setVisibility(View.GONE);

			return convertView;
		}
	}

	class LoadDataTask extends AsyncTask<String, Void, Integer> {

		Node	assessmentNode;
		Book	book	= null;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pb.setVisibility(View.VISIBLE);
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
				fetchClassMembers(classId);
			} else {
				Toast.makeText(getActivity(), "Error loading the assessment. Content not up to date?", Toast.LENGTH_LONG).show();
				dismiss();
			}
		}
	}

	private void fetchClassMembers(String classId) {
		try {
			JSONObject jsonRequest = new JSONObject();

			jsonRequest.put("uid", userSessionProvider.getUserData().uid);
			jsonRequest.put("token", userSessionProvider.getUserData().token);
			jsonRequest.put("classRoomId", classId);

			Log.d(TAG, "fetching classId - " + jsonRequest.toString());
			String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_GETCLASSMEMBERS;
			JsonObjectRequest requestClassMembersData = new JsonObjectRequest(Method.POST, url, jsonRequest, new Listener<JSONObject>() {
				@Override
				public void onResponse(JSONObject response) {
					Log.d(TAG, "got response:\n" + response.toString());
					// validate response
					Gson gson = new Gson();
					ClassMembers classMembers = gson.fromJson(response.toString(), ClassMembers.class);
					pb.setVisibility(View.GONE);
					if (classMembers.members == null || classMembers.members.length == 0) {
						Toast.makeText(getActivity(), "No members found for class..", Toast.LENGTH_SHORT).show();
						dismiss();
					} else {
						students.clear();
						for (ClassMember cm : classMembers.members)
							students.add(cm);
						fetchDashboardData(assessmentRef.courseId, assessmentRef.bookId, assessmentRef.itemId);
					}
				}
			}, new ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					Log.w(TAG, "error:" + error);
					Toast.makeText(getActivity(), "Error getting student scores.", Toast.LENGTH_LONG).show();
					dismiss();
				}
			});
			pb.setVisibility(View.VISIBLE);
			requestClassMembersData.setShouldCache(false);
			requestClassMembersData.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
					DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
			DiviApplication.get().getRequestQueue().add(requestClassMembersData).setTag(this);
		} catch (Exception e) {
			Log.e(TAG, "Error fetching student scores", e);
			Toast.makeText(getActivity(), "Error in dashboard", Toast.LENGTH_LONG).show();
		}
	}

	private void fetchDashboardData(String courseId, String bookId, String assessmentId) {
		try {
			JSONObject jsonRequest = new JSONObject();

			jsonRequest.put("uid", userSessionProvider.getUserData().uid);
			jsonRequest.put("token", userSessionProvider.getUserData().token);
			jsonRequest.put("classId", classId);
			jsonRequest.put("courseId", courseId);
			jsonRequest.put("bookId", bookId);
			jsonRequest.put("assessmentId", assessmentId);

			Log.d(TAG, "fetching class scores" + jsonRequest.toString());
			String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_GETDASHBOARDDATA;
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
					pb.setVisibility(View.GONE);
					refreshUI();
				}
			}, new ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					Log.w(TAG, "error:" + error);
					Toast.makeText(getActivity(), "Error getting dashboard.", Toast.LENGTH_LONG).show();
				}
			});
			pb.setVisibility(View.VISIBLE);
			requestDashboardData.setShouldCache(false);
			requestDashboardData.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
					DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
			DiviApplication.get().getRequestQueue().add(requestDashboardData).setTag(this);
		} catch (Exception e) {
			Log.e(TAG, "Error sending instruction", e);
			Toast.makeText(getActivity(), "Error in dashboard", Toast.LENGTH_LONG).show();
		}
	}

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
					Toast.makeText(getActivity(), "Error getting student scores.", Toast.LENGTH_LONG).show();
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
			Toast.makeText(getActivity(), "Error in dashboard", Toast.LENGTH_LONG).show();
		}
	}
}
