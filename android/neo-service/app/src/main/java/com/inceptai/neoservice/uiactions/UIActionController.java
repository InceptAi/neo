package com.inceptai.neoservice.uiactions;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.inceptai.neoservice.uiactions.views.ActionDetails;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by vivek on 10/19/17.
 */

public class UIActionController implements Callback<List<ActionDetails>> {
    private static final String BASE_URL = "http://dobby1743.duckdns.org/";

    public void start() {
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        UIActionsAPI uiActionsAPI = retrofit.create(UIActionsAPI.class);

        Call<List<ActionDetails>> call = uiActionsAPI.getAllUIActions();
        //loadChanges("status:open");
        call.enqueue(this);

    }

    @Override
    public void onResponse(Call<List<ActionDetails>> call, Response<List<ActionDetails>> response) {
        if(response.isSuccessful()) {
            List<ActionDetails> actionDetailsList = response.body();
            if (actionDetailsList != null) {
                for (ActionDetails actionDetails: actionDetailsList) {
                    Log.d("RETROFIT", actionDetails.toString());
                }
            }
        } else if (response.errorBody() != null){
            Log.e("RETROFIT", response.errorBody().toString());
        }
    }

    @Override
    public void onFailure(Call<List<ActionDetails>> call, Throwable t) {
        t.printStackTrace();
    }
}
