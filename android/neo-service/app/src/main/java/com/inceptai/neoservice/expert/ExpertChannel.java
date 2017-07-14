package com.inceptai.neoservice.expert;

/**
 * Created by arunesh on 7/1/17.
 */

import android.accessibilityservice.AccessibilityService;
import android.util.Log;

import com.inceptai.neoservice.NeoService;
import com.inceptai.neoservice.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents two-way communication with an expert system (a human or a bot expert).
 */
public class ExpertChannel implements  ServerConnection.Callback {
    private static final String VIEW_CLICKED_KEY = "viewId";
    private static final String ACTION_KEY = "actionName";
    private static final String END_ACTION = "end";
    private static final String BACK_ACTION = "back";

    private ServerConnection serverConnection;

    private String serverUrl;
    private OnExpertClick onExpertClick;
    private NeoService neoService;

    public ExpertChannel(String serverUrl, OnExpertClick onExpertClick, NeoService neoService) {
        this.serverUrl = serverUrl;
        this.onExpertClick = onExpertClick;
        this.neoService = neoService;
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
                processAction(message);
            } catch (JSONException e) {
                Log.e(Utils.TAG, "Exception processing click json: " + e);
            }
        }
    }

    private void processAction(String message) throws JSONException {
        JSONObject jsonObject = new JSONObject(message);
        String viewId = jsonObject.getString(VIEW_CLICKED_KEY);
        if (!Utils.nullOrEmpty(viewId) && onExpertClick != null && Integer.valueOf(viewId) != 0) {
            onExpertClick.onClick(viewId);
        }

        String action = jsonObject.getString(ACTION_KEY);
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
