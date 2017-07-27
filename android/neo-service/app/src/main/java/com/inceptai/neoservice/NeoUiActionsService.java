package com.inceptai.neoservice;

import android.accessibilityservice.AccessibilityService;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.inceptai.neoservice.expert.ExpertChannel;
import com.inceptai.neoservice.flatten.FlatViewHierarchy;
import com.inceptai.neoservice.flatten.UiManager;

import org.json.JSONObject;

import static android.view.View.GONE;

/**
 * Created by arunesh on 6/29/17.
 */

public class NeoUiActionsService extends AccessibilityService implements ExpertChannel.ExpertChannelCallback {
    public static final String UUID_INTENT_PARAM = "UUID";
    public static final String SERVER_ADDRESS = "SERVER_ADDRESS";

    private static final String TAG = Utils.TAG;
    private static final String DEFAULT_SERVER_IP = "192.168.1.129";
    private static final String NEO_INTENT = "com.inceptai.neo.ACTION";
    private static final int USER_STOP_DELAY_MS = 2500;

    private View overlayView;
    private LayoutParams neoOverlayLayout;
    private WindowManager windowManager;
    private DisplayMetrics primaryDisplayMetrics;
    private ExpertChannel expertChannel;
    private NeoThreadpool neoThreadpool;

    private UiManager uiManager;

    private NeoCustomIntentReceiver intentReceiver;
    private boolean isOverlayVisible;
    private String userUuid;
    private String serverAddress;
    private UiActionsServiceCallback uiActionsServiceCallback;

    private Button stopButton;
    private TextView overlayTitleTv;
    private TextView overlayStatusTv;
    private Handler handler;
    private boolean overlayPermissionGranted;

    public interface UiActionsServiceCallback {
        void onSettingsError();
        void onServiceReady();
        void onStopByUser();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler();
        isOverlayVisible = false;
        overlayView = View.inflate(getBaseContext(), R.layout.persistent_bottom_sheet, null);
        neoOverlayLayout = new LayoutParams(
                LayoutParams.MATCH_PARENT /* width */,
                LayoutParams.WRAP_CONTENT /* height */,
                LayoutParams.TYPE_SYSTEM_ALERT,
                LayoutParams.FLAG_DISMISS_KEYGUARD | LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.OPAQUE);
        neoOverlayLayout.gravity = Gravity.BOTTOM | Gravity.LEFT;
        neoOverlayLayout.x = 0;
        neoOverlayLayout.y = 0;
        setupOverlay(overlayView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            overlayPermissionGranted = Settings.canDrawOverlays(this);
        } else {
            overlayPermissionGranted = true;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        getDisplayDimensions();
        neoThreadpool = new NeoThreadpool();
        uiManager = new UiManager(this, neoThreadpool, primaryDisplayMetrics);

        handler = new Handler();
        intentReceiver = new NeoCustomIntentReceiver();
        IntentFilter intentFilter = new IntentFilter(NEO_INTENT);
        registerReceiver(intentReceiver, intentFilter);
        NeoService.registerService(this);
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
        if (isStarted()) {
            // Makes onStartCommand idempotent.
            Log.i(Utils.TAG, "Service already started.");
            return START_STICKY;
        }

        if (intent != null && intent.getExtras() != null) {
            userUuid = intent.getExtras().getString(UUID_INTENT_PARAM);
            serverAddress = intent.getExtras().getString(SERVER_ADDRESS);
        }

        if (serverAddress == null) {
            String serverIp = BuildConfig.SERVER_IP;
            if (Utils.nullOrEmpty(serverIp)) {
                serverIp = DEFAULT_SERVER_IP;
            }
            serverAddress = "ws://" + serverIp + ":8080/";
            Log.i(Utils.TAG, "serverAddress: " + serverAddress);
        }
        Log.i(Utils.TAG, "userUuid: " + userUuid);
        expertChannel = new ExpertChannel(serverAddress, this, this, neoThreadpool, userUuid);
        expertChannel.connect();
        if (!showAccessibilitySettings()) {
            Log.i(Utils.TAG, "Unable to show accessibility settings.");
            if (uiActionsServiceCallback != null) {
                uiActionsServiceCallback.onSettingsError();
            }
        }
        return START_STICKY;
    }

    public boolean isStarted () {
        return expertChannel != null;
    }

    public void registerUiActionsCallback(UiActionsServiceCallback uiActionsServiceCallback) {
        this.uiActionsServiceCallback = uiActionsServiceCallback;
    }

    public void clearUiActionsCallback() {
        this.uiActionsServiceCallback = null;
    }

    @Override
    public void onDestroy() {
        if (isOverlayVisible) {
            hideOverlay();
        }
        unregisterReceiver(intentReceiver);
        if (expertChannel != null) {
            expertChannel.cleanup();
            expertChannel = null;
        }
        if (uiManager != null) {
            uiManager.cleanup();
            uiManager = null;
        }
        uiActionsServiceCallback = null;
        NeoService.unregisterNeoUiActionsService();
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
                refreshFullUi();
            }
        }, 5000);

        if (overlayPermissionGranted) {
            showOverlay();
        }
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

    @Override
    public void onClick(String viewId) {
        Log.i(Utils.TAG, "Click event for viewId: " + viewId);
        uiManager.processClick(viewId);
    }

    @Override
    public void onAction(JSONObject actionJson) {
        uiManager.takeAction(actionJson);
    }

    private class NeoCustomIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            NeoUiActionsService.this.toggleOverlay();
        }
    }

    private void showOverlay() {
        if (windowManager != null) {
            overlayView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
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

    public void toggleOverlay() {
        if (isOverlayVisible) {
            hideOverlay();
        } else {
            showOverlay();
        }
    }

    private void setupOverlay(View overlayView) {
        stopButton = (Button) overlayView.findViewById(R.id.overlay_button_stop);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopClickedByUser();
            }
        });

        overlayStatusTv = (TextView) overlayView.findViewById(R.id.overlay_status);
        overlayTitleTv = (TextView) overlayView.findViewById(R.id.overlay_title);
    }

    public void setTitle(String title) {
        if (overlayTitleTv != null) {
            overlayTitleTv.setText(title);
        }
    }

    public void setStatus(String status) {
        if (overlayStatusTv != null) {
            overlayStatusTv.setText(status);
        }
    }

    public void stopServiceByUser() {
        hideOverlay();
        if (uiActionsServiceCallback != null) {
            uiActionsServiceCallback.onStopByUser();
        }
        stopSelf();
    }

    public void stopServiceByExpert() {
        hideOverlay();
        stopSelf();
    }

    private void stopClickedByUser() {
        if (overlayStatusTv != null) {
            overlayStatusTv.setText(R.string.overlay_status_stopping);
        }

        if (stopButton != null) {
            stopButton.setVisibility(GONE);
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopServiceByUser();
            }
        }, USER_STOP_DELAY_MS);
    }

    public void refreshFullUi() {
        FlatViewHierarchy viewHierarchy = uiManager.updateViewHierarchy(getRootInActiveWindow());
        sendViewSnapshot(viewHierarchy);
    }
}
