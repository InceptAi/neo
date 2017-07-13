package com.inceptai.neoservice.expert;

/**
 * Created by arunesh on 7/1/17.
 */

import android.util.Log;

import com.inceptai.neoservice.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents two-way communication with an expert system (a human or a bot expert).
 */
public class ExpertChannel implements  ServerConnection.Callback {
    private static final String VIEW_CLICKED_KEY = "viewId";
    private ServerConnection serverConnection;

    private String serverUrl;
    private OnExpertClick onExpertClick;

    public ExpertChannel(String serverUrl, OnExpertClick onExpertClick) {
        this.serverUrl = serverUrl;
        this.onExpertClick = onExpertClick;
    }

    public void connect() {
        serverConnection = new ServerConnection(serverUrl, this);
        serverConnection.connect();
    }

    public void sendViewHierarchy(String jsonHierarchy) {
        if (serverConnection != null) {
            serverConnection.sendMessage(jsonHierarchy);
        }
    }

    @Override
    public void onConnectionStatus(int status) {
        Log.i(Utils.TAG, "Got connection status: " + status);
    }

    @Override
    public void onMessage(String message) {
        Log.i(Utils.TAG, "Got message: " + message);
        if (onExpertClick != null) {
            try {
                processClickEvent(message);
            } catch (JSONException e) {
                Log.e(Utils.TAG, "Exception processing click json: " + e);
            }
        }
    }

    private void processClickEvent(String message) throws JSONException {
        JSONObject jsonObject = new JSONObject(message);
        String viewId = jsonObject.getString(VIEW_CLICKED_KEY);
        if (!Utils.nullOrEmpty(viewId) && onExpertClick != null) {
            onExpertClick.onClick(viewId);
        }
    }

    public static interface OnExpertClick {
        void onClick(String viewId);
    }
}
