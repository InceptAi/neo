package com.inceptai.neoservice.uiactions;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.inceptai.neoservice.Utils;
import com.inceptai.neoservice.uiactions.views.ActionDetails;
import com.inceptai.neoservice.uiactions.views.ActionResponse;

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
    private boolean requestInFlight;
    private UIActionControllerCallback uiActionControllerCallback;
    private HashMap<String, String> deviceInfo;
    private Executor uiActionServerCallExecutor;

    public interface UIActionControllerCallback {
        void onUIActionDetails(List<ActionDetails> actionDetailsList);
        void onUIActionError(String error);
    }

    public UIActionController(@Nullable UIActionControllerCallback uiActionControllerCallback,
                              Executor uiActionServerCallExecutor) {
        requestInFlight = false;
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
    }

    public void fetchUIActions(String query) {
        if (requestInFlight) {
            return;
        }
        Call<ActionResponse> call;
        if (!Utils.nullOrEmpty(query)) {
            if (deviceInfo != null && !Utils.nullOrEmpty(deviceInfo.toString())) {
                call = uiActionsAPI.getUIActionsForDeviceAndQuery(deviceInfo.toString(), query);
            } else {
                call = uiActionsAPI.getUIActionsForQuery(query);
            }
        }  else {
            call = uiActionsAPI.getAllUIActions();
        }
        call.enqueue(this);
        requestInFlight = true;
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
                uiActionControllerCallback.onUIActionDetails(actionDetailsList);
            }
            if (actionDetailsList != null) {
                //Process the action details list
                for (ActionDetails actionDetails: actionDetailsList) {
                    Log.d("RETROFIT", actionDetails.toString());
                }
            }
        } else if (response.errorBody() != null){
            Log.e("RETROFIT", response.errorBody().toString());
            if (uiActionControllerCallback != null) {
                uiActionControllerCallback.onUIActionError(response.errorBody().toString());
            }
        }
        requestInFlight = false;
    }

    @Override
    public void onFailure(Call<ActionResponse> call, Throwable t) {
        //Switch thread here -- this is returned by default on main thread
        requestInFlight = false;
        t.printStackTrace();
        if (uiActionControllerCallback != null) {
            uiActionControllerCallback.onUIActionError(t.toString());
        }
    }
}
