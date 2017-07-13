package com.inceptai.neoservice.expert;

/**
 * Created by arunesh on 7/1/17.
 */

import android.util.Log;

import com.inceptai.neoservice.Utils;

/**
 * Represents two-way communication with an expert system (a human or a bot expert).
 */
public class ExpertChannel implements  ServerConnection.Callback {
    private static final String SERVER_ADDRESS = "ws://192.168.1.129:8080/";
    private ServerConnection serverConnection;

    public void connect() {
        serverConnection = new ServerConnection(SERVER_ADDRESS, this);
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
    }
}
