package co.in.divi;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import co.in.divi.DiaryManager.DiaryChangeListener;
import co.in.divi.LectureSessionProvider.LectureStatusChangeListener;
import co.in.divi.LocationManager.LOCATION_TYPE;
import co.in.divi.UserSessionProvider.UserSessionChangeListener;
import co.in.divi.activity.BlackoutActivity;
import co.in.divi.activity.LoginActivity;
import co.in.divi.activity.SyncDownActivity;
import co.in.divi.fragment.DiaryContainerFragment;
import co.in.divi.fragment.HeaderFragment;
import co.in.divi.ui.HomeworkPickerPanel;
import co.in.divi.util.Config;
import co.in.divi.util.LogConfig;

public abstract class BaseActivity extends Activity implements UserSessionChangeListener, LectureStatusChangeListener, DiaryChangeListener {
    private static final String TAG = BaseActivity.class.getSimpleName();
    private static final String DIARY_FRAGMENT_TAG = "fragment_diary";

    protected UserSessionProvider userSessionProvider;
    protected LocationManager locationManager;
    protected LectureSessionProvider lectureSessionProvider;
    protected DiaryManager diaryManager;

    protected HeaderFragment header;
    protected View headerShadow;
    private HomeworkPickerPanel homeworkPickerPanel;

