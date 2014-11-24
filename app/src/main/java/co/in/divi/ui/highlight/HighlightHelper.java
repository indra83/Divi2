package co.in.divi.ui.highlight;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.util.Log;
import co.in.divi.util.Util;

public class HighlightHelper {

	static final String				TAG					= "HighlightHelper";

	private static final String[]	HILIGHT_JS_FILES	= new String[] {
			"content://co.in.divi.ui.highlight.JSAssetProvider/js/log4javascript.js",
			"content://co.in.divi.ui.highlight.JSAssetProvider/js/core.js",
			"content://co.in.divi.ui.highlight.JSAssetProvider/js/dom.js",
			"content://co.in.divi.ui.highlight.JSAssetProvider/js/domrange.js",
			"content://co.in.divi.ui.highlight.JSAssetProvider/js/wrappedrange.js",
			"content://co.in.divi.ui.highlight.JSAssetProvider/js/wrappedselection.js",
			"content://co.in.divi.ui.highlight.JSAssetProvider/js/divi-highlight.js",

			"content://co.in.divi.ui.highlight.JSAssetProvider/js/modules/rangy-serializer.js",
			"content://co.in.divi.ui.highlight.JSAssetProvider/js/modules/rangy-cssclassapplier.js",
			"content://co.in.divi.ui.highlight.JSAssetProvider/js/modules/rangy-selectionsaverestore.js",
			"content://co.in.divi.ui.highlight.JSAssetProvider/js/modules/rangy-highlighter.js",
			"content://co.in.divi.ui.highlight.JSAssetProvider/js/modules/rangy-textrange.js" };

	private static final String[]	HIGHLIGHT_CSS_FILES	= new String[] { "content://co.in.divi.ui.highlight.JSAssetProvider/js/highlight-style.css" };

	public static String getCustomHTMLFromUrl(String url) {
		try {
			if (url.contains("#"))
				url = url.split("#")[0];
			Document doc = Jsoup.parse(Util.getInputString(new FileInputStream(new File(URI.create(url)))));

			for (String cssFile : HIGHLIGHT_CSS_FILES)
				doc.head().prependElement("link").attr("href", cssFile).attr("rel", "stylesheet").attr("type", "text/css");

			for (String jsFile : HILIGHT_JS_FILES)
				doc.head().appendElement("script").attr("src", jsFile).attr("type", "text/javascript");

			Log.d(TAG,"html: "+doc.html());
			return doc.html();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "error opening file", e);
		} catch (IOException e) {
			Log.e(TAG, "error opening file", e);
		}
		return null;
	}

}
