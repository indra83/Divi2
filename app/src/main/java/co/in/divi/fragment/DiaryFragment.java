package co.in.divi.fragment;

import android.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Date;

import co.in.divi.BaseActivity;
import co.in.divi.DiviApplication;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.db.UserDBContract;
import co.in.divi.db.UserDBContract.Commands;
import co.in.divi.db.model.Command;
import co.in.divi.diary.DiaryEntry;
import co.in.divi.diary.DiaryManager;
import co.in.divi.model.UserData;
import co.in.divi.ui.DiaryEntryEditorUI;
import co.in.divi.ui.DiaryEntryViewerUI;

public class DiaryFragment extends Fragment implements DiaryManager.DiaryListener, DiaryEntryViewerUI.CloseViewer {
    private static final String TAG = DiaryFragment.class.getName();

    private UserSessionProvider userSessionProvider;
    private DiaryManager diaryManager;

    private Button newHomeworkButton, newAnnounceButton, syncButton;
    private TextView syncText;
    private ListView diaryEntriesList;
    private ViewGroup composeContainer;

    private DiaryEntryEditorUI deEditor;
    private DiaryEntryViewerUI deViewer;

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
        composeContainer = (ViewGroup) rootView.findViewById(R.id.compose_container);
        newHomeworkButton = (Button) rootView.findViewById(R.id.newHomeworkButton);
        newAnnounceButton = (Button) rootView.findViewById(R.id.newAnnounceButton);
        syncText = (TextView) rootView.findViewById(R.id.syncText);
        syncButton = (Button) rootView.findViewById(R.id.syncButton);
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
        diaryManager.addListener(this);
        // fill listview
        Cursor cursor;
        if (userSessionProvider.getUserData().isTeacher()) {
            String mSelectionClause = Commands.STATUS + " = ? AND " + Commands.TEACHER_ID + " = ? AND " + Commands.TYPE + " = ? ";
            String[] selectionArgs = new String[]{"" + Command.COMMAND_STATUS_ACTIVE, userSessionProvider.getUserData().uid, "" + Command.COMMAND_CATEGORY_DIARY};
            cursor = getActivity().getContentResolver().query(UserDBContract.Commands.CONTENT_URI, UserDBContract.Commands.PROJECTION_ALL,
                    mSelectionClause, selectionArgs, Commands.SORT_ORDER_ENDSAT_FIRST);

        } else {
            String mSelectionClause = Commands.STATUS + " = ? AND " + Commands.UID + " =? AND " + Commands.TYPE + " = ? ";
            String[] selectionArgs = new String[]{"" + Command.COMMAND_STATUS_ACTIVE, userSessionProvider.getUserData().uid, "" + Command.COMMAND_CATEGORY_DIARY};
            cursor = getActivity().getContentResolver().query(UserDBContract.Commands.CONTENT_URI, UserDBContract.Commands.PROJECTION_ALL,
                    mSelectionClause, selectionArgs, Commands.SORT_ORDER_ENDSAT_FIRST);
        }
        Log.d(TAG, "got entries:" + cursor.getCount());
        DiaryEntryAdapter adapter = new DiaryEntryAdapter(getActivity(), cursor);
        diaryEntriesList.setAdapter(adapter);
        diaryEntriesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DiaryEntry de = (DiaryEntry) view.getTag();
                deViewer = (DiaryEntryViewerUI) ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.ui_diaryentry_viewer, composeContainer, false);
                composeContainer.removeAllViews();
                composeContainer.addView(deViewer);
                composeContainer.setVisibility(View.VISIBLE);
                deViewer.init(de, DiaryFragment.this);
            }
        });
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
        if (deEditor != null)
            deEditor.stop();
        if (deViewer != null)
            deViewer.stop();
    }

    @Override
    public void onDiaryStateChange() {
        refreshUI();
    }

    private void launchEditor() {
        deEditor = (DiaryEntryEditorUI) ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.ui_diaryentry_editor, composeContainer, false);
        composeContainer.addView(deEditor);
        deEditor.init(diaryManager, userSessionProvider, getFragmentManager());
    }

    private void refreshUI() {
        Log.d(TAG, "refreshUI");
        if(!userSessionProvider.isLoggedIn()) {
            return;
        }
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
            if (deEditor == null)
                launchEditor();
            deEditor.refresh();
        } else {
            composeContainer.setVisibility(View.GONE);
            if (deEditor != null) {
                deEditor.stop();
                composeContainer.removeAllViews();
                deEditor = null;
            }
        }
    }

    @Override
    public void closeViewer() {
        composeContainer.removeAllViews();
        composeContainer.setVisibility(View.GONE);
        deViewer.stop();
        deViewer = null;
    }

    private class DiaryEntryAdapter extends CursorAdapter {
        LayoutInflater inflater;
        Gson gson;
        SimpleDateFormat format = new SimpleDateFormat("dd MMM");

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
            TextView tvLine2 = (TextView) view.findViewById(R.id.line2);
            TextView date = (TextView) view.findViewById(R.id.date);
            ImageView icon = (ImageView) view.findViewById(R.id.icon);

            DiaryEntry de = gson.fromJson(cursor.getString(cursor.getColumnIndex(Commands.DATA)), DiaryEntry.class);
            de.dueDate = new Date(cursor.getLong(cursor.getColumnIndex(Commands.END_TIMESTAMP)));
            de.createdAt = cursor.getLong(cursor.getColumnIndex(Commands.CREATE_TIMESTAMP));

            if (de.entryType == DiaryEntry.ENTRY_TYPE.ANNOUNCEMENT) {
                icon.setImageResource(R.drawable.bg_accuracy_0);
            } else {
                icon.setImageResource(R.drawable.bg_accuracy_7);
            }
            tvTitle.setText(de.title);
            if (userSessionProvider.getUserData().isTeacher()) {
                String className = de.classId;
                for (UserData.ClassRoom cr : userSessionProvider.getUserData().classRooms) {
                    if (cr.classId.equals(de.classId)) {
                        className = cr.className + " - " + cr.section;
                        break;
                    }
                }
                tvLine2.setText(" to "+className);
            } else {
                tvLine2.setText(" from "+de.teacherName);
            }
            date.setText(format.format(de.dueDate));
            view.setTag(de);
        }
    }

}
