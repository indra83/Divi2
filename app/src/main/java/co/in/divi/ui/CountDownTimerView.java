package co.in.divi.ui;

import co.in.divi.util.Util;
import android.content.Context;
import android.os.Handler;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class CountDownTimerView extends TextView {
	private static final String	TAG					= CountDownTimerView.class.getSimpleName();
	private static final long	UPDATE_FREQUENCY	= 499;										// ms

	public static final long	FOREVER				= Long.MAX_VALUE;

	public interface CountDownTimerViewListener {
		public void timerEvent();
	}

	private enum TimerState {
		BEFORE, DURING, AFTER
	}

	private CountDownTimerViewListener	listener			= null;
	private Handler						handler;
	private long						endTime;
	private String						preText;
	private TimerState					curState;
	private Runnable					updateTextRunnable	= new Runnable() {
																@Override
																public void run() {
																	long now = Util.getTimestampMillis();
																	if (now > endTime) {
																		if (listener != null)
																			listener.timerEvent();
																		stop();
																		return;
																	} else {
																		long diff_secs = (endTime - now) / 1000;
																		String text = preText
																				+ " <b>"
																				+ String.format("%02d:%02d", diff_secs / 60, diff_secs % 60)
																				+ "</b>";
																		setText(Html.fromHtml(text));
																	}
																	handler.postDelayed(updateTextRunnable, UPDATE_FREQUENCY);
																}
															};

	public CountDownTimerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		handler = new Handler();
	}

	public void start(long endTime, String preText, CountDownTimerViewListener listener) {
		if (Util.getTimestampMillis() > endTime)
			throw new RuntimeException("End time is in past!");
		this.listener = listener;
		this.preText = preText;
		this.endTime = endTime;
		handler.removeCallbacks(updateTextRunnable);
		handler.post(updateTextRunnable);
		setVisibility(View.VISIBLE);
	}

	public void stop() {
		handler.removeCallbacks(updateTextRunnable);
		listener = null;
		setVisibility(View.INVISIBLE);
	}
}
