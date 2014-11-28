package co.in.divi;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;

import co.in.divi.model.UserData;
import co.in.divi.model.UserData.ClassRoom;
import co.in.divi.model.UserData.Course;
import co.in.divi.util.Config;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Util;

/**
 * Provides access to User's login data.
 * <p/>
 * We could be in 3 states: 1. No User data (before first login or after logout) 2. Session expired (token invalidated,
 * could be from server or time expired) 3. Session active
 *
 * @author Indra
 */
public class UserSessionProvider {
    private static final String TAG = UserSessionProvider.class.getSimpleName();
    private static final String PREFS_FILE = "USERSESSION_PREFS";
    private static final String PREF_LOGIN_DETAILS = "LOGIN_DETAILS";
    private static final String PREF_LOGIN_SYNC_DONE = "PREF_LOGIN_SYNC_DONE";
    private static final String PREF_SELECTED_COURSE = "PREF_SELECTED_COURSE";

    // timestamps
    public static final String LAST_SYNC_CONTENT_TIMESTAMP = "LAST_SYNC_CONTENT_TIMESTAMP";
    public static final String LAST_SYNC_ATTEMTPS_TIMESTAMP = "LAST_SYNC_ATTEMTPS_TIMESTAMP";
    public static final String LAST_SYNC_COMMANDS_TIMESTAMP = "LAST_SYNC_COMMANDS_TIMESTAMP";
    public static final String LAST_SYNC_LOGS_TIMESTAMP = "LAST_SYNC_LOGS_TIMESTAMP";
    public static final String LAST_SYNC_REPORTS_TIMESTAMP = "LAST_SYNC_REPORTS_TIMESTAMP";
    public static final String LAST_SYNC_HEARTBEAT_TIMESTAMP = "LAST_SYNC_HEARTBEAT_TIMESTAMP";

    private static UserSessionProvider instance = null;

    private Context context;
    private SharedPreferences prefs;

    private UserData userData;
    private String selectedCourseId;
    private LoginStatus loginStatus;

    ArrayList<UserSessionChangeListener> listeners;

    private UserSessionProvider(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        this.listeners = new ArrayList<UserSessionChangeListener>();
        this.loginStatus = LoginStatus.LOGGEDOUT;
        // init data
        if (prefs.contains(PREF_LOGIN_DETAILS)) {
            try {
                String persistedUserData = prefs.getString(PREF_LOGIN_DETAILS, null);
                if (LogConfig.DEBUG_LOGIN)
                    Log.d(TAG, "persisted Login data:" + persistedUserData);
                userData = new Gson().fromJson(persistedUserData, UserData.class);
                selectedCourseId = prefs.getString(PREF_SELECTED_COURSE, null);
                if (prefs.getBoolean(PREF_LOGIN_SYNC_DONE, false))
                    loginStatus = LoginStatus.LOGGEDIN;
                else
                    loginStatus = LoginStatus.SYNCING;
            } catch (Exception e) {
                Log.e(TAG, "error parsing login details", e);
                prefs.edit().remove(PREF_LOGIN_DETAILS).apply();
            }
        }
    }

    public static UserSessionProvider getInstance(Context context) {
        if (instance == null) {
            instance = new UserSessionProvider(context);
        }
        return instance;
    }

    public interface UserSessionChangeListener {
        public void onSessionChange();

        public void onCourseChange();
    }

    public static enum LoginStatus {
        LOGGEDOUT, SYNCING, LOGGEDIN, EXPIRED
    }

