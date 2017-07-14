package com.inceptai.neoservice.expert;

/**
 * Created by arunesh on 7/1/17.
 */

import android.accessibilityservice.AccessibilityService;
import android.util.Log;

import com.inceptai.neoservice.NeoService;
import com.inceptai.neoservice.NeoThreadpool;
import com.inceptai.neoservice.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents two-way communication with an expert system (a human or a bot expert).
 */
public class ExpertChannel implements  ServerConnection.Callback {
    private static final String VIEW_CLICKED_KEY = "viewId";
    private static final String GLOBAL_ACTION_KEY = "actionName";
    private static final String END_ACTION = "end";
    private static final String BACK_ACTION = "back";

    private ServerConnection serverConnection;

    private String serverUrl;
    private OnExpertClick onExpertClick;
    private NeoService neoService;
    private NeoThreadpool neoThreadpool;

    public ExpertChannel(String serverUrl, OnExpertClick onExpertClick, NeoService neoService, NeoThreadpool neoThreadpool) {
        this.serverUrl = serverUrl;
        this.onExpertClick = onExpertClick;
        this.neoService = neoService;
        this.neoThreadpool = neoThreadpool;
    }

    public void connect() {
        serverConnection = new ServerConnection(serverUrl, this, neoThreadpool.getScheduledExecutorService(), 10 /* num attempts */);
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
                processExpertMessage(message);
            } catch (JSONException e) {
                Log.e(Utils.TAG, "Exception processing click json: " + e);
            }
        }
    }

    private void processExpertMessage(String message) throws JSONException {
        JSONObject jsonObject = new JSONObject(message);
        if (jsonObject.has(VIEW_CLICKED_KEY)) {
            processClickAction(jsonObject);
        } else if (jsonObject.has(GLOBAL_ACTION_KEY)) {
            processGlobalAction(jsonObject);
        }
    }

    private void processClickAction(JSONObject message) throws JSONException {
        String viewId = message.getString(VIEW_CLICKED_KEY);
        if (!Utils.nullOrEmpty(viewId) && onExpertClick != null && Integer.valueOf(viewId) != 0) {
            onExpertClick.onClick(viewId);
        }
    }

    private void processGlobalAction(JSONObject message) throws JSONException {
        String action = message.getString(GLOBAL_ACTION_KEY);
        if (!Utils.nullOrEmpty(action)) {
            if (BACK_ACTION.equals(action)) {
                // back global action.
                neoService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            } else if (END_ACTION.equals(action)) {
                // end global action.
            }
        }
    }

    public interface OnExpertClick {
        void onClick(String viewId);
    }
}
