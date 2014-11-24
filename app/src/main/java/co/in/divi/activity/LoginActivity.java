package co.in.divi.activity;

import org.json.JSONObject;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import co.in.divi.DiviApplication;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.fragment.WiFiSettingsFragment;
import co.in.divi.model.UserData;
import co.in.divi.util.LogConfig;
import co.in.divi.util.ServerConfig;

import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;

public class LoginActivity extends Activity {
	static final String		TAG	= LoginActivity.class.getSimpleName();

	private ProgressDialog	pd;
	private EditText		usernameTV;
	private EditText		passwordTV;
	private Button			loginButton;
	private ImageButton		settingsButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_login);
		usernameTV = (EditText) findViewById(R.id.login_name);
		passwordTV = (EditText) findViewById(R.id.login_password);
		loginButton = (Button) findViewById(R.id.login_button);
		settingsButton = (ImageButton) findViewById(R.id.settings_button);
		settingsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				DialogFragment wifiFragment = new WiFiSettingsFragment();
				FragmentManager fm = getFragmentManager();
				wifiFragment.show(fm, "WiFi");
			}
		});
		findViewById(R.id.home_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent startMain = new Intent(Intent.CATEGORY_HOME);
				startActivity(startMain);
			}
		});
		loginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				String username = usernameTV.getText().toString();
				String password = passwordTV.getText().toString();

				if (username.length() > 0 && password.length() > 0) {
					try {
						JSONObject jsonRequest = new JSONObject();
						jsonRequest.put("uid", username);
						jsonRequest.put("password", password);
						if (LogConfig.DEBUG_LOGIN)
							Log.d(TAG, "request:\n" + jsonRequest.toString());
						String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_LOGIN;
						JsonObjectRequest loginRequest = new JsonObjectRequest(url, jsonRequest, new Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								pd.dismiss();
								if (LogConfig.DEBUG_LOGIN)
									Log.d(TAG, "got response:\n" + response.toString());
								// validate response
								Gson gson = new Gson();
								UserData loginResponse = gson.fromJson(response.toString(), UserData.class);

								if (loginResponse.error != null) {
									Toast.makeText(LoginActivity.this, "Error : " + loginResponse.error.message, Toast.LENGTH_LONG).show();
								} else {
									UserSessionProvider.getInstance(LoginActivity.this).setUserSession(loginResponse);
								}
								finish();
								Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
								startActivity(intent);
							}
						}, new Response.ErrorListener() {
							@Override
							public void onErrorResponse(VolleyError error) {
								pd.dismiss();
								if (LogConfig.DEBUG_LOGIN) {
									Log.e(TAG, "error:" + error.toString());
									Log.e(TAG, "nr:" + error.getMessage());
								}
								String message = "Error occured, please ensure your WiFi is connected.";
								if (error.networkResponse != null) {
									switch (error.networkResponse.statusCode) {
									case 401:
									case 403:
									case 404:
										message = "Please verify the User Id and password entered.";
										break;
									case 500:
									case 501:
									case 502:
									case 503:
										message = "Server error, please try after some time. Contact Divi if error persists.";
										break;
									}
								}
								Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
							}
						});
						loginRequest.setShouldCache(false);
						DiviApplication.get().getRequestQueue().add(loginRequest).setTag(this);

						pd = ProgressDialog.show(LoginActivity.this, "Logging in...", "Please Wait...");
					} catch (Exception e) {
						Log.e(TAG, "Error logging in", e);
						Toast.makeText(LoginActivity.this, "Error logging into Divi", Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(LoginActivity.this, "Please enter your User Id & password", Toast.LENGTH_LONG).show();
				}
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		hideBars();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (pd != null)
			pd.dismiss();
		DiviApplication.get().getRequestQueue().cancelAll(this);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		hideBars();
	}

	private void hideBars() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
			// WindowManager.LayoutParams.FLAG_FULLSCREEN);
			int newUiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN;
			getWindow().addFlags(newUiOptions);
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
		} else {
			int newUiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
			newUiOptions ^= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN;
			newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
			getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
		}
	}
}
