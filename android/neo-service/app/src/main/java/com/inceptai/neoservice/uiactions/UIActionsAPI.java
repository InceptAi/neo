package com.inceptai.neoservice.uiactions;


import com.inceptai.neopojos.ActionResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

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

    @GET("actions/pkg/{pkgName}")
    Call<ActionResponse> getUIAppActionsForQuery(@Path("pkgName") String pkgName, @QueryMap Map<String, String> options);

    @GET("actions/pkg/{pkgName}/device/{deviceInfo}")
    Call<ActionResponse> getUIActions(@Path("pkgName") String pkgName, @Path("deviceInfo") String deviceInfo, @QueryMap Map<String, String> options);

    @GET("actions/pkg/{pkgName}/device/{deviceInfo}/version/{versionName}/code/{versionCode}")
    Call<ActionResponse> getUIActions(@Path("pkgName") String pkgName, @Path("deviceInfo") String deviceInfo, @Path("versionName") String versionName, @Path("versionCode") String versionCode, @QueryMap Map<String, String> options);

}
