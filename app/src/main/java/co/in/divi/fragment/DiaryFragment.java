package co.in.divi.fragment;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
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

import java.util.ArrayList;

import co.in.divi.BaseActivity;
import co.in.divi.DiviApplication;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.db.UserDBContract;
import co.in.divi.db.UserDBContract.Commands;
import co.in.divi.db.model.Command;
import co.in.divi.db.sync.SyncDownService;
import co.in.divi.diary.DiaryEntry;
import co.in.divi.diary.DiaryManager;
import co.in.divi.model.UserData;
import co.in.divi.ui.HomeworkResourceView;
import co.in.divi.util.ServerConfig;

public class DiaryFragment extends Fragment implements DiaryManager.DiaryListener {

    private static final String TAG = DiaryFragment.class.getName();

    private UserSessionProvider userSessionProvider;
    private DiaryManager diaryManager;

    private Button newHomeworkButton, newAnnounceButton, sendButton, cancelButton, syncButton;
    private TextView syncText, titleTV;
    private EditText titleET, messageET;
    private ListView diaryEntriesList;
    private View composeContainer, pickHomework;
    private LinearLayout resourcesContainer;
    private Spinner classSpinner;

    private UserData.ClassRoom[] classRooms;
    private String[] classRoomNames;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userSessionProvider = UserSessionProvider.getInstance(getActivity());
        diaryManager = DiaryManager.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_diary_container, container, false);
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BaseActivity) getActivity()).closeDiary();
            }
        });
        diaryEntriesList = (ListView) rootView.findViewById(R.id.list);
        composeContainer = rootView.findViewById(R.id.compose_container);
        newHomeworkButton = (Button) rootView.findViewById(R.id.newHomeworkButton);
        newAnnounceButton = (Button) rootView.findViewById(R.id.newAnnounceButton);
        syncButton = (Button) rootView.findViewById(R.id.syncButton);
        sendButton = (Button) rootView.findViewById(R.id.sendButton);
        cancelButton = (Button) rootView.findViewById(R.id.cancelButton);
        syncText = (TextView) rootView.findViewById(R.id.syncText);
        titleTV = (TextView) rootView.findViewById(R.id.title);
        titleET = (EditText) rootView.findViewById(R.id.titleET);
        messageET = (EditText) rootView.findViewById(R.id.messageET);
        pickHomework = rootView.findViewById(R.id.pickHomework);
        resourcesContainer = (LinearLayout) rootView.findViewById(R.id.resources);
        classSpinner = (Spinner) rootView.findViewById(R.id.classSpinner);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        ((MarginLayoutParams) getView().getLayoutParams()).setMargins(0, (int) getResources().getDimensionPixelSize(R.dimen.header_height),
                0, 0);
        getView().requestLayout();
        newHomeworkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                diaryManager.startNewEntry(DiaryEntry.ENTRY_TYPE.HOMEWORK);
            }
        });
        newAnnounceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                diaryManager.startNewEntry(DiaryEntry.ENTRY_TYPE.ANNOUNCEMENT);
            }
        });
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start sync
            }
        });

        pickHomework.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (diaryManager.isComposing()) {
                    diaryManager.startPicking();
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
        diaryManager.addListener(this);
        // fill listview
        Cursor cursor;
        UserData.ClassRoom[] classRooms = userSessionProvider.getUserData().classRooms;
        if (userSessionProvider.getUserData().isTeacher()) {
            String mSelectionClause = Commands.STATUS + " = ? AND " + Commands.CLASS_ID + " IN (" + makePlaceholders(classRooms.length) + ") AND " + Commands.TYPE + " = ? ";
            ArrayList<String> selectionArgs = new ArrayList<>();
            selectionArgs.add("" + Command.COMMAND_STATUS_ACTIVE);
            for (UserData.ClassRoom cr : classRooms)
                selectionArgs.add(cr.classId);
            selectionArgs.add("" + Command.COMMAND_CATEGORY_DIARY);
            cursor = getActivity().getContentResolver().query(UserDBContract.Commands.CONTENT_URI, UserDBContract.Commands.PROJECTION_ALL,
                    mSelectionClause, selectionArgs.toArray(new String[selectionArgs.size()]), Commands.SORT_ORDER_LATEST_FIRST);
        } else {
            String mSelectionClause = Commands.STATUS + " = ? AND " + Commands.TEACHER_ID + " = ? AND " + Commands.TYPE + " = ? ";
            String[] selectionArgs = new String[]{"" + Command.COMMAND_STATUS_ACTIVE, userSessionProvider.getUserData().uid, "" + Command.COMMAND_CATEGORY_DIARY};
            cursor = getActivity().getContentResolver().query(UserDBContract.Commands.CONTENT_URI, UserDBContract.Commands.PROJECTION_ALL,
                    mSelectionClause, selectionArgs, Commands.SORT_ORDER_LATEST_FIRST);
        }
        DiaryEntryAdapter adapter = new DiaryEntryAdapter(getActivity(), cursor);
        diaryEntriesList.setAdapter(adapter);
        classRooms = userSessionProvider.getUserData().classRooms;
        classRoomNames = new String[classRooms.length];
        for (int i = 0; i < classRooms.length; i++)
            classRoomNames[i] = classRooms[i].className + "-" + classRooms[i].section;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUI();
    }

    @Override
    public void onStop() {
        super.onStop();
        diaryManager.removeListener(this);
        if (diaryManager.isComposing()) {
            Log.d(TAG, "saving draft");
            DiaryEntry de = diaryManager.getCurrentEntry();
            de.title = titleET.getText().toString();
            de.message = messageET.getText().toString();
            de.classId = classRooms[classSpinner.getSelectedItemPosition()].classId;
        }
        DiviApplication.get().getRequestQueue().cancelAll(this);
    }

    @Override
    public void onDiaryStateChange() {
        refreshUI();
    }

    private void refreshUI() {
        Log.d(TAG, "refreshUI");
        if (userSessionProvider.getUserData().isTeacher()) {
            newHomeworkButton.setVisibility(View.VISIBLE);
            newAnnounceButton.setVisibility(View.VISIBLE);
            syncButton.setVisibility(View.GONE);
            syncText.setVisibility(View.GONE);
        } else {
            newHomeworkButton.setVisibility(View.GONE);
            newAnnounceButton.setVisibility(View.GONE);
            syncButton.setVisibility(View.VISIBLE);
            syncText.setVisibility(View.VISIBLE);
        }
        if (diaryManager.isComposing()) {
            composeContainer.setVisibility(View.VISIBLE);
            // fill up compose view.
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
            ArrayAdapter adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, classRoomNames);
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
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            for (DiaryEntry.Resource res : de.resources) {
                HomeworkResourceView hrv = (HomeworkResourceView) inflater.inflate(R.layout.item_homework_res, resourcesContainer, false);
                hrv.init(res);
                resourcesContainer.addView(hrv);
            }
        } else {
            composeContainer.setVisibility(View.GONE);
        }
    }

    private class DiaryEntryAdapter extends CursorAdapter {
        LayoutInflater inflater;
        Gson gson;

        public DiaryEntryAdapter(Context context, Cursor c) {
            super(context, c, true);
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            gson = new Gson();
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return inflater.inflate(R.layout.item_diaryentry, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView tvTitle = (TextView) view.findViewById(R.id.title);
            TextView tvTeacher = (TextView) view.findViewById(R.id.teacher);

            DiaryEntry de = gson.fromJson(cursor.getString(cursor.getColumnIndex(Commands.DATA)), DiaryEntry.class);

            tvTitle.setText(de.message);
            tvTeacher.setText(de.classId);
        }
    }

    private void sendHomework() {
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
                    Toast.makeText(getActivity(), "Shared successfully", Toast.LENGTH_SHORT).show();
                    Intent startSyncDownService = new Intent(getActivity(), SyncDownService.class);
                    startSyncDownService.putExtra(SyncDownService.INTENT_EXTRA_ONLY_COMMAND, true);
                    getActivity().startService(startSyncDownService);

                    diaryManager.clearCurrentEntry();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.w(TAG, "error:" + error);
                    Toast.makeText(getActivity(), "Error sending instruction.", Toast.LENGTH_LONG).show();
                }
            });
            sendInstructionRequest.setShouldCache(false);
            DiviApplication.get().getRequestQueue().add(sendInstructionRequest).setTag(this);
        } catch (Exception e) {
            Log.e(TAG, "Error sending instruction", e);
            Toast.makeText(getActivity(), "Error sending instruction", Toast.LENGTH_LONG).show();
        }

    }

    private String makePlaceholders(int len) {
        if (len < 1) {
            // It will lead to an invalid query anyway ..
            throw new RuntimeException("No placeholders");
        } else {
            StringBuilder sb = new StringBuilder(len * 2 - 1);
            sb.append("?");
            for (int i = 1; i < len; i++) {
                sb.append(",?");
            }
            return sb.toString();
        }
    }
}
