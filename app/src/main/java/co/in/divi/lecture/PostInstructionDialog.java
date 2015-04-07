package co.in.divi.lecture;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request.Method;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;

import org.json.JSONObject;

import co.in.divi.DiviApplication;
import co.in.divi.LectureSessionProvider;
import co.in.divi.Location;
import co.in.divi.LocationManager;
import co.in.divi.LocationManager.DiviLocationChangeListener;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.content.DiviReference;
import co.in.divi.db.UserDBContract;
import co.in.divi.db.UserDBContract.Commands;
import co.in.divi.db.model.Command;
import co.in.divi.model.Instruction;
import co.in.divi.model.UserData.ClassRoom;
import co.in.divi.util.Config;
import co.in.divi.util.ServerConfig;
import co.in.divi.util.Util;

public class PostInstructionDialog extends Dialog implements DiviLocationChangeListener {
    private static final String TAG = PostInstructionDialog.class.getSimpleName();

    private Context context;
    private LocationManager locationManager;
    private UserSessionProvider userSessionProvider;
    private LectureSessionProvider lectureSessionProvider;

    private boolean isBlackout;
    private String externalAppPackage;
    private String externalAppTitle;

    private ViewGroup dialogRoot;
    private TextView title, text;
    private CheckBox unlockCheck, followMeCheck;
    private Button postButton, cancelButton;

    private CheckProtectedResourceTask protectedResourceTask = null;
    private Command unlockCommand = null;

    public PostInstructionDialog(Context context, boolean isBlackout) {
        super(context);
        this.isBlackout = isBlackout;
        this.context = context;
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        userSessionProvider = UserSessionProvider.getInstance(context);
        lectureSessionProvider = LectureSessionProvider.getInstance(context);
        locationManager = LocationManager.getInstance(context);
    }

    public void init() {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        dialogRoot = (ViewGroup) inflater.inflate(R.layout.dialog_postinstruction, null);
        title = (TextView) dialogRoot.findViewById(R.id.title);
        text = (TextView) dialogRoot.findViewById(R.id.text);
        postButton = (Button) dialogRoot.findViewById(R.id.postButton);
        cancelButton = (Button) dialogRoot.findViewById(R.id.cancelButton);
        unlockCheck = (CheckBox) dialogRoot.findViewById(R.id.unlockCheck);
        followMeCheck = (CheckBox) dialogRoot.findViewById(R.id.followMe);
        followMeCheck.setVisibility(View.INVISIBLE);
        followMeCheck.setChecked(false);
        setContentView(dialogRoot);
    }

