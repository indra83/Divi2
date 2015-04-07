package co.in.divi.ui;

import android.app.FragmentManager;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import co.in.divi.BaseActivity;
import co.in.divi.Location;
import co.in.divi.LocationManager;
import co.in.divi.LocationManager.DiviLocationChangeListener;
import co.in.divi.R;
import co.in.divi.diary.DiaryManager;
import co.in.divi.fragment.DiaryEntryComposeFragment;

public class HomeworkPickerPanel extends LinearLayout implements DiviLocationChangeListener {

	private static final String	TAG	= HomeworkPickerPanel.class.getSimpleName();

	private Button				addButton, doneButton;
	private TextView			itemCountTextView, locationNameTextView;

	private LocationManager		locationManager;
	private DiaryManager		diaryManager;

	public HomeworkPickerPanel(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void init() {
		addButton = (Button) findViewById(R.id.add);
		doneButton = (Button) findViewById(R.id.done);
		itemCountTextView = (TextView) findViewById(R.id.items_count);
		locationNameTextView = (TextView) findViewById(R.id.location);
		locationManager = LocationManager.getInstance(getContext());
		locationManager.addListener(this);
		diaryManager = DiaryManager.getInstance(getContext());
		doneButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentManager fm = ((BaseActivity) getContext()).getFragmentManager();
				DiaryEntryComposeFragment diaryComposeFragment = new DiaryEntryComposeFragment();
				diaryComposeFragment.show(fm, "diary entry");
			}
		});

		addButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				diaryManager.addResourceToHomework();
			}
		});
		addButton.setVisibility(View.GONE);
	}

	public void stop() {
		locationManager.removeListener(this);
	}

	@Override
	public void onLocationChange(Location loc) {
		addButton.setVisibility(View.GONE);
		if (loc.getBreadcrumb() == null)
			locationNameTextView.setText("--");
		else {
			if (loc.getLocationType() == Location.LOCATION_TYPE.ASSESSMENT || loc.getLocationType() == Location.LOCATION_TYPE.TOPIC) {
				locationNameTextView.setText(loc.getBreadcrumb().toString());
				addButton.setVisibility(View.VISIBLE);
			}
		}
	}
}