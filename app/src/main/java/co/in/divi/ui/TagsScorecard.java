package co.in.divi.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

import co.in.divi.DiviApplication;
import co.in.divi.R;
import co.in.divi.content.AssessmentFileModel;
import co.in.divi.content.AssessmentFileModel.Question;
import co.in.divi.content.Book;
import co.in.divi.content.DiviReference;
import co.in.divi.content.TagModel;
import co.in.divi.db.model.Attempt;
import co.in.divi.fragment.questions.QuestionFragmentFactory;
import co.in.divi.util.TextUtil;
import co.in.divi.util.Util;
import co.in.divi.util.image.FadeInNetworkImageView;

public class TagsScorecard extends LinearLayout implements View.OnClickListener {
	private static final String								TAG				= TagsScorecard.class.getSimpleName();

	private LinearLayout									tagsContainer, topContainer;
	private View											summaryContainer;
	private ListView										questionsList;

	private TagsScorecardModel								model;

	private QuestionsAdapter								questionsAdapter;

	// data models
	private HashMap<String, AssessmentFileModel.Question>	questions;
	private HashMap<String, ArrayList<String>>				questionsByTag;

	private String											selectedTagId	= null;

	public TagsScorecard(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		tagsContainer = (LinearLayout) findViewById(R.id.tagsContainer);
		topContainer = (LinearLayout) findViewById(R.id.topContainer);
		questionsList = (ListView) findViewById(R.id.questionsContainer);
		summaryContainer = findViewById(R.id.summaryContainer);
	}