    @Override
    public void show() {
        super.show();
        locationManager.addListener(this);
        if (isBlackout) {
            title.setText("Blackout Class?");
            text.setVisibility(View.INVISIBLE);
            unlockCheck.setVisibility(View.INVISIBLE);
            postButton.setEnabled(true);
        } else {
            Location loc = locationManager.getLocation();
            if (loc.getLocationType() == Location.LOCATION_TYPE.ASSESSMENT || loc.getLocationType() == Location.LOCATION_TYPE.TOPIC) {
                Location.Breadcrumb b = loc.getBreadcrumb();
                String locDesc = b.bookName + " > " + b.chapterName + " > " + b.itemName;
                if (b.subItemName != null) {
                    locDesc = locDesc + " > " + b.subItemName;
                }
                switch (loc.getLocationSubType()) {
                    case ASSESSMENT_EXERCISE:
                        title.setText("Share exercise:");
                        break;
                    case ASSESSMENT_QUIZ:
                        title.setText("Share quiz");
                        break;
                    case ASSESSMENT_TEST:
                        title.setText("Share test");
                        break;
                    case TOPIC_TOPIC:
                        title.setText("Share page");
                        break;
                    case TOPIC_AUDIO:
                        title.setText("Share audio");
                        break;
                    case TOPIC_IMAGE:
                        title.setText("Share image");
                        break;
                    case TOPIC_VIDEO:
                        title.setText("Share video");
                        break;
                    case TOPIC_VM:
                        title.setText("Share interactive");
                        break;
                    case TOPIC_IMAGESET:
                        title.setText("Share slideshow");
                        break;
                    default:
                        break;
                }
                text.setText(locDesc);
            } else if (Config.ENABLE_EXTERNAL_APP_SHARING && loc.getLocationType() == Location.LOCATION_TYPE.UNKNOWN) {
                title.setText("Share App");
                text.setText(loc.getAppName());
                externalAppPackage = loc.getAppPackageName();
                externalAppTitle = loc.getAppName();
                //TODO: handle unable to get app info / not found
            } else {
                Toast.makeText(context, "Open the resource you want to share", Toast.LENGTH_LONG).show();
                dismiss();
                return;
            }
            if (loc.isLocationLocked()) {
                protectedResourceTask = new CheckProtectedResourceTask(loc.getLocationRef(),
                        loc.getProtectedResourceMetadata());
                protectedResourceTask.execute(new Void[0]);
            } else {
                unlockCheck.setVisibility(View.INVISIBLE);
                postButton.setEnabled(true);
            }
            if (loc.isLocationStreamable()) {
                followMeCheck.setVisibility(View.VISIBLE);
            } else {
                followMeCheck.setVisibility(View.INVISIBLE);
            }
        }

        postButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Location loc = locationManager.getLocation();
                Log.d(TAG, "loc type:" + loc.getLocationType());
                if (!lectureSessionProvider.isCurrentUserTeacher()) {
                    dismiss();
                    return;
                }
                Log.d(TAG, "is teacher");
                if (isBlackout) {
                    postInstruction(Instruction.INSTRUCTION_TYPE_BLACKOUT, null, null, null, false, false);
                } else {
                    if (loc.getLocationType() == Location.LOCATION_TYPE.ASSESSMENT
                            || loc.getLocationType() == Location.LOCATION_TYPE.TOPIC) {
                        if (unlockCommand != null && loc.getProtectedResourceMetadata() != null) {
                            // fill time just when clicked
                            // unlockCommand.id = "" + System.currentTimeMillis();
                            unlockCommand.teacherId = lectureSessionProvider.getCurrentLecture().teacherId;
                            unlockCommand.classRoomId = lectureSessionProvider.getCurrentLecture().classRoomId;
                            unlockCommand.category = Command.COMMAND_CATEGORY_UNLOCK;
                            unlockCommand.status = Command.COMMAND_STATUS_ACTIVE;
                            unlockCommand.itemCategory = loc.getProtectedResourceMetadata().itemType;
                            long now = Util.getTimestampMillis();
                            if (unlockCommand.itemCategory == Command.COMMAND_UNLOCK_ITEM_CATEGORY_QUIZ)
                                unlockCommand.appliedAt = now - 30000;// time irrelevant
                            else
                                unlockCommand.appliedAt = now + 20000;// time important
                            unlockCommand.endsAt = unlockCommand.appliedAt + loc.getProtectedResourceMetadata().duration;
                            unlockCommand.data = loc.getProtectedResourceMetadata().data;
                        }
                        postInstruction(Instruction.INSTRUCTION_TYPE_NAVIGATE, loc.getLocationRef().getUri().toString(),
                                loc.getBreadcrumb().getBreadcrumbArray(), unlockCommand, followMeCheck.isChecked(),
                                loc.getLocationSubType() == Location.LOCATION_SUBTYPE.TOPIC_VM);
                    } else if (Config.ENABLE_EXTERNAL_APP_SHARING && loc.getLocationType() == Location.LOCATION_TYPE.UNKNOWN) {
                        postInstruction(Instruction.INSTRUCTION_TYPE_NAVIGATE_EXTERNAL, externalAppPackage,
                                new String[]{externalAppTitle}, null, false, false);
                    } else {
                        dismiss();
                    }
                }
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    private void stop() {
        if (protectedResourceTask != null)
            protectedResourceTask.cancel(false);
        DiviApplication.get().getRequestQueue().cancelAll(this);
    }

    @Override
    public void dismiss() {
        stop();
        super.dismiss();
    }

    ;

    private class CheckProtectedResourceTask extends AsyncTask<Void, Void, Integer> {
        private DiviReference diviRef;
        private Location.ProtectedResourceMetadata prm;
        private String mSelectionClause;
        private String[] mSelectionArgs;
        private Command c;

        public CheckProtectedResourceTask(DiviReference ref, Location.ProtectedResourceMetadata prm) {
            this.diviRef = ref;
            this.prm = prm;
            mSelectionClause = Commands.STATUS + " = ? AND " + Commands.CLASS_ID + " = ? AND " + Commands.COURSE_ID + " = ? AND "
                    + Commands.BOOK_ID + " = ? AND " + Commands.ITEM_ID + " = ? AND " + Commands.TYPE + " = ? ";
            mSelectionArgs = new String[]{"" + Command.COMMAND_STATUS_ACTIVE, lectureSessionProvider.getCurrentLecture().classRoomId,
                    diviRef.courseId, diviRef.bookId, diviRef.itemId, "" + Command.COMMAND_CATEGORY_UNLOCK};
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            postButton.setEnabled(false);
            unlockCheck.setEnabled(false);
            unlockCheck.setOnCheckedChangeListener(null);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            Cursor cursor = context.getContentResolver().query(UserDBContract.Commands.CONTENT_URI, UserDBContract.Commands.PROJECTION_ALL,
                    mSelectionClause, mSelectionArgs, Commands.SORT_ORDER_LATEST_FIRST);
            int foundCommands = cursor.getCount();
            Log.d(TAG, "found commands:" + foundCommands);
            cursor.close();
            if (foundCommands > 0) {
                // already unlocked
                return 1;
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            String lectureClassId = lectureSessionProvider.getCurrentLecture().classRoomId;
            String className = "??";
            for (ClassRoom classRoom : userSessionProvider.getUserData().classRooms) {
                if (classRoom.classId.equals(lectureClassId)) {
                    className = classRoom.className + " - " + classRoom.section;
                    break;
                }
            }
            if (result == 0) {
                // needs unlocking, partially fill command; time gets filled in onclick
                unlockCommand = new Command();
                unlockCommand.courseId = diviRef.courseId;
                unlockCommand.bookId = diviRef.bookId;
                unlockCommand.itemCode = diviRef.itemId;

                unlockCheck.setEnabled(true);
                unlockCheck.setText("Confirm unlocking for class '" + className + "'");
                unlockCheck.setChecked(false);
                unlockCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked)
                            postButton.setEnabled(true);
                        else
                            postButton.setEnabled(false);
                    }
                });
            } else {
                unlockCheck.setChecked(true);
                unlockCheck.setText("Resource already unlocked for class '" + className + "'");
                postButton.setEnabled(true);
            }
        }
    }

    private void postInstruction(int type, String location, String[] breadcrumb, Command command, boolean followMe, boolean isVM) {
        postButton.setEnabled(false);
        postButton.setText("Posting...");
        try {
            Instruction instruction = new Instruction();
            instruction.type = type;
            instruction.location = location;
            instruction.breadcrumb = breadcrumb;
            instruction.followMe = followMe;
            instruction.isVM = isVM;

            JSONObject jsonRequest = new JSONObject();

            jsonRequest.put("uid", userSessionProvider.getUserData().uid);
            jsonRequest.put("token", userSessionProvider.getUserData().token);
            jsonRequest.put("lectureId", lectureSessionProvider.getCurrentLecture().id);
            if (command != null) {
                jsonRequest.put("command", new JSONObject(new Gson().toJson(command)));
                instruction.syncCommand = true;
            }
            jsonRequest.put("instruction", new Gson().toJson(instruction));
            String url = ServerConfig.SERVER_ENDPOINT + ServerConfig.METHOD_SENDINSTRUCTION;
            Log.d(TAG, "sending:" + jsonRequest.toString());
            JsonObjectRequest sendInstructionRequest = new JsonObjectRequest(Method.POST, url, jsonRequest, new Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d(TAG, "got response:\n" + response.toString());
                    // validate response
                    dismiss();
                    Toast.makeText(context, "Shared successfully", Toast.LENGTH_SHORT).show();
                }
            }, new ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.w(TAG, "error:" + error);
                    dismiss();
                    Toast.makeText(context, "Error sending instruction.", Toast.LENGTH_LONG).show();
                }
            });
            sendInstructionRequest.setShouldCache(false);
            DiviApplication.get().getRequestQueue().add(sendInstructionRequest).setTag(this);
        } catch (Exception e) {
            Log.e(TAG, "Error sending instruction", e);
            Toast.makeText(context, "Error sending instruction", Toast.LENGTH_LONG).show();
            dismiss();
        }
    }

    @Override
    public void onLocationChange(Location loc) {
        dismiss();
    }
}
