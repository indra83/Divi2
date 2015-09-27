package co.in.divi.ui;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
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

import java.util.HashMap;

import co.in.divi.DiviApplication;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.content.AssessmentFileModel;
import co.in.divi.content.DatabaseHelper;
import co.in.divi.content.DiviReference;
import co.in.divi.db.model.Attempt;
import co.in.divi.fragment.DashboardDialogFragment;
import co.in.divi.model.ClassMembers.ClassMember;
import co.in.divi.model.StudentScores;
import co.in.divi.util.ServerConfig;
import co.in.divi.util.Util;
import co.in.divi.util.image.FadeInNetworkImageView;

public class StudentScorecard extends LinearLayout {
	private static final String			TAG	= StudentScorecard.class.getSimpleName();

	DashboardDialogFragment				dashboardFragment;
	private Button						backButton;
	private TextView					studentNameText;
	private ListView					questionsList;
	private FadeInNetworkImageView		profilePic;

	private QuestionsAdapter			questionsAdapter;
	private AssessmentFileModel			assessmentModel;
	private String						courseId, bookId, studentId;
	private DatabaseHelper				dbHelper;
	private UserSessionProvider			userSessionProvider;

	private HashMap<String, Attempt>	attempts;

	public StudentScorecard(Context context, AttributeSet attrs) {
		super(context, attrs);
		attempts = new HashMap<String, Attempt>();
	}

	public void init(DashboardDialogFragment dashboard) {
		this.dashboardFragment = dashboard;
		studentNameText = (TextView) findViewById(R.id.student_name);
		questionsList = (ListView) findViewById(R.id.questions_grid);
		profilePic = (FadeInNetworkImageView) findViewById(R.id.profile_pic);
		backButton = (Button) findViewById(R.id.back);

		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				close();
			}
		});

		userSessionProvider = UserSessionProvider.getInstance(getContext());
		dbHelper = DatabaseHelper.getInstance(getContext());
		questionsAdapter = new QuestionsAdapter();
		questionsList.setAdapter(questionsAdapter);
	}

	public void showScores(ClassMember student, String courseId, String bookId, AssessmentFileModel assessment) {
		this.courseId = courseId;
		this.bookId = bookId;
		this.assessmentModel = assessment;
		studentNameText.setText(student.name);
		if (student.profilePic != null) {
			Uri picUri = Uri.parse(student.profilePic);
			if (picUri != null && picUri.getHost() != null)
				profilePic.setImageUrl(student.profilePic, DiviApplication.get().getImageLoader());
		}
		questionsList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				if (assessmentModel != null && assessmentModel.questions.length > position) {
					String qId = assessmentModel.questions[position].id;
					DiviReference ref = new DiviReference(StudentScorecard.this.courseId, StudentScorecard.this.bookId,
							DiviReference.REFERENCE_TYPE_ASSESSMENT, assessmentModel.assessmentId, qId);
					dashboardFragment.dismiss();
					Util.openInstruction(dashboardFragment.getActivity(), ref.getUri());
				}
			}
		});
		questionsAdapter.notifyDataSetChanged();
		fetchScores(student.uid, courseId, bookId, assessmentModel.assessmentId);
		setVisibility(View.VISIBLE);
	}

	public void close() {
		setVisibility(View.GONE);
		DiviApplication.get().getRequestQueue().cancelAll(this);
	}

	private class QuestionsAdapter extends BaseAdapter {
		LayoutInflater	inflater;

		public QuestionsAdapter() {
			inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
				convertView = inflater.inflate(R.layout.item_question, parent, false);
			}
			TextView title = (TextView) convertView.findViewById(R.id.question_title);
			ImageView status = (ImageView) convertView.findViewById(R.id.question_status);

			if (attempts.containsKey(assessmentModel.questions[position].id)) {
				status.setVisibility(View.VISIBLE);
				if (attempts.get(assessmentModel.questions[position].id).isSolved()) {
					status.setImageResource(R.drawable.green_circle);
				} else {
					status.setImageResource(R.drawable.yellow_circle);
				}
			} else {
				status.setVisibility(View.GONE);
			}

			title.setText("  Q " + (position + 1));

			return convertView;
		}
	}

	private void fetchScores(String studentId, String courseId, String bookId, String assessmentId) {
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
					attempts.clear();
					for (Attempt a : data.attempts) {
						attempts.put(a.questionId, a);
					}
					questionsAdapter.notifyDataSetChanged();
				}
			}, new ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					Log.w(TAG, "error:" + error);
					Toast.makeText(getContext(), "Error getting student scores.", Toast.LENGTH_LONG).show();
				}
			});
			requestDashboardData.setShouldCache(false);
			requestDashboardData.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
					DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
			DiviApplication.get().getRequestQueue().add(requestDashboardData).setTag(this);
		} catch (Exception e) {
			Log.e(TAG, "Error sending instruction", e);
			Toast.makeText(getContext(), "Error in dashboard", Toast.LENGTH_LONG).show();
		}
	}
}
