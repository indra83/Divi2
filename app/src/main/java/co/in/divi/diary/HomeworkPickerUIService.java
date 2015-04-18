package co.in.divi.diary;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import co.in.divi.R;
import co.in.divi.ui.HomeworkPickerPanel;

/**
 * Created by Indra on 4/2/2015.
 */
public class HomeworkPickerUIService extends Service implements DiaryManager.DiaryListener {
    private static final String TAG = HomeworkPickerUIService.class.getSimpleName();

    private static int FOREGROUND_ID = 1348;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(FOREGROUND_ID, new NotificationCompat.Builder(this)
                .setOngoing(true)
                .setContentTitle("Divi Homework Picker")
                .setContentText("Picking Homework...")
                .setSmallIcon(R.drawable.ic_header_connected).build());
        if (!DiaryManager.getInstance(this).isPickingHomework()) {
            stopSelf();
            removeHomeworkPickerPanel();
            return START_NOT_STICKY;
        }
        DiaryManager.getInstance(this).addListener(this);
        addHomeworkPickerPanel();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "stopping HomeworkPicker");
        DiaryManager.getInstance(this).removeListener(this);
        removeHomeworkPickerPanel();
    }

    // Teacher Panel stuff
    private HomeworkPickerPanel homeworkPickerPanel;

    private void addHomeworkPickerPanel() {
        removeHomeworkPickerPanel();
        homeworkPickerPanel = (HomeworkPickerPanel) ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.panel_homeworkpicker, null, false);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).addView(homeworkPickerPanel, params);
        homeworkPickerPanel.init();
    }

    private void removeHomeworkPickerPanel() {
        if (homeworkPickerPanel != null) {
            homeworkPickerPanel.stop();
            ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).removeView(homeworkPickerPanel);
            homeworkPickerPanel = null;
        }
    }

    @Override
    public void onDiaryStateChange() {
        if (!DiaryManager.getInstance(this).isPickingHomework()) {
            stopSelf();
//            removeHomeworkPickerPanel();
        }
    }
}
