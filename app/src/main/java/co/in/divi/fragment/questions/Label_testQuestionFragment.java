package co.in.divi.fragment.questions;

import java.io.File;
import java.util.ArrayList;

import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnMatrixChangedListener;
import android.content.ClipData;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import co.in.divi.R;
import co.in.divi.activity.AssessmentActivity.AssessmentMode;
import co.in.divi.content.questions.Label_Question;
import co.in.divi.content.questions.Label_QuestionXmlParser;
import co.in.divi.db.model.Attempt;
import co.in.divi.fragment.questions.state.Label_AttemptData;
import co.in.divi.util.TextUtil;

public class Label_testQuestionFragment extends BaseQuestionFragment implements OnLongClickListener {

	String						path			= null;
	ArrayList<Poi>				labelPois		= new ArrayList<Poi>();
	ArrayList<Button>			labelButtons	= new ArrayList<Button>();

	int							questionPoints	= 0;
	int							subquestions	= 0;

	boolean						latestAttempt	= false;
	private Label_AttemptData	data			= null;

	private ViewGroup			rootLayout;								// root
	private ImageView			mImageView;
	private LinearLayout		buttonContainer;

	private PhotoViewAttacher	mAttacher;
	private Label_Question		labelQuestionData;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.q_fragment_label, container, false);
		rootLayout = (ViewGroup) rootView.findViewById(R.id.rootLayout);
		pointsTV = (TextView) rootView.findViewById(R.id.points_text);
		attemptsTV = (TextView) rootView.findViewById(R.id.attempts_text);
		qnoTV = (TextView) rootView.findViewById(R.id.qno_text);
		tick = (ImageView) rootView.findViewById(R.id.tick);
		mImageView = (ImageView) rootLayout.findViewById(R.id.label_image);
		buttonContainer = (LinearLayout) rootLayout.findViewById(R.id.labelButtonsContainer);
		return rootView;
	}

	@Override
	void loadAttempt(Attempt attempt) {
		if (attempt != null) {
			wrongAttempts = attempt.wrongAttempts;
			correctAttempts = attempt.correctAttempts;
			data = gson.fromJson(attempt.data, Label_AttemptData.class);
			if (data.attempts == null) {
				data.attempts = new int[labelPois.size()];
				for (int i = 0; i < labelPois.size(); i++) {
					data.attempts[i] = -1;
				}
			}

			updateUI();

			// points UI
			isSolved = true;
			for (int i = 0; i < data.attempts.length; i++) {
				if (data.attempts[i] != -1) {
				} else {
					isSolved = false;
				}
			}
			if (isSolved)
				showTick(true);
			else {
				if (wrongAttempts > 0 && !latestAttempt)
					showTick(false);
			}
			if (isSolved) {
				pointsTV.setText(TextUtil.getPointsText(attempt.totalPoints, attempt.totalPoints));
			} else {
				int proratedPoints = (attempt.totalPoints * correctAttempts) / (subquestions);
				pointsTV.setText("Points  " + proratedPoints + "/" + attempt.totalPoints);
			}
			attemptsTV.setText(TextUtil.getAttemptsText(attempt.correctAttempts, attempt.wrongAttempts));
		} else {
			pointsTV.setText("Points  0/" + question.points);
			attemptsTV.setText(TextUtil.getPointsText(0, 0));
		}
	}

	private void doSave() {
		Label_AttemptData data = new Label_AttemptData();
		data.attempts = new int[labelPois.size()];
		int correct = 0;
		int wrong = 0;
		for (int i = 0; i < labelPois.size(); i++) {
			if (labelPois.get(i).isEmpty) {
				data.attempts[i] = -1;
			} else {
				data.attempts[i] = labelPois.get(i).labelNo;
				if (i == labelPois.get(i).labelNo)
					correct++;
				else
					wrong++;
			}
		}
		saveAttempt(subquestions, correct, wrong, gson.toJson(data));
	}

	@Override
	Object getQuestionData() {
		return new Label_QuestionXmlParser().getQuestionFromXml(questionXmlFile, ImageGetter);
	}

	@Override
	void fillUI(Object questionData) {
		labelQuestionData = (Label_Question) questionData;
		// define the image background
		File imageFile = new File(questionXmlFile.getParent(), labelQuestionData.imageFile);
		// TODO: do it async
		Bitmap bm = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
		mImageView.setImageBitmap(bm);
		// photo view stuff
		mAttacher = new PhotoViewAttacher(mImageView);
		mAttacher.setOnMatrixChangeListener(new MatrixChangeListener());

		updateUI();
	}

	private void updateUI() {
		// rebuild UI
		LayoutInflater inflater = LayoutInflater.from(activity);
		LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		reset();
		subquestions = labelQuestionData.labels.size();
		for (int i = 0; i < labelQuestionData.labels.size(); i++) {
			double x = labelQuestionData.labels.get(i).x;
			double y = labelQuestionData.labels.get(i).y;
			FrameLayout poiView = (FrameLayout) inflater.inflate(R.layout.q_item_labelquestion_label, rootLayout, false);
			poiView.setTag(i);
			TextView tv = (TextView) poiView.findViewById(R.id.centerMarker);
			labelPois.add(new Poi(i, x, y, true, poiView, tv));
			rootLayout.addView(poiView);

			Button labelButton = new Button(activity);
			labelButton.setText(labelQuestionData.labels.get(i).labelText);
			labelButton.setOnLongClickListener(this);
			labelButton.setTag(i);
			labelButton.setTextSize(20f);
			labelButton.setBackgroundResource(R.drawable.q_label);
			labelButtons.add(labelButton);
			buttonContainer.addView(labelButton, buttonLayoutParams);
		}

		// load previous attempt/answers
		if (getMode() == AssessmentMode.TEST_AFTER) {
			for (int i = 0; i < labelPois.size(); i++) {
				if (data != null && data.attempts[i] != -1) {
					if (data.attempts[i] == i)
						labelButtons.get(i).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_correct_small, 0);
					else
						labelButtons.get(i).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_wrong_small, 0);
				} else {
					labelButtons.get(i).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_wrong_small, 0);
				}
				solve(labelButtons.get(i), labelPois.get(i), null);
			}
		} else {
			if (data != null) {
				for (int i = 0; i < data.attempts.length; i++) {
					if (data.attempts[i] != -1) {
						solve(labelButtons.get(data.attempts[i]), labelPois.get(i), removeListener);
					}
				}
			}
		}
	}

	private void reset() {
		buttonContainer.removeAllViews();
		for (Poi p : labelPois) {
			rootLayout.removeView(p.frame);
		}
		labelButtons.clear();
		labelPois.clear();
	}

	/*
	 * Label drag & drop functionality
	 */
	class Poi {
		int			id;
		double		x, y;
		float		currentX, currentY;
		boolean		alignLeft;
		FrameLayout	frame;
		TextView	circleTextView;

		boolean		isEmpty	= true;
		int			labelNo	= -1;

		public Poi(int _id, double _x, double _y, boolean _alignLeft, FrameLayout _linearLayout, TextView _circleTextView) {
			id = _id;
			x = _x;
			y = _y;
			alignLeft = _alignLeft;
			frame = _linearLayout;
			circleTextView = _circleTextView;

			frame.setOnDragListener(mOnDragListener);
		}

		public void clearLabel() {
			this.isEmpty = true;
			this.labelNo = -1;
		}
	};

	private class MatrixChangeListener implements OnMatrixChangedListener {

		@Override
		public void onMatrixChanged(RectF rect) {
			// translate all the points
			Log.d(TAG, rect.toString() + "," + mAttacher.getScale());
			for (Poi point : labelPois) {
				point.currentX = (float) (point.x * ((rect.right - rect.left)) + rect.left);
				point.currentY = (float) point.y * ((rect.bottom - rect.top)) + rect.top;
				int xdel = point.frame.getWidth() / 2;
				int ydel = point.frame.getHeight() / 2;
				point.frame.setTranslationX(point.currentX - xdel);
				point.frame.setTranslationY(point.currentY - ydel);
			}
		}

	}

	@Override
	public boolean onLongClick(View v) {
		boolean whatToReturn = false;
		ClipData dragData;

		dragData = ClipData.newPlainText("code", String.valueOf(v.getTag()));

		DragShadowBuilder myShadow = new DragShadowBuilder(v);
		v.startDrag(dragData, myShadow, v.getTag(), 0);
		whatToReturn = true;

		return whatToReturn;
	}

	public OnDragListener	mOnDragListener	= new OnDragListener() {

												@Override
												public boolean onDrag(View v, DragEvent event) {
													Poi poi = labelPois.get((Integer) v.getTag());

													boolean whatToReturn = false;

													switch (event.getAction()) {
													case DragEvent.ACTION_DRAG_STARTED:
														if (poi.isEmpty)
															whatToReturn = true;
														else
															whatToReturn = false;
														break;
													case DragEvent.ACTION_DRAG_ENTERED:
														poi.circleTextView.setBackgroundResource(R.drawable.q_label_point_glow);
														v.setBackgroundResource(R.drawable.q_label_point_glow);
														v.invalidate();
														return true;
													case DragEvent.ACTION_DRAG_EXITED:
														poi.circleTextView.setBackgroundResource(R.drawable.q_label_holder_empty);
														v.setBackgroundResource(0);
														v.invalidate();
														return true;
													case DragEvent.ACTION_DROP:
														poi.circleTextView.setBackgroundResource(R.drawable.q_label_holder_empty);
														// check if label and drop poi match
														Log.d(TAG, "view tag:" + v.getTag() + ", localstate:" + event.getLocalState());
														int i = (Integer) event.getLocalState();
														Log.d(TAG, "drop for label:" + i);
														Button b = labelButtons.get(i);
														solve(b, poi, removeListener);
														doSave();
														v.setBackgroundColor(Color.TRANSPARENT);
														v.invalidate();
														whatToReturn = true;
														break;
													default:
														break;
													}
													return whatToReturn;
												}
											};

	private void solve(Button b, Poi poi, View.OnLongClickListener clickListener) {
		b.setOnLongClickListener(clickListener);
		buttonContainer.removeView(b);

		poi.frame.removeAllViews();
		poi.isEmpty = false;
		poi.labelNo = (Integer) b.getTag();
		android.widget.FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
				android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.CENTER;
		poi.frame.addView(b, lp);

		// align buttons again
		RectF rect = mAttacher.getDisplayRect();
		for (Poi point : labelPois) {
			point.currentX = (float) (point.x * ((rect.right - rect.left)) + rect.left);
			point.currentY = (float) point.y * ((rect.bottom - rect.top)) + rect.top;
			int xdel = point.frame.getWidth() / 2;
			int ydel = point.frame.getHeight() / 2;
			point.frame.setTranslationX(point.currentX - xdel);
			point.frame.setTranslationY(point.currentY - ydel);
		}
	}

	private View.OnLongClickListener	removeListener	= new View.OnLongClickListener() {
															@Override
															public boolean onLongClick(View v) {
																if (data != null) {
																	int bNo = (Integer) v.getTag();
																	for (Poi poi : labelPois) {
																		if (poi.labelNo == bNo) {
																			poi.clearLabel();
																			doSave();
																		}
																	}
																}
																return true;
															}
														};
}
