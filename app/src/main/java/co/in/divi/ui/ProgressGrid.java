package co.in.divi.ui;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.TextView;
import co.in.divi.R;
import co.in.divi.content.AssessmentFileModel.Question;

public class ProgressGrid extends FrameLayout {
	private static final String	TAG					= ProgressGrid.class.getSimpleName();

	private static final int	NUM_BOXES_PER_ROW	= 5;
	private static final int	BOX_MARGIN			= 10;

	public static enum ProgressState {
		NONE, SOLVED, ATTEMPTED
	}

	static int			BOX_HEIGHT		= 40;
	static int			BOX_WIDTH		= 10;

	boolean				boxWidthReady	= false;

	ArrayList<TextView>	questionViews;
	Question[]			questions		= null;

	public ProgressGrid(Context context, AttributeSet attrs) {
		super(context, attrs);
		questionViews = new ArrayList<TextView>();
		ViewTreeObserver vto = getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@SuppressWarnings("deprecation")
			@Override
			public void onGlobalLayout() {
				getViewTreeObserver().removeGlobalOnLayoutListener(this);
				BOX_WIDTH = (getMeasuredWidth() - ((NUM_BOXES_PER_ROW - 1) * BOX_MARGIN)) / NUM_BOXES_PER_ROW;
				boxWidthReady = true;
				if (questions != null)
					initQuestions(questions);
			}
		});
	}

	public void initQuestions(Question[] questions) {
		this.questions = questions;
		if (!boxWidthReady)
			return;
		questionViews.clear();
		removeAllViews();
		// FrameLayout.LayoutParams layoutParams = new LayoutParams(BOX_WIDTH, BOX_HEIGHT);
		// Log.d(TAG, "width - " + getWidth());
		// Log.d(TAG, "box width - " + BOX_WIDTH);
		for (int i = 0; i < questions.length; i++) {
			int row = i / NUM_BOXES_PER_ROW;
			int col = i % NUM_BOXES_PER_ROW;
			int x = col * BOX_WIDTH + col * BOX_MARGIN;
			int y = row * BOX_HEIGHT + row * BOX_MARGIN;
			// Log.d(TAG, "x,y - " + x + "," + y);
			TextView tv = new TextView(getContext());
			tv.setText("Q" + (i + 1));
			// tv.setText(questions[i].id);
			tv.setBackgroundResource(R.drawable.progress_box_grey);
			tv.setTextColor(Color.DKGRAY);
			tv.setTag(questions[i].id);
			questionViews.add(tv);
			tv.setGravity(Gravity.CENTER);
			addView(tv);
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) tv.getLayoutParams();
			lp.width = BOX_WIDTH;
			lp.height = BOX_HEIGHT;
			lp.setMargins(x, y, 0, 0);
			// lp.gravity = Gravity.CENTER;
			tv.setLayoutParams(lp);
			requestLayout();
		}
	}

	public void setattemptStates(HashMap<String, ProgressState> attemptStates) {
		for (TextView tv : questionViews) {
			if (attemptStates.containsKey(tv.getTag())) {
				switch (attemptStates.get(tv.getTag())) {
				case ATTEMPTED:
					tv.setBackgroundResource(R.drawable.progress_box_yellow);
					break;
				case SOLVED:
					tv.setBackgroundResource(R.drawable.progress_box_green);
					break;
				default:
					tv.setBackgroundResource(R.drawable.progress_box_grey);
					break;
				}
			} else {
				tv.setBackgroundResource(R.drawable.progress_box_grey);
			}
		}
	}

}
