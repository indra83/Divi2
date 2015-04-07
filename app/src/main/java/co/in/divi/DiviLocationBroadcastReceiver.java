package co.in.divi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import co.in.divi.content.DiviReference;

public class DiviLocationBroadcastReceiver extends BroadcastReceiver {
	private static final String	TAG							= DiviLocationBroadcastReceiver.class.getSimpleName();

	public static final String	INTENT_LOCATION_BROADCAST	= "co.in.divi.intent.LOCATION_BROADCAST";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive: " + intent.getAction());
		try {
			String courseId = intent.getStringExtra("COURSE_ID");
			String bookId = intent.getStringExtra("BOOK_ID");
			String topicId = intent.getStringExtra("TOPIC_ID");
			String vmId = intent.getStringExtra("VM_ID");
			String[] breadcrumb = intent.getStringArrayExtra("BREADCRUMB");
			if (courseId == null || bookId == null || topicId == null || vmId == null) {
				LocationManager.getInstance(context).setNewLocation(Location.LOCATION_TYPE.UNKNOWN, null, null, null, null);
			} else {
				LocationManager.getInstance(context).setNewLocation(Location.LOCATION_TYPE.TOPIC, Location.LOCATION_SUBTYPE.TOPIC_VM,
						new DiviReference(courseId, bookId, DiviReference.REFERENCE_TYPE_TOPIC, topicId, vmId),
						Location.Breadcrumb.get(breadcrumb[0], breadcrumb[1], breadcrumb[2], breadcrumb[3], breadcrumb[4]), null);
			}
		} catch (Exception e) {
			Log.w(TAG, "error setting location from vm:", e);
		}
	}

}
