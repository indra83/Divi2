package co.in.divi.fragment;

import android.app.Dialog;
import android.os.Bundle;
import co.in.divi.lecture.PostInstructionDialog;

public class PostInstructionDialogFragment extends BaseDialogFragment {
	public static final String	EXTRA_IS_BLACKOUT	= "EXTRA_IS_BLACKOUT";
	PostInstructionDialog		dialog;

	boolean						isBlackout;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		isBlackout = getArguments().getBoolean(EXTRA_IS_BLACKOUT, false);
		dialog = new PostInstructionDialog(getActivity(), isBlackout);
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