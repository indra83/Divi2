package co.in.divi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import co.in.divi.UserSessionProvider.UserSessionChangeListener;
import co.in.divi.content.AllowedAppsProvider;
import co.in.divi.lecture.LiveLectureService;
import co.in.divi.model.ClassMembers.ClassMember;
import co.in.divi.model.Instruction;
import co.in.divi.model.LectureDetails;
import co.in.divi.model.LectureInstruction;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Util;

import com.google.gson.Gson;

public class LectureSessionProvider implements UserSessionChangeListener {
	private static final String				TAG			= LectureSessionProvider.class.getSimpleName();

	private static LectureSessionProvider	instance	= null;

	public static enum ConnectionStatus {
		CONNECTED, CONNECTING, NOTCONNECTED;
	}

	public static interface LectureStatusChangeListener {
		public void onLectureJoinLeave();

		public void onConnectionStatusChange();

		public void onReceivedNewInstruction();
	}

	public static interface DashboardChangeListener {
		public void onDashboardChange();
	}

	public static interface FollowMeListener {
		public boolean tryFollowMe(Uri followUri);
	}

	public static class LocationHolder {
		public String							uid;
		public LocationManager.LOCATION_TYPE	locationType;
		public LocationManager.LOCATION_SUBTYPE	locationSubType;
		public String							locationUri;
		public LocationManager.Breadcrumb		breadcrumb;
		public String							externalAppName;

		public long								timestampInMillis;	// for tracking freshness
	}

	private UserSessionProvider						userSessionProvider;

	private ConnectionStatus						status;
	private Context									context;
	private ArrayList<LectureStatusChangeListener>	listeners;

	private LectureDetails							currentLecture;
	private LectureInstruction						instructions;
	private boolean									isFollowMe;

	// Dashboard
	private ArrayList<DashboardChangeListener>		dashboardListeners;
	private FollowMeListener						followMeListener;
	private HashMap<String, Long>					hereNow;
	private HashMap<String, LocationHolder>			locations;
	private ArrayList<ClassMember>					classMembers;

	private LectureSessionProvider(Context context) {
		this.context = context;
		userSessionProvider = UserSessionProvider.getInstance(context);
		userSessionProvider.addListener(this);
		listeners = new ArrayList<LectureSessionProvider.LectureStatusChangeListener>();
		dashboardListeners = new ArrayList<LectureSessionProvider.DashboardChangeListener>();
		hereNow = new HashMap<String, Long>();
		locations = new HashMap<String, LocationHolder>();
		classMembers = new ArrayList<ClassMember>();
		status = ConnectionStatus.NOTCONNECTED;
		isFollowMe = false;
	}

	public static LectureSessionProvider getInstance(Context context) {
		if (instance == null) {
			instance = new LectureSessionProvider(context);
		}
		return instance;
	}

	public void addListener(LectureStatusChangeListener listener) {
		if (!this.listeners.contains(listener))
			listeners.add(listener);
	}

	public void removeListener(LectureStatusChangeListener listener) {
		listeners.remove(listener);
	}

	public void addDashboardListener(DashboardChangeListener listener) {
		if (!this.dashboardListeners.contains(listener))
			dashboardListeners.add(listener);
	}

	public void removeDashboardListener(DashboardChangeListener listener) {
		dashboardListeners.remove(listener);
	}

	public void setFollowMeListener(FollowMeListener listener) {
		this.followMeListener = listener;
	}

	public boolean isLectureJoined() {
		return currentLecture != null;
	}

	public boolean isCurrentUserTeacher() {
		return isLectureJoined() && userSessionProvider.isLoggedIn() && userSessionProvider.getUserData().isTeacher()
				&& currentLecture.teacherId.equals(userSessionProvider.getUserData().uid);
	}

	public boolean isBlackout() {
		if (isLectureJoined()) {
			if (isCurrentUserTeacher())
				return false;
			if (instructions.instructions.length > 0) {
				Instruction instruction = new Gson().fromJson(instructions.instructions[0].data, Instruction.class);
				if (instruction.type == Instruction.INSTRUCTION_TYPE_BLACKOUT)
					return true;
			}
		}
		return false;
	}

	public boolean isFollowMe() {
		return isCurrentUserTeacher() && isFollowMe;
	}

