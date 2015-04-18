package co.in.divi.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import co.in.divi.Location;
import co.in.divi.LocationManager;
import co.in.divi.LocationManager.DiviLocationChangeListener;
import co.in.divi.R;
import co.in.divi.diary.DiaryManager;

public class HomeworkPickerPanel extends LinearLayout implements DiviLocationChangeListener, DiaryManager.DiaryListener {
    private static final String TAG = HomeworkPickerPanel.class.getSimpleName();

    private Button addButton, doneButton;
    private TextView itemCountTextView, smallLoc, bigLoc;
    private ProgressBar pb;

    private LocationManager locationManager;
    private DiaryManager diaryManager;

    public HomeworkPickerPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init() {
        addButton = (Button) findViewById(R.id.add);
        doneButton = (Button) findViewById(R.id.done);
        itemCountTextView = (TextView) findViewById(R.id.items_count);
        smallLoc = (TextView) findViewById(R.id.smallLoc);
        bigLoc = (TextView) findViewById(R.id.bigLoc);
        pb = (ProgressBar) findViewById(R.id.progress1);

        locationManager = LocationManager.getInstance(getContext());
        locationManager.addListener(this);
        diaryManager = DiaryManager.getInstance(getContext());
        diaryManager.addListener(this);

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                diaryManager.finishPicking();
            }
        });
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                diaryManager.addResourceToHomework();
            }
        });
        onLocationChange(locationManager.getLocation());
    }

    public void stop() {
        locationManager.removeListener(this);
        diaryManager.removeListener(this);
    }

    @Override
    public void onLocationChange(Location loc) {
        addButton.setEnabled(false);
        smallLoc.setVisibility(View.GONE);
        bigLoc.setVisibility(View.GONE);
        pb.setVisibility(View.VISIBLE);
        if (loc.getLocationType() == Location.LOCATION_TYPE.ASSESSMENT || loc.getLocationType() == Location.LOCATION_TYPE.TOPIC) {
            pb.setVisibility(View.GONE);
            smallLoc.setVisibility(View.VISIBLE);
            bigLoc.setVisibility(View.VISIBLE);
            addButton.setEnabled(true);

            smallLoc.setText(loc.getBreadcrumb().chapterName);
            bigLoc.setText(loc.getBreadcrumb().itemName);
        } else if (loc.getLocationType() == Location.LOCATION_TYPE.UNKNOWN) {
            if (loc.getAppPackageName() != null && loc.getAppPackageName() != getContext().getPackageName()) {
                pb.setVisibility(View.GONE);
                smallLoc.setVisibility(View.VISIBLE);
                bigLoc.setVisibility(View.VISIBLE);
                addButton.setEnabled(true);

                smallLoc.setText(loc.getAppPackageName());
                bigLoc.setText(loc.getAppName());
            }
        }
        itemCountTextView.setText("" + diaryManager.getCurrentEntry().resources.size() + " items \nin homework");
    }

    @Override
    public void onDiaryStateChange() {
        Log.d(TAG, "homework panel on diary state change");
        if (diaryManager.isComposing()) {
            itemCountTextView.setText("" + diaryManager.getCurrentEntry().resources.size() + " items \nin homework");
        }
    }
}