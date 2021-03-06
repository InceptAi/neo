package com.inceptai.neoservice.expert;

import android.util.Log;

import com.inceptai.neoservice.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Created by arunesh on 7/1/17.
 */

public class WebSocketConnection extends WebSocketListener {
    public static final int STATUS_CONNECTED = 1001;
    public static final int STATUS_CLOSED = 1002;
    private static final int RECONNECT_DELAY_MS = 3000;

    private String serverUrl;
    private WebSocket webSocket;
    private OkHttpClient client;
    private Callback callback;
    private String userUuid;
    private int numAttempts = 1;
    private ScheduledExecutorService executorService;

    public interface Callback {
        void onConnectionStatus(int status);
        void onMessage(String message);
    }

    public WebSocketConnection(String url, Callback callback, ScheduledExecutorService executorService, String userUuid, int numAttempts) {
        this.serverUrl = url;
        this.callback = callback;
        this.executorService = executorService;
        this.userUuid = userUuid;
        this.numAttempts = numAttempts;
        client = new OkHttpClient.Builder()
                .readTimeout(3, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public void connect() {
        if (serverUrl == null) {
            Log.e(Utils.TAG, "Server url null, returning from connect");
            return;
        }
        if (numAttempts > 0) {
            Request request = new Request.Builder()
                    .url(serverUrl)
                    .build();
            webSocket = client.newWebSocket(request, this);
            numAttempts --;
        } else {
            Log.i(Utils.TAG, "Ignoring connect request, numAttempts = " + numAttempts);
        }
    }

    public void sendMessage(String message) {
        if (webSocket != null) {
            webSocket.send(message);
        }
    }

    public void disconnect() {
        numAttempts = 0;
        if (webSocket != null) {
            webSocket.cancel();
            webSocket = null;
        }
        client = null;
        executorService = null;
        callback = null;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        Log.i(Utils.TAG, "WebSocket opened.");
        try {
            sendUserUuidMessage();
        } catch (JSONException e) {
            Log.e(Utils.TAG, "Exception sending user UUID message to relay server." + e);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        if (callback != null) {
            callback.onMessage(text);
        }
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        Log.i(Utils.TAG, "WebSocket closed: code " + code + " reason: " + reason);
        attemptReconnect();
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        Log.i(Utils.TAG, "WebSocket failed:  " + t + " Response object: " + response);
        attemptReconnect();
    }

    private boolean shouldReconnect() {
        Log.i(Utils.TAG, "WebSocket reconnect attempts remaining: " + numAttempts);
        return numAttempts > 0;
    }

    private void attemptReconnect() {
        if (shouldReconnect()) {
            executorService.schedule(new Runnable() {
                @Override
                public void run() {
                    connect();
                }
            }, RECONNECT_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void sendUserUuidMessage() throws JSONException{
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("uuid", userUuid);
        String userUuidMessage = jsonObject.toString();
        webSocket.send(userUuidMessage);
    }
}
