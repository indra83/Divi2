package co.in.divi.fragment;

import java.util.Timer;
import java.util.TimerTask;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import co.in.divi.R;
import co.in.divi.util.AccessPoint;
import co.in.divi.util.Util;

public class WiFiFragment extends Fragment {

	ImageView	signalView;
	TextView	networkName;

	WifiManager	wifiManager;
	Timer		timer	= new Timer();
	Handler		handler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handler = new Handler();
		wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_wifiwidget, container, false);
	}

	@Override
	public void onStart() {
		super.onStart();
		signalView = (ImageView) getView().findViewById(R.id.wifi_level);
		networkName = (TextView) getView().findViewById(R.id.wifi_name);
		IntentFilter intentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		getActivity().registerReceiver(myWifiReceiver, intentFilter);
		getView().setClickable(true);
		getView().setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DialogFragment wifiFragment = new WiFiSettingsFragment();
				FragmentManager fm = getFragmentManager();
				wifiFragment.show(fm, "WiFi");
			}
		});
	}

	@Override
	public void onStop() {
		super.onStop();
		getActivity().unregisterReceiver(myWifiReceiver);
		timer.cancel();
	}

	private void updateWiFiUI() {
		if (!Util.isNetworkOn(getActivity())) {
			networkName.setText("Configure WiFi");
			signalView.setImageResource(R.drawable.ic_signal_off);
			return;
		}
		networkName.setText(AccessPoint.removeDoubleQuotes(wifiManager.getConnectionInfo().getSSID()));
		int signalLevel = WifiManager.calculateSignalLevel(wifiManager.getConnectionInfo().getRssi(), 5);
		switch (signalLevel) {
		case 0:
			signalView.setImageResource(R.drawable.ic_signal_0);
			break;
		case 1:
			signalView.setImageResource(R.drawable.ic_signal_1);
			break;
		case 2:
			signalView.setImageResource(R.drawable.ic_signal_2);
			break;
		case 3:
			signalView.setImageResource(R.drawable.ic_signal_3);
			break;
		case 4:
			signalView.setImageResource(R.drawable.ic_signal_4);
			break;
		default:
			signalView.setImageResource(R.drawable.ic_signal_0);
			break;
		}
	}

	private BroadcastReceiver	myWifiReceiver	= new BroadcastReceiver() {

													@Override
													public void onReceive(Context context, Intent intent) {
														String action = intent.getAction();
														if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {

															int extraWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
																	WifiManager.WIFI_STATE_UNKNOWN);

															switch (extraWifiState) {
															case WifiManager.WIFI_STATE_DISABLED:
																timer.cancel();
																signalView.setImageResource(R.drawable.ic_signal_off);
																networkName.setText("WiFi Off");
																break;
															case WifiManager.WIFI_STATE_DISABLING:
																timer.cancel();
																break;
															case WifiManager.WIFI_STATE_ENABLED:
																timer.cancel();
																timer = new Timer();
																timer.scheduleAtFixedRate(new TimerTask() {
																	@Override
																	public void run() {
																		handler.post(new Runnable() {
																			@Override
																			public void run() {
																				updateWiFiUI();
																			}
																		});
																	}
																}, 0, 2000);

																return;
																// break;
															case WifiManager.WIFI_STATE_ENABLING:
																timer.cancel();
																break;
															case WifiManager.WIFI_STATE_UNKNOWN:
																timer.cancel();
																signalView.setImageResource(R.drawable.ic_signal_off);
																networkName.setText("WiFi state unknown");
																break;
															}
														}
													}
												};
}
