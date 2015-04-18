package co.in.divi.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.transition.Fade;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.text.SimpleDateFormat;

import co.in.divi.DiviApplication;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.activity.DiaryReportActivity;
import co.in.divi.diary.DiaryEntry;
import co.in.divi.model.ClassMembers;
import co.in.divi.util.ServerConfig;

/**
 * Created by Indra on 4/13/2015.
 */
public class DiaryEntryViewerUI extends LinearLayout {
    private static final String TAG = DiaryEntryViewerUI.class.getSimpleName();
    private static SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");

    public interface CloseViewer {
        public void closeViewer();
    }

    private TextView title, teacherName, dueDate, className, message;
    private LinearLayout resourcesContainer;
    private Button closeButton;
    private FadeInNetworkImageView profilePic;

    //Teacher panel
    private TextView reportText;
    private Button reportButton;
    private ImageView refreshButton;

    private CloseViewer closeViewer;
    private DiaryEntry de;
    private boolean statusFetchInProgress;
    private UserSessionProvider userSessionProvider;

    public DiaryEntryViewerUI(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(DiaryEntry diaryEntry, final CloseViewer closeViewer) {
        this.closeViewer = closeViewer;
        this.de = diaryEntry;
        userSessionProvider = UserSessionProvider.getInstance(getContext());
        title = (TextView) findViewById(R.id.title);
        message = (TextView) findViewById(R.id.message);
        teacherName = (TextView) findViewById(R.id.from);
        dueDate = (TextView) findViewById(R.id.date);
        className = (TextView) findViewById(R.id.to);
        resourcesContainer = (LinearLayout) findViewById(R.id.resources);
        closeButton = (Button) findViewById(R.id.closeButton);
        reportButton = (Button) findViewById(R.id.reportButton);
        refreshButton = (ImageView) findViewById(R.id.refresh);
        reportText = (TextView) findViewById(R.id.reportText);
        profilePic = (FadeInNetworkImageView) findViewById(R.id.profile_pic);

        teacherName.setText(de.teacherName);
        className.setText("to " + de.classId);
        dueDate.setText(sdf.format(de.dueDate).toString());
        title.setText(de.title);
        message.setText(de.message);
        profilePic.setErrorImageResId(R.drawable.ic_profile);
        profilePic.setImageUrl(null, DiviApplication.get().getImageLoader());
        if (de.teacherProfilePic != null) {
            Uri picUri = Uri.parse(de.teacherProfilePic);
            if (picUri != null && picUri.getHost() != null)
                profilePic.setImageUrl(de.teacherProfilePic, DiviApplication.get().getImageLoader());
        }

        closeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (closeViewer != null)
                    closeViewer.closeViewer();
            }
        });
        reportButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launchReport = new Intent(getContext(), DiaryReportActivity.class);
                launchReport.putExtra(DiaryReportActivity.INTENT_EXTRA_DIARYENTRY, new Gson().toJson(de));
                getContext().startActivity(launchReport);
            }
        });
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        for (DiaryEntry.Resource res : de.resources) {
            HomeworkResourceView hrv = (HomeworkResourceView) inflater.inflate(R.layout.item_homework_res, resourcesContainer, false);
            hrv.init(res, null);
            resourcesContainer.addView(hrv);
        }
        if (userSessionProvider.getUserData().isTeacher()) {
            refreshButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startStatusFetch();
                }
            });
            startStatusFetch();
        } else {
            findViewById(R.id.teacherPanel).setVisibility(View.GONE);
        }
    }

    public void stop() {
        DiviApplication.get().getRequestQueue().cancelAll(this);
    }

    private void startStatusFetch() {
        if (statusFetchInProgress)
            return;
        statusFetchInProgress = true;
        startAnim();
        reportButton.setVisibility(View.GONE);
        reportText.setText("Fetching report...");
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
                    reportText.setText("Synced " + synced + " out of " + total);
                    statusFetchInProgress = false;
                    stopAnim();
                    reportButton.setVisibility(View.VISIBLE);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.w(TAG, "error:" + error);
                    Toast.makeText(getContext(), "Error fetch class members.", Toast.LENGTH_LONG).show();
                    statusFetchInProgress = false;
                    stopAnim();
                    reportText.setText("Please check your internet.");
                }
            });
            getClassMemebersRequest.setShouldCache(false);
            DiviApplication.get().getRequestQueue().add(getClassMemebersRequest).setTag(this);
        } catch (Exception e) {
            Log.e(TAG, "Error sending instruction", e);
            Toast.makeText(getContext(), "Error fetching class members", Toast.LENGTH_LONG).show();
            statusFetchInProgress = false;
            stopAnim();
            reportText.setText("");
        }
    }

    private void startAnim() {
        Animation rotation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_refresh);
        rotation.setRepeatCount(Animation.INFINITE);
        refreshButton.startAnimation(rotation);
    }

    private void stopAnim() {
        refreshButton.clearAnimation();
    }
}
