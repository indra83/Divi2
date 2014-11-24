package co.in.divi;

public class AdminPasswordManager {
	private static final String			TAG						= AdminPasswordManager.class.getSimpleName();

	public static final String			DIVI_WIFI_SSID_PREFIX	= "Divi_Auto_";
	private static final String			DIVI_WIFI_PASS_PREFIX	= "SdPfj93rdJ48D";

	private static AdminPasswordManager	instance				= null;

	public static AdminPasswordManager getInstance() {
		if (instance == null) {
			instance = new AdminPasswordManager();
		}
		return instance;
	}

	private long	timestamp;

	private AdminPasswordManager() {
		timestamp = 0L;
	}

	public boolean isAuthorized(int challenge, String response) {
		int intResponse = -1;
		try {
			intResponse = Integer.parseInt(response);
		} catch (NumberFormatException nfe) {
			// ignore
		}
		return (((challenge * 997) + 7919) % 10000 == intResponse);
	}

	public String getWifiPass(int challenge) {
		int key = ((challenge * 997) + 7919) % 10000;
		return DIVI_WIFI_PASS_PREFIX + key;
	}

	public void setLastAuthorizedTime(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getLastAuthorizedTime() {
		return timestamp;
	}

}
