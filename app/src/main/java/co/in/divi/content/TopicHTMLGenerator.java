package co.in.divi.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import co.in.divi.util.AppUtil;
import co.in.divi.util.Config;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Util;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

/*
 *   <!-- Hilight stuff 
 <link href="content://co.in.divi.util.JSAssetProvider/js/highlight-style.css" rel="stylesheet" type="text/css" />
 <script src="content://co.in.divi.util.JSAssetProvider/js/log4javascript.js" type="text/javascript"></script>
 <script src="content://co.in.divi.util.JSAssetProvider/js/core.js" type="text/javascript"></script>
 <script src="content://co.in.divi.util.JSAssetProvider/js/dom.js" type="text/javascript"></script>
 <script src="content://co.in.divi.util.JSAssetProvider/js/domrange.js" type="text/javascript"></script>
 <script src="content://co.in.divi.util.JSAssetProvider/js/wrappedrange.js" type="text/javascript"></script>
 <script src="content://co.in.divi.util.JSAssetProvider/js/wrappedselection.js" type="text/javascript"></script>
 <script src="content://co.in.divi.util.JSAssetProvider/js/divi-highlight.js" type="text/javascript"></script>
 <script src="content://co.in.divi.util.JSAssetProvider/js/modules/rangy-serializer.js" type="text/javascript"></script>
 <script src="content://co.in.divi.util.JSAssetProvider/js/modules/rangy-cssclassapplier.js" type="text/javascript"></script>
 <script src="content://co.in.divi.util.JSAssetProvider/js/modules/rangy-selectionsaverestore.js" type="text/javascript"></script>
 <script src="content://co.in.divi.util.JSAssetProvider/js/modules/rangy-highlighter.js" type="text/javascript"></script>
 <script src="content://co.in.divi.util.JSAssetProvider/js/modules/rangy-textrange.js" type="text/javascript"></script>
 <script type="text/javascript">
 function openResource(resId) {
 console.log("resId:"+resId);
 MainWindow.openResource(resId);
 }
 </script>
 -->
 */
@SuppressWarnings("unused")
public class TopicHTMLGenerator {
	private static final String	TAG				= TopicHTMLGenerator.class.getSimpleName();
	private static final String	ns				= null;
	private static final String	TEMPLATE_PATH	= "bookdesign/templates/";

	static Template				topicContentTemplate, chapterTemplate, topicTitleTemplace, subTopicTitleTemplate, subHeadingTemplate,
			textTemplate, imageTemplate, imageNoBorderTemplate, imageTextTemplate, imageSetTemplate, videoTemplate, audioTemplate,
			box1Template, vmTemplate;

    private Context context;

	public TopicHTMLGenerator(Context context) {
        this.context = context;
		if (topicContentTemplate == null) {// lazy initialize once.
			topicContentTemplate = Mustache.compiler().compile(getTemplateText(context, "topic_content.txt"));
			chapterTemplate = Mustache.compiler().compile(getTemplateText(context, "chapter.txt"));
			topicTitleTemplace = Mustache.compiler().compile(getTemplateText(context, "topic_title.txt"));
			subTopicTitleTemplate = Mustache.compiler().compile(getTemplateText(context, "subtopic.txt"));
			subHeadingTemplate = Mustache.compiler().compile(getTemplateText(context, "subheading.txt"));
			textTemplate = Mustache.compiler().compile(getTemplateText(context, "text.txt"));
			box1Template = Mustache.compiler().compile(getTemplateText(context, "box1.txt"));
			imageTemplate = Mustache.compiler().compile(getTemplateText(context, "image.txt"));
			imageNoBorderTemplate = Mustache.compiler().compile(getTemplateText(context, "image_noborder.txt"));
			imageTextTemplate = Mustache.compiler().compile(getTemplateText(context, "image_text.txt"));
			imageSetTemplate = Mustache.compiler().compile(getTemplateText(context, "imageset.txt"));
			videoTemplate = Mustache.compiler().compile(getTemplateText(context, "video.txt"));
			audioTemplate = Mustache.compiler().compile(getTemplateText(context, "audio.txt"));
			vmTemplate = Mustache.compiler().compile(getTemplateText(context, "vm.txt"));
		}
	}

