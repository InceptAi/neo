package com.inceptai.neoservice.uiactions;

import android.util.Log;

import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.inceptai.neopojos.ActionDetails;
import com.inceptai.neopojos.ActionResponse;
import com.inceptai.neopojos.CrawlingInput;
import com.inceptai.neopojos.DeviceInfo;
import com.inceptai.neoservice.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;

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
    private DeviceInfo deviceInfo;
    private Executor uiActionServerCallExecutor;
    private String packageNameForRequest;
    private String queryForRequest;
    private SettableFuture<UIActionResult> serverCallFuture;

    public UIActionController(Executor uiActionServerCallExecutor) {
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

    public SettableFuture<UIActionResult> fetchUIActionsForSettings(String versionName, String versionCode, String query) {
        //TODO: Check if this works.
        String packageName = Utils.SETTINGS_PACKAGE_NAME;
        String startingScreenTitle = Utils.SETTINGS_BASE_TITLE;
        String startingScreenType = CrawlingInput.FULL_SCREEN_MODE;
        return fetchUIActions(packageName, startingScreenTitle, startingScreenType, versionName, versionCode, query);
    }


    public SettableFuture<UIActionResult> fetchUIActions(String packageName, String startingScreenTitle,
                                         String startingScreenType, String versionName,
                                         String versionCode, String query) {
        //TODO: Check if this works.
        if (serverCallFuture != null && !serverCallFuture.isDone()) {
            serverCallFuture.set(new UIActionResult(UIActionResult.UIActionResultCodes.ANOTHER_SERVER_REQUEST_IN_FLIGHT, query, packageName));
            return serverCallFuture;
        }

        serverCallFuture = SettableFuture.create();

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
        return serverCallFuture;
    }


    @Override
    public void onResponse(Call<ActionResponse> call, Response<ActionResponse> response) {
        //Switch thread here -- this is returned by default on main thread
        UIActionResult uiActionResult = new UIActionResult(UIActionResult.UIActionResultCodes.UNKNOWN, queryForRequest, packageNameForRequest);
        if(response.isSuccessful()) {
            ActionResponse actionResponse = response.body();
            List<ActionDetails> actionDetailsList = new ArrayList<>();
            if (actionResponse != null) {
                actionDetailsList = actionResponse.getActionList();
            }
            uiActionResult.setStatus(UIActionResult.UIActionResultCodes.SUCCESS);
            uiActionResult.setPayload(actionDetailsList);
        } else {
            Log.e("RETROFIT", response.errorBody() != null ? response.errorBody().toString() : Utils.EMPTY_STRING );
            uiActionResult.setStatus(UIActionResult.UIActionResultCodes.SERVER_ERROR);
            uiActionResult.setPayload(response.code());
        }
        serverCallFuture.set(uiActionResult);
    }

    @Override
    public void onFailure(Call<ActionResponse> call, Throwable t) {
        //Switch thread here -- this is returned by default on main thread
        t.printStackTrace();
        UIActionResult uiActionResult = new UIActionResult(
                UIActionResult.UIActionResultCodes.SERVER_TIMED_OUT,
                queryForRequest,
                packageNameForRequest);
        serverCallFuture.set(uiActionResult);
    }
}
