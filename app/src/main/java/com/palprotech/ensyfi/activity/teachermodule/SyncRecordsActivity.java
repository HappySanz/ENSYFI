package com.palprotech.ensyfi.activity.teachermodule;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.palprotech.ensyfi.R;
import com.palprotech.ensyfi.activity.loginmodule.UserLoginActivity;
import com.palprotech.ensyfi.bean.database.SQLiteHelper;
import com.palprotech.ensyfi.helper.AlertDialogHelper;
import com.palprotech.ensyfi.helper.ProgressDialogHelper;
import com.palprotech.ensyfi.interfaces.DialogClickListener;
import com.palprotech.ensyfi.servicehelpers.ServiceHelper;
import com.palprotech.ensyfi.serviceinterfaces.IServiceListener;
import com.palprotech.ensyfi.utils.CommonUtils;
import com.palprotech.ensyfi.utils.EnsyfiConstants;
import com.palprotech.ensyfi.utils.PreferenceStorage;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Admin on 06-07-2017.
 */

public class SyncRecordsActivity extends AppCompatActivity implements IServiceListener, View.OnClickListener, DialogClickListener {

    private static final String TAG = SyncRecordsActivity.class.getName();
    private ServiceHelper serviceHelper;
    private Button btnSyncAttendanceRecords;
    private SyncAttendanceHistoryRecordsActivity syncAttendanceHistoryRecordsActivity;
    private ProgressDialogHelper progressDialogHelper;
    SQLiteHelper db;
    String localAttendanceId, ac_year, class_id, class_total, no_of_present, no_of_absent, attendance_period, created_by, created_at, status;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_records);
        db = new SQLiteHelper(getApplicationContext());
        btnSyncAttendanceRecords = (Button) findViewById(R.id.btnSyncAttendanceRecords);
        btnSyncAttendanceRecords.setOnClickListener(this);

        serviceHelper = new ServiceHelper(this);
        serviceHelper.setServiceListener(this);
        syncAttendanceHistoryRecordsActivity = new SyncAttendanceHistoryRecordsActivity(this);
        progressDialogHelper = new ProgressDialogHelper(this);
    }

    @Override
    public void onClick(View v) {
        if (CommonUtils.isNetworkAvailable(this)) {
            if (v == btnSyncAttendanceRecords) {
                try {
                    Cursor c = db.getAttendanceList();
                    if (c.getCount() > 0) {
                        if (c.moveToFirst()) {
                            do {
                                localAttendanceId = c.getString(0);
                                ac_year = c.getString(1);
                                class_id = c.getString(2);
                                class_total = c.getString(3);
                                no_of_present = c.getString(4);
                                no_of_absent = c.getString(5);
                                attendance_period = c.getString(6);
                                created_by = c.getString(7);
                                created_at = c.getString(8);
                                status = c.getString(9);

                                JSONObject jsonObject = new JSONObject();
                                try {

                                    jsonObject.put(EnsyfiConstants.KEY_ATTENDANCE_AC_YEAR, ac_year);
                                    jsonObject.put(EnsyfiConstants.KEY_ATTENDANCE_CLASS_ID, class_id);
                                    jsonObject.put(EnsyfiConstants.KEY_ATTENDANCE_CLASS_TOTAL, class_total);
                                    jsonObject.put(EnsyfiConstants.KEY_ATTENDANCE_NO_OF_PRESSENT, no_of_present);
                                    jsonObject.put(EnsyfiConstants.KEY_ATTENDANCE_NO_OF_ABSENT, no_of_absent);
                                    jsonObject.put(EnsyfiConstants.KEY_ATTENDANCE_PERIOD, attendance_period);
                                    jsonObject.put(EnsyfiConstants.KEY_ATTENDANCE_CREATED_BY, created_by);
                                    jsonObject.put(EnsyfiConstants.KEY_ATTENDANCE_CREATED_AT, created_at);
                                    jsonObject.put(EnsyfiConstants.KEY_ATTENDANCE_STATUS, status);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                progressDialogHelper.showProgressDialog(getString(R.string.progress_loading));
                                String url = EnsyfiConstants.BASE_URL + PreferenceStorage.getInstituteCode(this) + EnsyfiConstants.GET_TEACHERS_CLASS_ATTENDANCE_API;
                                serviceHelper.makeGetServiceCall(jsonObject.toString(), url);

                            } while (c.moveToNext());
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    @Override
    public void onAlertPositiveClicked(int tag) {

    }

    @Override
    public void onAlertNegativeClicked(int tag) {

    }

    private boolean validateSignInResponse(JSONObject response) {
        boolean signInSuccess = false;
        if ((response != null)) {
            try {
                String status = response.getString("status");
                String msg = response.getString(EnsyfiConstants.PARAM_MESSAGE);
                Log.d(TAG, "status val" + status + "msg" + msg);

                if ((status != null)) {
                    if (((status.equalsIgnoreCase("activationError")) || (status.equalsIgnoreCase("alreadyRegistered")) ||
                            (status.equalsIgnoreCase("notRegistered")) || (status.equalsIgnoreCase("error")))) {
                        signInSuccess = false;
                        Log.d(TAG, "Show error dialog");
                        AlertDialogHelper.showSimpleAlertDialog(this, msg);

                    } else {
                        signInSuccess = true;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return signInSuccess;
    }

    @Override
    public void onResponse(JSONObject response) {
        progressDialogHelper.hideProgressDialog();
        if (validateSignInResponse(response)) {
            try {
                String latestAttendanceInsertedServerId = response.getString("last_attendance_id");
                if (!latestAttendanceInsertedServerId.isEmpty()) {
                    db.updateAttendanceId(latestAttendanceInsertedServerId, localAttendanceId);
                    db.updateAttendanceSyncStatus(localAttendanceId);
                    db.updateAttendanceHistoryServerId(latestAttendanceInsertedServerId, localAttendanceId);
                    syncAttendanceHistoryRecordsActivity.syncAttendanceHistoryRecords(latestAttendanceInsertedServerId);
                } else {
                    Toast.makeText(getApplicationContext(), "Insert Error..", Toast.LENGTH_LONG).show();
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onError(String error) {

        progressDialogHelper.hideProgressDialog();
        AlertDialogHelper.showSimpleAlertDialog(this, error);
    }
}