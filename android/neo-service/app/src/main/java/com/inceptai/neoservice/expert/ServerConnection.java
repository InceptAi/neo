package com.inceptai.neoservice.expert;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Created by arunesh on 7/1/17.
 */

public class ServerConnection extends WebSocketListener {
    public static final int STATUS_CONNECTED = 1001;
    public static final int STATUS_CLOSED = 1002;

    private String serverUrl;
    private WebSocket webSocket;
    private OkHttpClient client;
    private Callback callback;

    public interface Callback {
        void onConnectionStatus(int status);
        void onMessage(String message);
    }

    public ServerConnection(String url, Callback callback) {
        this.serverUrl = url;
        this.callback = callback;
    }

    public void connect() {
        client = new OkHttpClient.Builder()
                .readTimeout(3, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        Request request = new Request.Builder()
                .url(serverUrl)
                .build();
        webSocket = client.newWebSocket(request, this);
    }

    public void sendMessage(String message) {
        if (webSocket != null) {
            webSocket.send(message);
        }
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        if (callback != null) {
            callback.onMessage(text);
        }
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
    }
}
