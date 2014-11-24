package co.in.divi.ui.highlight;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import co.in.divi.R;
import co.in.divi.util.Config;

public class HilighterModule implements HighlightFetchCallback {

	static final String			TAG				= "HilighterModule";
	static final int			SELECTOR_WIDTH	= 32;
	static final int			SELECTOR_HEIGHT	= 32;

	WebView						webView;
	HighlightPersistenceManager	manager;

	FrameLayout					selectionFrame;
	LinearLayout				hilightToolbar;
	ImageView					leftCursor, rightCursor;
	Button						cancel, hilight, remove;

	State						state;

	public HilighterModule(WebView webView, FrameLayout selectionFrame, HighlightPersistenceManager manager) {
		this.webView = webView;
		this.manager = manager;
		this.selectionFrame = selectionFrame;
		hilightToolbar = (LinearLayout) selectionFrame.findViewById(R.id.hilight_toolbar);
		leftCursor = (ImageView) selectionFrame.findViewById(R.id.left_cursor);
		rightCursor = (ImageView) selectionFrame.findViewById(R.id.right_cursor);
		cancel = (Button) hilightToolbar.findViewById(R.id.cancel);
		hilight = (Button) hilightToolbar.findViewById(R.id.hilight_yellow);
		remove = (Button) hilightToolbar.findViewById(R.id.hilight_remove);
		state = new State();

		webView.addJavascriptInterface(new JavaWindow(), "JavaWindow");
		webView.setOnTouchListener(selectionTouchListener);

	}

	public void stopHilighter() {
		state.setReadyForSelection(false);
		finishCancelSelection();
	}

	public boolean isInSelection() {
		return state.isInSelection();
	}

