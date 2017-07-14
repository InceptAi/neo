package com.inceptai.neoservice;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import com.inceptai.neoservice.expert.ExpertChannel;
import com.inceptai.neoservice.flatten.FlatViewHierarchy;
import com.inceptai.neoservice.flatten.UiManager;

/**
 * Created by arunesh on 6/29/17.
 */

public class NeoService extends AccessibilityService implements ExpertChannel.OnExpertClick {
    private static final String TAG = Utils.TAG;
    private static final String DEFAULT_SERVER_IP = "192.168.1.128";

    private View overlayView;
    private LayoutParams neoOverlayLayout;
    private WindowManager windowManager;
    private DisplayMetrics primaryDisplayMetrics;
    private ExpertChannel expertChannel;
    private NeoThreadpool neoThreadpool;
    private String serverUrl;

    private Handler handler;
    private UiManager uiManager;

    @Override
    public void onCreate() {
        super.onCreate();

        overlayView = View.inflate(getBaseContext(), R.layout.persistent_bottom_sheet, null);
        neoOverlayLayout = new LayoutParams(
                LayoutParams.MATCH_PARENT /* width */,
                LayoutParams.WRAP_CONTENT /* height */,
                LayoutParams.TYPE_SYSTEM_ERROR,
                LayoutParams.FLAG_DISMISS_KEYGUARD | LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        neoOverlayLayout.gravity = Gravity.BOTTOM | Gravity.CENTER;
        neoOverlayLayout.x = 200;
        neoOverlayLayout.y = 200;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        getDisplayDimensions();
        fetchServerUrl();
        neoThreadpool = new NeoThreadpool();
        uiManager = new UiManager(this, neoThreadpool, primaryDisplayMetrics);
        expertChannel = new ExpertChannel(serverUrl, this, this, neoThreadpool);
        expertChannel.connect();
        handler = new Handler();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i(TAG, "Got event:" + event);
        sendViewSnapshot(uiManager.updateViewHierarchy(getRootInActiveWindow()));
    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "onInterrupt");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (windowManager != null) {
            windowManager.addView(overlayView, neoOverlayLayout);
        } else {
            Toast.makeText(this, "NULL WINDOW MANAGER.", Toast.LENGTH_SHORT).show();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (windowManager != null && overlayView != null) {
            windowManager.removeView(overlayView);
        }
        super.onDestroy();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                FlatViewHierarchy viewHierarchy = uiManager.updateViewHierarchy(getRootInActiveWindow());
                sendViewSnapshot(viewHierarchy);
            }
        }, 10000);
    }

    private void getDisplayDimensions() {
        primaryDisplayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(primaryDisplayMetrics);
    }

    private void sendViewSnapshot(FlatViewHierarchy flatViewHierarchy) {
        // expertChannel.sendViewHierarchy(flatViewHierarchy.toJson());
        expertChannel.sendViewHierarchy(flatViewHierarchy.toSimpleJson());
    }

    private void fetchServerUrl() {
        String serverIp = BuildConfig.SERVER_IP;
        if (Utils.nullOrEmpty(serverIp)) {
            serverIp = DEFAULT_SERVER_IP;
        }
        serverUrl = "ws://" + serverIp + ":8080/";
        Log.i(TAG, "Using server URL: " + serverUrl);
    }

    @Override
    public void onClick(String viewId) {
        Log.i(Utils.TAG, "Click event for viewId: " + viewId);
        uiManager.processClick(viewId);
    }
}
