package co.in.divi.content.questions;

import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.google.gson.Gson;

import android.util.Log;
import co.in.divi.content.QuestionMetadata;
import co.in.divi.content.QuestionMetadata.BLOOMS;
import co.in.divi.util.LogConfig;

public abstract class BaseQuestionXmlParser {
	protected static final String	TAG	= BaseQuestionXmlParser.class.getName();
	protected static final String	ns	= null;

	protected String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "readText");
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		} else {
			Log.w(TAG, "expected text not found?");
		}
		return result;
	}

	protected void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "skip:" + parser.getName());
		if (parser.getEventType() != XmlPullParser.START_TAG) {
			throw new IllegalStateException();
		}
		int depth = 1;
		while (depth != 0) {
			switch (parser.next()) {
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			}
		}
	}

	protected QuestionMetadata readTags(XmlPullParser parser) throws IOException, XmlPullParserException {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "reading tags");
		parser.require(XmlPullParser.START_TAG, ns, QuestionXmlTags.QUESTION_TAGS_TAG);
		if (parser.getEventType() != XmlPullParser.START_TAG) {
			throw new IllegalStateException();
		}
		QuestionMetadata tags = new QuestionMetadata();
		try {
			tags.difficulty = Integer.parseInt(parser.getAttributeValue(ns, QuestionXmlTags.QUESTION_DIFFICULTY_ATTRIBUTE));
			tags.languageLevel = Integer.parseInt(parser.getAttributeValue(ns, QuestionXmlTags.QUESTION_LANGUAGELEVEL_ATTRIBUTE));
			tags.blooms = BLOOMS.APPLICATION;// BLOOMS.valueOf(parser.getAttributeValue(ns,
												// QuestionXmlTags.QUESTION_BLOOMS_ATTRIBUTE));
		} catch (Exception e) {
			Log.w(TAG, "error reading tag data", e);
		}
		ArrayList<String> tagIds = new ArrayList<String>();
		while (parser.next() != XmlPullParser.END_TAG) {// tags end tag
			if (LogConfig.DEBUG_XML)
				Log.d(TAG, "parsing tag ids");
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String tagName = parser.getName();
			if (tagName.equals(QuestionXmlTags.QUESTION_TAG_TAG)) {
				tagIds.add(parser.getAttributeValue(ns, QuestionXmlTags.QUESTION_TAG_ID_ATTRIBUTE));
				parser.nextTag();
			} else
				skip(parser);
		}
		tags.tagIds = tagIds.toArray(new String[tagIds.size()]);

		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "metadata: " + new Gson().toJson(tags));
		return tags;
	}

}
