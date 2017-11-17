package com.inceptai.neoservice.uiactions;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.inceptai.neopojos.ActionDetails;
import com.inceptai.neopojos.ActionResponse;
import com.inceptai.neopojos.DeviceInfo;
import com.inceptai.neoservice.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by vivek on 10/19/17.
 */

public class UIActionController implements Callback<ActionResponse> {
    private static final String BASE_URL = "http://dobby1743.duckdns.org:9000/";
    private UIActionsAPI uiActionsAPI;
    private AtomicBoolean requestInFlight;
    private UIActionControllerCallback uiActionControllerCallback;
    private DeviceInfo deviceInfo;
    private Executor uiActionServerCallExecutor;
    private String packageNameForRequest;
    private String queryForRequest;

    public interface UIActionControllerCallback {
        void onUIActionDetails(List<ActionDetails> actionDetailsList, String query, String packageName);
        void onUIActionError(int errorCode, String query, String packageName);
    }

    public UIActionController(@Nullable UIActionControllerCallback uiActionControllerCallback,
                              Executor uiActionServerCallExecutor) {
        requestInFlight = new AtomicBoolean(false);
        this.uiActionControllerCallback = uiActionControllerCallback;
        this.uiActionServerCallExecutor = uiActionServerCallExecutor;

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .callbackExecutor(this.uiActionServerCallExecutor)
                .build();

        uiActionsAPI = retrofit.create(UIActionsAPI.class);
        deviceInfo = Utils.createDeviceInfo();
        packageNameForRequest = Utils.EMPTY_STRING;
        queryForRequest = Utils.EMPTY_STRING;
    }

//    public void fetchUIActionsForSettings(String query) {
//        if (requestInFlight) {
//            return;
//        }
//        Call<ActionResponse> call;
//        if (!Utils.nullOrEmpty(query)) {
//            if (deviceInfo != null && !Utils.nullOrEmpty(Utils.gson.toJson(deviceInfo))) {
//                call = uiActionsAPI.getUIActionsForDeviceAndQuery(Utils.gson.toJson(deviceInfo), query);
//            } else {
//                call = uiActionsAPI.getUIActionsForQuery(query);
//            }
//        }  else {
//            call = uiActionsAPI.getAllUIActions();
//        }
//        call.enqueue(this);
//        requestInFlight = true;
//    }
//
//    public void fetchUIActionsForApps(String packageName, String startingScreenTitle, String query) {
//        if (requestInFlight) {
//            return;
//        }
//        Call<ActionResponse> call;
//        if (!Utils.nullOrEmpty(query) && !Utils.nullOrEmpty(startingScreenTitle)) {
//            HashMap<String, String> options  = new HashMap<>();
//            options.put("query", query);
//            options.put("title", startingScreenTitle);
//            call = uiActionsAPI.getUIAppActionsForQuery(packageName, options);
//            call.enqueue(this);
//            requestInFlight = true;
//        }
//    }

    public void fetchUIActions(String packageName, String startingScreenTitle,
                               String startingScreenType, String versionName,
                               String versionCode, String query) {
        //TODO: Check if this works.
        if (!requestInFlight.compareAndSet(false, true)) {
            if (uiActionControllerCallback != null) {
                uiActionControllerCallback.onUIActionError(UIActionResult.UIActionResultCodes.ANOTHER_SERVER_REQUEST_IN_FLIGHT, query, packageName);
            }
            return;
        }
        //requestInFlight.set(true);
        Call<ActionResponse> call;
        HashMap<String, String> options  = new HashMap<>();
        if (!Utils.nullOrEmpty(query) && !Utils.nullOrEmpty(startingScreenTitle)) {
            options.put("query", query);
            options.put("title", startingScreenTitle);
            options.put("type", startingScreenType);
        }
        call = uiActionsAPI.getUIActions(packageName, Utils.gson.toJson(deviceInfo), versionName, versionCode,  options);
        packageNameForRequest = packageName;
        queryForRequest = query;
        call.enqueue(this);
    }

    @Override
    public void onResponse(Call<ActionResponse> call, Response<ActionResponse> response) {
        //Switch thread here -- this is returned by default on main thread
        if(response.isSuccessful()) {
            ActionResponse actionResponse = response.body();
            List<ActionDetails> actionDetailsList = new ArrayList<>();
            if (actionResponse != null) {
                actionDetailsList = actionResponse.getActionList();
            }
            if (uiActionControllerCallback != null) {
                uiActionControllerCallback.onUIActionDetails(actionDetailsList, queryForRequest, packageNameForRequest);
            }
        } else if (response.errorBody() != null){
            Log.e("RETROFIT", response.errorBody().toString());
            if (uiActionControllerCallback != null) {
                uiActionControllerCallback.onUIActionError(UIActionResult.UIActionResultCodes.SERVER_ERROR, queryForRequest, packageNameForRequest);
            }
        }
        requestInFlight.set(false);
    }

    @Override
    public void onFailure(Call<ActionResponse> call, Throwable t) {
        //Switch thread here -- this is returned by default on main thread
        requestInFlight.set(false);
        t.printStackTrace();
        if (uiActionControllerCallback != null) {
            uiActionControllerCallback.onUIActionError(UIActionResult.UIActionResultCodes.SERVER_TIMED_OUT, queryForRequest, packageNameForRequest);
        }
    }
}
