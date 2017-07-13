package com.inceptai.neoexpert;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements ServerConnection.Callback {
    private static final String DEFAULT_SERVER_IP = "192.168.1.128";
    private static final String NUM_VIEWS_KEY = "numViews";
    private static final String VIEW_MAP_KEY = "viewMap";

    private ListView listView;
    private String serverUrl;
    private ServerConnection serverConnection;
    private Handler handler;
    private ArrayAdapter<ViewEntry> listViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fetchServerUrl();
        setContentView(R.layout.activity_main);
        handler = new Handler();
        listView = (ListView) findViewById(R.id.ui_listview);
        listViewAdapter = new ListViewArrayAdapter(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(listViewAdapter);
        startServerConnection();
    }

    private void startServerConnection() {
        if (Utils.nullOrEmpty(serverUrl)) {
            Log.e(Utils.TAG, "Null or empty server URL.");
        }
        serverConnection = new ServerConnection(serverUrl, this);
        serverConnection.connect();
    }

    private void fetchServerUrl() {
        String serverIp = BuildConfig.SERVER_IP;
        if (Utils.nullOrEmpty(serverIp)) {
            serverIp = DEFAULT_SERVER_IP;
        }
        serverUrl = "ws://" + serverIp + ":8080/";
        Log.i(Utils.TAG, "Setting serverUrl to: " + serverUrl);
    }

    @Override
    public void onConnectionStatus(int status) {

    }

    @Override
    public void onMessage(final String message) {
        // Parse message and populate listView.
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    processIncomingMessage(message);
                } catch (JSONException e) {
                    Log.e(Utils.TAG, "JSONException: " + e);
                }
            }
        });

    }

    private void processIncomingMessage(String message) throws JSONException {
        ArrayList<ViewEntry> viewEntries = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(message);
        int numViews = jsonObject.getInt(NUM_VIEWS_KEY);
        JSONObject viewMap = jsonObject.getJSONObject(VIEW_MAP_KEY);
        Iterator<String> keysIterator = viewMap.keys();
        while (keysIterator.hasNext()) {
            String key = keysIterator.next();
            String value = viewMap.getString(key);
            ViewEntry entry = new ViewEntry(key, value);
            viewEntries.add(entry);
        }
        listViewAdapter.clear();
        listViewAdapter.addAll(viewEntries);
    }
}
