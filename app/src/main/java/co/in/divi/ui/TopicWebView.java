package co.in.divi.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import co.in.divi.content.DiviReference;
import co.in.divi.util.LogConfig;

public class TopicWebView extends WebView {
    private static final String TAG = TopicWebView.class.getSimpleName();

    public MotionEvent prevUpEvent = null;
    TopicWebViewListener listener = null;

    public TopicWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TopicWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    public void setListener(TopicWebViewListener listener) {
        this.listener = listener;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("SetJavaScriptEnabled")
    private void init(Context context) {
        setWebViewClient(new MyWebViewClient());
        getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        WebSettings s = getSettings();
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(false);
        s.setSavePassword(false);
        s.setSaveFormData(false);
        s.setJavaScriptEnabled(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // setInitialScale(110);
        s.setGeolocationEnabled(false);
        s.setDomStorageEnabled(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP)
            this.prevUpEvent = ev;
        return super.onTouchEvent(ev);
    }

    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (listener != null)
            listener.onScroll(l, t, oldl, oldt);
    }

    private class MyWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Log.d(TAG, " in shouldOverrideUrlLoading");

            DiviReference ref = null;
            Uri uri = Uri.parse(url);
            try {
                ref = new DiviReference(uri);
            } catch (IllegalArgumentException iae) {
                Log.w(TAG, "invalid url - " + url);
                Toast.makeText(getContext(), "Malformed url", Toast.LENGTH_SHORT).show();
            }
            if (ref != null) {
                if (LogConfig.DEBUG_ACTIVITIES)
                    Log.d(TAG, "link clicked:" + ref);
                listener.handleUrlClick(ref);
            }
            return true; // never let the webview load the url
        }
/*
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (url.startsWith(AppUtil.APP_ICON_PROVIDER_PREFIX)) {
                try {
                    String pkgName = Uri.parse(url).getPath().substring(1);
                    Log.d(TAG, "app pkg found! - " + pkgName);
                    InputStream iconStream = AppUtil.bitmapToInputStream(AppUtil.drawableToBitmap(AppUtil.getAppIcon(pkgName, getContext())));
                    if(iconStream==null)
                        Log.d(TAG,"icon couldn't be loaded.");
                    else {
                        Log.d(TAG,"found icon!");
                        return new WebResourceResponse("image/*", "base64", iconStream);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "error getting bitmap", e);
                }
            }
            return null;
        }
        */

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(getContext(), "Oh no! " + description, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            if (LogConfig.DEBUG_ACTIVITIES)
                Log.d(TAG, "loading resource-" + url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            listener.onPageFinished();
            if (LogConfig.DEBUG_ACTIVITIES)
                Log.d(TAG, "onPageFinished - " + url);
            // view.loadUrl("javascript:blah()");
            // onScrollChanged(getScrollX(), getScrollY(), getScrollX(), getScrollY());
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            super.doUpdateVisitedHistory(view, url, isReload);
            if (LogConfig.DEBUG_ACTIVITIES)
                Log.d(TAG, "doUpdateVisitedHistory - " + url);
        }
    }

    public interface TopicWebViewListener {
        public void handleUrlClick(DiviReference ref);

        public void onPageFinished();

        public void onScroll(int l, int t, int oldl, int oldt);
    }
}
