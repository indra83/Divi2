package co.in.divi.ui;

import co.in.divi.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

public class TwoLetterIcon extends TextView {

	public TwoLetterIcon(Context context, AttributeSet attribs) {
		super(context, attribs);
		setBackgroundResource(R.drawable.bg_home_section_selected);
		setGravity(Gravity.CENTER);
	}

}
