package co.in.divi.ui;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class ScoreView extends TextView {

	public static final int		SIZE_SMALL	= 0;
	public static final int		SIZE_MEDIUM	= 1;
	public static final int		SIZE_LARGE	= 2;

	private double				score;

	private static NumberFormat	format		= new DecimalFormat("0.0");

	public ScoreView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setScore(double score) {
		this.score = score;
		setText(format.format(score));
	}

}
