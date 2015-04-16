package co.in.divi.ui;

import android.content.Context;
import android.transition.Fade;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.toolbox.FadeInNetworkImageView;

import java.text.SimpleDateFormat;

import co.in.divi.R;
import co.in.divi.diary.DiaryEntry;

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
    private FadeInNetworkImageView imageView;

private CloseViewer closeViewer;

    public DiaryEntryViewerUI(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(DiaryEntry de, final CloseViewer closeViewer) {
        this.closeViewer  = closeViewer;
        title = (TextView) findViewById(R.id.title);
        message = (TextView) findViewById(R.id.message);
        teacherName = (TextView) findViewById(R.id.from);
        dueDate = (TextView) findViewById(R.id.date);
        className = (TextView) findViewById(R.id.to);
        resourcesContainer = (LinearLayout) findViewById(R.id.resources);
        closeButton = (Button) findViewById(R.id.closeButton);
        imageView = (FadeInNetworkImageView) findViewById(R.id.profile_pic);

        teacherName.setText(de.teacherName);
        className.setText("to " + de.classId);
        dueDate.setText(sdf.format(de.dueDate).toString());
        title.setText(de.title);
        message.setText(de.message);

        closeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(closeViewer!=null)
                    closeViewer.closeViewer();
            }
        });
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        for (DiaryEntry.Resource res : de.resources) {
            HomeworkResourceView hrv = (HomeworkResourceView) inflater.inflate(R.layout.item_homework_res, resourcesContainer, false);
            hrv.init(res);
            resourcesContainer.addView(hrv);

        }
    }
}
