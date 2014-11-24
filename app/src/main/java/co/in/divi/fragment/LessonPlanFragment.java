package co.in.divi.fragment;

import java.io.File;

import android.app.DialogFragment;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import co.in.divi.R;
import co.in.divi.activity.LearnActivity;
import co.in.divi.content.DiviReference;
import co.in.divi.content.LessonPlan;
import co.in.divi.content.Node;
import co.in.divi.util.Util;

import com.google.gson.Gson;

public class LessonPlanFragment extends BaseDialogFragment {
	private static final String	TAG	= LessonPlanFragment.class.getSimpleName();

	LearnActivity				learnActivity;
	LinearLayout				content;

	LoadLessonPlanTask			loadLessonPlanTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View planView = inflater.inflate(R.layout.fragment_lessonplan, container, false);
		return planView;
	}

	@Override
	public void onStart() {
		super.onStart();
		learnActivity = (LearnActivity) getActivity();
		content = (LinearLayout) getView().findViewById(R.id.content);
		View close = getView().findViewById(R.id.close);
		close.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				dismiss();
			}
		});

		if (loadLessonPlanTask != null)
			loadLessonPlanTask.cancel(false);
		loadLessonPlanTask = new LoadLessonPlanTask();
		loadLessonPlanTask.execute(new Void[0]);
	}

	class LoadLessonPlanTask extends AsyncTask<Void, Void, Integer> {

		LessonPlan	lp;
		String		chapName;

		Node		dispTopic;
		File		chapPath;

		@Override
		protected void onPreExecute() {
			dispTopic = learnActivity.getDisplayedTopic();
			if (dispTopic == null || dispTopic.getParent() == null) {
				cancel(false);
				return;
			}
			chapPath = new File(learnActivity.bookBaseDir, dispTopic.getParent().id);
			chapName = dispTopic.getParent().name;
		}

		@Override
		protected Integer doInBackground(Void... params) {
			File lessonPlanFile = new File(chapPath, "lessonplan.json");
			if (!lessonPlanFile.exists())
				return 2;
			lp = new Gson().fromJson(Util.openJSONFile(lessonPlanFile), LessonPlan.class);
			if (lp != null)
				return 0;
			return 1;
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result == 0) {
				// build UI
				LayoutParams commonParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				content.removeAllViews();
				TextView title = new TextView(learnActivity);
				title.setGravity(Gravity.CENTER);
				title.setText(chapName);
				title.setPadding(0, 20, 0, 20);
				title.setTextAppearance(learnActivity, R.style.LP_Chapter);
				content.addView(title, commonParams);

				if (lp.modules != null) {
					for (int i = 0; i < lp.modules.length; i++) {
						TextView topics = new TextView(learnActivity);
						topics.setTextAppearance(learnActivity, R.style.LP_Module);
						topics.setText(Html.fromHtml("<b>Module  " + (i + 1) + ":</b><br/><br/>" + "<u>Topics covered:</u>  "
								+ lp.modules[i].topics + "<br/><u>Time:</u>  " + lp.modules[i].time));
						topics.setPadding(30, 10, 10, 10);
						content.addView(topics, commonParams);
						if (lp.modules[i].instructions != null) {
							for (int j = 0; j < lp.modules[i].instructions.length; j++) {
								TextView instr = new TextView(learnActivity);
								instr.setPadding(60, 10, 10, 10);
								instr.setTextAppearance(learnActivity, R.style.LP_Instruction);
								instr.setText("" + (j + 1) + ".  " + lp.modules[i].instructions[j].text);
								content.addView(instr, commonParams);
								if (lp.modules[i].instructions[j].resources != null) {
									for (int k = 0; k < lp.modules[i].instructions[j].resources.length; k++) {
										Button resButton = (Button) learnActivity.getLayoutInflater().inflate(
												R.layout.item_lp_resourcebutton, null);
										resButton.setText(lp.modules[i].instructions[j].resources[k].text);
										String[] parts = lp.modules[i].instructions[j].resources[k].src.split("/");
										String itemId = parts[0];
										String subItemId = null;
										if (parts.length > 1) {
											subItemId = parts[1];
										}
										final Uri uriToLaunch = new DiviReference(learnActivity.getDisplayedTopic().courseId,
												learnActivity.getDisplayedTopic().bookId, DiviReference.REFERENCE_TYPE_TOPIC, itemId,
												subItemId).getUri();
										resButton.setOnClickListener(new View.OnClickListener() {
											@Override
											public void onClick(View v) {
												dismiss();
												Util.openInstruction(learnActivity, uriToLaunch);
											}
										});
										content.addView(resButton);
									}
								}
							}
						}
						if (lp.modules[i].quizzes != null) {
							for (int j = 0; j < lp.modules[i].quizzes.length; j++) {
								Button quizButton = (Button) learnActivity.getLayoutInflater().inflate(R.layout.item_lp_resourcebutton,
										null);
								quizButton.setText(lp.modules[i].quizzes[j].text);
								String[] parts = lp.modules[i].quizzes[j].src.split("/");
								String itemId = parts[0];
								String subItemId = null;
								if (parts.length > 1) {
									subItemId = parts[1];
								}
								final Uri uriToLaunch = new DiviReference(learnActivity.getDisplayedTopic().courseId,
										learnActivity.getDisplayedTopic().bookId, DiviReference.REFERENCE_TYPE_ASSESSMENT, itemId,
										subItemId).getUri();
								quizButton.setOnClickListener(new View.OnClickListener() {
									@Override
									public void onClick(View v) {
										dismiss();
										Util.openInstruction(learnActivity, uriToLaunch);
									}
								});
								content.addView(quizButton);
							}
						}
					}
				}

			} else {
				Toast.makeText(learnActivity, "Lesson plan couldn't be loaded.", Toast.LENGTH_SHORT).show();
			}
		}
	}
}
