package co.in.divi.vms.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;

public class ChallengeXmlParser {
	private static final String	TAG	= ChallengeXmlParser.class.getSimpleName();
	private static final String	ns	= null;

	public HashMap<String, VMChallenges> getVMsFromXml(XmlPullParser parser) {
		HashMap<String, VMChallenges> toReturn = new HashMap<String, VMChallenges>();
		try {
			parser.next();
			parser.next();
			Log.d(TAG, "tag:" + parser.getEventType());
			parser.require(XmlPullParser.START_TAG, ns, "challenges");
			while (parser.next() != XmlPullParser.END_TAG) {// topic end tag
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
				}
				String name = parser.getName();
				if (name.equals(ChallengeXmlTags.VM_TAG)) {
					VMChallenges vm = readVM(parser);
					toReturn.put(vm.id, vm);
				} else {
					skip(parser);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "error parsing", e);
		}
		return toReturn;
	}

	private VMChallenges readVM(XmlPullParser parser) throws XmlPullParserException, IOException {
		Log.d(TAG, "readVM");
		parser.require(XmlPullParser.START_TAG, ns, ChallengeXmlTags.VM_TAG);
		String id = parser.getAttributeValue(ns, ChallengeXmlTags.ID_ATTRIBUTE);
		String title = parser.getAttributeValue(ns, ChallengeXmlTags.VM_TITLE_ATTRIBUTE);
		ArrayList<Challenge> challenges = new ArrayList<Challenge>();
		while (parser.next() != XmlPullParser.END_TAG) {// image end tag
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Log.d(TAG, "name:" + name);
			// Starts by looking for the entry tag
			if (name.equals(ChallengeXmlTags.CHALLENGE_TAG)) {
				challenges.add(readChallenge(parser));
			} else {
				skip(parser);
			}
		}
		VMChallenges vm = new VMChallenges();
		vm.id = id;
		vm.title = title;
		vm.challenges = challenges.toArray(new Challenge[challenges.size()]);
		return vm;
	}

	private Challenge readChallenge(XmlPullParser parser) throws XmlPullParserException, IOException {
		Log.d(TAG, "readChallenge");
		parser.require(XmlPullParser.START_TAG, ns, ChallengeXmlTags.CHALLENGE_TAG);
		Challenge challenge = new Challenge();
		challenge.id = parser.getAttributeValue(ns, ChallengeXmlTags.ID_ATTRIBUTE);
		while (parser.next() != XmlPullParser.END_TAG) {// VIDEO end tag
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Log.d(TAG, "name:" + name);
			// Starts by looking for the entry tag
			if (name.equals(ChallengeXmlTags.CHALLENGE_TITLE_TAG)) {
				challenge.title = readText(parser);
			} else if (name.equals(ChallengeXmlTags.CHALLENGE_DESCRIPTION_TAG)) {
				challenge.description = readText(parser);
			} else {
				skip(parser);
			}
		}
		return challenge;
	}

	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
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
