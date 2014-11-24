package co.in.divi.content.questions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.text.Html;
import android.text.Html.ImageGetter;
import android.util.Log;
import android.util.Xml;
import co.in.divi.content.TopicXmlTags;
import co.in.divi.util.LogConfig;

public class Vocab_QuestionXmlParser extends BaseQuestionXmlParser {

	public Vocab_Question getQuestionFromXml(File questionXmlFile, ImageGetter imageGetter) {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "processing file: " + questionXmlFile.getAbsolutePath());
		Vocab_Question question = new Vocab_Question();
		question.statements = new ArrayList<Vocab_Question.Statement>();
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
				if (tagName.equals(QuestionXmlTags.TITLE_TAG)) {
					question.title = readText(parser);
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
					parser.require(XmlPullParser.END_TAG, ns, TopicXmlTags.HTML_TAG);
				} else if (tagName.equals(QuestionXmlTags.SUBQUESTIONS_TAG)) {
					// add labels
					if (LogConfig.DEBUG_XML)
						Log.d(TAG, "filling statements");
					while (parser.next() != XmlPullParser.END_TAG) {// labels end tag
						if (parser.getEventType() != XmlPullParser.START_TAG) {
							continue;
						}
						if (LogConfig.DEBUG_XML)
							Log.d(TAG, "statement?" + parser.getName());
						if (parser.getName().equals(QuestionXmlTags.SUBQUESTION_TAG))
							question.statements.add(fillStatement(question, parser, imageGetter));
						else
							skip(parser);
					}
				} else {
					skip(parser);
				}
			}

			if (LogConfig.DEBUG_XML) {
				Log.d(TAG, "question content:" + question.questionHTML);
				for (Vocab_Question.Statement statement : question.statements)
					Log.d(TAG, "statement:" + statement.statementHTML);
			}
		} catch (Exception e) {
			Log.w(TAG, "error parsing question xml:", e);
		}
		return question;
	}

	private Vocab_Question.Statement fillStatement(Vocab_Question question, XmlPullParser parser, ImageGetter imageGetter)
			throws IOException, XmlPullParserException {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "fillStatement");
		Vocab_Question.Statement statement = question.new Statement();
		parser.require(XmlPullParser.START_TAG, ns, QuestionXmlTags.SUBQUESTION_TAG);
		statement.isTrue = Boolean.parseBoolean(parser.getAttributeValue(ns, QuestionXmlTags.SUBQUESTION_ISTRUE_ATTRIBUTE));
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
		statement.statementHTML = Html.fromHtml(tempData, imageGetter, null);
		parser.require(XmlPullParser.END_TAG, ns, QuestionXmlTags.SUBQUESTION_TAG);
		return statement;
	}

}