package co.in.divi.fragment;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public class BaseDialogFragment extends DialogFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();
		hideBars(getDialog());
	}

	private void hideBars(Dialog d) {
		// if (Build.VERSION.SDK_INT < 16) {
		// getDialog().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		// WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// } else {
		// int newUiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
		// newUiOptions ^= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
		// | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN;
		// if (Build.VERSION.SDK_INT >= 18) {
		// newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		// }
		// d.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
		// }
		if (getDialog() == null || getDialog().getWindow() == null)
			return;
		if (Build.VERSION.SDK_INT < 18) {
			getDialog().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			int newUiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
			newUiOptions ^= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN;
			newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
			getDialog().getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
		}
	}
}