	public String getTopicHTML(File topicXmlFile, final String chapterTitle, final String chapterAuthor, final String topicTitle) {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "processing file: " + topicXmlFile.getAbsolutePath());
		final StringBuilder contentBuilder = new StringBuilder();
		if (chapterTitle != null) {
			contentBuilder.append(chapterTemplate.execute(new Object() {
				String	title	= chapterTitle;
				String	author	= chapterAuthor;
			}));
		}
		contentBuilder.append(topicTitleTemplace.execute(new Object() {
			String	title	= topicTitle;
		}));
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(new FileInputStream(topicXmlFile), null);
			parser.nextTag();
			parser.require(XmlPullParser.START_TAG, ns, "topic");

			while (parser.next() != XmlPullParser.END_TAG) {// topic end tag
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				// Starts by looking for the entry tag
				if (name.equals(TopicXmlTags.SUBTOPIC_TAG)) {
					processSubtopic(parser, contentBuilder);
				} else if (name.equals(TopicXmlTags.SUBHEADER_TAG)) {
					processSubheading(parser, contentBuilder);
				} else if (name.equals(TopicXmlTags.IMAGE_TAG)) {
					processImage(parser, contentBuilder);
				} else if (name.equals(TopicXmlTags.IMAGESET_TAG)) {
					processImageSet(parser, contentBuilder);
				} else if (name.equals(TopicXmlTags.VIDEO_TAG)) {
					processVideo(parser, contentBuilder);
				} else if (name.equals(TopicXmlTags.AUDIO_TAG)) {
					processAudio(parser, contentBuilder);
				} else if (name.equals(TopicXmlTags.APP_TAG)) {
					processApp(parser, contentBuilder, topicXmlFile.getParentFile());
				} else if (name.equals(TopicXmlTags.HTML_TAG)) {
					processHtml(parser, contentBuilder);
				} else {
					skip(parser);
				}
			}
		} catch (XmlPullParserException xppe) {
			Log.e(TAG, "error parsing topic xml", xppe);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "error reading topic xml", e);
		} catch (IOException e) {
			Log.e(TAG, "error reading topic xml", e);
		}
		String ret = topicContentTemplate.execute(new Object() {
			String	content	= contentBuilder.toString();
		});
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, ret);
		return ret;
	}

	private void processImage(XmlPullParser parser, StringBuilder sb) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, ns, TopicXmlTags.IMAGE_TAG);
		final String _id = parser.getAttributeValue(ns, TopicXmlTags.ID_ATTRIBUTE);
		final String imageSrc = parser.getAttributeValue(ns, TopicXmlTags.SRC_ATTRIBUTE);
		final String showBorder = parser.getAttributeValue(ns, TopicXmlTags.IMAGE_SHOWBORDER_ATTRIBUTE);
		final String showFullscreen = parser.getAttributeValue(ns, TopicXmlTags.IMAGE_FULLSCREEN_ATTRIBUTE);
		final String imageWithText = parser.getAttributeValue(ns, TopicXmlTags.IMAGE_WITH_TEXT_ATTRIBUTE);
		String tempImageDesc = null;
		String tempImageTitle = null;
		while (parser.next() != XmlPullParser.END_TAG) {// image end tag
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Starts by looking for the entry tag
			if (name.equals(TopicXmlTags.IMAGE_DESCRIPTION_TAG)) {
				tempImageDesc = readText(parser);
			} else if (name.equals(TopicXmlTags.IMAGE_TITLE_TAG)) {
				tempImageTitle = readText(parser);
			} else {
				skip(parser);
			}
		}
		final String imageDesc = tempImageDesc;
		final String imageTitle = tempImageTitle;
		final boolean isFullscreen = showFullscreen != null && Boolean.parseBoolean(showFullscreen);
		parser.require(XmlPullParser.END_TAG, ns, TopicXmlTags.IMAGE_TAG);
		if (imageTitle == null || Boolean.parseBoolean(imageWithText)) {
			sb.append(imageTextTemplate.execute(new Object() {
				String	id			= _id;
				String	image_path	= imageSrc;
				String	caption		= imageTitle;
				String	desc		= imageDesc;
			}));
		} else if (showBorder == null || Boolean.parseBoolean(showBorder)) {
			sb.append(imageTemplate.execute(new Object() {
				String	id			= _id;
				String	image_path	= imageSrc;
				String	caption		= imageTitle;
				String	desc		= imageDesc;
			}));
		} else {
			sb.append(imageNoBorderTemplate.execute(new Object() {
				String	id					= _id;
				String	image_path			= imageSrc;
				String	caption				= imageTitle;
				String	desc				= imageDesc;
				boolean	allow_fullscreen	= isFullscreen;
			}));
		}
	}

	private void processImageSet(XmlPullParser parser, StringBuilder sb) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, ns, TopicXmlTags.IMAGESET_TAG);
		final String _id = parser.getAttributeValue(ns, TopicXmlTags.ID_ATTRIBUTE);
		final String title = parser.getAttributeValue(ns, TopicXmlTags.IMAGESET_TITLE_ATTRIBUTE);
		String tempSrc = null;
		while (parser.next() != XmlPullParser.END_TAG) {// topic end tag
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Log.d(TAG, "top:" + name);
			// Starts by looking for the entry tag
			if (name.equals(TopicXmlTags.IMAGESET_ITEM_TAG)) {
				tempSrc = parser.getAttributeValue(ns, TopicXmlTags.SRC_ATTRIBUTE);
				while (parser.next() != XmlPullParser.END_TAG) {// item end tag
					if (parser.getEventType() != XmlPullParser.START_TAG) {
						continue;
					}
					// Log.d(TAG, "inside:" + parser.getName());
					if (parser.getName().equals(TopicXmlTags.IMAGESET_ITEM_DESCRIPTION_TAG)) {
						// tempSrc = parser.getAttributeValue(ns, TopicXmlTags.SRC_ATTRIBUTE);
						skip(parser);
					} else {
						skip(parser);
					}
				}
			} else {
				skip(parser);
			}
		}
		final String src = tempSrc;
		parser.require(XmlPullParser.END_TAG, ns, TopicXmlTags.IMAGESET_TAG);
		sb.append(imageSetTemplate.execute(new Object() {
			String	id			= _id;
			String	caption		= title;
			String	image_path	= src;
		}));
	}

	private void processVideo(XmlPullParser parser, StringBuilder sb) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, ns, TopicXmlTags.VIDEO_TAG);
		final String _id = parser.getAttributeValue(ns, TopicXmlTags.ID_ATTRIBUTE);
		final String videoThumb;
		final String youtubeId = parser.getAttributeValue(ns, TopicXmlTags.VIDEO_YOUTUBE_ID);
		if (youtubeId == null) {
			videoThumb = parser.getAttributeValue(ns, TopicXmlTags.VIDEO_THUMB_ATTRIBUTE);
		} else {
			videoThumb = "http://img.youtube.com/vi/" + youtubeId + "/hqdefault.jpg";
		}
		String tempVideoDesc = null;
		String tempVideoTitle = null;
		while (parser.next() != XmlPullParser.END_TAG) {// topic end tag
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Starts by looking for the entry tag
			if (name.equals(TopicXmlTags.VIDEO_DESCRIPTION_TAG)) {
				tempVideoDesc = readText(parser);
			} else if (name.equals(TopicXmlTags.VIDEO_TITLE_TAG)) {
				tempVideoTitle = readText(parser);
			} else {
				skip(parser);
			}
		}
		final String videoDesc = tempVideoDesc;
		final String videoTitle = tempVideoTitle;
		parser.require(XmlPullParser.END_TAG, ns, TopicXmlTags.VIDEO_TAG);
		sb.append(videoTemplate.execute(new Object() {
			String	id				= _id;
			String	thumbnail_path	= videoThumb;
			String	caption			= videoTitle;
			String	desc			= videoDesc;
		}));
	}

	private void processAudio(XmlPullParser parser, StringBuilder sb) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, ns, TopicXmlTags.AUDIO_TAG);
		final String _id = parser.getAttributeValue(ns, TopicXmlTags.ID_ATTRIBUTE);
		String tempAudioDesc = null;
		String tempAudioTitle = "";
		while (parser.next() != XmlPullParser.END_TAG) {// topic end tag
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Starts by looking for the entry tag
			if (name.equals(TopicXmlTags.AUDIO_DESCRIPTION_TAG)) {
				tempAudioDesc = readText(parser);
			} else if (name.equals(TopicXmlTags.AUDIO_TITLE_TAG)) {
				tempAudioTitle = readText(parser);
			} else {
				skip(parser);
			}
		}
		final String audioDesc = tempAudioDesc;
		final String audioTitle = tempAudioTitle;
		parser.require(XmlPullParser.END_TAG, ns, TopicXmlTags.AUDIO_TAG);
		sb.append(audioTemplate.execute(new Object() {
			String	id		= _id;
			String	title	= audioTitle;
			String	desc	= audioDesc;
		}));
	}

	private void processApp(XmlPullParser parser, StringBuilder sb, File topicDir) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, ns, TopicXmlTags.APP_TAG);
		final String _id = parser.getAttributeValue(ns, TopicXmlTags.ID_ATTRIBUTE);
        final String pkgName = parser.getAttributeValue(ns, TopicXmlTags.APP_PACKAGE_ATTRIBUTE);
        final String src = parser.getAttributeValue(ns, TopicXmlTags.SRC_ATTRIBUTE);
		final String appThumb;
        if(AppUtil.isPackageInstalled(pkgName, context))
            appThumb = AppUtil.getAppIconUrl(pkgName);
        else
            appThumb = AppUtil.getApkIconUrl(new File(topicDir, src).getAbsolutePath());
		String tempVmDesc = null;
		String tempVmTitle = null;
		while (parser.next() != XmlPullParser.END_TAG) {// topic end tag
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Starts by looking for the entry tag
			if (name.equals(TopicXmlTags.APP_DESCRIPTION_TAG)) {
				tempVmDesc = readText(parser);
			} else if (name.equals(TopicXmlTags.APP_TITLE_TAG)) {
				tempVmTitle = readText(parser);
			} else {
				skip(parser);
			}
		}
		final String vmDesc = tempVmDesc;
		final String vmTitle = tempVmTitle;
		parser.require(XmlPullParser.END_TAG, ns, TopicXmlTags.APP_TAG);
		sb.append(videoTemplate.execute(new Object() {
			String	id				= _id;
			String	thumbnail_path	= appThumb;
			String	caption			= vmTitle;
			String	desc			= vmDesc;
		}));
	}

	private void processHtml(XmlPullParser parser, StringBuilder sb) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, ns, TopicXmlTags.HTML_TAG);
		final String boxType = parser.getAttributeValue(ns, TopicXmlTags.HTML_BOXTYPE_ATTRIBUTE);
		final String boxTitle = parser.getAttributeValue(ns, TopicXmlTags.HTML_BOXTITLE_ATTRIBUTE);
		String tempData = null;
		while (parser.next() != XmlPullParser.END_TAG) {// topic end tag
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Starts by looking for the entry tag
			if (name.equals(TopicXmlTags.HTML_DATA_TAG)) {
				// get to cdata tag:
				tempData = readText(parser);
			} else {
				skip(parser);
			}
		}
		final String data = tempData;// StringEscapeUtils.unescapeXml(tempData);
		parser.require(XmlPullParser.END_TAG, ns, TopicXmlTags.HTML_TAG);
		if (StringUtils.isEmpty(boxType) || boxType.equalsIgnoreCase(TopicXmlTags.HTML_BOXTYPE_NONE) || boxType.equalsIgnoreCase("null")) {
			sb.append(textTemplate.execute(new Object() {
				String	content	= data;
			}));
		} else if (boxType.equalsIgnoreCase(TopicXmlTags.HTML_BOXTYPE_INFO)) {
			sb.append(box1Template.execute(new Object() {
				String	title	= boxTitle;
				String	content	= data;
			}));
		}
	}

	private void processSubtopic(XmlPullParser parser, StringBuilder sb) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, ns, TopicXmlTags.SUBTOPIC_TAG);
		final String _id = parser.getAttributeValue(ns, TopicXmlTags.ID_ATTRIBUTE);
		final String subheading = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, TopicXmlTags.SUBTOPIC_TAG);
		sb.append(subTopicTitleTemplate.execute(new Object() {
			String	id		= _id;
			String	title	= subheading;
		}));
	}

	private void processSubheading(XmlPullParser parser, StringBuilder sb) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, ns, TopicXmlTags.SUBHEADER_TAG);
		final String _id = parser.getAttributeValue(ns, TopicXmlTags.ID_ATTRIBUTE);
		final String subheading = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, TopicXmlTags.SUBHEADER_TAG);
		sb.append(subHeadingTemplate.execute(new Object() {
			String	id		= _id;
			String	title	= subheading;
		}));
	}

	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}

	private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
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

	private static String getTemplateText(Context contex, String fileName) {
		try {
			return Util.getInputString(contex.getAssets().open(TEMPLATE_PATH + fileName));
		} catch (IOException e) {
			Log.e(TAG, "error reading template", e);
		}
		return null;
	}
}
