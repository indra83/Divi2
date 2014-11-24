package co.in.divi.logs;

import java.util.ArrayList;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;
import co.in.divi.logs.LogsDBContract.Logs;

public class LogsRecorderService extends IntentService {
	private static final String	TAG					= LogsRecorderService.class.getSimpleName();

	public static final String	INTENT_EXTRA_LOGS	= "INTENT_EXTRA_LOGS";

	private ContentResolver		contentResolver;

	public LogsRecorderService() {
		super("LogsRecorderService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		contentResolver = getContentResolver();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			Log.d(TAG, "inserting:");
			ArrayList<DiviLog> logs = intent.getParcelableArrayListExtra(INTENT_EXTRA_LOGS);

			if (logs != null) {
				ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
				for (DiviLog log : logs) {
					Log.d(TAG, "log:::" + log.toString());
					ops.add(ContentProviderOperation.newInsert(Logs.CONTENT_URI).withValues(log.toCV()).build());
					// addKeenEvent(log);
				}
				if (ops.size() > 0)
					try {
						contentResolver.applyBatch(LogsDBContract.AUTHORITY, ops);
					} catch (RemoteException e) {
						e.printStackTrace();
					} catch (OperationApplicationException e) {
						e.printStackTrace();
					}
				printLogsSize();
				// uploadToKeen();
			} else {
				Log.w(TAG, "No logs added to intent!");
			}
		} catch (Exception e) {
			Log.w(TAG, "Error saving logs", e);
		}
	}

	// private void uploadToKeen() {
	// KeenClient.client().upload(new UploadFinishedCallback() {
	// @Override
	// public void callback() {
	// Log.d(TAG, "upload finished?");
	// }
	// });
	// }
	//
	// private void addKeenEvent(DiviLog log) {
	// // TODO: imp constraint
	// if (log.duration < 2000)
	// return; // ignore navigation events
	// // create an event to eventually upload to Keen
	// Map<String, Object> event = new HashMap<String, Object>();
	// Map<String, Object> userData = new HashMap<String, Object>();
	// Map<String, Object> resourceData = new HashMap<String, Object>();
	// userData.put("uid", log.uid);
	// userData.put("token", log.token);
	//
	// DiviReference diviRef = new DiviReference(Uri.parse(log.uri));
	// Log.d(TAG, "inserting  :" + log.resourceType);
	// Log.d(TAG, "inserting  :" + diviRef);
	// resourceData.put("courseId", diviRef.courseId);
	// resourceData.put("bookId", diviRef.bookId);
	// resourceData.put("itemId", diviRef.itemId);
	// resourceData.put("subItemId", diviRef.subItemId);
	// resourceData.put("fragment", diviRef.fragment);
	// resourceData.put("type", log.resourceType.toString());
	//
	// event.put("user", userData);
	// event.put("resource", resourceData);
	// event.put("duration", log.duration);
	// event.put("type", log.type);
	// // override the Keen timestamp
	// Calendar c = Calendar.getInstance();
	// c.setTimeInMillis(log.openedAt);
	// Map<String, Object> keenProperties = new HashMap<String, Object>();
	// keenProperties.put("timestamp", c);
	//
	// // add it to the "purchases" collection in your Keen Project
	// try {
	// KeenClient.client().addEvent("logs", event, keenProperties);
	// } catch (KeenException e) {
	// // handle the exception in a way that makes sense to you
	// e.printStackTrace();
	// }
	// }

	private void printLogsSize() {
		Cursor c = contentResolver.query(Logs.CONTENT_URI, Logs.PROJECTION_ALL, null, null, Logs.SORT_ORDER_DEFAULT);
		if (c.moveToFirst()) {
			Log.d(TAG, "# of logs - " + c.getCount());
		}
	}
}
