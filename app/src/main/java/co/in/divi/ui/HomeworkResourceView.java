package co.in.divi.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import co.in.divi.R;
import co.in.divi.diary.DiaryEntry;

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
        ImageView iconView = (ImageView) findViewById(R.id.icon);
        TextView chapterText = (TextView) findViewById(R.id.chapterText);
        TextView titleText = (TextView) findViewById(R.id.titleText);
        ImageView remove = (ImageView) findViewById(R.id.remove);

        iconView.setImageResource(R.drawable.ic_apps);
        if (res.breadcrumb.chapterName != null)
            chapterText.setText(res.breadcrumb.chapterName);
        if (res.breadcrumb.itemName != null)
            chapterText.setText(res.breadcrumb.itemName);

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
