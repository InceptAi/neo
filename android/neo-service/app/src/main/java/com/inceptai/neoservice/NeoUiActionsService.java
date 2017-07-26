package com.inceptai.neoservice;

import android.accessibilityservice.AccessibilityService;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.provider.Settings;
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

import static android.R.attr.start;

/**
 * Created by arunesh on 6/29/17.
 */

public class NeoUiActionsService extends AccessibilityService implements ExpertChannel.OnExpertClick {
    public static final String UUID_INTENT_PARAM = "UUID";
    public static final String SERVER_ADDRESS = "SERVER_ADDRESS";
    public static NeoService PARENT_INSTANCE;  // HACK.

    private static final String TAG = Utils.TAG;
    private static final String DEFAULT_SERVER_IP = "192.168.1.128";
    private static final String NEO_INTENT = "com.inceptai.neo.ACTION";

    private View overlayView;
    private LayoutParams neoOverlayLayout;
    private WindowManager windowManager;
    private DisplayMetrics primaryDisplayMetrics;
    private ExpertChannel expertChannel;
    private NeoThreadpool neoThreadpool;
    private String serverUrl;

    private Handler handler;
    private UiManager uiManager;

    private NeoCustomIntentReceiver intentReceiver;
    private boolean isOverlayVisible;
    private String userUuid;
    private String serverAddress;
    private UiActionsServiceCallback uiActionsServiceCallback;

    public interface UiActionsServiceCallback {
        void onSettingsError();
        void onServiceReady();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        isOverlayVisible = false;
        overlayView = View.inflate(getBaseContext(), R.layout.persistent_bottom_sheet, null);
        neoOverlayLayout = new LayoutParams(
                LayoutParams.MATCH_PARENT /* width */,
                LayoutParams.WRAP_CONTENT /* height */,
                LayoutParams.TYPE_SYSTEM_ERROR,
                LayoutParams.FLAG_DISMISS_KEYGUARD | LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.OPAQUE);
        neoOverlayLayout.gravity = Gravity.BOTTOM | Gravity.LEFT;
        neoOverlayLayout.x = 0;
        neoOverlayLayout.y = -100;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        getDisplayDimensions();
        fetchServerUrl();
        neoThreadpool = new NeoThreadpool();
        uiManager = new UiManager(this, neoThreadpool, primaryDisplayMetrics);

        handler = new Handler();
        intentReceiver = new NeoCustomIntentReceiver();
        IntentFilter intentFilter = new IntentFilter(NEO_INTENT);
        registerReceiver(intentReceiver, intentFilter);
        PARENT_INSTANCE.registerService(this);
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
        userUuid = intent.getExtras().getString(UUID_INTENT_PARAM);
        serverAddress = intent.getExtras().getString(SERVER_ADDRESS);
        if (serverAddress == null) {
            serverAddress = "ws://" + BuildConfig.SERVER_IP + ":8080/";
        }
        expertChannel = new ExpertChannel(serverAddress, this, this, neoThreadpool, userUuid);
        expertChannel.connect();
        if (!showAccessibilitySettings()) {
            Log.i(Utils.TAG, "Unable to show accessibility settings.");
        }
        return START_STICKY;
    }

    public void registerUiActionsCallback(UiActionsServiceCallback uiActionsServiceCallback) {
        this.uiActionsServiceCallback = uiActionsServiceCallback;
    }

    @Override
    public void onDestroy() {
        if (isOverlayVisible) {
            hideOverlay();
        }
        unregisterReceiver(intentReceiver);
        super.onDestroy();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (uiActionsServiceCallback != null) {
            uiActionsServiceCallback.onServiceReady();
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                FlatViewHierarchy viewHierarchy = uiManager.updateViewHierarchy(getRootInActiveWindow());
                sendViewSnapshot(viewHierarchy);
            }
        }, 10000);
    }

    private boolean showAccessibilitySettings() {
        Intent settingsIntent = new Intent(
                Settings.ACTION_ACCESSIBILITY_SETTINGS);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        boolean isOk = true;
        try {
            this.startActivity(settingsIntent);
        } catch (ActivityNotFoundException e) {
            isOk = false;
        }
        return isOk;
    }

    private void getDisplayDimensions() {
        primaryDisplayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(primaryDisplayMetrics);
    }

    private void sendViewSnapshot(FlatViewHierarchy flatViewHierarchy) {
        // expertChannel.sendViewHierarchy(flatViewHierarchy.toJson());
        if (expertChannel != null) {
            expertChannel.sendViewHierarchy(flatViewHierarchy.toSimpleJson());
        }
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

    private class NeoCustomIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            NeoUiActionsService.this.toggleOverlay();
        }
    }

    private void showOverlay() {
        if (windowManager != null) {
            windowManager.addView(overlayView, neoOverlayLayout);
            overlayView.requestLayout();
            isOverlayVisible = true;
        } else {
            Toast.makeText(this, "NULL WINDOW MANAGER.", Toast.LENGTH_SHORT).show();
        }
    }

    private void hideOverlay() {
        if (windowManager != null && overlayView != null) {
            windowManager.removeView(overlayView);
        }
        isOverlayVisible = false;
    }

    private void toggleOverlay() {
        if (isOverlayVisible) {
            hideOverlay();
        } else {
            showOverlay();
        }
    }
}
