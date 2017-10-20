package com.inceptai.neoservice.uiactions;

import com.inceptai.neoservice.uiactions.views.ActionDetails;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by vivek on 10/20/17.
 */

public interface UIActionsAPI {
    @GET("settings")
    Call<List<ActionDetails>> getUIActions(@Query("query") String actionQuery);
    //Call<List<ActionDetails>> getUIActions( @QueryMap Map<String, String> options);

    @GET("actions/all")
    Call<List<ActionDetails>> getAllUIActions();

    @GET("settings/device/{deviceInfo}/")
    Call<List<ActionDetails>> getUIActionsForDevice(@Path("deviceInfo") String deviceInfo, @Query("query") String actionQuery);

}
