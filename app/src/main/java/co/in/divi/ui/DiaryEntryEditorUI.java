package co.in.divi.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import co.in.divi.BaseActivity;
import co.in.divi.DiviApplication;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.db.model.Command;
import co.in.divi.db.sync.SyncDownService;
import co.in.divi.diary.DiaryEntry;
import co.in.divi.diary.DiaryManager;
import co.in.divi.model.UserData;
import co.in.divi.util.ServerConfig;

/**
 * Created by Indra on 4/12/2015.
 */
public class DiaryEntryEditorUI extends LinearLayout implements HomeworkResourceView.RemoveResourceHelper {
    private static final String TAG = DiaryEntryEditorUI.class.getSimpleName();

    private static SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");

    private EditText titleET, messageET;
    private Button sendButton, cancelButton;
    private TextView pickHomework;

    private LinearLayout resourcesContainer;
    private Spinner classSpinner;
    private TextView titleTV, dueET;


    private UserData.ClassRoom[] classRooms;
    private String[] classRoomNames;
    private DiaryManager diaryManager;
    private UserSessionProvider userSessionProvider;
    private FragmentManager fragmentManager;


    public DiaryEntryEditorUI(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(DiaryManager dm, UserSessionProvider usp, FragmentManager fm) {
        this.diaryManager = dm;
        this.userSessionProvider = usp;
        this.fragmentManager = fm;
        sendButton = (Button) findViewById(R.id.sendButton);
        cancelButton = (Button) findViewById(R.id.cancelButton);
        titleTV = (TextView) findViewById(R.id.title);
        titleET = (EditText) findViewById(R.id.titleET);
        dueET = (TextView) findViewById(R.id.dueET);
        messageET = (EditText) findViewById(R.id.messageET);
        pickHomework = (TextView) findViewById(R.id.pickHomework);
        resourcesContainer = (LinearLayout) findViewById(R.id.resources);
        classSpinner = (Spinner) findViewById(R.id.classSpinner);

        pickHomework.setPaintFlags(pickHomework.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        pickHomework.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (diaryManager.isComposing()) {
                    saveDiaryEntry();
                    diaryManager.startPicking();
                    ((BaseActivity) getContext()).closeDiary();
                }
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (diaryManager.isComposing()) {
                    diaryManager.clearCurrentEntry();
                }
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (diaryManager.isComposing()) {
                    sendHomework();
                }
            }
        });
        dueET.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new DatePickerFragment();
                newFragment.show(fragmentManager, "datePicker");
            }
        });
        classRooms = userSessionProvider.getUserData().classRooms;
        classRoomNames = new String[classRooms.length];
        for (int i = 0; i < classRooms.length; i++)
            classRoomNames[i] = classRooms[i].className + "-" + classRooms[i].section;

    }

    public void refresh() {
        if (diaryManager.getCurrentEntry().entryType == DiaryEntry.ENTRY_TYPE.ANNOUNCEMENT) {
            titleTV.setText("Compose Announcement");
            resourcesContainer.removeAllViews();
            pickHomework.setVisibility(View.GONE);
        } else {
            titleTV.setText("Compose Homework");
            resourcesContainer.removeAllViews();
            pickHomework.setVisibility(View.VISIBLE);
        }
        DiaryEntry de = diaryManager.getCurrentEntry();
        titleET.setText(de.title);
        messageET.setText(de.message);
        dueET.setText(sdf.format(de.dueDate).toString());
        ArrayAdapter adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, classRoomNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        classSpinner.setAdapter(adapter);
//                classSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//                    @Override
//                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                    }
//
//                    @Override
//                    public void onNothingSelected(AdapterView<?> parent) {
//                    }
//                });
        resourcesContainer.removeAllViews();
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        for (DiaryEntry.Resource res : de.resources) {
            HomeworkResourceView hrv = (HomeworkResourceView) inflater.inflate(R.layout.item_homework_res, resourcesContainer, false);
            hrv.init(res, this);
            resourcesContainer.addView(hrv);
        }
    }

    public void stop() {
        saveDiaryEntry();
        DiviApplication.get().getRequestQueue().cancelAll(this);
    }

    private void saveDiaryEntry() {
        if (diaryManager.isComposing()) {
            Log.d(TAG, "saving draft - " + classSpinner.getSelectedItemPosition());
            DiaryEntry de = diaryManager.getCurrentEntry();
            de.title = titleET.getText().toString();
            de.message = messageET.getText().toString();
            de.classId = classRooms[classSpinner.getSelectedItemPosition()].classId;
        }
    }

    private void sendHomework() {
        saveDiaryEntry();
        sendButton.setEnabled(false);
        sendButton.setText("Posting...");
        try {
            DiaryEntry de = diaryManager.getCurrentEntry();
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("uid", userSessionProvider.getUserData().uid);
            jsonRequest.put("token", userSessionProvider.getUserData().token);
            JSONArray commandsArray = new JSONArray();
            Command deCommand = new Command();
            deCommand.category = Command.COMMAND_CATEGORY_DIARY;
            deCommand.classRoomId = de.classId;
            deCommand.itemCategory = de.entryType == DiaryEntry.ENTRY_TYPE.ANNOUNCEMENT ?
                    Command.COMMAND_DIARY_ITEM_CATEGORY_ANNOUNCEMENT : Command.COMMAND_DIARY_ITEM_CATEGORY_HOMEWORK;
            deCommand.status = Command.COMMAND_STATUS_ACTIVE;
            deCommand.teacherId = userSessionProvider.getUserData().uid;
            deCommand.endsAt = de.dueDate.getTime();
            deCommand.data = new Gson().toJson(de);
            commandsArray.put(new JSONObject(new Gson().toJson(deCommand)));
            for (DiaryEntry.Resource res : de.resources) {
                if (res.unlockCommand != null) {
                    commandsArray.put(new JSONObject((new Gson().toJson(res.unlockCommand))));
                }
            }
            jsonRequest.put("commands", commandsArray);
            String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_CREATECOMMANDS;
            Log.d(TAG, "sending:" + jsonRequest.toString());
            JsonObjectRequest sendInstructionRequest = new JsonObjectRequest(Request.Method.POST, url, jsonRequest, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d(TAG, "got response:\n" + response.toString());
                    // validate response
                    Toast.makeText(getContext(), "Published successfully", Toast.LENGTH_SHORT).show();
                    Intent startSyncDownService = new Intent(getContext(), SyncDownService.class);
                    startSyncDownService.putExtra(SyncDownService.INTENT_EXTRA_ONLY_COMMAND, true);
                    getContext().startService(startSyncDownService);

                    diaryManager.clearCurrentEntry();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.w(TAG, "error:" + error);
                    Toast.makeText(getContext(), "Error sending instruction.", Toast.LENGTH_LONG).show();
                }
            });
            sendInstructionRequest.setShouldCache(false);
            DiviApplication.get().getRequestQueue().add(sendInstructionRequest).setTag(this);
        } catch (Exception e) {
            Log.e(TAG, "Error sending instruction", e);
            Toast.makeText(getContext(), "Error sending instruction", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void removeResource(DiaryEntry.Resource res) {
        diaryManager.removeResourceFromHomework(res);
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar c = Calendar.getInstance();
            DiaryManager dm = DiaryManager.getInstance(getActivity());
            if (dm.isComposing()) {
                c.setTime(dm.getCurrentEntry().dueDate);
            } else {
                dismiss();
            }
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day);
            Log.d(TAG, "date selected - " + cal);
            DiaryManager dm = DiaryManager.getInstance(getActivity());
            if (dm.isComposing()) {
                dm.getCurrentEntry().dueDate = cal.getTime();
                dm.pingListeners();
            }
        }
    }
}
