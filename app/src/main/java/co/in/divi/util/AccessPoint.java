package co.in.divi.util;

import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class AccessPoint implements Comparable<AccessPoint> {
	private static final String	TAG					= "AccessPoint";

	private static final int	INVALID_NETWORK_ID	= -1;

	/**
	 * These values are matched in string arrays -- changes must be kept in sync
	 */
	public static final int		SECURITY_NONE		= 0;
	public static final int		SECURITY_WEP		= 1;
	public static final int		SECURITY_PSK		= 2;
	public static final int		SECURITY_EAP		= 3;

	enum PskType {
		UNKNOWN, WPA, WPA2, WPA_WPA2
	}

	public String				ssid;
	public String				bssid;
	public int					security;
	public int					networkId;
	boolean						wpsAvailable	= false;

	PskType						pskType			= PskType.UNKNOWN;

	private WifiConfiguration	mConfig;
	public ScanResult			mScanResult;

	private int					mRssi;
	private WifiInfo			mInfo;
	private DetailedState		mState;

	static int getSecurity(WifiConfiguration config) {
		if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
			return SECURITY_PSK;
		}
		if (config.allowedKeyManagement.get(KeyMgmt.WPA_EAP) || config.allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
			return SECURITY_EAP;
		}
		return (config.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
	}

	private static int getSecurity(ScanResult result) {
		if (result.capabilities.contains("WEP")) {
			return SECURITY_WEP;
		} else if (result.capabilities.contains("PSK")) {
			return SECURITY_PSK;
		} else if (result.capabilities.contains("EAP")) {
			return SECURITY_EAP;
		}
		return SECURITY_NONE;
	}

	//
	// public String getSecurityString(boolean concise) {
	// switch (security) {
	// case SECURITY_EAP:
	// return concise ? context.getString(R.string.wifi_security_short_eap) :
	// context.getString(R.string.wifi_security_eap);
	// case SECURITY_PSK:
	// switch (pskType) {
	// case WPA:
	// return concise ? context.getString(R.string.wifi_security_short_wpa) :
	// context.getString(R.string.wifi_security_wpa);
	// case WPA2:
	// return concise ? context.getString(R.string.wifi_security_short_wpa2) :
	// context.getString(R.string.wifi_security_wpa2);
	// case WPA_WPA2:
	// return concise ? context.getString(R.string.wifi_security_short_wpa_wpa2)
	// : context.getString(R.string.wifi_security_wpa_wpa2);
	// case UNKNOWN:
	// default:
	// return concise ?
	// context.getString(R.string.wifi_security_short_psk_generic) :
	// context.getString(R.string.wifi_security_psk_generic);
	// }
	// case SECURITY_WEP:
	// return concise ? context.getString(R.string.wifi_security_short_wep) :
	// context.getString(R.string.wifi_security_wep);
	// case SECURITY_NONE:
	// default:
	// return concise ? "" : context.getString(R.string.wifi_security_none);
	// }
	// }

	private static PskType getPskType(ScanResult result) {
		boolean wpa = result.capabilities.contains("WPA-PSK");
		boolean wpa2 = result.capabilities.contains("WPA2-PSK");
		if (wpa2 && wpa) {
			return PskType.WPA_WPA2;
		} else if (wpa2) {
			return PskType.WPA2;
		} else if (wpa) {
			return PskType.WPA;
		} else {
			Log.w(TAG, "Received abnormal flag string: " + result.capabilities);
			return PskType.UNKNOWN;
		}
	}

	public AccessPoint(WifiConfiguration config) {
		loadConfig(config);
	}

	public AccessPoint(ScanResult result) {
		loadResult(result);
	}

	private void loadConfig(WifiConfiguration config) {
		ssid = (config.SSID == null ? "" : removeDoubleQuotes(config.SSID));
		bssid = config.BSSID;
		security = getSecurity(config);
		networkId = config.networkId;
		mRssi = Integer.MAX_VALUE;
		mConfig = config;
	}

	private void loadResult(ScanResult result) {
		ssid = result.SSID;
		bssid = result.BSSID;
		security = getSecurity(result);
		wpsAvailable = security != SECURITY_EAP && result.capabilities.contains("WPS");
		if (security == SECURITY_PSK)
			pskType = getPskType(result);
		networkId = INVALID_NETWORK_ID;
		mRssi = result.level;
		mScanResult = result;
	}

	@Override
	public int compareTo(AccessPoint other) {
		// Active one goes first.
		if (mInfo != null && other.mInfo == null)
			return -1;
		if (mInfo == null && other.mInfo != null)
			return 1;

		// Reachable one goes before unreachable one.
		if (mRssi != Integer.MAX_VALUE && other.mRssi == Integer.MAX_VALUE)
			return -1;
		if (mRssi == Integer.MAX_VALUE && other.mRssi != Integer.MAX_VALUE)
			return 1;

		// Configured one goes before unconfigured one.
		if (networkId != INVALID_NETWORK_ID && other.networkId == INVALID_NETWORK_ID)
			return -1;
		if (networkId == INVALID_NETWORK_ID && other.networkId != INVALID_NETWORK_ID)
			return 1;

		// Sort by signal strength.
		int difference = WifiManager.compareSignalLevel(other.mRssi, mRssi);
		if (difference != 0) {
			return difference;
		}
		// Sort by ssid.
		return ssid.compareToIgnoreCase(other.ssid);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof AccessPoint))
			return false;
		return (this.compareTo((AccessPoint) other) == 0);
	}

	@Override
	public int hashCode() {
		int result = 0;
		if (mInfo != null)
			result += 13 * mInfo.hashCode();
		result += 19 * mRssi;
		result += 23 * networkId;
		result += 29 * ssid.hashCode();
		return result;
	}

	public static String removeDoubleQuotes(String string) {
		if (string == null)
			string = "null";
		int length = string.length();
		if ((length > 1) && (string.charAt(0) == '"') && (string.charAt(length - 1) == '"')) {
			return string.substring(1, length - 1);
		}
		return string;
	}

	public static String convertToQuotedString(String string) {
		return "\"" + string + "\"";
	}

	public int getLevel() {
		if (mRssi == Integer.MAX_VALUE) {
			return -1;
		}
		return WifiManager.calculateSignalLevel(mRssi, 5);
	}

	public boolean update(ScanResult result) {
		bssid = result.BSSID;
		if (ssid.equals(result.SSID) && security == getSecurity(result)) {
			if (WifiManager.compareSignalLevel(result.level, mRssi) > 0) {
				int oldLevel = getLevel();
				mRssi = result.level;
			}
			// This flag only comes from scans, is not easily saved in config
			if (security == SECURITY_PSK) {
				pskType = getPskType(result);
			}
			return true;
		}
		return false;
	}

	void update(WifiInfo info, DetailedState state) {
		boolean reorder = false;
		if (info != null && networkId != INVALID_NETWORK_ID && networkId == info.getNetworkId()) {
			reorder = (mInfo == null);
			mRssi = info.getRssi();
			mInfo = info;
			mState = state;
		} else if (mInfo != null) {
			reorder = true;
			mInfo = null;
			mState = null;
		}
	}

}