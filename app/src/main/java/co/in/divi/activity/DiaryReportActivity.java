package co.in.divi.activity;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.FadeInNetworkImageView;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;

import org.json.JSONObject;

import co.in.divi.BaseActivity;
import co.in.divi.DiviApplication;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.diary.DiaryEntry;
import co.in.divi.model.ClassMembers;
import co.in.divi.util.ServerConfig;

/**
 * Created by Indra on 4/16/2015.
 */
public class DiaryReportActivity extends BaseActivity {
    private static final String TAG = DiaryReportActivity.class.getSimpleName();
    public static final String INTENT_EXTRA_DIARYENTRY = "INTENT_EXTRA_DIARYENTRY";

    private GridView gridView;
    private ProgressBar pb1;

    private DiaryEntry de;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diaryreport);
        gridView = (GridView) findViewById(R.id.grid);
        pb1 = (ProgressBar) findViewById(R.id.progress1);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!userSessionProvider.isLoggedIn() || !userSessionProvider.getUserData().isTeacher()) {
            Toast.makeText(this, "User is logged out!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        de = new Gson().fromJson(getIntent().getStringExtra(INTENT_EXTRA_DIARYENTRY), DiaryEntry.class);
        startStatusFetch();
    }

    @Override
    protected void onStop() {
        super.onStop();
        DiviApplication.get().getRequestQueue().cancelAll(this);
    }

    private void startStatusFetch() {
        pb1.setVisibility(View.VISIBLE);
        try {
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("uid", userSessionProvider.getUserData().uid);
            jsonRequest.put("token", userSessionProvider.getUserData().token);
            jsonRequest.put("classRoomId", de.classId);
            String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_GETCLASSMEMBERS;
            Log.d(TAG, "sending:" + jsonRequest.toString());
            JsonObjectRequest getClassMemebersRequest = new JsonObjectRequest(Request.Method.POST, url, jsonRequest, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d(TAG, "got response:\n" + response.toString());
                    ClassMembers resp = new Gson().fromJson(response.toString(), ClassMembers.class);
                    int total = 0;
                    int synced = 0;
                    for (ClassMembers.ClassMember member : resp.members) {
                        if (member.role.equals(ClassMembers.ClassMember.ROLE_STUDENT)) {
                            total++;
                            if (member.lastSyncTimes != null && member.lastSyncTimes.commands > de.createdAt)
                                synced++;
                        }
                    }
                    pb1.setVisibility(View.GONE);
                    StudentsAdapter adapter = new StudentsAdapter(resp.members);
                    gridView.setAdapter(adapter);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.w(TAG, "error:" + error);
                    Toast.makeText(DiaryReportActivity.this, "Error fetch class members.", Toast.LENGTH_LONG).show();
                    finish();
                }
            });
            getClassMemebersRequest.setShouldCache(false);
            DiviApplication.get().getRequestQueue().add(getClassMemebersRequest).setTag(this);
        } catch (Exception e) {
            Log.e(TAG, "Error sending instruction", e);
            Toast.makeText(DiaryReportActivity.this, "Error fetch class members.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    class StudentsAdapter extends BaseAdapter {
        ClassMembers.ClassMember[] classMembers;
        LayoutInflater inflater;

        Typeface bold;

        public StudentsAdapter(ClassMembers.ClassMember[] members) {
            inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            bold = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf");
            this.classMembers = members;
        }

        public int getCount() {
            return classMembers.length;
        }

        public Object getItem(int position) {
            return classMembers[position];
        }

        public long getItemId(int position) {
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_diaryreport, parent, false);
            }

            ClassMembers.ClassMember member = (ClassMembers.ClassMember) getItem(position);
            FadeInNetworkImageView profilePic = (FadeInNetworkImageView) convertView.findViewById(R.id.profile_pic);
            profilePic.setDefaultImageResId(R.drawable.ic_profile);
            TextView name = (TextView) convertView.findViewById(R.id.name);

            profilePic.setErrorImageResId(R.drawable.ic_profile);
            profilePic.setImageUrl(null, DiviApplication.get().getImageLoader());
            if (member.profilePic != null) {
                Uri picUri = Uri.parse(member.profilePic);
                if (picUri != null && picUri.getHost() != null)
                    profilePic.setImageUrl(member.profilePic, DiviApplication.get().getImageLoader());
            }
            name.setText(member.name);

            return convertView;
        }
    }

    @Override
    public void onCourseChange() {
        finish();
    }
}
