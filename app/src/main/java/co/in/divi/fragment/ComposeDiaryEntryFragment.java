package co.in.divi.fragment;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.ui.UserDataPopup;

public class ComposeDiaryEntryFragment extends BaseDialogFragment {
	private static final String	TAG	= UserDataPopup.class.getSimpleName();

	LayoutInflater				layoutInflater;

	UserSessionProvider			userSessionProvider;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_FRAME, R.style.UserDataDialog);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		userSessionProvider = UserSessionProvider.getInstance(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		layoutInflater = inflater;
		ViewGroup popupRoot = (ViewGroup) layoutInflater.inflate(R.layout.popup_userdata, null);
		// name = (TextView) popupRoot.findViewById(R.id.name);

		return popupRoot;
	}

	@Override
	public void onStart() {
		super.onStart();

	}
}
