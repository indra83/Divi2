package co.in.divi.fragment;

import co.in.divi.lecture.DashboardDialog;
import android.app.Dialog;
import android.os.Bundle;

public class DashboardDialogFragment extends BaseDialogFragment {

	DashboardDialog	dialog;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		dialog = new DashboardDialog(getActivity());
		return dialog;
	}

	@Override
	public void onStart() {
		dialog.init();
		super.onStart();
	}

	@Override
	public void onStop() {
		super.onStop();
		dialog.dismiss();
	}

}
