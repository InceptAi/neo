package com.inceptai.neoservice;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import com.inceptai.neoservice.expert.ExpertChannel;
import com.inceptai.neoservice.flatten.FlatView;
import com.inceptai.neoservice.flatten.FlatViewHierarchy;

/**
 * Created by arunesh on 6/29/17.
 */

public class NeoService extends AccessibilityService {
    private static final String TAG = Utils.TAG;
    private static final String SERVER_IP_PROPERTY_KEY = "com.inceptai.server_ip";
    private static final String DEFAULT_SERVER_IP = "192.168.1.128";

    private View overlayView;
    private LayoutParams neoOverlayLayout;
    private WindowManager windowManager;
    private DisplayMetrics primaryDisplayMetrics;
    private ExpertChannel expertChannel;
    private NeoThreadpool neoThreadpool;
    private String serverUrl;

    private Handler handler;

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
        expertChannel = new ExpertChannel(serverUrl);
        expertChannel.connect();
        neoThreadpool = new NeoThreadpool();
        handler = new Handler();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i(TAG, "Got event:" + event);
        sendViewSnapshot(computeViewHierarchy());
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
                FlatViewHierarchy viewHierarchy = computeViewHierarchy();
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

    private FlatViewHierarchy computeViewHierarchy() {
        FlatViewHierarchy flatViewHierarchy = new FlatViewHierarchy(getRootInActiveWindow(), primaryDisplayMetrics);
        flatViewHierarchy.flatten();
        return flatViewHierarchy;
    }

    private void fetchServerUrl() {
        String serverIp = System.getProperty(SERVER_IP_PROPERTY_KEY, DEFAULT_SERVER_IP);
        serverUrl = "ws://" + serverIp + ":8080/";
        Log.i(TAG, "Using server URL: " + serverUrl);
    }
}
