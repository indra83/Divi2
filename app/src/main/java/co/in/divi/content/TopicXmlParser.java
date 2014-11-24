package co.in.divi.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import android.util.Xml;
import co.in.divi.content.Topic.Audio;
import co.in.divi.content.Topic.Image;
import co.in.divi.content.Topic.ImageSet;
import co.in.divi.content.Topic.ImageSet.ImageItem;
import co.in.divi.content.Topic.Section;
import co.in.divi.content.Topic.VM;
import co.in.divi.content.Topic.Video;
import co.in.divi.content.importer.BookDefinition.TopicNode;
import co.in.divi.util.LogConfig;

import com.google.gson.Gson;

public class TopicXmlParser {
	private static final String	TAG	= TopicXmlParser.class.getSimpleName();
	private static final String	ns	= null;

	public TopicNode getNodeFromXml(String title, File topicXmlFile, String relFilePath) {
		if (LogConfig.DEBUG_XML) {
			Log.d(TAG, "processing file: " + topicXmlFile.getAbsolutePath());
			Log.d(TAG, "rel path: " + relFilePath);
		}
		TopicNode nodeToReturn = new TopicNode();
		Topic topicData = new Topic();
		ArrayList<Section> sections = new ArrayList<Topic.Section>();
		ArrayList<Video> videos = new ArrayList<Topic.Video>();
		ArrayList<Image> images = new ArrayList<Topic.Image>();
		ArrayList<Audio> audios = new ArrayList<Topic.Audio>();
		ArrayList<Topic.VM> vms = new ArrayList<Topic.VM>();
		ArrayList<ImageSet> imageSets = new ArrayList<Topic.ImageSet>();
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(new FileInputStream(topicXmlFile), null);
			parser.nextTag();

			parser.require(XmlPullParser.START_TAG, ns, "topic");
			nodeToReturn.id = parser.getAttributeValue(ns, "id");
			nodeToReturn.name = title;
			while (parser.next() != XmlPullParser.END_TAG) {// topic end tag
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				if (name.equals(TopicXmlTags.SUBHEADER_TAG)) {
					// add section
					skip(parser);
				} else if (name.equals(TopicXmlTags.SUBTOPIC_TAG)) {
					// add heading
					sections.add(fillSection(parser));
				} else if (name.equals(TopicXmlTags.IMAGE_TAG)) {
					// add IMAGE
					images.add(fillImage(parser));
				} else if (name.equals(TopicXmlTags.VIDEO_TAG)) {
					// add VIDEO
					videos.add(fillVideo(parser));
				} else if (name.equals(TopicXmlTags.AUDIO_TAG)) {
					// add AUDIO
					audios.add(fillAudio(parser));
				} else if (name.equals(TopicXmlTags.IMAGESET_TAG)) {
					// add IMAGESET
					imageSets.add(fillImageSet(parser));
				} else if (name.equals(TopicXmlTags.VM_TAG)) {
					// add VIDEO
					vms.add(fillVM(parser));
				} else {
					skip(parser);
				}
			}
			topicData.title = title;
			topicData.pagePath = relFilePath;
			topicData.sections = sections.toArray(new Section[0]);
			topicData.videos = videos.toArray(new Video[0]);
			topicData.images = images.toArray(new Image[0]);
			topicData.audios = audios.toArray(new Audio[0]);
			topicData.imageSets = imageSets.toArray(new ImageSet[0]);
			topicData.vms = vms.toArray(new VM[0]);

			nodeToReturn.content = new Gson().toJson(topicData);

			if (LogConfig.DEBUG_XML)
				Log.d(TAG, "topic content:" + nodeToReturn.content);
			return nodeToReturn;
		} catch (Exception e) {
			Log.w(TAG, "error parsing topic xml:", e);
		}
		return null;
	}

	private Section fillSection(XmlPullParser parser) throws IOException, XmlPullParserException {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "fillSection");
		parser.require(XmlPullParser.START_TAG, ns, TopicXmlTags.SUBTOPIC_TAG);
		String id = parser.getAttributeValue(ns, TopicXmlTags.ID_ATTRIBUTE);
		String title = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, TopicXmlTags.SUBTOPIC_TAG);
		Section section = new Section();
		section.id = id;
		section.title = title;
		return section;
	}

	private Image fillImage(XmlPullParser parser) throws IOException, XmlPullParserException {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "fillImage");
		parser.require(XmlPullParser.START_TAG, ns, TopicXmlTags.IMAGE_TAG);
		String id = parser.getAttributeValue(ns, TopicXmlTags.ID_ATTRIBUTE);
		String src = parser.getAttributeValue(ns, TopicXmlTags.SRC_ATTRIBUTE);

		Image image = new Image();
		image.id = id;
		image.src = src;
		image.allowFullscreen = "true".equalsIgnoreCase(parser.getAttributeValue(ns, TopicXmlTags.IMAGE_FULLSCREEN_ATTRIBUTE));
		// get description
		while (parser.next() != XmlPullParser.END_TAG) {// image end tag
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Log.d(TAG, "name:" + name);
			// Starts by looking for the entry tag
			if (name.equals(TopicXmlTags.IMAGE_DESCRIPTION_TAG)) {
				image.desc = readText(parser);
			} else if (name.equals(TopicXmlTags.IMAGE_TITLE_TAG)) {
				image.title = readText(parser);
			} else {
				skip(parser);
			}
		}
		image.hideInToc = "true".equalsIgnoreCase(parser.getAttributeValue(ns, TopicXmlTags.HIDETOC_ATTRIBUTE));
		return image;
	}

	private Video fillVideo(XmlPullParser parser) throws IOException, XmlPullParserException {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "fillVideo");
		parser.require(XmlPullParser.START_TAG, ns, TopicXmlTags.VIDEO_TAG);
		String id = parser.getAttributeValue(ns, TopicXmlTags.ID_ATTRIBUTE);
		String src = parser.getAttributeValue(ns, TopicXmlTags.SRC_ATTRIBUTE);
		String thumb = parser.getAttributeValue(ns, TopicXmlTags.VIDEO_THUMB_ATTRIBUTE);
		String youtubeId = parser.getAttributeValue(ns, TopicXmlTags.VIDEO_YOUTUBE_ID);

		Video video = new Video();
		video.id = id;
		video.src = src;
		video.thumb = thumb;
		video.youtubeId = youtubeId;
		// get description
		while (parser.next() != XmlPullParser.END_TAG) {// VIDEO end tag
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Log.d(TAG, "name:" + name);
			// Starts by looking for the entry tag
			if (name.equals(TopicXmlTags.VIDEO_DESCRIPTION_TAG)) {
				video.desc = readText(parser);
			} else if (name.equals(TopicXmlTags.VIDEO_TITLE_TAG)) {
				video.title = readText(parser);
			} else {
				skip(parser);
			}
		}
		video.hideInToc = "true".equalsIgnoreCase(parser.getAttributeValue(ns, TopicXmlTags.HIDETOC_ATTRIBUTE));
		return video;
	}

	private Audio fillAudio(XmlPullParser parser) throws IOException, XmlPullParserException {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "fillAudio");
		parser.require(XmlPullParser.START_TAG, ns, TopicXmlTags.AUDIO_TAG);
		String id = parser.getAttributeValue(ns, TopicXmlTags.ID_ATTRIBUTE);
		String src = parser.getAttributeValue(ns, TopicXmlTags.SRC_ATTRIBUTE);

		Audio audio = new Audio();
		audio.id = id;
		audio.src = src;
		// get description
		while (parser.next() != XmlPullParser.END_TAG) {// AUDIO end tag
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Log.d(TAG, "name:" + name);
			// Starts by looking for the entry tag
			if (name.equals(TopicXmlTags.AUDIO_DESCRIPTION_TAG)) {
				audio.desc = readText(parser);
			} else if (name.equals(TopicXmlTags.AUDIO_TITLE_TAG)) {
				audio.title = readText(parser);
			} else {
				skip(parser);
			}
		}
		audio.hideInToc = "true".equalsIgnoreCase(parser.getAttributeValue(ns, TopicXmlTags.HIDETOC_ATTRIBUTE));
		return audio;
	}

	private VM fillVM(XmlPullParser parser) throws IOException, XmlPullParserException {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "fillVM");
		parser.require(XmlPullParser.START_TAG, ns, TopicXmlTags.VM_TAG);
		String id = parser.getAttributeValue(ns, TopicXmlTags.ID_ATTRIBUTE);
		String src = parser.getAttributeValue(ns, TopicXmlTags.SRC_ATTRIBUTE);
		String thumb = parser.getAttributeValue(ns, TopicXmlTags.VM_THUMB_ATTRIBUTE);

		VM vm = new VM();
		vm.id = id;
		vm.src = src;
		vm.thumb = thumb;
		vm.appPackage = parser.getAttributeValue(ns, TopicXmlTags.VM_APP_PACKAGE_ATTRIBUTE);
		vm.appVersionCode = Integer.parseInt(parser.getAttributeValue(ns, TopicXmlTags.VM_APP_VERSIONCODE_ATTRIBUTE));
		vm.appActivityName = parser.getAttributeValue(ns, TopicXmlTags.VM_APP_ACTIVITY_NAME_ATTRIBUTE);
		vm.showInApps = Boolean.parseBoolean(parser.getAttributeValue(ns, TopicXmlTags.VM_SHOW_IN_APPS_ATTRIBUTE));
		// get description
		while (parser.next() != XmlPullParser.END_TAG) {// VIDEO end tag
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Log.d(TAG, "name:" + name);
			// Starts by looking for the entry tag
			if (name.equals(TopicXmlTags.VM_DESCRIPTION_TAG)) {
				vm.desc = readText(parser);
			} else if (name.equals(TopicXmlTags.VM_TITLE_TAG)) {
				vm.title = readText(parser);
			} else {
				skip(parser);
			}
		}
		vm.hideInToc = "true".equalsIgnoreCase(parser.getAttributeValue(ns, TopicXmlTags.HIDETOC_ATTRIBUTE));
		return vm;
	}

	private ImageSet fillImageSet(XmlPullParser parser) throws IOException, XmlPullParserException {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "fillImageSet");
		parser.require(XmlPullParser.START_TAG, ns, TopicXmlTags.IMAGESET_TAG);
		String id = parser.getAttributeValue(ns, TopicXmlTags.ID_ATTRIBUTE);
		String title = parser.getAttributeValue(ns, TopicXmlTags.IMAGESET_TITLE_ATTRIBUTE);

		ImageSet imageSet = new ImageSet();
		imageSet.id = id;
		imageSet.title = title;
		ArrayList<ImageItem> items = new ArrayList<Topic.ImageSet.ImageItem>();
		while (parser.next() != XmlPullParser.END_TAG) {// image end tag
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Log.d(TAG, "name:" + name);
			// Starts by looking for the entry tag
			if (name.equals(TopicXmlTags.IMAGESET_ITEM_TAG)) {
				items.add(fillImageItem(parser));
			} else {
				skip(parser);
			}
		}
		imageSet.imagesItems = items.toArray(new ImageItem[0]);
		return imageSet;
	}

	private ImageItem fillImageItem(XmlPullParser parser) throws IOException, XmlPullParserException {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "fillImageSet");
		parser.require(XmlPullParser.START_TAG, ns, TopicXmlTags.IMAGESET_ITEM_TAG);
		String src = parser.getAttributeValue(ns, TopicXmlTags.SRC_ATTRIBUTE);
		ImageItem item = new ImageItem();
		item.src = src;
		// get description
		while (parser.next() != XmlPullParser.END_TAG) {// image end tag
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Log.d(TAG, "name:" + name);
			// Starts by looking for the entry tag
			if (name.equals(TopicXmlTags.IMAGE_DESCRIPTION_TAG)) {
				item.desc = readText(parser);
				// } else if (name.equals(TopicXmlTags.IMAGE_TITLE_TAG)) {
				// image.title = readText(parser);
			} else {
				skip(parser);
			}
		}
		return item;
	}

	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
		if (LogConfig.DEBUG_XML)
			Log.d(TAG, "readText");
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
}
