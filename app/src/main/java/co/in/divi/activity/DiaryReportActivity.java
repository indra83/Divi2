package co.in.divi.activity;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.GridView;
import android.widget.Toast;

import com.google.gson.Gson;

import co.in.divi.BaseActivity;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.diary.DiaryEntry;

/**
 * Created by Indra on 4/16/2015.
 */
public class DiaryReportActivity extends BaseActivity {
    private static final String TAG = DiaryReportActivity.class.getSimpleName();
    public static final String INTENT_EXTRA_DIARYENTRY = "INTENT_EXTRA_DIARYENTRY";

    private GridView gridView;

    private UserSessionProvider userSessionProvider;

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.activity_diaryreport);
        gridView = (GridView) findViewById(R.id.grid);
        userSessionProvider = UserSessionProvider.getInstance(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!userSessionProvider.isLoggedIn() || !userSessionProvider.getUserData().isTeacher()) {
            Toast.makeText(this, "User is logged out!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        DiaryEntry de = new Gson().fromJson(getIntent().getStringExtra(INTENT_EXTRA_DIARYENTRY), DiaryEntry.class);
        fetchStudents(de);
    }

    private void fetchStudents(DiaryEntry de) {
        
    }

    @Override
    public void onCourseChange() {
        finish();
    }
}
