package com.palprotech.ensyfi.activity.adminmodule;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.gson.Gson;
import com.palprotech.ensyfi.R;
import com.palprotech.ensyfi.activity.studentmodule.ExamMarksActivity;
import com.palprotech.ensyfi.activity.studentmodule.ExamOnlyTotalMarksActivity;
import com.palprotech.ensyfi.adapter.adminmodule.ClassStudentListAdapter;
import com.palprotech.ensyfi.bean.admin.viewlist.ClassStudent;
import com.palprotech.ensyfi.bean.admin.viewlist.ClassStudentList;
import com.palprotech.ensyfi.bean.student.viewlist.Exams;
import com.palprotech.ensyfi.helper.AlertDialogHelper;
import com.palprotech.ensyfi.helper.ProgressDialogHelper;
import com.palprotech.ensyfi.interfaces.DialogClickListener;
import com.palprotech.ensyfi.servicehelpers.ServiceHelper;
import com.palprotech.ensyfi.serviceinterfaces.IServiceListener;
import com.palprotech.ensyfi.utils.CommonUtils;
import com.palprotech.ensyfi.utils.EnsyfiConstants;
import com.palprotech.ensyfi.utils.PreferenceStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Admin on 18-07-2017.
 */

public class StudentResultView extends AppCompatActivity implements IServiceListener, DialogClickListener, AdapterView.OnItemClickListener {

    private static final String TAG = StudentResultView.class.getName();
    private ProgressDialogHelper progressDialogHelper;
    private ServiceHelper serviceHelper;
    private String storeClassId, storeSectionId;
    ListView loadMoreListView;
    ClassStudentListAdapter classStudentListAdapter;
    ArrayList<ClassStudent> classStudentArrayList;
    int pageNumber = 0, totalCount = 0;
    protected boolean isLoadingForFirstTime = true;
    Handler mHandler = new Handler();
    private Exams exams;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.student_result_view);
        serviceHelper = new ServiceHelper(this);
        serviceHelper.setServiceListener(this);
        progressDialogHelper = new ProgressDialogHelper(this);
        loadMoreListView = (ListView) findViewById(R.id.listView_events);
        loadMoreListView.setOnItemClickListener(this);
        classStudentArrayList = new ArrayList<>();
        exams = (Exams) getIntent().getSerializableExtra("eventObj");
        Bundle extras = getIntent().getExtras();
        storeClassId = extras.getString("storeClassId");
        storeSectionId = extras.getString("storeSectionId");

        ImageView bckbtn = (ImageView) findViewById(R.id.back_res);
        bckbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        GetStudentData();
    }

    private void GetStudentData() {
        if (classStudentArrayList != null)
            classStudentArrayList.clear();

        if (CommonUtils.isNetworkAvailable(this)) {

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(EnsyfiConstants.PARAMS_CLASS_ID_LIST, storeClassId);
                jsonObject.put(EnsyfiConstants.PARAMS_SECTION_ID_LIST, storeSectionId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            progressDialogHelper.showProgressDialog(getString(R.string.progress_loading));
            String url = EnsyfiConstants.BASE_URL + PreferenceStorage.getInstituteCode(getApplicationContext()) + EnsyfiConstants.GET_STUDENT_LISTS;
            serviceHelper.makeGetServiceCall(jsonObject.toString(), url);
        } else {
            AlertDialogHelper.showSimpleAlertDialog(this, "No Network connection");
        }
    }

    private boolean validateSignInResponse(JSONObject response) {
        boolean signInsuccess = false;
        if ((response != null)) {
            try {
                String status = response.getString("status");
                String msg = response.getString(EnsyfiConstants.PARAM_MESSAGE);
                Log.d(TAG, "status val" + status + "msg" + msg);

                if ((status != null)) {
                    if (((status.equalsIgnoreCase("activationError")) || (status.equalsIgnoreCase("alreadyRegistered")) ||
                            (status.equalsIgnoreCase("notRegistered")) || (status.equalsIgnoreCase("error")))) {
                        signInsuccess = false;
                        Log.d(TAG, "Show error dialog");
                        AlertDialogHelper.showSimpleAlertDialog(this, msg);

                    } else {
                        signInsuccess = true;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return signInsuccess;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "onEvent list item click" + position);
        ClassStudent classStudent = null;
        if ((classStudentListAdapter != null) && (classStudentListAdapter.ismSearching())) {
            Log.d(TAG, "while searching");
            int actualindex = classStudentListAdapter.getActualEventPos(position);
            Log.d(TAG, "actual index" + actualindex);
            classStudent = classStudentArrayList.get(actualindex);
        } else {
            classStudent = classStudentArrayList.get(position);
        }

        PreferenceStorage.saveStudentRegisteredIdPreference(this, classStudent.getEnrollId());

        String isInternalExternal = exams.getIsInternalExternal();

        if (isInternalExternal.equalsIgnoreCase("1")) {
            Intent intent = new Intent(this, ExamMarksActivity.class);
            intent.putExtra("eventObj", exams);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, ExamOnlyTotalMarksActivity.class);
            intent.putExtra("eventObj", exams);
            startActivity(intent);
        }
    }

    @Override
    public void onAlertPositiveClicked(int tag) {

    }

    @Override
    public void onAlertNegativeClicked(int tag) {

    }

    @Override
    public void onResponse(final JSONObject response) {
        progressDialogHelper.hideProgressDialog();

        if (validateSignInResponse(response)) {

            try {
                JSONArray getData = response.getJSONArray("data");

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressDialogHelper.hideProgressDialog();
                        Gson gson = new Gson();
                        ClassStudentList classStudentList = gson.fromJson(response.toString(), ClassStudentList.class);
                        if (classStudentList.getClassStudent() != null && classStudentList.getClassStudent().size() > 0) {
                            totalCount = classStudentList.getCount();
                            isLoadingForFirstTime = false;
                            updateListAdapter(classStudentList.getClassStudent());
                        }
                    }
                });

            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {
            Log.d(TAG, "Error while sign In");
        }
    }

    @Override
    public void onError(final String error) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                progressDialogHelper.hideProgressDialog();
                AlertDialogHelper.showSimpleAlertDialog(StudentResultView.this, error);
            }
        });
    }

    protected void updateListAdapter(ArrayList<ClassStudent> classStudentArrayList) {
        this.classStudentArrayList.addAll(classStudentArrayList);
        if (classStudentListAdapter == null) {
            classStudentListAdapter = new ClassStudentListAdapter(this, this.classStudentArrayList);
            loadMoreListView.setAdapter(classStudentListAdapter);
        } else {
            classStudentListAdapter.notifyDataSetChanged();
        }
    }
}
