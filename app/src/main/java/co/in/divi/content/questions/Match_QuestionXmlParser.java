package co.in.divi.content.questions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.text.Html;
import android.text.Html.ImageGetter;
import android.util.Log;
import android.util.Xml;
import co.in.divi.content.TopicXmlTags;
import co.in.divi.util.LogConfig;
import co.in.divi.util.TextUtil;

public class Match_QuestionXmlParser extends BaseQuestionXmlParser {

	public Match_Question getQuestionFromXml(File questionXmlFile, ImageGetter imageGetter) {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "processing file: " + questionXmlFile.getAbsolutePath());
		Match_Question question = new Match_Question();
		question.matches = new ArrayList<Match_Question.Match>();
		int matchCount = 0;// for generating ids
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(new FileInputStream(questionXmlFile), null);
			parser.nextTag();

			parser.require(XmlPullParser.START_TAG, ns, "question");
			question.id = parser.getAttributeValue(ns, "id");
			while (parser.next() != XmlPullParser.END_TAG) {// topic end tag
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String tagName = parser.getName();
				if (tagName.equals(QuestionXmlTags.QUESTION_TAGS_TAG)) {
					question.metadata = readTags(parser);
				} else if (tagName.equals(QuestionXmlTags.HTML_TAG)) {
					// question html
					String tempData = "";
					while (parser.next() != XmlPullParser.END_TAG) {// html end tag
						if (parser.getEventType() != XmlPullParser.START_TAG) {
							continue;
						}
						String name = parser.getName();
						if (name.equals(TopicXmlTags.HTML_DATA_TAG)) {
							// get to cdata tag:
							tempData = readText(parser);
						} else {
							skip(parser);
						}
					}
					question.questionHTML = Html.fromHtml(tempData, imageGetter, null);
					question.questionText = Jsoup.parse(tempData).text();
					if (question.questionText.length() > 100)
						question.questionText = question.questionText.substring(0, 99);
					parser.require(XmlPullParser.END_TAG, ns, TopicXmlTags.HTML_TAG);
				} else if (tagName.equals(QuestionXmlTags.MATCHES_TAG)) {
					// add labels
					if (LogConfig.DEBUG_XML)
						Log.d(TAG, "filling matches");
					while (parser.next() != XmlPullParser.END_TAG) {// labels end tag
						if (parser.getEventType() != XmlPullParser.START_TAG) {
							continue;
						}
						if (parser.getName().equals(QuestionXmlTags.MATCH_TAG)) {
							matchCount++;
							Match_Question.Match m = fillMatch(question, parser, imageGetter);
							m.id = "" + matchCount;
							question.matches.add(m);
						} else
							skip(parser);
					}
				} else {
					skip(parser);
				}
			}

			if (LogConfig.DEBUG_XML) {
				Log.d(TAG, "question content:" + question.questionHTML);
				for (Match_Question.Match match : question.matches)
					Log.d(TAG, "label:" + match.leftHTML + "  --  " + match.rightHTML);
			}
		} catch (Exception e) {
			Log.w(TAG, "error parsing question xml:", e);
		}
		return question;
	}

	private Match_Question.Match fillMatch(Match_Question question, XmlPullParser parser, ImageGetter imageGetter) throws IOException,
			XmlPullParserException {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "fillMatch");
		Match_Question.Match match = question.new Match();

		parser.require(XmlPullParser.START_TAG, ns, QuestionXmlTags.MATCH_TAG);
		while (parser.next() != XmlPullParser.END_TAG) {// label end tag
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals(QuestionXmlTags.MATCH_LEFT_TAG)) {
				match.leftHTML = Html.fromHtml(TextUtil.trimPTag(readText(parser)), imageGetter, null);
			} else if (name.equals(QuestionXmlTags.MATCH_RIGHT_TAG)) {
				match.rightHTML = Html.fromHtml(TextUtil.trimPTag(readText(parser)), imageGetter, null);
			} else {
				skip(parser);
			}
		}
		parser.require(XmlPullParser.END_TAG, ns, QuestionXmlTags.MATCH_TAG);
		return match;
	}
}