	@Override
	public void foundHighlights(String highlights) {
		// initialize and setup highlighter
		if (highlights == null)
			highlights = "";
		String jsString = "javascript:initHilightStuff('" + highlights + "');";
		if (Config.DEBUG_HILIGHT)
			Log.i(TAG, jsString);
		webView.loadUrl(jsString);

		cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				initCancelSelection();
			}
		});

		hilight.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				initHighlight();
			}
		});

		remove.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				initRemoveHighlight();
			}
		});
	}

	private class State {
		static final int	CURSOR_LEFT				= 0;
		static final int	CURSOR_RIGHT			= 1;
		private boolean		readyForSelection		= false;
		private boolean		inSelection				= false;
		private boolean		waitingForCaretRefresh	= false;
		private boolean		moving					= false;
		private int			movingCursor;

		public boolean isReadyForSelection() {
			return readyForSelection;
		}

		public void setReadyForSelection(boolean readyForSelection) {
			this.readyForSelection = readyForSelection;
		}

		public boolean isInSelection() {
			return inSelection;
		}

		public void setInSelection(boolean inSelection) {
			this.inSelection = inSelection;
		}

		public boolean isWaitingForCaretRefresh() {
			return waitingForCaretRefresh;
		}

		public void setWaitingForCaretRefresh(boolean waitingForCaretRefresh) {
			this.waitingForCaretRefresh = waitingForCaretRefresh;
		}

		public boolean isMoving() {
			return moving;
		}

		public void setMoving(boolean moving) {
			this.moving = moving;
		}

		public int getMovingCursor() {
			return movingCursor;
		}

		public void setMovingCursor(int movingCursor) {
			this.movingCursor = movingCursor;
		}
	}

	View.OnTouchListener	selectionTouchListener	= new View.OnTouchListener() {

														private boolean isViewContains(View view, int rx, int ry) {
															int x = view.getLeft();
															int y = view.getTop();
															int w = view.getWidth();
															int h = view.getHeight();
															if (rx < x || rx > x + w || ry < y || ry > y + h) {
																return false;
															}
															return true;
														}

														public boolean onTouch(View v, MotionEvent event) {
															if (!state.isReadyForSelection() || !state.isInSelection())
																return false;// gestureDetector.onTouchEvent(event);
															switch (event.getAction()) {
															case MotionEvent.ACTION_DOWN:
																// TODO: handle multi-touch better.
																if (state.isMoving())
																	return true;// ignore multiple fingers
																final int x = (int) event.getX();
																final int y = (int) event.getY();
																if (isViewContains(leftCursor, x, y)) {
																	state.setMoving(true);
																	state.setMovingCursor(State.CURSOR_LEFT);
																} else if (isViewContains(rightCursor, x, y)) {
																	state.setMoving(true);
																	state.setMovingCursor(State.CURSOR_RIGHT);
																} else {
																	initCancelSelection();
																}
																if (Config.DEBUG_HILIGHT)
																	Log.d(TAG,
																			"moving:" + state.isMoving() + ", cursor:"
																					+ state.getMovingCursor());
																return true;
															case MotionEvent.ACTION_MOVE:
																if (state.isMoving()) {
																	final int x_new = (int) (event.getX() / webView.getScale());
																	final int y_new = (int) (event.getY() / webView.getScale());
																	if (!state.isWaitingForCaretRefresh()) {
																		state.setWaitingForCaretRefresh(true);
																		initCaretMove(state.getMovingCursor(), x_new, y_new);
																	}
																}
																return true;
															case MotionEvent.ACTION_UP:
																state.setMoving(false);
																return true;
															}
															return false;
														}
													};

	public void initStartSelection(int x, int y) {
		String jsString = "javascript:translateToJS(" + ((int) x) + "," + ((int) y) + ");";
		if (Config.DEBUG_HILIGHT)
			Log.i(TAG, jsString);
		webView.loadUrl(jsString);
	}

	protected void finishStartSelection(int leftX, int leftY, int rightX, int rightY) {
		if (Config.DEBUG_HILIGHT)
			Log.d(TAG, "webView.getScale()::" + webView.getScale());
		int corrLeftX = (int) (leftX * webView.getScale());
		int corrLeftY = (int) (leftY * webView.getScale());
		int corrRightX = (int) (rightX * webView.getScale());
		int corrRightY = (int) (rightY * webView.getScale());
		if (Config.DEBUG_HILIGHT)
			Log.d(TAG, "starting selection!");
		selectionFrame.setVisibility(View.VISIBLE);

		MarginLayoutParams params = (MarginLayoutParams) leftCursor.getLayoutParams();
		params.leftMargin = corrLeftX - SELECTOR_WIDTH / 2;
		params.topMargin = corrLeftY;
		leftCursor.setLayoutParams(params);

		params = (MarginLayoutParams) rightCursor.getLayoutParams();
		params.leftMargin = corrRightX - SELECTOR_WIDTH / 2;
		params.topMargin = corrRightY;
		rightCursor.setLayoutParams(params);
		state.setInSelection(true);
		hilightToolbar.setVisibility(View.VISIBLE);
	}

	protected void initCancelSelection() {
		String jsString = "javascript:cancel();";
		if (Config.DEBUG_HILIGHT)
			Log.i(TAG, jsString);
		webView.loadUrl(jsString);
	}

	protected void finishCancelSelection() {
		state.setInSelection(false);
		state.setMoving(false);
		state.setWaitingForCaretRefresh(false);
		selectionFrame.setVisibility(View.GONE);
		hilightToolbar.setVisibility(View.GONE);
	}

	protected void initHighlight() {
		String jsString = "javascript:highlightSelectedText();";
		if (Config.DEBUG_HILIGHT)
			Log.i(TAG, jsString);
		webView.loadUrl(jsString);
	}

	protected void initRemoveHighlight() {
		String jsString = "javascript:removeHighlightFromSelectedText();";
		if (Config.DEBUG_HILIGHT)
			Log.i(TAG, jsString);
		webView.loadUrl(jsString);
	}

	protected void finishHighlight(String curHighlights) {
		Log.d(TAG, "curHighlights" + curHighlights);
		manager.setNewHighlights(curHighlights);
		initCancelSelection();
	}

	protected void initCaretMove(int caret, int newX, int newY) {
		int correctedX = newX;
		int correctedY = newY - SELECTOR_HEIGHT / 2;
		String jsString = "javascript:caretMove(" + caret + "," + ((int) correctedX) + "," + ((int) correctedY) + ");";
		if (Config.DEBUG_HILIGHT)
			Log.i(TAG, jsString);
		webView.loadUrl(jsString);
	}

	protected void finishCaretMove(int finX, int finY) {
		int corrFinX = (int) (finX * webView.getScale());
		int corrFinY = (int) (finY * webView.getScale());
		state.setWaitingForCaretRefresh(false);
		if (finX < 0 || finY < 0)
			return;
		ImageView cursor;
		if (state.getMovingCursor() == State.CURSOR_LEFT) {
			cursor = leftCursor;
		} else {
			cursor = rightCursor;
		}
		MarginLayoutParams params = (MarginLayoutParams) cursor.getLayoutParams();
		params.leftMargin = corrFinX - SELECTOR_WIDTH / 2;
		params.topMargin = corrFinY;
		cursor.setLayoutParams(params);
	}

	private class JavaWindow {

		@JavascriptInterface
		public void log(String message) {
			if (Config.DEBUG_HILIGHT)
				Log.i(TAG, "" + message);
		}

		@JavascriptInterface
		public void finishRangyInit() {
			if (Config.DEBUG_HILIGHT)
				Log.i(TAG, "finishRangyInit");
			webView.post(new Runnable() {
				public void run() {
					manager.startHighlightsFetch(HilighterModule.this);
				}
			});
		}

		@JavascriptInterface
		public void finishStartSelection(final int leftX, final int leftY, final int rightX, final int rightY) {
			if (Config.DEBUG_HILIGHT)
				Log.d(TAG, "startSelection::" + leftX + "," + leftY + ", " + rightX + "," + rightY);
			webView.post(new Runnable() {
				public void run() {
					HilighterModule.this.finishStartSelection(leftX, leftY, rightX, rightY);
				}
			});
		}

		@JavascriptInterface
		public void finishCaretMove(final int finX, final int finY) {
			if (Config.DEBUG_HILIGHT)
				Log.d(TAG, "finishCaretMove::" + finX + "," + finY);
			webView.post(new Runnable() {

				public void run() {
					HilighterModule.this.finishCaretMove(finX, finY);
				}
			});
		}

		@JavascriptInterface
		public void finishHilightInitialization() {
			if (Config.DEBUG_HILIGHT)
				Log.d(TAG, "finishHilightInitialization::");
			webView.post(new Runnable() {
				public void run() {
					state.setReadyForSelection(true);
				}
			});
		}

		@JavascriptInterface
		public void finishCancel() {
			if (Config.DEBUG_HILIGHT)
				Log.d(TAG, "finishCancel::");
			webView.post(new Runnable() {
				public void run() {
					finishCancelSelection();
				}
			});
		}

		@JavascriptInterface
		public void finishHighlight(final String curHighlights) {
			if (Config.DEBUG_HILIGHT)
				Log.d(TAG, "finishHighlight::");
			webView.post(new Runnable() {
				public void run() {
					HilighterModule.this.finishHighlight(curHighlights);
				}
			});
		}
	}

	public static interface HighlightPersistenceManager {
		public void startHighlightsFetch(HighlightFetchCallback callback);

		public void setNewHighlights(String newHighlights);
	}

}
