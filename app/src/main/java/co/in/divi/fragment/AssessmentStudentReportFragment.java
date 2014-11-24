package co.in.divi.fragment;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.content.AssessmentFileModel;
import co.in.divi.progress.AssessmentSummary;
import co.in.divi.ui.TagsScorecard;

@SuppressLint("ValidFragment")
public class AssessmentStudentReportFragment extends BaseDialogFragment {
	private static final String	TAG	= AssessmentStudentReportFragment.class.getSimpleName();

	private TagsScorecard		tagsScorecard;
	private ViewGroup			root;

	private AssessmentSummary	summary;
	private UserSessionProvider	userSessionProvider;

	public AssessmentStudentReportFragment(AssessmentSummary summary) {
		this.summary = summary;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		root = (ViewGroup) inflater.inflate(R.layout.fragment_assessment_student_report, container, false);
		tagsScorecard = (TagsScorecard) root.findViewById(R.id.scorecard);
		root.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				dismiss();
			}
		});
		return root;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (summary == null) {
			dismissAllowingStateLoss();
			return;
		}
		userSessionProvider = UserSessionProvider.getInstance(getActivity());

		TagsScorecard.TagsScorecardModel model = new TagsScorecard.TagsScorecardModel();
		model.book = summary.book;
		model.assessmentModel = ((AssessmentFileModel) summary.assessmentNode.getTag());
		model.attempts = summary.attempts;
		model.chapterName = summary.assessmentNode.parentName;
		if (userSessionProvider.hasUserData()) {
			model.userName = userSessionProvider.getUserData().name;
			model.profilePicUrl = userSessionProvider.getUserData().profilePic;
		}

		Log.d(TAG, "onStart1:" + System.currentTimeMillis());
		tagsScorecard.setData(model, null);
		Log.d(TAG, "onStart2:" + System.currentTimeMillis());
	}

	@Override
	public void onPause() {
		super.onPause();
		dismiss();
	}
}
