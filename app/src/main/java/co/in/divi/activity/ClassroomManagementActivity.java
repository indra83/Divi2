package co.in.divi.activity;

import android.os.Bundle;

import co.in.divi.BaseActivity;
import co.in.divi.R;

public class ClassroomManagementActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classroom_management);
    }

    @Override
    public void onCourseChange() {
        finish();
    }
}