	public void setData(TagsScorecardModel model, View.OnClickListener backListener) {
		this.model = model;
		if (backListener == null)
			topContainer.findViewById(R.id.back).setVisibility(View.GONE);
		else
			topContainer.findViewById(R.id.back).setOnClickListener(backListener);
		((FadeInNetworkImageView) topContainer.findViewById(R.id.profile_pic)).setImageUrl(model.profilePicUrl, DiviApplication.get()
				.getImageLoader());
		((TextView) topContainer.findViewById(R.id.student_name)).setText(model.userName);
		((TextView) topContainer.findViewById(R.id.chapter_name)).setText(model.chapterName);
		((TextView) topContainer.findViewById(R.id.assessment_name)).setText(model.assessmentModel.name);
		((TextView) topContainer.findViewById(R.id.total_questions)).setText("" + model.assessmentModel.questions.length + " questions");
		((TextView) topContainer.findViewById(R.id.score)).setText("" + model.assessmentModel.getScorecard(model.attempts).diviScore);
		summaryContainer.setTag(null);
		summaryContainer.setOnClickListener(this);
		summaryContainer.setSelected(true);
		// calculate the required data
		// Log.d(TAG, "tags     -       " + model.book.bookTags);
		TagModel[] allTags = (TagModel[]) new Gson().fromJson(model.book.bookTags, TagModel[].class);
		HashMap<String, TagModel> tagsMap = new HashMap<String, TagModel>();
		for (TagModel tag : allTags)
			tagsMap.put(tag.id, tag);
		questions = new HashMap<String, AssessmentFileModel.Question>();
		questionsByTag = new HashMap<String, ArrayList<String>>();
		for (AssessmentFileModel.Question q : model.assessmentModel.questions) {
			questions.put(q.id, q);
			// Log.d(TAG, "q" + q.metadata);
			for (String tagId : q.metadata.tagIds) {
				if (!tagsMap.containsKey(tagId)) {
					Log.w(TAG, "tag details missing for tagId:" + tagId);
					continue;
				}
				if (!questionsByTag.containsKey(tagId))
					questionsByTag.put(tagId, new ArrayList<String>());
				questionsByTag.get(tagId).add(q.id);
			}
		}
		// draw tags
		LayoutInflater inflater = LayoutInflater.from(getContext());
		tagsContainer.removeAllViews();
		for (String tagId : questionsByTag.keySet()) {
			TagModel t = tagsMap.get(tagId);
			View tagView = inflater.inflate(R.layout.item_tag, tagsContainer, false);
			TextView tagNameView = (TextView) tagView.findViewById(R.id.tag_title);
			TextView tagScoreView = (TextView) tagView.findViewById(R.id.tag_score);
			TextView tagQuestionsView = (TextView) tagView.findViewById(R.id.tag_questions);
			// tagScoreView.setBackgroundDrawable(scoreBg);

			tagNameView.setText(t.name);
			tagQuestionsView.setText("" + questionsByTag.get(tagId).size() + "/" + model.assessmentModel.questions.length + " questions");
			if (model.assessmentModel.type.equalsIgnoreCase(AssessmentFileModel.TYPE_TEST)) {
				int userPoints = 0;
				int maxPoints = 0;
				for (String qId : questionsByTag.get(tagId)) {
					if (!model.attempts.containsKey(qId))
						continue;
					userPoints += model.attempts.get(qId).getCurrentPoints();
					maxPoints += questions.get(qId).points;
				}
				int tagScore = (100 * userPoints) / maxPoints;
				tagScoreView.setText("" + tagScore);
				tagScoreView.setTextColor(TextUtil.getAccuracyBgColor(tagScore));
			} else {
				double totAccuracy = 0;
				for (String qId : questionsByTag.get(tagId)) {
					if (!model.attempts.containsKey(qId))
						continue;
					totAccuracy += model.attempts.get(qId).getCurrentAccuracy();
				}
				int tagScore = ((int) (totAccuracy / questionsByTag.get(tagId).size()));
				tagScoreView.setText("" + tagScore);
				tagScoreView.setTextColor(TextUtil.getAccuracyBgColor(tagScore));
			}
			tagView.setTag(tagId);
			tagView.setOnClickListener(this);
			tagsContainer.addView(tagView);
		}

		// setup tag listener to show only tag questions
		questionsAdapter = new QuestionsAdapter();
		questionsList.setAdapter(questionsAdapter);
		questionsList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String qId = ((Question) questionsAdapter.getItem(position)).id;
				DiviReference ref = new DiviReference(TagsScorecard.this.model.book.courseId, TagsScorecard.this.model.book.id,
						DiviReference.REFERENCE_TYPE_ASSESSMENT, TagsScorecard.this.model.assessmentModel.assessmentId, qId);
				Util.openInstruction(getContext(), ref.getUri());
			}
		});
	}

	public void setLoading() {
		// show a loading progressbar
	}

	private class QuestionsAdapter extends BaseAdapter {
		LayoutInflater	inflater;

		public QuestionsAdapter() {
			inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			if (model == null)
				return 0;
			else {
				if (selectedTagId == null)
					return model.assessmentModel.questions.length;
				else
					return questionsByTag.get(selectedTagId).size();
			}
		}

		@Override
		public Object getItem(int position) {
			if (selectedTagId == null)
				return model.assessmentModel.questions[position];
			else
				return questions.get(questionsByTag.get(selectedTagId).get(position));
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.item_question_analysis, parent, false);
			}
			Question q = (Question) getItem(position);
			// Log.d(TAG, "selTag:	" + selectedTagId);
			// Log.d(TAG, "pos:" + position);
			// Log.d(TAG, "que:" + q.text);
			ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
			TextView question = (TextView) convertView.findViewById(R.id.question_title);
			ImageView status = (ImageView) convertView.findViewById(R.id.question_status);
			TextView attempts = (TextView) convertView.findViewById(R.id.attempts);

			question.setText(q.text);
			if (model.attempts.containsKey(q.id)) {
				Attempt a = model.attempts.get(q.id);
				status.setVisibility(View.VISIBLE);
				if (a.isSolved()) {
					status.setImageResource(R.drawable.green_circle);
				} else {
					status.setImageResource(R.drawable.yellow_circle);
				}
				if (a.isAttempted()) {
					attempts.setVisibility(View.VISIBLE);
					attempts.setText(TextUtil.getAttemptsText2(a.correctAttempts, a.wrongAttempts));
				}
			} else {
				status.setVisibility(View.GONE);
				attempts.setVisibility(View.GONE);
			}
			String questionType = q.type;
			if (QuestionFragmentFactory.QUESTION_TYPE_MCQ.equals(questionType)) {
				icon.setImageResource(R.drawable.ic_mcq);
			} else if (QuestionFragmentFactory.QUESTION_TYPE_LABEL.equals(questionType)) {
				icon.setImageResource(R.drawable.ic_label);
			} else if (QuestionFragmentFactory.QUESTION_TYPE_FILLBLANK.equals(questionType)) {
				icon.setImageResource(R.drawable.ic_fob);
			} else if (QuestionFragmentFactory.QUESTION_TYPE_MATCH.equals(questionType)) {
				icon.setImageResource(R.drawable.ic_match);
			} else if (QuestionFragmentFactory.QUESTION_TYPE_TORF.equals(questionType)) {
				icon.setImageResource(R.drawable.ic_truefalse);
			}

			return convertView;
		}
	}

	public static class TagsScorecardModel {
		public AssessmentFileModel		assessmentModel;
		public Book						book;
		public HashMap<String, Attempt>	attempts;

		public String					userName;
		public String					profilePicUrl;
		public String					chapterName;
	}

	@Override
	public void onClick(View v) {
		if (v.getTag() == null) {
			selectedTagId = null;
		} else {
			String viewTag = (String) v.getTag();
			if (viewTag.equals(selectedTagId))
				selectedTagId = null;
			else
				selectedTagId = viewTag;
		}
		summaryContainer.setSelected(false);
		for (int i = 0; i < tagsContainer.getChildCount(); i++)
			tagsContainer.getChildAt(i).setSelected(false);
		if (selectedTagId != null)
			v.setSelected(true);
		else
			summaryContainer.setSelected(true);
		questionsAdapter.notifyDataSetChanged();
	}
}