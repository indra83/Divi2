package co.in.divi.ui;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import co.in.divi.LectureSessionProvider;
import co.in.divi.LectureSessionProvider.DashboardChangeListener;
import co.in.divi.Location;
import co.in.divi.LocationManager;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.lecture.DashboardDialog;
import co.in.divi.lecture.PostInstructionDialog;
import co.in.divi.model.ClassMembers.ClassMember;
import co.in.divi.util.Config;

public class TeacherPanel extends LinearLayout implements DashboardChangeListener {
	private static final String	TAG	= TeacherPanel.class.getSimpleName();

	LocationManager				locationManager;
	UserSessionProvider			userSessionProvider;
	LectureSessionProvider		lectureSessionProvider;

	DashboardDialog				dashDialog;
	PostInstructionDialog		postInstructionDialog;

	TextView					attendanceText;

	public TeacherPanel(Context context, AttributeSet attrs) {
		super(context, attrs);
		locationManager = LocationManager.getInstance(context);
		userSessionProvider = UserSessionProvider.getInstance(context);
		lectureSessionProvider = LectureSessionProvider.getInstance(context);
	}

	public void initialize() {
		attendanceText = (TextView) findViewById(R.id.attendance);
		findViewById(R.id.btn_dashboard).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!lectureSessionProvider.isCurrentUserTeacher())
					return;
				if (dashDialog != null && dashDialog.isShowing())
					dashDialog.dismiss();
				dashDialog = new DashboardDialog(getContext());
				dashDialog.init();
				dashDialog.show();
			}
		});
		findViewById(R.id.btn_share).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!lectureSessionProvider.isCurrentUserTeacher())
					return;
				if (postInstructionDialog != null && postInstructionDialog.isShowing())
					postInstructionDialog.dismiss();

				if (!Config.ENABLE_EXTERNAL_APP_SHARING && locationManager.getLocation().getLocationRef() == null) {
					Toast.makeText(getContext(), "Open the resource you want to share", Toast.LENGTH_LONG).show();
					return;
				}
				postInstructionDialog = new PostInstructionDialog(getContext(), false);
				postInstructionDialog.init();
				postInstructionDialog.show();
			}
		});
		findViewById(R.id.btn_blackout).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!lectureSessionProvider.isCurrentUserTeacher())
					return;
				if (postInstructionDialog != null && postInstructionDialog.isShowing())
					postInstructionDialog.dismiss();
				postInstructionDialog = new PostInstructionDialog(getContext(), true);
				postInstructionDialog.init();
				postInstructionDialog.show();
			}
		});
		// attendance
		lectureSessionProvider.addDashboardListener(this);
		updateAttendance();
	}

	public void stopAndRemove() {
		lectureSessionProvider.removeDashboardListener(this);
		if (postInstructionDialog != null && postInstructionDialog.isShowing())
			postInstructionDialog.dismiss();
		if (dashDialog != null && dashDialog.isShowing())
			dashDialog.dismiss();
		((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).removeView(this);
	}

	private void updateAttendance() {
		int online = 0;
		int offline = 0;
		for (ClassMember member : lectureSessionProvider.getClassMembers()) {
			if (member.role.equalsIgnoreCase(ClassMember.ROLE_TEACHER) || member.role.equalsIgnoreCase(ClassMember.ROLE_TESTER))
				continue;
			if (lectureSessionProvider.hereNow().contains(member.uid)) {
				online++;
			} else {
				offline++;
			}
		}
		attendanceText.setText(Html.fromHtml(String.format("<b><font color='#006600'>%d</font> - <font color='#CC0000'>%d</font></b>",
				online, offline)));
	}

	@Override
	public void onDashboardChange() {
		updateAttendance();
	}
}
