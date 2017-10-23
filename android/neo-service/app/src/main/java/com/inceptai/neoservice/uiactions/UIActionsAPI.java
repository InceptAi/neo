package com.inceptai.neoservice.uiactions;

import com.inceptai.neoservice.uiactions.views.ActionResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by vivek on 10/20/17.
 */

public interface UIActionsAPI {
    @GET("settings")
    Call<ActionResponse> getUIActionsForQuery(@Query("query") String actionQuery);
    //Call<List<ActionDetails>> getUIActionsForQuery( @QueryMap Map<String, String> options);

    @GET("actions/all")
    Call<ActionResponse> getAllUIActions();

    @GET("settings/device/{deviceInfo}/")
    Call<ActionResponse> getUIActionsForDeviceAndQuery(@Path("deviceInfo") String deviceInfo, @Query("query") String actionQuery);

}