    public void addListener(UserSessionChangeListener listener) {
        if (!this.listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeListener(UserSessionChangeListener listener) {
        listeners.remove(listener);
    }

    public boolean hasUserData() {
        return userData != null;
    }

    public boolean isLoggedIn() {
        return loginStatus == LoginStatus.LOGGEDIN;
    }

    public LoginStatus getLoginStatus() {
        return loginStatus;
    }

    public UserData getUserData() {
        return userData;
    }

    public String getCourseId() {
        // return "courseid";
        return selectedCourseId;
    }

    public String[] getAllCourseIds() {
        HashSet<String> allCourses = new HashSet<String>();
        for (ClassRoom classroom : userData.classRooms) {
            for (Course course : classroom.courses) {
                allCourses.add(course.id);
            }
        }
        return allCourses.toArray(new String[allCourses.size()]);
    }

    public String getCourseName() {
        if (selectedCourseId == null)
            return "--";
        if (userData.classRooms != null) {
            for (ClassRoom classroom : userData.classRooms) {
                if (classroom.courses == null)
                    break;
                for (Course course : classroom.courses) {
                    if (course.id.equals(selectedCourseId))
                        return course.name;
                }
            }
        }
        return "N/A";
    }

    public void setCourseId(String courseId) {
        if (LogConfig.DEBUG_LOGIN)
            Log.d(TAG, "changing course to:" + courseId);
        if (this.selectedCourseId != null && this.selectedCourseId.equals(courseId))
            return;
        this.selectedCourseId = courseId;
        prefs.edit().putString(PREF_SELECTED_COURSE, courseId).apply();
        for (UserSessionChangeListener listener : listeners) {
            listener.onCourseChange();
        }
    }

    public void setUserSession(UserData data) {
        // ensure correct time
        long timeDiff = Math.abs(Util.getTimestampMillis() - data.time);
        if (LogConfig.DEBUG_LOGIN)
            Log.d(TAG, "timeDiff  " + timeDiff);
        if (timeDiff > Config.MAX_TIME_DIFFERENCE) {
            Toast.makeText(context, "Please set correct time...", Toast.LENGTH_LONG).show();
            return;
        }
        this.userData = data;
        try {
            String loginDataString = new Gson().toJson(data);
            if (LogConfig.DEBUG_LOGIN) {
                Log.d(TAG, "login data saving:" + loginDataString);
            }
            prefs.edit().putString(PREF_LOGIN_DETAILS, loginDataString).putBoolean(PREF_LOGIN_SYNC_DONE, false).apply();
            loginStatus = LoginStatus.SYNCING;

            if (LogConfig.DEBUG_LOGIN)
                Log.d(TAG, "prev course id: " + selectedCourseId);

            boolean resetCourseId = true;
            for (ClassRoom classroom : userData.classRooms) {
                for (Course course : classroom.courses) {
                    if (course.id.equals(selectedCourseId)) {
                        resetCourseId = false;
                        break;
                    }
                }
            }
            if (LogConfig.DEBUG_LOGIN)
                Log.d(TAG, "reset course id? " + resetCourseId);
            if (resetCourseId) {
                String newCourseId = null;
                if (userData.classRooms.length > 0) {
                    if (userData.classRooms[0].courses.length > 0)
                        newCourseId = userData.classRooms[0].courses[0].id;
                }
                setCourseId(newCourseId);
            }

            callSessionChangeOnListeners();

            // end
        } catch (Exception e) {
            Log.e(TAG, "error persisiting login data", e);
        }
    }

    public void setSyncDone() {
        prefs.edit().putBoolean(PREF_LOGIN_SYNC_DONE, true).apply();
        loginStatus = LoginStatus.LOGGEDIN;
        callSessionChangeOnListeners();
        // check for content updates
        ContentUpdateManager.getInstance(context).startContentUpdates(false);
    }

    public long getTimestamp(String tag) {
        return prefs.getLong(tag, 0);
    }

    public void setTimestamp(String tag, long value) {
        prefs.edit().putLong(tag, value).apply();
    }

    public void logout() {
        this.userData = null;
        prefs.edit().remove(PREF_LOGIN_DETAILS).remove(PREF_LOGIN_SYNC_DONE).remove(LAST_SYNC_ATTEMTPS_TIMESTAMP)
                .remove(LAST_SYNC_COMMANDS_TIMESTAMP).remove(LAST_SYNC_CONTENT_TIMESTAMP).remove(LAST_SYNC_LOGS_TIMESTAMP)
                .remove(LAST_SYNC_REPORTS_TIMESTAMP).remove(LAST_SYNC_HEARTBEAT_TIMESTAMP).apply();
        loginStatus = LoginStatus.LOGGEDOUT;
        DiaryManager.getInstance(context).clearCurrentEntry();
        callSessionChangeOnListeners();
    }

    private void callSessionChangeOnListeners() {
        for (UserSessionChangeListener listener : listeners) {
            listener.onSessionChange();
        }
    }
}