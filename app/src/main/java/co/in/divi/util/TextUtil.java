package co.in.divi.util;

import android.graphics.Color;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;

import co.in.divi.LectureSessionProvider.LocationHolder;
import co.in.divi.R;
import co.in.divi.content.DiviReference;
import co.in.divi.model.Instruction;

public class TextUtil {

	private static final String	TEMPLATE_LOCATION_BLACK		= "<font color='#1F1F1F'>%s</font>";
	private static final String	TEMPLATE_LOCATION_GREEN		= "<font color='#1FFF1F'>%s</font>";
	private static final String	TEMPLATE_LOCATION_ORANGE	= "<font color='#FFAF1F'>%s</font>";
	private static final String	TEMPLATE_LOCATION_RED		= "<font color='#FF1F1F'>%s</font>";

	public static String trimPTag(String html) {
		if (html.startsWith("<p>") && html.endsWith("</p>")) {
			return html.substring(3, html.length() - 4);
		} else if (html.startsWith("<p style=\"text-align: justify;\">") && html.endsWith("</p>")) {
			return html.substring(32, html.length() - 4);
		} else if (html.startsWith("<p style=\"text-align: center;\">") && html.endsWith("</p>")) {
			return html.substring(31, html.length() - 4);
		}
		return html;
	}

	public static Spanned getAttemptsText(int correctAttempts, int wrongAttempts) {
		return Html.fromHtml(String.format("Attempts  <b><font color='#006600'>%d</font> / <font color='#CC0000'>%d</font></b>",
				correctAttempts, wrongAttempts));
	}

	public static Spanned getAttemptsText2(int correctAttempts, int wrongAttempts) {
		return Html.fromHtml(String.format("<b><font color='#006600'>%d</font> / <font color='#CC0000'>%d</font></b>", correctAttempts,
				wrongAttempts));
	}

	public static Spanned getPointsText(int curPoints, int totalPoints) {
		return Html.fromHtml(String.format("Points  <b>%d / %d</b>", curPoints, totalPoints));
	}

	public static String getTimeText(long millis) {
		int mins = (int) millis / (1000 * 60);
		return String.format("%02d : %02d", mins / 60, mins % 60);
	}

	public static int getAccuracyBg(int accuracy) {
		if (accuracy <= 50) {
			return R.drawable.bg_accuracy_1;
		} else if (accuracy <= 60) {
			return R.drawable.bg_accuracy_2;
		} else if (accuracy <= 70) {
			return R.drawable.bg_accuracy_3;
		} else if (accuracy <= 80) {
			return R.drawable.bg_accuracy_4;
		} else if (accuracy <= 90) {
			return R.drawable.bg_accuracy_5;
		} else if (accuracy <= 95) {
			return R.drawable.bg_accuracy_6;
		} else if (accuracy <= 100) {
			return R.drawable.bg_accuracy_7;
		} else {
			return R.drawable.bg_accuracy_0;
		}
	}

	public static int getAccuracyBgColor(int accuracy) {
		if (accuracy <= 50) {
			return Color.parseColor("#97511e");
		} else if (accuracy <= 60) {
			return Color.parseColor("#c64c1c");
		} else if (accuracy <= 70) {
			return Color.parseColor("#e5960a");
		} else if (accuracy <= 80) {
			return Color.parseColor("#f9e132");
		} else if (accuracy <= 90) {
			return Color.parseColor("#d1db07");
		} else if (accuracy <= 95) {
			return Color.parseColor("#a1da55");
		} else if (accuracy <= 100) {
			return Color.parseColor("#00be5a");
		} else {
			return Color.parseColor("#808285");
		}
	}

	public static Spanned getLocationText(Instruction curInstruction, LocationHolder studentLoc) {
		String ret;
		switch (studentLoc.locationType) {
		case BLACKOUT:
			ret = String.format(TEMPLATE_LOCATION_BLACK, "Blackout");
			break;
		case HOME:
			ret = String.format(TEMPLATE_LOCATION_BLACK, "Home");
			break;
		case OFF:
			ret = String.format(TEMPLATE_LOCATION_BLACK, "Off");
			break;
		case ASSESSMENT:
			if (curInstruction != null && curInstruction.type == Instruction.INSTRUCTION_TYPE_NAVIGATE) {
				DiviReference instructionRef = new DiviReference(Uri.parse(curInstruction.location));
				DiviReference locRef = new DiviReference(Uri.parse(studentLoc.locationUri));
				if (!instructionRef.courseId.equals(locRef.courseId) || !instructionRef.bookId.equals(locRef.bookId)) {
					ret = String.format(TEMPLATE_LOCATION_RED, studentLoc.breadcrumb.toString());
				} else {
					if (!instructionRef.itemId.equals(locRef.itemId)) {
						ret = String.format(TEMPLATE_LOCATION_ORANGE, studentLoc.breadcrumb.toItemString());
					} else {
						ret = String.format(TEMPLATE_LOCATION_GREEN, studentLoc.breadcrumb.subItemName == null ? "Ok"
								: studentLoc.breadcrumb.subItemName);
					}
				}
			} else {
				ret = String.format(TEMPLATE_LOCATION_BLACK, studentLoc.breadcrumb.toString());
			}
			break;
		case TOPIC:
			if (curInstruction != null && curInstruction.type == Instruction.INSTRUCTION_TYPE_NAVIGATE) {
				DiviReference instructionRef = new DiviReference(Uri.parse(curInstruction.location));
				DiviReference locRef = new DiviReference(Uri.parse(studentLoc.locationUri));
				if (!instructionRef.courseId.equals(locRef.courseId) || !instructionRef.bookId.equals(locRef.bookId)) {
					ret = String.format(TEMPLATE_LOCATION_RED, studentLoc.breadcrumb.toString());
				} else {
					if (!instructionRef.itemId.equals(locRef.itemId)) {
						ret = String.format(TEMPLATE_LOCATION_ORANGE, studentLoc.breadcrumb.toItemString());
					} else {
						ret = String.format(TEMPLATE_LOCATION_GREEN, studentLoc.breadcrumb.subItemName == null ? "Ok"
								: studentLoc.breadcrumb.subItemName);
					}
				}
			} else {
				ret = String.format(TEMPLATE_LOCATION_BLACK, studentLoc.breadcrumb.toString());
			}
			break;
		case UNKNOWN:
            ret = String.format(TEMPLATE_LOCATION_RED, studentLoc.externalAppName == null ? "Unknown" : studentLoc.externalAppName);
			if (curInstruction != null && curInstruction.type == Instruction.INSTRUCTION_TYPE_NAVIGATE_EXTERNAL) {
				if (curInstruction.location.equals(studentLoc.locationUri)) {
					ret = String.format(TEMPLATE_LOCATION_GREEN, "Ok");
				}
			}
            break;
		default:
			ret = String.format(TEMPLATE_LOCATION_RED, "n / a");
			break;
		}
		return Html.fromHtml(ret);
	}
}
