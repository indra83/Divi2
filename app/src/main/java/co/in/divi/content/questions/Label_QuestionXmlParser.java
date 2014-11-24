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
import co.in.divi.content.questions.Label_Question.Label;
import co.in.divi.util.LogConfig;
import co.in.divi.util.TextUtil;

public class Label_QuestionXmlParser extends BaseQuestionXmlParser {

	public Label_Question getQuestionFromXml(File questionXmlFile, ImageGetter imageGetter) {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "processing file: " + questionXmlFile.getAbsolutePath());
		Label_Question question = new Label_Question();
		question.labels = new ArrayList<Label_Question.Label>();
		int labelCount = 0;// for generating ids.
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
				} else if (tagName.equals(QuestionXmlTags.TITLE_TAG)) {
					question.title = readText(parser);
					question.questionText = question.title;
				} else if (tagName.equals(QuestionXmlTags.IMAGE_TAG)) {
					question.imageFile = parser.getAttributeValue(ns, QuestionXmlTags.IMAGE_SRC_ATTRIBUTE);
					skip(parser);
				} else if (tagName.equals(QuestionXmlTags.LABELS_TAG)) {
					// add labels
					if (LogConfig.DEBUG_XML)
						Log.d(TAG, "filling labels");
					while (parser.next() != XmlPullParser.END_TAG) {// labels end tag
						if (parser.getEventType() != XmlPullParser.START_TAG) {
							continue;
						}
						if (LogConfig.DEBUG_XML)
							Log.d(TAG, "label?" + parser.getName());
						if (parser.getName().equals(QuestionXmlTags.LABEL_TAG)) {
							labelCount++;
							Label label = fillLabel(question, parser, imageGetter);
							label.id = "" + labelCount;
							question.labels.add(label);
						} else
							skip(parser);
					}
				} else {
					skip(parser);
				}
			}

			if (LogConfig.DEBUG_XML) {
				Log.d(TAG, "question content:" + question.imageFile);
				for (Label label : question.labels)
					Log.d(TAG, "label:" + label.labelText);
			}
		} catch (Exception e) {
			Log.w(TAG, "error parsing question xml:", e);
		}
		return question;
	}

	private Label fillLabel(Label_Question question, XmlPullParser parser, ImageGetter imageGetter) throws IOException,
			XmlPullParserException {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "fillLabel");
		Label label = question.new Label();
		parser.require(XmlPullParser.START_TAG, ns, QuestionXmlTags.LABEL_TAG);
		label.x = Double.parseDouble(parser.getAttributeValue(ns, QuestionXmlTags.LABEL_X_ATTRIBUTE));
		label.y = Double.parseDouble(parser.getAttributeValue(ns, QuestionXmlTags.LABEL_Y_ATTRIBUTE));
		while (parser.next() != XmlPullParser.END_TAG) {// label end tag
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			Log.d(TAG, "name:" + name);
			if (name.equals(QuestionXmlTags.HTML_DATA_TAG)) {
				label.labelText = Html.fromHtml(TextUtil.trimPTag("  " + readText(parser)) + "  ", imageGetter, null);
			} else {
				skip(parser);
			}
		}
		parser.require(XmlPullParser.END_TAG, ns, QuestionXmlTags.LABEL_TAG);
		return label;
	}

}
