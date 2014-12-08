package co.in.divi.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import co.in.divi.DiviApplication;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.model.UserData;
import co.in.divi.util.Config;
import co.in.divi.util.LogConfig;
import co.in.divi.util.ServerConfig;

public class GoogleLoginActivity extends Activity implements ConnectionCallbacks, OnConnectionFailedListener, View.OnClickListener {

    private static final String TAG = GoogleLoginActivity.class.getSimpleName();

    private static final int STATE_DEFAULT = 0;
    private static final int STATE_SIGN_IN = 1;
    private static final int STATE_IN_PROGRESS = 2;

    private static final int RC_SIGN_IN = 0;

    private static final int DIALOG_PLAY_SERVICES_ERROR = 0;

    private static final String SAVED_PROGRESS = "sign_in_progress";

    private GoogleApiClient mGoogleApiClient;

    // STATE_DEFAULT: The default state of the application before the user
    // has clicked 'sign in', or after they have clicked
    // 'sign out'. In this state we will not attempt to
    // resolve sign in errors and so will display our
    // Activity in a signed out state.
    // STATE_SIGN_IN: This state indicates that the user has clicked 'sign
    // in', so resolve successive errors preventing sign in
    // until the user has successfully authorized an account
    // for our app.
    // STATE_IN_PROGRESS: This state indicates that we have started an intent to
    // resolve an error, and so we should not start further
    // intents until the current intent completes.
    private int mSignInProgress;

    // Used to store the PendingIntent most recently returned by Google Play
    // services until the user clicks 'sign in'.
    private PendingIntent mSignInIntent;

    // Used to store the error code most recently returned by Google Play services
    // until the user clicks 'sign in'.
    private int mSignInError;

