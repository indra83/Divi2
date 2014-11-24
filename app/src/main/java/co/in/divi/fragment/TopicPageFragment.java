package co.in.divi.fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import co.in.divi.R;
import co.in.divi.activity.LearnActivity;
import co.in.divi.content.DiviReference;
import co.in.divi.content.Node;
import co.in.divi.content.Topic;
import co.in.divi.content.TopicHTMLGenerator;
import co.in.divi.ui.TopicWebView;
import co.in.divi.ui.TopicWebView.TopicWebViewListener;
import co.in.divi.ui.highlight.HilighterModule;
import co.in.divi.util.LogConfig;

public class TopicPageFragment extends Fragment implements TopicWebViewListener {
	static final String			TAG			= TopicPageFragment.class.getSimpleName();
	private static final String	TOPIC_NO	= "TOPIC_NO";

	private int					topicNo;
	private Node				topicToLoad	= null;
	private boolean				pageLoaded;

	private TopicWebView		webView;
	private LoadTopicTask		loadTask	= null;

	// highlight stuff
	private FrameLayout			selectionFrame;
	private HilighterModule		hm;

	// html generation
	static TopicHTMLGenerator	htmlGenerator;

	public TopicPageFragment() {
		super();
	}

	public static TopicPageFragment newInstance(int topicNo) {
		Bundle b = new Bundle();
		b.putInt(TOPIC_NO, topicNo);
		TopicPageFragment f = new TopicPageFragment();
		f.setArguments(b);
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (LogConfig.DEBUG_ACTIVITIES)
			Log.d(TAG, "onCreate");
		final Bundle args = getArguments();
		topicNo = args.getInt(TOPIC_NO);
		if (htmlGenerator == null)
			htmlGenerator = new TopicHTMLGenerator(getActivity());
		pageLoaded = false;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_topicpage, container, false);
		webView = (TopicWebView) rootView.findViewById(R.id.webView);
		webView.setListener(this);
		webView.addJavascriptInterface(new MainWindow(), "MainWindow");
		if (LogConfig.DEBUG_ACTIVITIES) {
			webView.setWebChromeClient(new WebChromeClient() {
				public boolean onConsoleMessage(ConsoleMessage cm) {
					Log.d(TAG, cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
					return true;
				}
			});
		}
		webView.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return true;
			}
		});
		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!pageLoaded) {
			if (LogConfig.DEBUG_ACTIVITIES)
				Log.d(TAG, "loading topic no. - " + topicNo);
			final Node[] chapters = ((LearnActivity) getActivity()).chapters;
			if (chapters == null) {
				Log.w(TAG, "something went wrong earlier, chapters is null!");
				Toast.makeText(getActivity(), "Failed to open book, try again.", Toast.LENGTH_LONG).show();
				getActivity().finish();
				return;
			}
			int count = 0;
			topicToLoad = null;
			String chapterTitle = null;
			for (Node n : chapters) {
				chapterTitle = n.name;
				for (Node t : n.getChildren()) {
					if (count == topicNo) {
						topicToLoad = t;
						break;
					}
					count++;
					chapterTitle = null;// no need to show chapterTitle
				}
				if (topicToLoad != null)
					break;
			}

			try {
				Topic topic = (Topic) topicToLoad.getTag();
				File pageFile = new File(((LearnActivity) getActivity()).bookBaseDir, topic.pagePath);
				String pagePath = pageFile.toURI().toString();
				loadTask = new LoadTopicTask();
				loadTask.execute(new String[] { pagePath, chapterTitle, ((LearnActivity) getActivity()).currentBook.name, topicToLoad.name });
			} catch (Exception e) {
				Log.w(TAG, "Error loading topic", e);
				Toast.makeText(getActivity(), "Error loading topic, data corrupted?", Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (loadTask != null)
			loadTask.cancel(true);
	}

	@Override
	public void handleUrlClick(DiviReference ref) {
		((LearnActivity) getActivity()).handleUrlClick(ref);
	}

	@Override
	public void onPageFinished() {
	}

	class LoadTopicTask extends AsyncTask<String, Void, Integer> {

		String	topicData;
		String	pagePath, chapterTitle, publisher, topicTitle;

		@Override
		protected Integer doInBackground(String... params) {
			pagePath = params[0];
			chapterTitle = params[1];
			publisher = params[2];
			topicTitle = params[3];
			try {
				topicData = htmlGenerator.getTopicHTML(new File(URI.create(pagePath)), chapterTitle, publisher, topicTitle);
				// debug begin
				try {
					File file = new File(Environment.getExternalStorageDirectory() + File.separator + "test_" + topicNo + ".html");
					if (LogConfig.DEBUG_ACTIVITIES)
						Log.d(TAG, "file is " + file.getAbsolutePath());
					OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file));
					outputStreamWriter.write(topicData);
					outputStreamWriter.close();
				} catch (IOException e) {
					Log.e("Exception", "File write failed: " + e.toString());
				}
				// debug end
				return 0;
			} catch (Exception e) {
				Log.w(TAG, "Error loading topic - " + pagePath, e);
				return 1;
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result == 0) {
				webView.clearHistory();
				webView.loadDataWithBaseURL(pagePath, topicData, "text/html", "utf-8", null);
				pageLoaded = true;
			} else {
				Toast.makeText(getActivity(), "Error loading topic", Toast.LENGTH_LONG).show();
			}
		}
	}

	private class MainWindow {
		@JavascriptInterface
		public void openResource(final String resourceId) {
			if (LogConfig.DEBUG_ACTIVITIES)
				Log.d(TAG, "Trying to open resource: " + resourceId);
			((LearnActivity) getActivity()).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (topicToLoad == null || ((LearnActivity) getActivity()).getDisplayedTopic() == null
							|| !topicToLoad.id.equals(((LearnActivity) getActivity()).getDisplayedTopic().id)) {
						Toast.makeText(getActivity(), "Resource could not be opened...", Toast.LENGTH_LONG).show();
					} else {
						((LearnActivity) getActivity()).openResource(resourceId, null);
					}
				}
			});
		}
	}

	@Override
	public void onScroll(int l, int t, int oldl, int oldt) {
		// Log.d(TAG, "l:" + l + ", t:" + t);
		if (oldt > t) {// scrolling down
			if (scrollTracker >= 0)
				scrollTracker++;
			else
				scrollTracker = 0;
		} else {
			if (scrollTracker <= 0)
				scrollTracker--;
			else
				scrollTracker = 0;
		}
		if (scrollTracker > 5) {
			((LearnActivity) getActivity()).showHeader();
			scrollTracker = 0;
		}
		if (scrollTracker < -5) {
			((LearnActivity) getActivity()).hideHeader();
			scrollTracker = 0;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
		super.onSaveInstanceState(outState);
	}

	private int	scrollTracker	= 0;
}
