package co.in.divi.logs;

import android.content.Context;
import co.in.divi.UserSessionProvider;
import co.in.divi.util.Util;

public class LogsManager {
	private static final String	TAG			= LogsManager.class.getSimpleName();

	private static LogsManager	instance	= null;

	public static LogsManager getInstance(Context context) {
		if (instance == null) {
			instance = new LogsManager(context);
		}
		return instance;
	}

	private Context	context;

	private LogsManager(Context context) {
		this.context = context;
	}

	public long getLastSyncTime() {
		return UserSessionProvider.getInstance(context).getTimestamp(UserSessionProvider.LAST_SYNC_LOGS_TIMESTAMP);
	}

	public void setLastSyncTime(long syncTime) {
		UserSessionProvider.getInstance(context).setTimestamp(UserSessionProvider.LAST_SYNC_LOGS_TIMESTAMP, Util.getTimestampMillis());
	}
}
