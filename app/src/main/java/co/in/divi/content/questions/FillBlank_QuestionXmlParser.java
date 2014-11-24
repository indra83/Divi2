package co.in.divi.content.questions;

import java.io.File;
import java.io.FileInputStream;

import org.jsoup.Jsoup;
import org.xmlpull.v1.XmlPullParser;

import android.text.Html;
import android.text.Html.ImageGetter;
import android.util.Log;
import android.util.Xml;
import co.in.divi.content.TopicXmlTags;
import co.in.divi.content.questions.Label_Question.Label;
import co.in.divi.util.LogConfig;

public class FillBlank_QuestionXmlParser extends BaseQuestionXmlParser {

	public FillBlank_Question getQuestionFromXml(File questionXmlFile, ImageGetter imageGetter) {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "processing file: " + questionXmlFile.getAbsolutePath());
		FillBlank_Question question = new FillBlank_Question();
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
				} else if (tagName.equals(QuestionXmlTags.BLANKS_TAG)) {
					// add blanks
					if (LogConfig.DEBUG_XML)
						Log.d(TAG, "filling blanks");
					while (parser.next() != XmlPullParser.END_TAG) {// blanks end tag
						if (parser.getEventType() != XmlPullParser.START_TAG) {
							continue;
						}
						if (LogConfig.DEBUG_XML)
							Log.d(TAG, "blank?" + parser.getName());
						if (parser.getName().equals(QuestionXmlTags.BLANK_TAG)) {
							question.blanks.add(readText(parser));
						} else
							skip(parser);
					}
				} else {
					skip(parser);
				}
			}

			if (LogConfig.DEBUG_XML) {
				Log.d(TAG, "question content:" + question.questionHTML);
				for (String b : question.blanks)
					Log.d(TAG, "blank: " + b);
			}
		} catch (Exception e) {
			Log.w(TAG, "error parsing question xml:", e);
		}
		return question;
	}
}