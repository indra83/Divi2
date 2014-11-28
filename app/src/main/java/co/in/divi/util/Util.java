package co.in.divi.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import co.in.divi.activity.AssessmentActivity;
import co.in.divi.activity.LearnActivity;
import co.in.divi.activity.ListAssessmentsActivity;
import co.in.divi.content.DiviReference;
import co.in.divi.content.Node;

public final class Util {
	private static final String	TAG					= "Util";

	public static final String	INTENT_EXTRA_BOOK	= "INTENT_EXTRA_BOOK";
	public static final String	INTENT_EXTRA_URI	= "INTENT_EXTRA_URI";

	// public static long getTimestamp() {
	// return System.currentTimeMillis() / 1000;
	// }

	public static long getTimestampMillis() {
		return System.currentTimeMillis();
	}

	public static boolean isNetworkOn(Context context) {
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected())
			return true;
		else
			return false;
	}

	public static boolean isVMExists(PackageManager pm, String appPackage, int appVersionCode) {
		try {
			PackageInfo info = pm.getPackageInfo(appPackage, PackageManager.GET_META_DATA);
			if (info.versionCode < appVersionCode) {
				return false;
			}
		} catch (NameNotFoundException e) {
			return false;
		}
		return true;
	}

    public static int getCodeFromClassId(int classId) {
        return classId;
    }

    public static int getClassIdFromCode(int code) {
        return code;
    }

	public static URL convertToURLEscapingIllegalCharacters(String string) {
		try {
			String decodedURL = URLDecoder.decode(string, "UTF-8");
			URL url = new URL(decodedURL);
			URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(),
					url.getRef());
			return uri.toURL();
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static Node getTopicNodeFromIndex(Node[] chapters, int index) {
		Node newTopic = null;
		int count = 0;
		for (Node n : chapters) {
			for (Node t : n.getChildren()) {
				if (index == count) {
					newTopic = t;
					break;
				}
				count++;
			}
			if (newTopic != null)
				break;
		}
		return newTopic;
	}

	public static void openInstruction(Context context, Uri uri) {
		DiviReference ref = new DiviReference(uri);
		if (ref != null) {
			Log.d(TAG, "link clicked:" + ref);
			if (ref.type == DiviReference.REFERENCE_TYPE_TOPIC) {
				Intent intent = new Intent(context, LearnActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.putExtra(Util.INTENT_EXTRA_URI, ref.getUri().toString());
				context.startActivity(intent);
			} else if (ref.type == DiviReference.REFERENCE_TYPE_ASSESSMENT) {
				// Toast.makeText(context, "Remote command from teacher, begin exercise : " + name,
				// Toast.LENGTH_LONG).show();
				Intent intent;
				if (ref.subItemId == null)
					intent = new Intent(context, ListAssessmentsActivity.class);
				else
					intent = new Intent(context, AssessmentActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.putExtra(Util.INTENT_EXTRA_URI, ref.getUri().toString());
				context.startActivity(intent);
			}
		}
	}

	public static String getInputString(InputStream is) throws IOException {
		if (is == null)
			return "";
		BufferedReader in = new BufferedReader(new InputStreamReader(is), 8192);
		String line;
		StringBuilder sb = new StringBuilder();

		try {
			while ((line = in.readLine()) != null)
				sb.append(line);
		} finally {
			is.close();
		}
		return sb.toString();
	}

	public static int getAccuracyColor(int startColor, int endColor, int percentValue) {
		// int sr = Color.red(startColor);
		// int sg = Color.green(startColor);
		// int sb = Color.blue(startColor);
		//
		// int er = Color.red(endColor);
		// int eg = Color.green(endColor);
		// int eb = Color.blue(endColor);
		//
		// int cr = sr + ((er - sr) * percentValue) / 100;
		// int cg = sg + ((eg - sg) * percentValue) / 100;
		// int cb = sb + ((eb - sb) * percentValue) / 100;
		//
		// return Color.rgb(cr, cg, cb);
		if (percentValue < 50)
			return Color.parseColor("#c80000");
		else if (percentValue < 60)
			return Color.parseColor("#cc3300");
		else if (percentValue < 70)
			return Color.parseColor("#cc6600");
		else if (percentValue < 80)
			return Color.parseColor("#cccc00");
		else if (percentValue < 90)
			return Color.parseColor("#ccff00");
		else
			return Color.parseColor("#66ff00");
	}

	// Copies src file to dst file.
	// If the dst file does not exist, it is created
	public static void copy(InputStream in, File dst) throws IOException {
		// InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.flush();
		out.close();
	}

	public static boolean unzip(InputStream zipFileStream, File outputDir) {
		byte[] buffer = new byte[8192];
		int length;
		try {
			ZipInputStream zin = new ZipInputStream(zipFileStream);
			ZipEntry ze = null;
			while ((ze = zin.getNextEntry()) != null) {
				Log.d("Decompress", " -Unzipping " + ze.getName());

				if (ze.isDirectory()) {
					File f = new File(outputDir.getAbsolutePath() + "/" + ze.getName());

					// if (!f.isDirectory()) {
					f.mkdirs();
					// }

				} else {
					File toCreate = new File(outputDir, ze.getName());
					toCreate.getParentFile().mkdirs();
					FileOutputStream fout = new FileOutputStream(toCreate);
					while ((length = zin.read(buffer)) > 0) {
						fout.write(buffer, 0, length);
					}
					fout.close();
				}
				// zin.closeEntry();
			}
			zin.close();
			return true;
		} catch (Exception e) {
			Log.e("Decompress", "unzip", e);
			return false;
		}
	}

	public static String openJSONFile(File jsonFile) {
		String ret = null;
		try {
			InputStream is = new FileInputStream(jsonFile);
			String line = "";
			StringBuilder total = new StringBuilder();

			// Wrap a BufferedReader around the InputStream
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));

			// Read response until the end
			while ((line = rd.readLine()) != null) {
				total.append(line);
			}
			ret = total.toString();
		} catch (IOException ioe) {
			Log.e(TAG, "error reading file", ioe);
			return null;
		}
		// Return full string
		return ret;
	}

	public static void copyFolder(File src, File dest) throws IOException {
		if (src.isDirectory()) {

			// if directory not exists, create it
			if (!dest.exists()) {
				dest.mkdir();
				Log.d(TAG, "Directory copied from " + src + "  to " + dest);
			}

			// list all the directory contents
			String files[] = src.list();

			for (String file : files) {
				// construct the src and dest file structure
				File srcFile = new File(src, file);
				File destFile = new File(dest, file);
				// recursive copy
				copyFolder(srcFile, destFile);
			}

		} else {
			// if file, then copy it
			// Use bytes stream to support all file types
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dest);

			byte[] buffer = new byte[8 * 1024];

			int length;
			// copy the file content in bytes
			while ((length = in.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}

			in.close();
			out.close();
			Log.d(TAG, "File copied from " + src + " to " + dest);
		}
	}

	public static boolean moveFolder(File src, File dest) throws IOException {
		if (src.isDirectory()) {

			// if directory not exists, create it
			if (!dest.exists()) {
				dest.mkdirs();
				Log.d(TAG, "Directory moved from " + src + "  to " + dest);
			}

			// list all the directory contents
			String files[] = src.list();

			boolean ret = true;
			for (String file : files) {
				// construct the src and dest file structure
				File srcFile = new File(src, file);
				File destFile = new File(dest, file);
				// recursive move
				ret = ret && moveFolder(srcFile, destFile);
				if (!ret) {
					Log.d(TAG, "move failed - " + srcFile + " to " + destFile);
					break;
				}
			}
			return ret;
		} else {
			// if file, then rename it
			return src.renameTo(dest);
		}
	}

	public static void deleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory())
			for (File child : fileOrDirectory.listFiles())
				deleteRecursive(child);

		fileOrDirectory.delete();
	}

	/** Transform ISO 8601 string to Calendar. */
	public static Calendar iso8601ToCalendar(final String iso8601string) throws IllegalArgumentException {
		Calendar calendar = GregorianCalendar.getInstance();
		String s = iso8601string.replace("Z", "+00:00");
		try {
			s = s.substring(0, 22) + s.substring(23); // to get rid of the ":"

			Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(s);
			calendar.setTime(date);
			return calendar;
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid length", e);
		}
	}
}
