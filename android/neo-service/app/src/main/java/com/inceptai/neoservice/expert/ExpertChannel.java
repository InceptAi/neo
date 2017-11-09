package com.inceptai.neoservice.expert;

/**
 * Created by arunesh on 7/1/17.
 */

import android.util.Log;

import com.inceptai.neoservice.NeoThreadpool;
import com.inceptai.neoservice.NeoUiActionsService;
import com.inceptai.neoservice.Utils;
import com.inceptai.neoservice.flatten.UiManager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents two-way communication with an expert system (a human or a bot expert).
 */
public class ExpertChannel implements  WebSocketConnection.Callback {
    private static final String VIEW_CLICKED_KEY = "viewId";
    private WebSocketConnection webSocketConnection;

    private String serverUrl;
    private ExpertChannelCallback expertChannelCallback;
    private NeoUiActionsService neoService;
    private NeoThreadpool neoThreadpool;
    private String userUuid;

    public interface ExpertChannelCallback {
        void onClick(String viewId);
        void onAction(JSONObject actionJson);
    }

    public ExpertChannel(String serverUrl, ExpertChannelCallback expertChannelCallback, NeoUiActionsService neoService, NeoThreadpool neoThreadpool, String userUuid) {
        this.serverUrl = serverUrl;
        this.expertChannelCallback = expertChannelCallback;
        this.neoService = neoService;
        this.neoThreadpool = neoThreadpool;
        this.userUuid = userUuid;
    }

    public void connect() {
        webSocketConnection = new WebSocketConnection(serverUrl, this, neoThreadpool.getScheduledExecutorService(), userUuid, 10 /* num attempts */);
        webSocketConnection.connect();
    }

    public void sendViewHierarchy(String jsonHierarchy) {
        if (webSocketConnection != null) {
            webSocketConnection.sendMessage(jsonHierarchy);
        }
    }

    @Override
    public void onConnectionStatus(int status) {
        Log.i(Utils.TAG, "Got connection status: " + status);
    }

    @Override
    public void onMessage(String message) {
        Log.i(Utils.TAG, "Got message: " + message);
        if (expertChannelCallback != null) {
            try {
                processExpertMessage(message);
            } catch (JSONException e) {
                Log.e(Utils.TAG, "Exception processing click json: " + e);
            }
        }
    }

    public void cleanup() {
        if (webSocketConnection != null) {
            webSocketConnection.disconnect();
            webSocketConnection = null;
        }
        expertChannelCallback = null;
        neoService = null;
        neoThreadpool = null;
    }

    private void processExpertMessage(String message) throws JSONException {
        JSONObject jsonObject = new JSONObject(message);
        if (jsonObject.has(VIEW_CLICKED_KEY)) {
            processClickAction(jsonObject);
        } else if (jsonObject.has(UiManager.GLOBAL_ACTION_KEY)) {
            processGlobalAction(jsonObject);
        }
    }

    private void processClickAction(JSONObject message) throws JSONException {
        String viewId = message.getString(VIEW_CLICKED_KEY);
        if (!Utils.nullOrEmpty(viewId) && expertChannelCallback != null && Integer.valueOf(viewId) != 0) {
            expertChannelCallback.onClick(viewId);
        }
    }

    private void processGlobalAction(JSONObject message) throws JSONException {
        if (expertChannelCallback != null) {
            expertChannelCallback.onAction(message);
        }
    }
}