    private SignInButton mSignInButton;
    private LinearLayout panel;
    private TextView mStatus;
    private ProgressBar pb;
    private boolean first = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_googlelogin);

        // temp
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }
        // end teemp

        mSignInButton = (SignInButton) findViewById(R.id.sign_in_button);
        mStatus = (TextView) findViewById(R.id.sign_in_status);
        panel = (LinearLayout) findViewById(R.id.buttons_panel);
        pb = (ProgressBar)findViewById(R.id.progress);
        pb.setVisibility(View.GONE);
        panel.setVisibility(View.GONE);

        mSignInButton.setOnClickListener(this);
        mSignInButton.setSize(SignInButton.SIZE_WIDE);
        findViewById(R.id.button_divi_login).setOnClickListener(this);
        findViewById(R.id.button_disconnect_google).setOnClickListener(this);

        if (savedInstanceState != null) {
            mSignInProgress = savedInstanceState.getInt(SAVED_PROGRESS, STATE_DEFAULT);
        }

        mGoogleApiClient = buildGoogleApiClient();
    }

    private GoogleApiClient buildGoogleApiClient() {
        // When we build the GoogleApiClient we specify where connected and
        // connection failed callbacks should be returned, which Google APIs our
        // app uses and which OAuth 2.0 scopes our app requests.
        return new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build()).addScope(Plus.SCOPE_PLUS_PROFILE).build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        DiviApplication.get().getRequestQueue().cancelAll(this);
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVED_PROGRESS, mSignInProgress);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideBars();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        hideBars();
    }


    @Override
    public void onClick(View v) {
        if (!mGoogleApiClient.isConnecting()) {
            // We only process button clicks when GoogleApiClient is not transitioning
            // between connected and not connected.
            switch (v.getId()) {
                case R.id.sign_in_button:
                    pb.setVisibility(View.VISIBLE);
                    mStatus.setText("Signing in with Google+");
                    resolveSignInError();
                    break;
                case -100:
                    // We clear the default account on sign out so that Google Play
                    // services will not return an onConnected callback without user
                    // interaction.
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    mGoogleApiClient.disconnect();
                    mGoogleApiClient.connect();
                    break;
                case R.id.button_disconnect_google:
                    // After we revoke permissions for the user with a GoogleApiClient
                    // instance, we must discard it and create a new one.
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    // Our sample has caches no user data from Google+, however we
                    // would normally register a callback on revokeAccessAndDisconnect
                    // to delete user data so that we comply with Google developer
                    // policies.
                    Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
                    mGoogleApiClient = buildGoogleApiClient();
                    mGoogleApiClient.connect();
                    break;
                case R.id.button_divi_login:
                    // Retrieve some profile information to personalize our app for the user.
                    String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
                    Person currentUser = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);

                    String name = currentUser.getDisplayName();
                    String id = currentUser.getId();
                    String profilePic = currentUser.getImage().getUrl();
                    String url = currentUser.getUrl();
                    if (name == null || name.length() == 0)
                        name = email;

                    performDiviLogin(id, name, email, profilePic, url);
                    break;
            }
        }
    }

    /*
     * onConnected is called when our Activity successfully connects to Google Play services. onConnected indicates that
     * an account was selected on the device, that the selected account has granted any requested permissions to our app
     * and that we were able to establish a service connection to Google Play services.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Reaching onConnected means we consider the user signed in.
        Log.i(TAG, "onConnected");

        // Update the user interface to reflect that the user is signed in.
        mSignInButton.setEnabled(false);
        panel.setVisibility(View.VISIBLE);
        Person currentUser = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
        String name = currentUser.getDisplayName();
        if (name == null || name.length() == 0)
            name = Plus.AccountApi.getAccountName(mGoogleApiClient);
        mStatus.setText(Html.fromHtml(String.format("Authenticated as <b>%s</b>", name)));
        pb.setVisibility(View.GONE);

        // Indicate that the sign in process is complete.
        mSignInProgress = STATE_DEFAULT;

    }

    private void performDiviLogin(String id, String name, String email, String profilePic, String profileUrl) {
        try {
            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("googleID", id);
            jsonRequest.put("name", name);
            jsonRequest.put("profilePic", profilePic);
            jsonRequest.put("role", Config.IS_TEACHER_ONLY ? "teacher" : "student");
            jsonRequest.put("email", email);
            if (LogConfig.DEBUG_LOGIN)
                Log.d(TAG, "request:\n" + jsonRequest.toString());
            String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_GOOGLELOGIN;
            JsonObjectRequest loginRequest = new JsonObjectRequest(url, jsonRequest, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    mStatus.setText("Processing login info...");
                    pb.setVisibility(View.VISIBLE);
                    if (LogConfig.DEBUG_LOGIN)
                        Log.d(TAG, "got response:\n" + response.toString());
                    // validate response
                    Gson gson = new Gson();
                    UserData loginResponse = gson.fromJson(response.toString(), UserData.class);

                    if (loginResponse.error != null) {
                        Toast.makeText(GoogleLoginActivity.this, "Error : " + loginResponse.error.message, Toast.LENGTH_LONG).show();
                        mStatus.setText("Login failed!");
                        pb.setVisibility(View.GONE);
                    } else {
                        UserSessionProvider.getInstance(GoogleLoginActivity.this).setUserSession(loginResponse);
                        finish();
                    }

                    Intent intent = new Intent(GoogleLoginActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (LogConfig.DEBUG_LOGIN) {
                        Log.e(TAG, "error:" + error.toString());
                        Log.e(TAG, "nr:" + error.getMessage());
                    }
                    mStatus.setText("Login failed!");
                    pb.setVisibility(View.GONE);
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
                    Toast.makeText(GoogleLoginActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
            loginRequest.setShouldCache(false);
            DiviApplication.get().getRequestQueue().add(loginRequest).setTag(this);

            mStatus.setText("Logging in to Divi...");
            pb.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Error logging in", e);
            Toast.makeText(GoogleLoginActivity.this, "Error logging into Divi", Toast.LENGTH_LONG).show();
        }
    }

    /*
     * onConnectionFailed is called when our Activity could not connect to Google Play services. onConnectionFailed
     * indicates that the user needs to select an account, grant permissions or resolve an error in order to sign in.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might
        // be returned in onConnectionFailed.
        Log.i(TAG, "onConnectionFailed: ConnectionResult.getErrorCode() = " + result.getErrorCode());

        // if (result.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
        // An API requested for GoogleApiClient is not available. The device's current
        // configuration might not be supported with the requested API or a required component
        // may not be installed, such as the Android Wear application. You may need to use a
        // second GoogleApiClient to manage the application's optional APIs.
        // } else
        if (mSignInProgress != STATE_IN_PROGRESS) {
            // We do not have an intent in progress so we should store the latest
            // error resolution intent for use when the sign in button is clicked.
            mSignInIntent = result.getResolution();
            mSignInError = result.getErrorCode();

            if (mSignInProgress == STATE_SIGN_IN) {
                // STATE_SIGN_IN indicates the user already clicked the sign in button
                // so we should continue processing errors until the user is signed in
                // or they click cancel.
                resolveSignInError();
            }
        }

        // In this sample we consider the user signed out whenever they do not have
        // a connection to Google Play services.
        onSignedOut();
    }

    /*
     * Starts an appropriate intent or dialog for user interaction to resolve the current error preventing the user from
     * being signed in. This could be a dialog allowing the user to select an account, an activity allowing the user to
     * consent to the permissions being requested by your app, a setting to enable device networking, etc.
     */
    private void resolveSignInError() {
        if (mSignInIntent != null) {
            // We have an intent which will allow our user to sign in or
            // resolve an error. For example if the user needs to
            // select an account to sign in with, or if they need to consent
            // to the permissions your app is requesting.

            try {
                // Send the pending intent that we stored on the most recent
                // OnConnectionFailed callback. This will allow the user to
                // resolve the error currently preventing our connection to
                // Google Play services.
                mSignInProgress = STATE_IN_PROGRESS;
                startIntentSenderForResult(mSignInIntent.getIntentSender(), RC_SIGN_IN, null, 0, 0, 0);
            } catch (SendIntentException e) {
                Log.i(TAG, "Sign in intent could not be sent: " + e.getLocalizedMessage());
                // The intent was canceled before it was sent. Attempt to connect to
                // get an updated ConnectionResult.
                mSignInProgress = STATE_SIGN_IN;
                mGoogleApiClient.connect();
            }
        } else {
            // Google Play services wasn't able to provide an intent for some
            // error types, so we show the default Google Play services error
            // dialog which may still start an intent on our behalf if the
            // user can resolve the issue.
            showDialog(DIALOG_PLAY_SERVICES_ERROR);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RC_SIGN_IN:
                if (resultCode == RESULT_OK) {
                    // If the error resolution was successful we should continue
                    // processing errors.
                    mSignInProgress = STATE_SIGN_IN;
                } else {
                    // If the error resolution was not successful or the user canceled,
                    // we should stop processing errors.
                    mSignInProgress = STATE_DEFAULT;
                }

                if (!mGoogleApiClient.isConnecting()) {
                    // If Google Play services resolved the issue with a dialog then
                    // onStart is not called so we need to re-attempt connection here.
                    mGoogleApiClient.connect();
                }
                break;
        }
    }

    private void onSignedOut() {
        // Update the UI to reflect that the user is signed out.
        mSignInButton.setEnabled(true);
        panel.setVisibility(View.GONE);
        mStatus.setText("Signed out");
        pb.setVisibility(View.GONE);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason.
        // We call connect() to attempt to re-establish the connection or get a
        // ConnectionResult that we can attempt to resolve.
        mGoogleApiClient.connect();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_PLAY_SERVICES_ERROR:
                if (GooglePlayServicesUtil.isUserRecoverableError(mSignInError)) {
                    return GooglePlayServicesUtil.getErrorDialog(mSignInError, this, RC_SIGN_IN, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            Log.e(TAG, "Google Play services resolution cancelled");
                            mSignInProgress = STATE_DEFAULT;
                            mStatus.setText("Signed out");
                            pb.setVisibility(View.GONE);
                        }
                    });
                } else {
                    return new AlertDialog.Builder(this).setMessage("Google Play services is not available.  This application will close.")
                            .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.e(TAG, "Google Play services error could not be " + "resolved: " + mSignInError);
                                    mSignInProgress = STATE_DEFAULT;
                                    mStatus.setText("Signed out");
                                    pb.setVisibility(View.GONE);
                                }
                            }).create();
                }
            default:
                return super.onCreateDialog(id);
        }
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