package co.in.divi.content;

import java.util.List;

import android.net.Uri;
import android.util.Log;
import co.in.divi.util.Config;
import co.in.divi.util.LogConfig;

/**
 * scheme is divi://divi.co.in/<courseId>/<bookId>/[topic|exercise]/<itemId>/[<resourceId|questionId>]
 * 
 * subItemId is either questionId or resourceId based on whether its a topic or exercise
 * 
 * ALL ELEMENTS HAVE TO BE URLSAFE (alphanumeric & '_' only) itemId cannot be null (only book url not allowed!)
 * 
 * @author indraneel
 * 
 */
public class DiviReference {
	private static final String	TAG							= DiviReference.class.getName();

	public static final String	DIVI_URI_SCHEME				= "divi";
	public static final String	DIVI_URI_AUTHORITY			= "divi.co.in";

	public static final String	DIVI_TOPIC					= "topic";
	public static final String	DIVI_ASSESSMENT				= "assessment";
	// public static final String DIVI_RESOURCE = "resource";

	public static final int		REFERENCE_TYPE_TOPIC		= 0;
	public static final int		REFERENCE_TYPE_ASSESSMENT	= 1;
	// public static final int REFERENCE_TYPE_RESOURCE = 2;

	public String				courseId;
	public String				bookId;
	public int					type;
	public String				itemId;
	// questionId or resourceId based on whether its a topic/exercise
	public String				subItemId;
	public String				fragment;

	public DiviReference(Uri uri) {
		if (LogConfig.DEBUG_REFERENCE)
			Log.d(TAG, "uri:" + uri);
		if (DIVI_URI_SCHEME.equals(uri.getScheme()) && DIVI_URI_AUTHORITY.equals(uri.getAuthority())) {
			List<String> segments = uri.getPathSegments();

			if (segments.size() < 4) {
				throw new IllegalArgumentException("Not enough parts in Uri");
			}
			courseId = segments.get(0);
			bookId = segments.get(1);
			type = getType(segments.get(2));
			itemId = segments.get(3);
			subItemId = segments.size() > 4 ? segments.get(4) : null;

			fragment = uri.getFragment();

			if (LogConfig.DEBUG_REFERENCE)
				Log.d(TAG, "decoded uri: " + toString());

		} else {
			throw new IllegalArgumentException("Uri provided is invalid");
		}
	}

	public DiviReference(String courseId, String bookId, int type, String itemId, String subItemId) {
		if (courseId == null || bookId == null || itemId == null)
			throw new IllegalArgumentException("Uri provided is invalid");
		this.courseId = courseId;
		this.bookId = bookId;
		this.type = type;
		this.itemId = itemId;
		this.subItemId = subItemId;

		if (LogConfig.DEBUG_REFERENCE)
			Log.d(TAG, "decoded uri: " + toString());
	}

	public Uri getUri() {
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(DIVI_URI_SCHEME);
		builder.authority(DIVI_URI_AUTHORITY);
		builder.appendPath(courseId);
		builder.appendPath(bookId);
		builder.appendPath(getTypeString());
		builder.appendPath(itemId);
		if (subItemId != null)
			builder.appendPath(subItemId);
		if (fragment != null)
			builder.fragment(fragment);
		Uri uri = builder.build();
		if (LogConfig.DEBUG_REFERENCE)
			Log.d(TAG, "returning uri:" + uri.toString());

		return uri;
	}

	public void setFragment(String fragment) {
		this.fragment = fragment;
	}

	public boolean isTopic() {
		return type == REFERENCE_TYPE_TOPIC && itemId != null;
	}

	public boolean isExercise() {
		return type == REFERENCE_TYPE_ASSESSMENT;
	}

	public boolean isResource() {
		return isTopic() && subItemId != null;
	}

	private int getType(String type) {
		if (type.equalsIgnoreCase(DIVI_TOPIC)) {
			return REFERENCE_TYPE_TOPIC;
		} else if (type.equalsIgnoreCase(DIVI_ASSESSMENT)) {
			return REFERENCE_TYPE_ASSESSMENT;
		}
		// else if (type.equalsIgnoreCase(DIVI_RESOURCE)) {
		// return REFERENCE_TYPE_RESOURCE;
		// }
		return -1;
	}

	private String getTypeString() {
		if (type == REFERENCE_TYPE_TOPIC) {
			return DIVI_TOPIC;
		} else if (type == REFERENCE_TYPE_ASSESSMENT) {
			return DIVI_ASSESSMENT;
		}
		// else if (type == REFERENCE_TYPE_RESOURCE) {
		// return DIVI_RESOURCE;
		// }
		return null;
	}

	public boolean isSameResourceAs(DiviReference to) {
		Log.d(TAG, "comparing - " + toString() + "   ----    " + to.toString());
		if (to == null || subItemId == null)
			return false;
		boolean isEqual = courseId.equals(to.courseId) && bookId.equals(to.bookId) && type == to.type && itemId.equals(to.itemId)
				&& subItemId.equals(to.subItemId);

		return isEqual;
	}

	@Override
	public String toString() {
		return courseId + ", " + bookId + ", " + getTypeString() + ", " + itemId + ", " + subItemId + " ::: " + fragment;
	}

	@Override
	public boolean equals(Object o) {
		DiviReference to = (DiviReference) o;
		boolean isEqual = courseId.equals(to.courseId) && bookId.equals(to.bookId) && type == to.type && itemId.equals(to.itemId);
		if (subItemId == null) {
			isEqual = isEqual && (subItemId == to.subItemId);
		} else {
			isEqual = isEqual && subItemId.equals(to.subItemId);
		}
		if (fragment == null) {
			return isEqual && (fragment == to.fragment);
		} else {
			return isEqual && fragment.equals(to.fragment);
		}
	}
}
