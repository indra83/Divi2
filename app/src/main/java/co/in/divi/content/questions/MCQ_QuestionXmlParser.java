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
import co.in.divi.content.questions.MCQ_Question.Option;
import co.in.divi.util.LogConfig;
import co.in.divi.util.TextUtil;

public class MCQ_QuestionXmlParser extends BaseQuestionXmlParser {

	public MCQ_Question getQuestionFromXml(File questionXmlFile, ImageGetter imageGetter) {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "processing file: " + questionXmlFile.getAbsolutePath());
		MCQ_Question question = new MCQ_Question();
		question.options = new ArrayList<MCQ_Question.Option>();
		int optionCount = 0;// for generating ids.
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
					String tempData = null;
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
				} else if (tagName.equals(QuestionXmlTags.OPTIONS_TAG)) {
					// add option
					if (LogConfig.DEBUG_XML)
						Log.d(TAG, "filling options");
					while (parser.next() != XmlPullParser.END_TAG) {// labels end tag
						if (parser.getEventType() != XmlPullParser.START_TAG) {
							continue;
						}
						if (LogConfig.DEBUG_XML)
							Log.d(TAG, "option?" + parser.getName());
						if (parser.getName().equals(QuestionXmlTags.OPTION_TAG)) {
							optionCount++;
							Option option = fillOption(question, parser, imageGetter);
							option.id = "" + optionCount;
							question.options.add(option);
						} else
							skip(parser);
					}
				} else {
					skip(parser);
				}
			}

			if (LogConfig.DEBUG_XML) {
				Log.d(TAG, "question content:" + question.questionHTML);
				for (Option option : question.options)
					Log.d(TAG, "option:" + option.optionHTML);
			}
		} catch (Exception e) {
			Log.w(TAG, "error parsing question xml:", e);
		}
		return question;
	}

	private Option fillOption(MCQ_Question question, XmlPullParser parser, ImageGetter imageGetter) throws IOException,
			XmlPullParserException {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "fillLabel");
		Option option = question.new Option();
		parser.require(XmlPullParser.START_TAG, ns, QuestionXmlTags.OPTION_TAG);
		option.isAnswer = Boolean.parseBoolean(parser.getAttributeValue(ns, QuestionXmlTags.OPTION_ISANSWER_ATTRIBUTE));
		String tempData = null;
		while (parser.next() != XmlPullParser.END_TAG) {// option end tag
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
		option.optionHTML = Html.fromHtml(TextUtil.trimPTag(tempData), imageGetter, null);
		parser.require(XmlPullParser.END_TAG, ns, QuestionXmlTags.OPTION_TAG);
		return option;
	}
}