    private boolean showHeader;
    private boolean headerIsAnimating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        userSessionProvider = UserSessionProvider.getInstance(this);
        locationManager = LocationManager.getInstance(this);
        lectureSessionProvider = LectureSessionProvider.getInstance(this);
        diaryManager = DiaryManager.getInstance(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        userSessionProvider.addListener(this);
        diaryManager.addListener(this);
        lectureSessionProvider.addListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideBars();

        switch (userSessionProvider.getLoginStatus()) {
            case LOGGEDOUT:
            case EXPIRED:
                Intent startLogin = getLoginIntent();
                startLogin.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(startLogin);
                break;
            case SYNCING:
                Intent startSyncActivity = new Intent(this, SyncDownActivity.class);
                startSyncActivity.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(startSyncActivity);
                break;

            default:
                break;
        }
        if (!(this instanceof BlackoutActivity)) {
            if (lectureSessionProvider.isBlackout()) {
                Intent intent = new Intent(this, BlackoutActivity.class);
                startActivity(intent);
            }
        }
        if (userSessionProvider.hasUserData() && userSessionProvider.getUserData().isTeacher()) {
            if (diaryManager.isPickingHomework()) {
                addHomeworkPickerPanel();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Clear location
        locationManager.setNewLocation(LOCATION_TYPE.UNKNOWN, null, null, null, null);
        closeDiary();
    }

    @Override
    protected void onStop() {
        super.onStop();
        userSessionProvider.removeListener(this);
        diaryManager.removeListener(this);
        lectureSessionProvider.removeListener(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // if (hasFocus) {
        hideBars();
        // } else {
        // Log.d(TAG, "window lost focus!");
        // }
    }

    protected void hideBars() {
        // if (Build.VERSION.SDK_INT <= 15) {
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // } else
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            // WindowManager.LayoutParams.FLAG_FULLSCREEN);
            int newUiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN;
            getWindow().addFlags(newUiOptions);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else {
            int newUiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            newUiOptions ^= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN;
            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
        }
    }

    @Override
    public void onSessionChange() {
        if (LogConfig.DEBUG_ACTIVITIES)
            Log.d(TAG, "onSessionChange!");
        switch (userSessionProvider.getLoginStatus()) {
            case LOGGEDOUT:
                finish();// not start login activity (no break)
            case EXPIRED:
                Intent startLogin = getLoginIntent();
                startLogin.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(startLogin);
                break;
            case SYNCING:
                Intent startSyncActivity = new Intent(this, SyncDownActivity.class);
                startSyncActivity.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(startSyncActivity);
                break;

            default:
                break;
        }
    }

    public void hideHeader() {
        showHeader = false;
        if (header.getView() != null && header.getView().isShown() && !headerIsAnimating) {
            ObjectAnimator oa = ObjectAnimator.ofFloat(headerShadow, "translationY", 0, -300f);
            oa.setDuration(500);
            oa.start();
            headerShadow.setVisibility(View.GONE);

            // fragment view
            ObjectAnimator oa2 = ObjectAnimator.ofFloat(header.getView(), "translationY", 0, -300f);
            oa2.setDuration(500);
            oa2.addListener(new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    headerIsAnimating = true;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    headerIsAnimating = false;
                    if (header != null && header.getView() != null)
                        header.getView().setVisibility(View.GONE);
                    checkHeader();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    headerIsAnimating = false;
                    checkHeader();
                }
            });
            oa2.start();
        }
    }

    public void showHeader() {
        showHeader = true;
        if (!header.getView().isShown() && !headerIsAnimating) {
            ObjectAnimator oa = ObjectAnimator.ofFloat(headerShadow, "translationY", -300f, 0);
            oa.setDuration(500);
            oa.addListener(new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    headerIsAnimating = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    headerIsAnimating = false;
                    checkHeader();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    headerIsAnimating = false;
                    checkHeader();
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            oa.start();
            headerShadow.setVisibility(View.VISIBLE);

            // fragment view
            ObjectAnimator oa2 = ObjectAnimator.ofFloat(header.getView(), "translationY", -300f, 0);
            oa2.setDuration(500);
            oa2.start();
            header.getView().setVisibility(View.VISIBLE);
        }
    }

    public void showDiary() {
        if (getFragmentManager().findFragmentByTag(DIARY_FRAGMENT_TAG) == null) {
            FrameLayout rootLayout = (FrameLayout) findViewById(android.R.id.content);
            DiaryContainerFragment diaryFragment = new DiaryContainerFragment();
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.add(rootLayout.getId(), diaryFragment, DIARY_FRAGMENT_TAG);
            fragmentTransaction.commit();
        } else {
            closeDiary();
        }
    }

    public void closeDiary() {
        if (getFragmentManager().findFragmentByTag(DIARY_FRAGMENT_TAG) != null) {
            getFragmentManager().beginTransaction().remove(getFragmentManager().findFragmentByTag(DIARY_FRAGMENT_TAG)).commit();
        }
    }

    private void checkHeader() {
        if (showHeader) {
            if (header != null && header.getView() != null && !header.getView().isShown()) {
                showHeader();
            }
        } else {
            if (header != null && header.getView() != null && header.getView().isShown()) {
                hideHeader();
            }
        }
    }

    private Intent getLoginIntent() {
        return new Intent(this, LoginActivity.class);
    }

    @Override
    public void onConnectionStatusChange() {
    }

    @Override
    public void onLectureJoinLeave() {
    }

    @Override
    public void onReceivedNewInstruction() {
    }

    @Override
    public abstract void onCourseChange();

    @Override
    public void onHomeworkPickerStatusChange() {
        if (userSessionProvider.isLoggedIn() && userSessionProvider.getUserData().isTeacher()) {
            if (diaryManager.isPickingHomework()) {
                addHomeworkPickerPanel();
            } else {
                removeHomeworkPickerPanel();
            }
        }
    }

    // homework picker
    private void addHomeworkPickerPanel() {
        removeHomeworkPickerPanel();
        FrameLayout rootLayout = (FrameLayout) findViewById(android.R.id.content);
        homeworkPickerPanel = (HomeworkPickerPanel) ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(
                R.layout.panel_homeworkpicker, rootLayout, false);
        homeworkPickerPanel.init();
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER | Gravity.BOTTOM);
        lp.leftMargin = 100;
        lp.rightMargin = 100;
        rootLayout.addView(homeworkPickerPanel, lp);
    }

    private void removeHomeworkPickerPanel() {
        if (homeworkPickerPanel != null) {
            homeworkPickerPanel.stop();
            ((FrameLayout) findViewById(android.R.id.content)).removeView(homeworkPickerPanel);
            homeworkPickerPanel = null;
        }
    }
}
