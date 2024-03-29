package co.in.divi.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.FadeInNetworkImageView;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import co.in.divi.BaseActivity;
import co.in.divi.DiviApplication;
import co.in.divi.R;
import co.in.divi.model.ClassMembers;
import co.in.divi.model.UserData;
import co.in.divi.util.Config;
import co.in.divi.util.LogConfig;
import co.in.divi.util.ServerConfig;
import co.in.divi.util.Util;

public class ClassroomManagementActivity extends BaseActivity {

    private static final String TAG = ClassroomManagementActivity.class.getSimpleName();

    private ProgressDialog pd;
    private ProgressBar pb;
    private ExpandableListView studentsList;
    private Button addClassButton;

    private StudentsListAdapter listAdapter;

    private FetchStudentsTask fetchStudentsTask;
    private HashMap<String, ClassMembers> allStudents = new HashMap<String, ClassMembers>();
    private ArrayList<UserData.ClassRoom> classRooms = new ArrayList<UserData.ClassRoom>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classroom_management);
        addClassButton = (Button) findViewById(R.id.add_class_button);
        studentsList = (ExpandableListView) findViewById(R.id.students_list);
        pb = (ProgressBar) findViewById(R.id.progress);
        listAdapter = new StudentsListAdapter();
        studentsList.setAdapter(listAdapter);
        addClassButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Add classroom");
                final EditText input = new EditText(ClassroomManagementActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                new AlertDialog.Builder(ClassroomManagementActivity.this).setTitle("New Classroom")
                        .setMessage("Give a name for the classroom : ").setView(input)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                        if (pd != null)
                                            pd.cancel();
                                        pd = ProgressDialog.show(ClassroomManagementActivity.this, "Please wait", "Creating new classroom...");
                                        try {
                                            JSONObject jsonRequest = new JSONObject();
                                            jsonRequest.put("uid", userSessionProvider.getUserData().uid);
                                            jsonRequest.put("token", userSessionProvider.getUserData().token);
                                            jsonRequest.put("standard", input.getText().toString());
                                            jsonRequest.put("section", "");
                                            String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_CREATECLASSROOM;
                                            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonRequest, new Response.Listener<JSONObject>() {
                                                @Override
                                                public void onResponse(JSONObject response) {
                                                    pd.cancel();
                                                    if (LogConfig.DEBUG_ACTIVITIES)
                                                        Log.d(TAG, "response:\n" + response);
                                                    UserData loginResponse = new Gson().fromJson(response.toString(), UserData.class);

                                                    if (loginResponse.error != null) {
                                                        Toast.makeText(ClassroomManagementActivity.this, "Error : " + loginResponse.error.message, Toast.LENGTH_LONG).show();
                                                        finish();
                                                    } else {
                                                        Toast.makeText(ClassroomManagementActivity.this, "New classroom created! Please ask your students to join...", Toast.LENGTH_LONG).show();
//                                                        userSessionProvider.logout();
                                                        userSessionProvider.setUserSession(loginResponse);
                                                        startStudentFetch();
                                                    }
                                                }
                                            }, new Response.ErrorListener() {
                                                @Override
                                                public void onErrorResponse(VolleyError error) {
                                                    pd.cancel();
                                                    Log.w(TAG, "Error creating a new classroom..." + error.getMessage());
                                                    Toast.makeText(ClassroomManagementActivity.this, "Error creating a new classroom", Toast.LENGTH_LONG).show();
                                                }
                                            }
                                            );
                                            request.setShouldCache(false);
                                            DiviApplication.get().getRequestQueue().add(request).setTag(ClassroomManagementActivity.this);
                                        } catch (Exception e) {
                                            if (pd != null)
                                                pd.cancel();
                                            Log.w(TAG, "Error creating a new classroom...", e);
                                            Toast.makeText(ClassroomManagementActivity.this, "Error creating a new classroom", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }

                        ).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }

                ).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        startStudentFetch();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (pd != null)
            pd.cancel();
        DiviApplication.get().getRequestQueue().cancelAll(this);
    }

    @Override
    public void onCourseChange() {
        finish();
    }

    private void startStudentFetch() {
        if (fetchStudentsTask != null)
            fetchStudentsTask.cancel(false);
        fetchStudentsTask = new FetchStudentsTask();
        classRooms.clear();
        allStudents.clear();
        ArrayList<UserData.ClassRoom> classRoomsToFetch = new ArrayList<UserData.ClassRoom>();
        for (int i = 0; i < userSessionProvider.getUserData().classRooms.length; i++) {
            if (!userSessionProvider.getUserData().classRooms[i].classId.equalsIgnoreCase(Config.IGNORE_CLASS_ID)) {
                classRoomsToFetch.add(userSessionProvider.getUserData().classRooms[i]);
            }
        }
        fetchStudentsTask.execute(classRoomsToFetch.toArray(new UserData.ClassRoom[0]));
    }

    private class StudentsListAdapter extends BaseExpandableListAdapter {
        LayoutInflater inflater;

        public StudentsListAdapter() {
            inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getGroupCount() {
            return classRooms.size();
        }

        @Override
        public int getChildrenCount(int i) {
            return allStudents.get(classRooms.get(i).classId).members.length;
        }

        @Override
        public Object getGroup(int i) {
            return classRooms.get(i);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return allStudents.get(classRooms.get(groupPosition).classId).members[childPosition];
        }

        @Override
        public long getGroupId(int i) {
            return i;
        }

        @Override
        public long getChildId(int i, int i2) {
            return i2;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            UserData.ClassRoom classRoom = (UserData.ClassRoom) getGroup(groupPosition);
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_classname, parent, false);
            }
            TextView nameView = (TextView) convertView.findViewById(R.id.class_title);
            TextView classCodeView = (TextView) convertView.findViewById(R.id.class_code);
            nameView.setText(Html.fromHtml("<b>" + classRoom.className + "</b>  (" + getChildrenCount(groupPosition) + " members)"));
            classCodeView.setText("Classroom Code : " + Util.getCodeFromClassId(Integer.parseInt(classRoom.classId)));
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            ClassMembers.ClassMember classMember = (ClassMembers.ClassMember) getChild(groupPosition, childPosition);
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_student, parent, false);
            }
            TextView name = (TextView) convertView.findViewById(R.id.name);
            TextView loc = (TextView) convertView.findViewById(R.id.location);
            name.setText(classMember.name);
            FadeInNetworkImageView profilePic = (FadeInNetworkImageView) convertView.findViewById(R.id.profile_pic);
            profilePic.setDefaultImageResId(R.drawable.ic_profile);
            profilePic.setErrorImageResId(R.drawable.ic_profile);
            profilePic.setImageUrl(null, DiviApplication.get().getImageLoader());
            if (classMember.profilePic != null) {
                Uri picUri = Uri.parse(classMember.profilePic);
                if (picUri != null && picUri.getHost() != null)
                    profilePic.setImageUrl(classMember.profilePic, DiviApplication.get().getImageLoader());
            }
            // hide unused
            loc.setVisibility(View.VISIBLE);
            if (classMember.role.equalsIgnoreCase(ClassMembers.ClassMember.ROLE_TEACHER))
                loc.setText("Teacher");
            else
                loc.setVisibility(View.INVISIBLE);
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int i, int i2) {
            return false;
        }
    }

    private class FetchStudentsTask extends AsyncTask<UserData.ClassRoom, ClassMemberHolder, Integer> {
        private String url;
        //        private HashMap<String, ClassMembers> allClassMembers;
//        ArrayList<UserData.ClassRoom> classRoomsFetched = new ArrayList<UserData.ClassRoom>();
        private Gson gson;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_GETCLASSMEMBERS;
//            allClassMembers = new HashMap<String, ClassMembers>();
            allStudents.clear();
            classRooms.clear();
            gson = new Gson();
            pb.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(UserData.ClassRoom... classroomsToFetch) {
            Log.d(TAG, "ids:" + classroomsToFetch.length);
            try {
                for (UserData.ClassRoom classRoom : classroomsToFetch) {
                    JSONObject jsonRequest = new JSONObject();
                    jsonRequest.put("uid", userSessionProvider.getUserData().uid);
                    jsonRequest.put("token", userSessionProvider.getUserData().token);
                    jsonRequest.put("classRoomId", classRoom.classId);
                    if (LogConfig.DEBUG_ACTIVITIES)
                        Log.d(TAG, "request:\n" + jsonRequest);
                    RequestFuture<JSONObject> future = RequestFuture.newFuture();
                    JsonObjectRequest fetchUpdatesRequest = new JsonObjectRequest(Request.Method.POST, url, jsonRequest, future, future);
                    fetchUpdatesRequest.setShouldCache(false);
                    fetchUpdatesRequest.setRetryPolicy(new DefaultRetryPolicy(5000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    DiviApplication.get().getRequestQueue().add(fetchUpdatesRequest).setTag(ClassroomManagementActivity.this);

                    JSONObject response = future.get(11000, TimeUnit.MILLISECONDS);
                    if (LogConfig.DEBUG_ACTIVITIES)
                        Log.d(TAG, "got response:\n" + response.toString());

                    ClassMembers classMembers = gson.fromJson(response.toString(), ClassMembers.class);

                    ClassMemberHolder cmh = new ClassMemberHolder();
                    cmh.classMembers = classMembers;
                    cmh.classRoom = classRoom;
                    publishProgress(new ClassMemberHolder[]{cmh});
                }
                return 0;
            } catch (TimeoutException te) {
                Toast.makeText(ClassroomManagementActivity.this, "Timeout fetching details...", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.w(TAG, "Error fetching classroom details", e);
                Toast.makeText(ClassroomManagementActivity.this, "Error - " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return 1;
        }

        @Override
        protected void onProgressUpdate(ClassMemberHolder... values) {
            ClassMemberHolder cmh = values[0];
            allStudents.put(cmh.classRoom.classId, cmh.classMembers);
            classRooms.add(cmh.classRoom);
            listAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Integer ret) {
            pb.setVisibility(View.GONE);
            if (ret != 0) {
                Toast.makeText(ClassroomManagementActivity.this, "Error fetching classroom details, please check your internet connection.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }


    }

    static class ClassMemberHolder {
        ClassMembers classMembers;
        UserData.ClassRoom classRoom;
    }
}
