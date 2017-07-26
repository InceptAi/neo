package com.inceptai.neoservice.expert;

/**
 * Created by arunesh on 7/1/17.
 */

import android.util.Log;

import com.inceptai.neoservice.NeoUiActionsService;
import com.inceptai.neoservice.NeoThreadpool;
import com.inceptai.neoservice.Utils;
import com.inceptai.neoservice.flatten.UiManager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents two-way communication with an expert system (a human or a bot expert).
 */
public class ExpertChannel implements  ServerConnection.Callback {
    private static final String VIEW_CLICKED_KEY = "viewId";
    private ServerConnection serverConnection;

    private String serverUrl;
    private ExpertChannelCallback expertChannelCallback;
    private NeoUiActionsService neoService;
    private NeoThreadpool neoThreadpool;
    private String userUuid;

    public ExpertChannel(String serverUrl, ExpertChannelCallback expertChannelCallback, NeoUiActionsService neoService, NeoThreadpool neoThreadpool, String userUuid) {
        this.serverUrl = serverUrl;
        this.expertChannelCallback = expertChannelCallback;
        this.neoService = neoService;
        this.neoThreadpool = neoThreadpool;
        this.userUuid = userUuid;
    }

    public void connect() {
        serverConnection = new ServerConnection(serverUrl, this, neoThreadpool.getScheduledExecutorService(), userUuid, 10 /* num attempts */);
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
        if (expertChannelCallback != null) {
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

    }

    public interface ExpertChannelCallback {
        void onClick(String viewId);
        void onAction(JSONObject actionJson);
    }
}
