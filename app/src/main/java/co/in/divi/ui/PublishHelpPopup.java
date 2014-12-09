package co.in.divi.ui;

import android.app.DialogFragment;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import co.in.divi.R;
import co.in.divi.fragment.BaseDialogFragment;

/**
 * Created by indraneel on 09-12-2014.
 */
public class PublishHelpPopup extends BaseDialogFragment {
    private static final String TAG = PublishHelpPopup.class.getSimpleName();

    private ViewGroup popupRoot;
    private LayoutInflater layoutInflater;
    private TextView helpText;
    private Button contactNowButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.UserDataDialog);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layoutInflater = inflater;
        popupRoot = (ViewGroup) layoutInflater.inflate(R.layout.popup_publish_help, container);
        helpText = (TextView) popupRoot.findViewById(R.id.help_text);
        contactNowButton = (Button) popupRoot.findViewById(R.id.contact_button);

        helpText.setText(Html.fromHtml("<h3>Publish your own books!</h3><b>Divi</b> allows you to publish your own books to your classrooms.<br/><br/>Publishing to the Divi platform is a multi-step process. Please drop us an email to get started."));

        contactNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + "publishing@divi.co.in"));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Interested in publishing content");
                intent.putExtra(Intent.EXTRA_TEXT, "Hi, I'm interested in publishing my own content to the Divi platform. Please send me the required info.");
                startActivity(intent);
            }
        });

        return popupRoot;
    }

    @Override
    public void onStart() {
        super.onStart();
        Point size = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(size);
        popupRoot.getLayoutParams().width = size.x / 2;
        popupRoot.requestLayout();
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
