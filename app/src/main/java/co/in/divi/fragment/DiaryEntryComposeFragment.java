package co.in.divi.fragment;

import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import co.in.divi.diary.DiaryEntry;
import co.in.divi.diary.DiaryManager;
import co.in.divi.Location.LOCATION_SUBTYPE;
import co.in.divi.Location.LOCATION_TYPE;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.model.UserData;

public class DiaryEntryComposeFragment extends BaseDialogFragment {
	private static final String		TAG	= DiaryEntryComposeFragment.class.getSimpleName();

	private UserSessionProvider		userSessionProvider;
	private DiaryManager			diaryManager;

	private ViewGroup				root;
	private Spinner					entryTypeSpinner, recipientSpinner;
	private EditText				message;
	private LinearLayout			resourcesContainer;
	private Button					addResource, publishButton, cancelButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog);
		userSessionProvider = UserSessionProvider.getInstance(getActivity());
		diaryManager = DiaryManager.getInstance(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		root = (ViewGroup) inflater.inflate(R.layout.fragment_diarycompose, container, false);
		entryTypeSpinner = (Spinner) root.findViewById(R.id.entry_type_spinner);
		recipientSpinner = (Spinner) root.findViewById(R.id.recipients);
		resourcesContainer = (LinearLayout) root.findViewById(R.id.resources_container);
		addResource = (Button) root.findViewById(R.id.add_resource);
		publishButton = (Button) root.findViewById(R.id.publish);
		cancelButton = (Button) root.findViewById(R.id.cancel);
		message = (EditText) root.findViewById(R.id.message);
		root.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				diaryManager.clearCurrentEntry();
				dismiss();
			}
		});
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(getActivity(), android.R.layout.simple_spinner_item);
		adapter.add("Homework");
		adapter.add("Announcement");
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		entryTypeSpinner.setAdapter(adapter);

		ArrayAdapter<CharSequence> adapter2 = new ArrayAdapter<CharSequence>(getActivity(), android.R.layout.simple_spinner_item);
		for (UserData.ClassRoom c : userSessionProvider.getUserData().classRooms) {
			adapter2.add(c.className);
		}
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		recipientSpinner.setAdapter(adapter);
		return root;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!userSessionProvider.getUserData().isTeacher()) {
			dismiss();
			return;
		}
		resumeDiaryEditing();
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				diaryManager.clearCurrentEntry();
				dismiss();
			}
		});

		addResource.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		publishButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// get commands for protected resources
				// call api
				// sync commands
			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();
		saveDiaryEditing();
		dismiss();
	}

	private void resumeDiaryEditing() {
		DiaryEntry de = diaryManager.getCurrentEntry();
		entryTypeSpinner.setOnItemSelectedListener(null);
		switch (de.entryType) {
		case HOMEWORK:
			entryTypeSpinner.setSelection(0);
			break;
		case ANNOUNCEMENT:
			entryTypeSpinner.setSelection(1);
			break;
		}
		entryTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				Log.d(TAG, "item selected - " + parent.getItemAtPosition(position));
				if (position == 0) {
					addResource.setVisibility(View.VISIBLE);
					resourcesContainer.setVisibility(View.VISIBLE);
					diaryManager.getCurrentEntry().entryType = DiaryEntry.ENTRY_TYPE.HOMEWORK;
				} else if (position == 1) {
					addResource.setVisibility(View.GONE);
					resourcesContainer.setVisibility(View.GONE);
					diaryManager.getCurrentEntry().entryType = DiaryEntry.ENTRY_TYPE.HOMEWORK;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		message.setText(de.message);
		resourcesContainer.removeAllViews();
		LayoutInflater lf = LayoutInflater.from(getActivity());
		for (DiaryEntry.Resource r : de.resources) {
			View resourceView = lf.inflate(R.layout.item_homework_resource, resourcesContainer, false);
			ImageView iconView = (ImageView) resourceView.findViewById(R.id.icon);
			TextView chapName = (TextView) resourceView.findViewById(R.id.chapName);
			TextView itemName = (TextView) resourceView.findViewById(R.id.itemName);
			if (r.locationType == LOCATION_TYPE.ASSESSMENT) {
				iconView.setImageResource(R.drawable.ic_hw_quiz_n);
			} else if (r.locationType == LOCATION_TYPE.TOPIC) {
				if (r.locationSubType == LOCATION_SUBTYPE.TOPIC_TOPIC)
					iconView.setImageResource(R.drawable.ic_hw_topic_n);
				else if (r.locationSubType == LOCATION_SUBTYPE.TOPIC_VIDEO)
					iconView.setImageResource(R.drawable.ic_hw_video_n);
				else if (r.locationSubType == LOCATION_SUBTYPE.TOPIC_IMAGE || r.locationSubType == LOCATION_SUBTYPE.TOPIC_IMAGESET)
					iconView.setImageResource(R.drawable.ic_hw_image_n);
			}
			chapName.setText(r.breadcrumb.chapterName);
			itemName.setText(r.breadcrumb.itemName);
			resourcesContainer.addView(resourceView);
		}
	}

	private void saveDiaryEditing() {
		DiaryEntry de = diaryManager.getCurrentEntry();
		if (de != null) {
			de.message = message.getText().toString();
		}
	}
}
