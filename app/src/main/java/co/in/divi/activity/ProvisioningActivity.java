package co.in.divi.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import co.in.divi.DiviApplication;
import co.in.divi.R;
import co.in.divi.SyncManager;
import co.in.divi.UserSessionProvider;
import co.in.divi.background.UniversalSyncCheckReceiver;
import co.in.divi.db.sync.SyncDownService;
import co.in.divi.model.UserData;
import co.in.divi.util.LogConfig;
import co.in.divi.util.ServerConfig;
import co.in.divi.util.Util;

/**
 * Created by Indra on 3/23/2015.
 */
public class ProvisioningActivity extends Activity {
    private static final String TAG = ProvisioningActivity.class.getSimpleName();

    private Button cancelButton;
    private TextView label;

    private UserSessionProvider userSessionProvider;
    private ProvisionTask provisionTask;

    private enum STATE {WAITING_FOR_SYNC, CHECKING_PROVISIONING, PERFORMING_LOGIN}

    ;

    private STATE state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_syncdown);

        label = (TextView) findViewById(R.id.text);
        cancelButton = (Button) (findViewById(R.id.cancelButton));
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        userSessionProvider = UserSessionProvider.getInstance(ProvisioningActivity.this);
        provisionTask = new ProvisionTask();
        provisionTask.execute((Void[]) null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideBars();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (provisionTask != null) {
            provisionTask.cancel(true);
        }
    }

    private void hideBars() {
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            int newUiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            newUiOptions ^= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN;
            if (Build.VERSION.SDK_INT >= 18) {
                newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
            getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
        }
    }

    private class ProvisionTask extends AsyncTask<Void, String, Integer> {

        JSONObject jsonRequest;
        UserData loginResponse;
        private int ONE_MIN = 60 * 1000;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            jsonRequest = new JSONObject();
            label.setText("Waiting for Sync...");
            try {
                jsonRequest.put("labId", ((DiviApplication) getApplication()).getLabId());
            } catch (JSONException e) {
                Log.w(TAG, "error getting lab id", e);
                cancel(true);
            }
            // start sync
            UniversalSyncCheckReceiver.syncNow(ProvisioningActivity.this, true);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // wait till sync complete
            while (true) {
                long delta1 = (Util.getTimestampMillis() - SyncManager.getInstance(ProvisioningActivity.this).getLastSyncTime());
                long delta2 = Util.getTimestampMillis()
                        - userSessionProvider.getTimestamp(UserSessionProvider.LAST_SYNC_LOGS_TIMESTAMP);
                Log.d(TAG, "del1 - " + delta1 + ", del2 - " + delta2);
                if (delta1 < ONE_MIN && delta2 < ONE_MIN) {
                    break;
                }
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    Log.w(TAG, "error waiting for sync to complete...", e);
                    return 1;
                }
            }
            publishProgress("Checking provisioning...");
            try {
                String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_PROVISIONING;
                if (LogConfig.DEBUG_SYNC)
                    Log.d(TAG, "posting:   " + jsonRequest.toString());

                RequestFuture<JSONObject> future = RequestFuture.newFuture();
                JsonObjectRequest fetchUpdatesRequest = new JsonObjectRequest(Request.Method.POST, url, jsonRequest, future, future);
                fetchUpdatesRequest.setShouldCache(false);
                DiviApplication.get().getRequestQueue().add(fetchUpdatesRequest).setTag(this);

                JSONObject response = future.get();
                if (LogConfig.DEBUG_SYNC)
                    Log.d(TAG, "got response:\n" + response.toString());

                if (response.has("uid")) {
                    // perform login
                    Gson gson = new Gson();
                    loginResponse = gson.fromJson(response.toString(), UserData.class);
                    return 0;
                } else {
                    return 1;
                }

            } catch (Exception e) {
//                Log.e(TAG, "Error fetching content updates", e);
                // 404 if no lab provisioining.
            }
            return 2;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            label.setText(values[0]);
        }

        @Override
        protected void onPostExecute(Integer resultCode) {
            if (resultCode == 0) {
                Toast.makeText(ProvisioningActivity.this, "Logging in " + loginResponse.name, Toast.LENGTH_LONG);
                userSessionProvider.logout();
                userSessionProvider.setUserSession(loginResponse);

                Intent intent = new Intent(ProvisioningActivity.this, HomeActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(ProvisioningActivity.this, "Error provisioning - " + resultCode, Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }
}
