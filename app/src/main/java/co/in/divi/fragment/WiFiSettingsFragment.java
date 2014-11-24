package co.in.divi.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import co.in.divi.AdminPasswordManager;
import co.in.divi.R;
import co.in.divi.util.AccessPoint;
import co.in.divi.util.Config;
import co.in.divi.util.LogConfig;

public class WiFiSettingsFragment extends BaseDialogFragment {
	private static final String	TAG						= WiFiSettingsFragment.class.getName();

	private WifiManager			wifiManager;
	private Scanner				mScanner;

	Switch						wifiButton;
	ListView					networks;
	View						connectForm;
	TextView					title, networkName, passwordLabel;
	EditText					passwordText;
	Button						connect, cancel;

	public NetworksAdapter		networksAdapter			= null;

	AccessPoint					selectedAP				= null;
	boolean						listenToWifiState		= true;
	String						wifiStatusString		= null;
	String						connectBSSID			= null;
	ProgressDialog				pd						= null;

	private static final int	WIFI_RESCAN_INTERVAL_MS	= 10 * 1000;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_FRAME, R.style.UserDataDialog);
		mScanner = new Scanner();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		wifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_wifisettings, container, false);
	}

	@Override
	public void onStart() {
		super.onStart();
		networks = (ListView) getView().findViewById(R.id.networks);
		wifiButton = (Switch) getView().findViewById(R.id.btn_wifi);
		connectForm = getView().findViewById(R.id.connect_form);
		// connect view
		title = (TextView) getView().findViewById(R.id.title);
		networkName = (TextView) getView().findViewById(R.id.network_name);
		passwordText = (EditText) getView().findViewById(R.id.password);
		connect = (Button) getView().findViewById(R.id.button_connect);
		cancel = (Button) getView().findViewById(R.id.button_cancel);

		if (networksAdapter == null) {
			networksAdapter = new NetworksAdapter();
			networks.setAdapter(networksAdapter);
		}
		networks.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				selectedAP = (AccessPoint) networksAdapter.getItem(position);
				Log.d(TAG, "ssid::	" + selectedAP.ssid);
				if (selectedAP.ssid.startsWith(AdminPasswordManager.DIVI_WIFI_SSID_PREFIX)) {
					try {
						int challenge = Integer.parseInt(selectedAP.ssid.split(AdminPasswordManager.DIVI_WIFI_SSID_PREFIX)[1]);
						connectToAP(selectedAP, AdminPasswordManager.getInstance().getWifiPass(challenge));
						return;
					} catch (Exception e) {
						Log.w(TAG, "Error parsing divi ssid", e);
					}
				}
				connectForm.setVisibility(View.VISIBLE);
				networks.setVisibility(View.GONE);
				networkName.setText(selectedAP.ssid);
				passwordText.setText("");
			}
		});
		getView().findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		IntentFilter intentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		getActivity().registerReceiver(myWifiReceiver, intentFilter);

		intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		getActivity().registerReceiver(wifiConnectReceiver, intentFilter);

		refreshAP();
		updateWifiState();
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(networks.getWindowToken(), 0);

		// connect
		connectForm.setVisibility(View.GONE);
		cancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				connectForm.setVisibility(View.GONE);
				networks.setVisibility(View.VISIBLE);
				selectedAP = null;
			}
		});
		connect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				connectForm.setVisibility(View.GONE);
				networks.setVisibility(View.VISIBLE);
				connectToAP(selectedAP, passwordText.getText().toString());
			}
		});
	}

	@Override
	public void onStop() {
		super.onStop();
		mScanner.pause();
		getActivity().unregisterReceiver(myWifiReceiver);
		getActivity().unregisterReceiver(wifiConnectReceiver);
		if (pd != null)
			pd.cancel();
	}

	private void updateWifiState() {
		wifiButton.setOnCheckedChangeListener(null);
		if (wifiManager.isWifiEnabled())
			wifiButton.setChecked(true);
		else {
			wifiButton.setChecked(false);
			networksAdapter.setAccessPoints(new ArrayList<AccessPoint>());
		}
		wifiButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked)
					wifiManager.setWifiEnabled(true);
				else
					wifiManager.setWifiEnabled(false);
			}
		});
	}

	private BroadcastReceiver	myWifiReceiver	= new BroadcastReceiver() {

													@Override
													public void onReceive(Context context, Intent intent) {
														String action = intent.getAction();
														if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
															// Toast.makeText(getActivity(),
															// "SCAN_RESULTS_AVAILABLE_ACTION",
															// Toast.LENGTH_LONG).show();
															updateNetworksList();
														} else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {

															int extraWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
																	WifiManager.WIFI_STATE_UNKNOWN);

															switch (extraWifiState) {
															case WifiManager.WIFI_STATE_DISABLED:
																// Toast.makeText(getActivity(), "WIFI_STATE_DISABLED",
																// Toast.LENGTH_LONG).show();
																updateWifiState();
																break;
															case WifiManager.WIFI_STATE_DISABLING:
																// Toast.makeText(getActivity(), "WIFI_STATE_DISABLING",
																// Toast.LENGTH_LONG).show();
																break;
															case WifiManager.WIFI_STATE_ENABLED:
																// Toast.makeText(getActivity(), "WIFI_STATE_ENABLED",
																// Toast.LENGTH_LONG).show();
																updateWifiState();
																updateNetworksList();
																mScanner.resume();
																return;
																// break;
															case WifiManager.WIFI_STATE_ENABLING:
																Toast.makeText(getActivity(), "Turning WiFi on...", Toast.LENGTH_LONG)
																		.show();
																break;
															case WifiManager.WIFI_STATE_UNKNOWN:
																updateWifiState();
																break;
															}
															mScanner.pause();
														}
													}
												};

	public void connectToAP(AccessPoint ap, String password) {
		if (LogConfig.DEBUG_WIFI)
			Log.d(TAG, "Connecting to ap:" + ap.ssid);
		// clear existing connection infos (so we don't auto connect)
		if (Config.AUTO_CLEAR_WIFI_PASSWORDS) {
			List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
			if (configs != null) {
				for (WifiConfiguration config : configs) {
					if (LogConfig.DEBUG_WIFI)
						Log.d(TAG, "removing wifi config: " + config.SSID);
					wifiManager.removeNetwork(config.networkId);
				}
				wifiManager.saveConfiguration();
			}
		}
		WifiConfiguration wc = new WifiConfiguration();
		// IMP! should be in Quotes!!
		wc.SSID = AccessPoint.convertToQuotedString(ap.ssid);
		this.connectBSSID = ap.bssid;
		if (ap.security == AccessPoint.SECURITY_WEP) {
			wc.hiddenSSID = true;
			wc.status = WifiConfiguration.Status.DISABLED;
			wc.priority = 40;
			wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
			wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
			wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
			wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

			if (password.length() != 0) {
				int length = password.length();
				// WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
				if ((length == 10 || length == 26 || length == 58) && password.matches("[0-9A-Fa-f]*")) {
					wc.wepKeys[0] = password;
				} else {
					wc.wepKeys[0] = '"' + password + '"';
				}
			}
			wc.wepTxKeyIndex = 0;
		} else if (ap.security == AccessPoint.SECURITY_PSK) {
			wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			if (password.matches("[0-9A-Fa-f]{64}")) {
				wc.preSharedKey = password;
			} else {
				wc.preSharedKey = '"' + password + '"';
			}
		}

		// boolean res1 = wifiManag.setWifiEnabled(true);
		if (LogConfig.DEBUG_WIFI)
			Log.d(TAG, "saving:" + wc.SSID + "," + wc.preSharedKey);
		int res = wifiManager.addNetwork(wc);
		if (LogConfig.DEBUG_WIFI)
			Log.d("WifiPreference", "add Network returned " + res);
		boolean es = wifiManager.saveConfiguration();
		if (LogConfig.DEBUG_WIFI)
			Log.d("WifiPreference", "saveConfiguration returned " + es);
		wifiManager.disconnect();
		listenToWifiState = wifiManager.enableNetwork(res, true);
		// listenToWifiState = true;
		if (listenToWifiState) {
			wifiStatusString = "Connecting...";
			if (networksAdapter != null)
				networksAdapter.notifyDataSetChanged();
		} else {
			Toast.makeText(getActivity(), "Connection failed, wrong password?", Toast.LENGTH_LONG).show();
		}
		if (LogConfig.DEBUG_WIFI)
			Log.d("WifiPreference", "enableNetwork returned " + listenToWifiState);
		wifiManager.reconnect();
	}

	private BroadcastReceiver	wifiConnectReceiver	= new BroadcastReceiver() {

														boolean	gotAssociating	= false;

														@Override
														public void onReceive(Context context, Intent intent) {
															String action = intent.getAction();
															if (LogConfig.DEBUG_WIFI)
																Log.d(TAG, "onReceive:" + intent.toString() + ", iswifilisten?"
																		+ listenToWifiState);
															if (connectBSSID == null)
																return;
															if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
																SupplicantState extraWifiState = intent
																		.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
																// if (!listenToWifiState)
																// return;
																if (LogConfig.DEBUG_WIFI)
																	Log.d(TAG, "extra:" + extraWifiState);
																if (extraWifiState == SupplicantState.ASSOCIATING
																		|| extraWifiState == SupplicantState.ASSOCIATED) {
																	if (listenToWifiState) {
																		gotAssociating = true;
																		if (pd != null)
																			pd.cancel();
																		pd = ProgressDialog.show(getActivity(), "Connecting...",
																				"Please wait while we connect to the WIFI network");
																	}
																	wifiStatusString = "Connecting...";
																	if (networksAdapter != null)
																		networksAdapter.notifyDataSetChanged();
																} else if (extraWifiState == SupplicantState.FOUR_WAY_HANDSHAKE) {
																	wifiStatusString = "Authenticating";
																	if (networksAdapter != null)
																		networksAdapter.notifyDataSetChanged();
																} else if (extraWifiState == SupplicantState.DISCONNECTED) {
																	if (!gotAssociating)
																		return;
																	if (listenToWifiState)
																		Toast.makeText(getActivity(), "Connection failed, wrong password?",
																				Toast.LENGTH_LONG).show();
																	wifiStatusString = null;
																	listenToWifiState = false;
																	gotAssociating = false;
																	if (networksAdapter != null)
																		networksAdapter.notifyDataSetChanged();
																	if (pd != null)
																		pd.cancel();
																} else if (extraWifiState == SupplicantState.COMPLETED) {
																	// if (!gotAssociating)
																	// return;
																	wifiStatusString = null;
																	listenToWifiState = false;
																	gotAssociating = false;
																	if (pd != null)
																		pd.cancel();
																	Toast.makeText(getActivity(), "Connected!", Toast.LENGTH_LONG).show();
																}
															} else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
																NetworkInfo networkInfo = (NetworkInfo) intent.getExtras().get(
																		WifiManager.EXTRA_NETWORK_INFO);
																if (networkInfo.getState().equals(NetworkInfo.State.CONNECTED)
																		&& networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
																	if (pd != null)
																		pd.cancel();
																	if (connectBSSID.equals(wifiManager.getConnectionInfo().getBSSID())) {
																		if (LogConfig.DEBUG_WIFI)
																			Log.d(TAG, "connected to wifi!!");
																		Toast.makeText(getActivity(), "Connected!", Toast.LENGTH_LONG)
																				.show();
																	}
																}
															}
														}
													};

	class NetworksAdapter extends BaseAdapter {

		ArrayList<AccessPoint>	accessPoints	= new ArrayList<AccessPoint>();

		LayoutInflater			inflater;

		public NetworksAdapter() {
			inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public void setAccessPoints(List<AccessPoint> accessPoints) {
			if (LogConfig.DEBUG_WIFI) {
				Log.d(TAG, "networks:" + accessPoints.size());
				Log.d(TAG, "connected to:" + wifiManager.getConnectionInfo().getBSSID());
			}
			this.accessPoints.clear();
			AccessPoint connectedAP = null;
			for (AccessPoint ap : accessPoints) {
				if (ap.bssid.equals(wifiManager.getConnectionInfo().getBSSID())) {
					connectedAP = ap;
					break;
				}
			}
			if (connectedAP != null) {
				accessPoints.remove(connectedAP);
				accessPoints.add(0, connectedAP);
			}
			this.accessPoints.addAll(accessPoints);
			notifyDataSetChanged();
		}

		public ArrayList<AccessPoint> getAccessPoints() {
			return accessPoints;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public int getCount() {
			return accessPoints.size();
		}

		@Override
		public Object getItem(int position) {
			return accessPoints.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.item_network, parent, false);
			}
			TextView networkName = (TextView) convertView.findViewById(R.id.text1);
			TextView networkState = (TextView) convertView.findViewById(R.id.text2);
			ImageView wifiSignal = (ImageView) convertView.findViewById(R.id.ic_signal);
			ImageView wifiSignalSecurity = (ImageView) convertView.findViewById(R.id.ic_signal_security);
			AccessPoint ap = accessPoints.get(position);
			networkName.setText(ap.ssid);
			networkState.setText("");
			if (ap.bssid != null && ap.bssid.equals(wifiManager.getConnectionInfo().getBSSID())) {
				if (wifiManager.getConnectionInfo().getSupplicantState() == SupplicantState.COMPLETED)
					networkState.setText(" Connected!");
			}
			// else if (ap.bssid.equals(((MainActivity) getActivity()).connectBSSID)) {
			// if (((MainActivity) getActivity()).listenToWifiState) {
			// networkState.setText(" " + ((MainActivity) getActivity()).wifiStatusString);
			// }
			// }
			int signalLevel = ap.getLevel();
			if (ap.security == AccessPoint.SECURITY_NONE) {
				wifiSignalSecurity.setImageResource(0);
			} else {
				wifiSignalSecurity.setImageResource(R.drawable.ic_network_locked);
			}
			switch (signalLevel) {
			case 0:
				wifiSignal.setImageResource(R.drawable.ic_signal_0);
				break;
			case 1:
				wifiSignal.setImageResource(R.drawable.ic_signal_1);
				break;
			case 2:
				wifiSignal.setImageResource(R.drawable.ic_signal_2);
				break;
			case 3:
				wifiSignal.setImageResource(R.drawable.ic_signal_3);
				break;
			case 4:
				wifiSignal.setImageResource(R.drawable.ic_signal_4);
				break;
			default:
				wifiSignal.setImageResource(R.drawable.ic_signal_0);
				break;
			}
			return convertView;
		}
	}

	private void updateNetworksList() {
		if (LogConfig.DEBUG_WIFI)
			Log.d(TAG, "updateNetworksList");

		ArrayList<AccessPoint> accessPoints = new ArrayList<AccessPoint>();
		/**
		 * Lookup table to more quickly update AccessPoints by only considering objects with the correct SSID. Maps SSID
		 * -> List of AccessPoints with the given SSID.
		 */
		Multimap<String, AccessPoint> apMap = new Multimap<String, AccessPoint>();

		final List<ScanResult> results = wifiManager.getScanResults();
		if (results != null) {
			for (ScanResult result : results) {
				// Ignore hidden and ad-hoc networks.
				if (result.SSID == null || result.SSID.length() == 0 || result.capabilities.contains("[IBSS]")) {
					continue;
				}
				// Log.d(TAG, "scan::" + result.SSID + ",," +
				// result.capabilities);

				boolean found = false;
				for (AccessPoint accessPoint : apMap.getAll(result.SSID)) {
					if (accessPoint.update(result))
						found = true;
				}
				if (!found) {
					AccessPoint accessPoint = new AccessPoint(result);
					accessPoints.add(accessPoint);
					apMap.put(accessPoint.ssid, accessPoint);
				}
			}
		}

		// Pre-sort accessPoints to speed preference insertion
		Collections.sort(accessPoints);
		networksAdapter.setAccessPoints(accessPoints);
	}

	private class Multimap<K, V> {
		private HashMap<K, List<V>>	store	= new HashMap<K, List<V>>();

		/** retrieve a non-null list of values with key K */
		List<V> getAll(K key) {
			List<V> values = store.get(key);
			return values != null ? values : Collections.<V> emptyList();
		}

		void put(K key, V val) {
			List<V> curVals = store.get(key);
			if (curVals == null) {
				curVals = new ArrayList<V>(3);
				store.put(key, curVals);
			}
			curVals.add(val);
		}
	}

	private void refreshAP() {
		if (LogConfig.DEBUG_WIFI)
			Log.d(TAG, "refreshAP");
		if (wifiManager.isWifiEnabled()) {
			mScanner.resume();
		}
	}

	// TODO: handler leak??
	private class Scanner extends Handler {
		private int	mRetry	= 0;

		private int	count	= 0;

		void resume() {
			if (!hasMessages(0)) {
				sendEmptyMessage(0);
			}
		}

		void forceScan() {
			removeMessages(0);
			sendEmptyMessage(0);
		}

		void pause() {
			mRetry = 0;
			removeMessages(0);
		}

		@Override
		public void handleMessage(Message message) {
			count++;
			if (wifiManager.startScan()) {
				mRetry = 0;
			} else if (++mRetry >= 3) {
				mRetry = 0;
				Activity activity = getActivity();
				if (activity != null) {
					Toast.makeText(activity, "Scan failed", Toast.LENGTH_LONG).show();
				}
				return;
			}
			sendEmptyMessageDelayed(0, WIFI_RESCAN_INTERVAL_MS);
		}
	}

}
