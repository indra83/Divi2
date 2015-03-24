package co.in.divi.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.File;
import java.util.Random;

import co.in.divi.AdminPasswordManager;
import co.in.divi.BaseActivity;
import co.in.divi.ContentUpdateManager;
import co.in.divi.DiviApplication;
import co.in.divi.LocationManager;
import co.in.divi.R;
import co.in.divi.SyncManager;
import co.in.divi.content.Book;
import co.in.divi.content.DatabaseHelper;
import co.in.divi.db.UserDBContract;
import co.in.divi.logs.LogsSyncService;
import co.in.divi.model.ContentUpdates;
import co.in.divi.model.UserData;
import co.in.divi.util.Config;
import co.in.divi.util.LogConfig;
import co.in.divi.util.ServerConfig;
import co.in.divi.util.Util;

public class AdminSettingsActivity extends BaseActivity {
    private static final String TAG = AdminSettingsActivity.class.getSimpleName();

    TextView contentText, downloadText, cleanUserdataText, cleanContentText, deviceIdText, appText, classroomText;
    Button contentUpdateButton, downloadButton, cleanUserdataButton, cleanContentButton, syncButton, syncLogsButton,
            tagButton, classroomButton;
    ProgressDialog pd;

    ComputeSpaceTask computeSpaceTask = null;
    CleanContentTask cleanContentTask = null;
    CleanUserdataTask cleanUserdataTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adminsettings);
        contentText = (TextView) findViewById(R.id.content_details);
        appText = (TextView) findViewById(R.id.app_details);
        contentUpdateButton = (Button) findViewById(R.id.content_update_button);
        downloadText = (TextView) findViewById(R.id.download_text);
        downloadButton = (Button) findViewById(R.id.content_download_button);
        cleanUserdataText = (TextView) findViewById(R.id.clean_userdata_text);
        cleanContentText = (TextView) findViewById(R.id.clean_content_text);
        cleanUserdataButton = (Button) findViewById(R.id.clean_userdata_button);
        cleanContentButton = (Button) findViewById(R.id.clean_content_button);
        syncButton = (Button) findViewById(R.id.sync_button);
        syncLogsButton = (Button) findViewById(R.id.sync_logs_button);
        deviceIdText = (TextView) findViewById(R.id.device_id);
        tagButton = (Button) findViewById(R.id.tag_button);
        classroomText = (TextView) findViewById(R.id.classroom_details);
        classroomButton = (Button) findViewById(R.id.classroom_button);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!userSessionProvider.isLoggedIn()) {
            Toast.makeText(this, "User is logged out!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        DiviApplication app = (DiviApplication) getApplication();
        deviceIdText.setText(app.deviceId());
        String versionName = "--";
        int versionCode = -1;
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            Log.w(TAG, "error getting version name", e);
        }

        String contentDesc = "";
        for (String courseId : userSessionProvider.getAllCourseIds()) {
            contentDesc = contentDesc + "\nCourse Id: " + courseId + "\n";
            for (Book b : DatabaseHelper.getInstance(this).getBooks(courseId)) {
                contentDesc = contentDesc + "        " + b.name + "(  " + b.id + ")" + "   ---   v." + b.version + "\n";
            }
        }
        contentText.setText(contentDesc);

        if (Config.ENABLE_PROVISIONING) {
            findViewById(R.id.lab_id_container).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.lab_id_details)).setText("Current id : " + ((DiviApplication) getApplication()).getLabId());
        } else {
            findViewById(R.id.lab_id_container).setVisibility(View.GONE);
        }
        findViewById(R.id.labId_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Config.ENABLE_OTP) {
                    doit();
                } else {
                    final int challenge = new Random(System.currentTimeMillis()).nextInt(10000);
                    final EditText input = new EditText(AdminSettingsActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    AlertDialog ad = new AlertDialog.Builder(AdminSettingsActivity.this).setTitle("Enter password")
                            .setMessage("Enter the key for challenge: " + challenge).setView(input)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                    boolean isAuthorized = AdminPasswordManager.getInstance().isAuthorized(challenge,
                                            input.getText().toString());
                                    if (isAuthorized) {
                                        doit();
                                    } else {
                                        Toast.makeText(AdminSettingsActivity.this, "Authorization failed", Toast.LENGTH_SHORT).show();
                                        // resetTimer();
                                    }
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // resetTimer();
                                    dialog.dismiss();
                                }
                            }).show();
                }
            }

            private void doit() {
                final EditText input = new EditText(AdminSettingsActivity.this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                AlertDialog ad = new AlertDialog.Builder(AdminSettingsActivity.this).setTitle("Set Lab Id").setMessage("Enter the new lab id")
                        .setView(input).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                                try {
                                    ((DiviApplication) getApplication()).setLabId(Integer.parseInt(input.getText().toString()));
                                } catch (Exception e) {
                                    Log.w(TAG, "error setting new lab id", e);
                                }
                                finish();
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }).show();
            }
        });

        tagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(AdminSettingsActivity.this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                AlertDialog ad = new AlertDialog.Builder(AdminSettingsActivity.this).setTitle("Set Tag").setMessage("Enter the new tag")
                        .setView(input).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                                ((DiviApplication) getApplication()).setDeviceTag(input.getText().toString());
                                finish();
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // resetTimer();
                                dialog.dismiss();
                            }
                        }).show();
            }
        });

        // App update
        appText.setText(" Tag : " + app.getDeviceTag() + "\n Current version : '" + versionName + "' --- " + versionCode + "\n CDN : "
                + Config.USE_CDN + ", OTP : " + Config.ENABLE_OTP + "\n Server : " + ServerConfig.SERVER_ENDPOINT + " \n mmax hacks : "
                + Config.USE_HARDCODED_LOCATION_MICROMAX);

        contentUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentUpdateManager.getInstance(AdminSettingsActivity.this).startContentUpdates(true);
                finish();
            }
        });

        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(SyncManager.getInstance(AdminSettingsActivity.this).getSyncUpIntent());
                finish();
            }
        });

        syncLogsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launchSync = new Intent(AdminSettingsActivity.this, LogsSyncService.class);
                startService(launchSync);
                finish();
            }
        });

        // Content download
        if (!Config.SHOW_CONTENT_IMPORT_BUTTON) {
            downloadButton.setVisibility(View.GONE);
            downloadText.setVisibility(View.GONE);
        }
        downloadText.setText("Content update url:\n" + userSessionProvider.getUserData().metadata);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContentUpdateManager.getInstance(AdminSettingsActivity.this).getCurrentUpdate() != null) {
                    Toast.makeText(AdminSettingsActivity.this, "An update is already in progress, cancelling", Toast.LENGTH_LONG).show();
                    ContentUpdateManager.getInstance(AdminSettingsActivity.this).cancelUpdate();
                    return;
                }
                if (userSessionProvider.getUserData().metadata != null
                        && userSessionProvider.getUserData().getMetadata().dropboxContentUrl != null
                        && userSessionProvider.getUserData().getMetadata().dropboxContentUrl.length() > 0) {
                    ContentUpdates contentUpdates = new ContentUpdates();
                    contentUpdates.cdn = new String[0];
                    contentUpdates.updates = new ContentUpdates.Update[1];
                    contentUpdates.updates[0] = new ContentUpdates.Update();
                    contentUpdates.updates[0].isDropboxImport = true;
                    contentUpdates.updates[0].strategy = "replace";
                    contentUpdates.updates[0].isApplicable = true;
                    contentUpdates.updates[0].webUrl = userSessionProvider.getUserData().getMetadata().dropboxContentUrl;
                    ContentUpdateManager.getInstance(AdminSettingsActivity.this).setContentUpdates(contentUpdates);
                    finish();
                } else {
                    Toast.makeText(AdminSettingsActivity.this, "Please update your dropbox url on website", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Clean
        if (!Config.SHOW_CONTENT_CLEAN_BUTTON) {
            cleanContentButton.setVisibility(View.GONE);
            cleanUserdataButton.setVisibility(View.GONE);
        }
        computeSpaceTask = new ComputeSpaceTask();
        computeSpaceTask.execute(new Void[0]);
        cleanContentText.setText("Computing space used...");

        cleanContentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Config.ENABLE_OTP) {
                    doit();
                } else {
                    final int challenge = new Random(System.currentTimeMillis()).nextInt(10000);
                    final EditText input = new EditText(AdminSettingsActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    AlertDialog ad = new AlertDialog.Builder(AdminSettingsActivity.this).setTitle("Enter password")
                            .setMessage("Enter the key for challenge: " + challenge).setView(input)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                    boolean isAuthorized = AdminPasswordManager.getInstance().isAuthorized(challenge,
                                            input.getText().toString());
                                    if (isAuthorized) {
                                        doit();
                                    } else {
                                        Toast.makeText(AdminSettingsActivity.this, "Authorization failed", Toast.LENGTH_SHORT).show();
                                        // resetTimer();
                                    }
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // resetTimer();
                                    dialog.dismiss();
                                }
                            }).show();
                }
            }

            private void doit() {
                cleanContentTask = new CleanContentTask();
                cleanContentTask.execute(new Void[0]);
            }
        });

        cleanUserdataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Config.ENABLE_OTP) {
                    doit();
                } else {
                    final int challenge = new Random(System.currentTimeMillis()).nextInt(10000);
                    final EditText input = new EditText(AdminSettingsActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    AlertDialog ad = new AlertDialog.Builder(AdminSettingsActivity.this).setTitle("Enter password")
                            .setMessage("Enter the key for challenge: " + challenge).setView(input)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                    boolean isAuthorized = AdminPasswordManager.getInstance().isAuthorized(challenge,
                                            input.getText().toString());
                                    if (isAuthorized) {
                                        doit();
                                    } else {
                                        Toast.makeText(AdminSettingsActivity.this, "Authorization failed", Toast.LENGTH_SHORT).show();
                                        // resetTimer();
                                    }
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // resetTimer();
                                    dialog.dismiss();
                                }
                            }).show();
                }
            }

            private void doit() {
                cleanUserdataTask = new CleanUserdataTask();
                cleanUserdataTask.execute(new Void[0]);
            }
        });

        // Classroom management
        if (Config.IS_PLAYSTORE_APP) {
            if (Config.IS_TEACHER_ONLY) {
                classroomButton.setText("Manage Classrooms");
                classroomText.setText("Create and view your classrooms");
                classroomButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        finish();
                        Intent launchClassroomManagement = new Intent(AdminSettingsActivity.this, ClassroomManagementActivity.class);
                        startActivity(launchClassroomManagement);
                    }
                });
            } else {
                classroomButton.setText("Join Classroom");
                classroomText.setText("Join a classroom");
                classroomButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final EditText input = new EditText(AdminSettingsActivity.this);
                        input.setInputType(InputType.TYPE_CLASS_NUMBER);
                        new AlertDialog.Builder(AdminSettingsActivity.this).setTitle("Enter Classroom Id")
                                .setMessage("Enter the classroom code : ").setView(input)
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                        if (pd != null)
                                            pd.cancel();
                                        try {
                                            JSONObject jsonRequest = new JSONObject();
                                            jsonRequest.put("uid", userSessionProvider.getUserData().uid);
                                            jsonRequest.put("token", userSessionProvider.getUserData().token);
                                            jsonRequest.put("classId", "" + Util.getClassIdFromCode(Integer.parseInt(input.getText().toString())));
                                            String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_JOINCLASSROOM;
                                            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonRequest, new Response.Listener<JSONObject>() {
                                                @Override
                                                public void onResponse(JSONObject response) {
                                                    pd.cancel();
                                                    if (LogConfig.DEBUG_ACTIVITIES)
                                                        Log.d(TAG, "response:\n" + response);
                                                    UserData loginResponse = new Gson().fromJson(response.toString(), UserData.class);

                                                    if (loginResponse.error != null) {
                                                        Toast.makeText(AdminSettingsActivity.this, "Error : " + loginResponse.error.message, Toast.LENGTH_LONG).show();
                                                        finish();
                                                    } else {
                                                        Toast.makeText(AdminSettingsActivity.this, "Classroom joined!", Toast.LENGTH_LONG).show();
//                                                        userSessionProvider.logout();
                                                        userSessionProvider.setUserSession(loginResponse);
                                                    }
                                                }
                                            }, new Response.ErrorListener() {
                                                @Override
                                                public void onErrorResponse(VolleyError error) {
                                                    pd.cancel();
                                                    Log.w(TAG, "Server Error: joining classroom..." + error.getMessage());
                                                    Toast.makeText(AdminSettingsActivity.this, "Server error: joining classroom", Toast.LENGTH_LONG).show();
                                                }
                                            }
                                            );
                                            pd = ProgressDialog.show(AdminSettingsActivity.this, "Please wait", "Joining classroom...");
                                            request.setShouldCache(false);
                                            DiviApplication.get().getRequestQueue().add(request).setTag(AdminSettingsActivity.this);
                                        } catch (Exception e) {
                                            if (pd != null)
                                                pd.cancel();
                                            Log.w(TAG, "Error joining classroom...", e);
                                            Toast.makeText(AdminSettingsActivity.this, "Error joining classroom, please verify the code entered.", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }).show();
                    }
                });
            }
        } else {
            classroomButton.setVisibility(View.GONE);
            classroomText.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocationManager.getInstance(this).setNewLocation(LocationManager.LOCATION_TYPE.HOME, null, null, null, null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (computeSpaceTask != null)
            computeSpaceTask.cancel(true);
        if (cleanContentTask != null)
            cleanContentTask.cancel(true);
        if (cleanUserdataTask != null)
            cleanUserdataTask.cancel(false);
        if (pd != null)
            pd.cancel();
        DiviApplication.get().getRequestQueue().cancelAll(this);
        finish();
    }

    @Override
    public void onCourseChange() {
        finish();
    }

    private class ComputeSpaceTask extends AsyncTask<Void, Void, Long> {
        File contentFolder;

        @Override
        protected void onPreExecute() {
            contentFolder = DiviApplication.get().getBooksBaseDir(userSessionProvider.getCourseId()).getParentFile();
        }

        @Override
        protected Long doInBackground(Void... arg0) {

            return (dirSize(contentFolder) / (1024 * 1024));
        }

        @Override
        protected void onPostExecute(Long result) {
            cleanContentText.setText("Space used:  " + result + " MB\n Free space:  " + (contentFolder.getFreeSpace() / (1024 * 1024))
                    + " MB");
        }

    }

    private class CleanContentTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            if (pd != null)
                pd.cancel();
            pd = ProgressDialog.show(AdminSettingsActivity.this, "Please wait", "Clearing all content");
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // clean file store
            Util.deleteRecursive(DiviApplication.get().getBooksBaseDir(userSessionProvider.getCourseId()).getParentFile());
            // clean db
            DatabaseHelper.getInstance(AdminSettingsActivity.this).cleanDatabase();
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            pd.cancel();
            // logout
            userSessionProvider.logout();
        }
    }

    private class CleanUserdataTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            if (pd != null)
                pd.cancel();
            pd = ProgressDialog.show(AdminSettingsActivity.this, "Please wait", "Clearing all data");
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // clean userdb
            int deletedAttempts = getContentResolver().delete(UserDBContract.Attempts.CONTENT_URI, UserDBContract.Attempts.UID + " = ?",
                    new String[]{userSessionProvider.getUserData().uid});
            Log.d(TAG, "deleted(attempts) - " + deletedAttempts);
            int deletedCommands = getContentResolver().delete(UserDBContract.Commands.CONTENT_URI, UserDBContract.Commands.UID + " = ?",
                    new String[]{userSessionProvider.getUserData().uid});
            Log.d(TAG, "deleted(commands) - " + deletedCommands);
            // clean analytics store - !! deletes for all users
            Util.deleteRecursive(DiviApplication.get().getReportsDir());
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            pd.cancel();
            // logout
            userSessionProvider.logout();
        }

    }

    // util functions
    private static long dirSize(File dir) {

        if (dir.exists()) {
            long result = 0;
            File[] fileList = dir.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                // Recursive call if it's a directory
                if (fileList[i].isDirectory()) {
                    result += dirSize(fileList[i]);
                } else {
                    // Sum the file size in bytes
                    result += fileList[i].length();
                }
            }
            return result; // return the file size
        }
        return 0;
    }
}
