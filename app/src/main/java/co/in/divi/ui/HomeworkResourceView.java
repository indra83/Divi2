package co.in.divi.ui;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import co.in.divi.Location;
import co.in.divi.R;
import co.in.divi.diary.DiaryEntry;
import co.in.divi.util.Util;

/**
 * Created by Indra on 4/8/2015.
 */
public class HomeworkResourceView extends RelativeLayout {

    public interface RemoveResourceHelper {
        public void removeResource(DiaryEntry.Resource res);
    }

    public HomeworkResourceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(final DiaryEntry.Resource res, final RemoveResourceHelper removeResourceHelper) {
        setClickable(true);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Util.openInstruction(getContext(), Uri.parse(res.uri));
            }
        });
        ImageView iconView = (ImageView) findViewById(R.id.icon);
        TextView chapterText = (TextView) findViewById(R.id.chapterText);
        TextView titleText = (TextView) findViewById(R.id.titleText);
        ImageView remove = (ImageView) findViewById(R.id.remove);

        if (res.locationType == Location.LOCATION_TYPE.ASSESSMENT)
            iconView.setImageResource(R.drawable.ic_hw_quiz_n);
        else if (res.locationType == Location.LOCATION_TYPE.TOPIC) {
            if (res.locationSubType == Location.LOCATION_SUBTYPE.TOPIC_IMAGE || res.locationSubType == Location.LOCATION_SUBTYPE.TOPIC_IMAGESET)
                iconView.setImageResource(R.drawable.ic_hw_image_n);
            else if (res.locationSubType == Location.LOCATION_SUBTYPE.TOPIC_VIDEO)
                iconView.setImageResource(R.drawable.ic_hw_video_n);
            else if (res.locationSubType == Location.LOCATION_SUBTYPE.TOPIC_TOPIC)
                iconView.setImageResource(R.drawable.ic_hw_topic_n);
        }
        if (res.breadcrumb.chapterName != null)
            chapterText.setText(res.breadcrumb.chapterName);
        if (res.breadcrumb.itemName != null) {
            String title = res.breadcrumb.itemName;
            if (res.breadcrumb.subItemName != null)
                title = title + " > " + res.breadcrumb.subItemName;
            titleText.setText(title);
        }

        if (removeResourceHelper != null) {
            remove.setVisibility(View.VISIBLE);
            remove.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeResourceHelper.removeResource(res);
                }
            });
        }
    }
}