	public void setFollowMe(boolean followMe) {
		this.isFollowMe = followMe;
	}

	public ConnectionStatus getStatus() {
		return status;
	}

	public void setStatus(ConnectionStatus status) {
		if (LogConfig.DEBUG_LIVE_LECTURE)
			Log.d(TAG, "setting status:" + status);
		this.status = status;
		for (LectureStatusChangeListener listener : listeners)
			listener.onConnectionStatusChange();
	}

	public void setHereNow(ArrayList<String> uids) {
		hereNow.clear();
		for (String uid : uids)
			hereNow.put(uid, 0L);
		callDashboardListeners();
	}

	public void joinHereNow(String uid, long timestamp) {
		hereNow.put(uid, timestamp);
		callDashboardListeners();
	}

	public void leaveHereNow(String uid, long timestamp) {
		if (hereNow.containsKey(uid) && hereNow.get(uid) < timestamp) {
			hereNow.remove(uid);
			callDashboardListeners();
		} else {
			Log.d(TAG, "ignoring leaveHereNow!! - " + uid);
		}
	}

	public Set<String> hereNow() {
		return hereNow.keySet();
	}

	public void setLocation(String uid, LocationHolder locHolder) {
		locations.put(uid, locHolder);
		callDashboardListeners();
	}

	public HashMap<String, LocationHolder> getAllLocations() {
		return locations;
	}

	public void onConnectionError(String error) {
		if (LogConfig.DEBUG_LIVE_LECTURE)
			Log.d(TAG, "error connecting to class:" + error);
		Toast.makeText(context, "Error connecting to lecture, retrying...", Toast.LENGTH_LONG).show();
	}

	public LectureDetails getCurrentLecture() {
		return currentLecture;
	}

	public LectureInstruction getInstructions() {
		return instructions;
	}

	public void setInstructions(LectureInstruction instructions) {
		this.instructions = instructions;
		this.isFollowMe = false;
		if (isCurrentUserTeacher()) {
			try {
				Instruction instruction = new Gson().fromJson(instructions.instructions[0].data, Instruction.class);
				if (instruction.followMe)
					isFollowMe = true;
			} catch (Exception e) {
				Log.w(TAG, "error in follow me", e);
			}
		}

		for (LectureStatusChangeListener listener : listeners)
			listener.onReceivedNewInstruction();
		context.getContentResolver().update(AllowedAppsProvider.Apps.CONTENT_URI, new ContentValues(), null, null);
	}

	public void setFollowMeInstruction(Uri uri) {
		if (followMeListener == null || !followMeListener.tryFollowMe(uri)) {
			Util.openInstruction(context, uri);
		}
	}

	/* presence */
	public void joinLecture(LectureDetails lecture) {
		this.currentLecture = lecture;
		this.instructions = null;
		Intent intent = new Intent(context, LiveLectureService.class);
		intent.putExtra(LiveLectureService.INTENT_EXTRA_CHANNEL, lecture.channel);
		context.startService(intent);
		isFollowMe = false;
		for (LectureStatusChangeListener listener : listeners)
			listener.onLectureJoinLeave();
	}

	public void leaveLecture() {
		Intent intent = new Intent(context, LiveLectureService.class);
		intent.putExtra(LiveLectureService.INTENT_EXTRA_STOP_SERVICE, true);
		context.startService(intent);
		for (LectureStatusChangeListener listener : listeners)
			listener.onLectureJoinLeave();
	}

	public void resetLecture() {
		reset();
		setStatus(ConnectionStatus.NOTCONNECTED);
	}

	public ArrayList<ClassMember> getClassMembers() {
		return this.classMembers;
	}

	public void setClassMembers(ClassMember[] members) {
		classMembers.clear();
		for (ClassMember member : members) {
			classMembers.add(member);
		}
		callDashboardListeners();
	}

	private void callDashboardListeners() {
		// TODO: throttle calls?
		for (DashboardChangeListener listener : dashboardListeners)
			listener.onDashboardChange();
	}

	private void reset() {
		currentLecture = null;
		hereNow.clear();
		locations.clear();
		classMembers.clear();
	}

	@Override
	public void onSessionChange() {
		if (isLectureJoined())
			leaveLecture();
	}

	@Override
	public void onCourseChange() {
	}
}
