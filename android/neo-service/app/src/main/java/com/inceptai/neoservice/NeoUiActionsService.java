package com.inceptai.neoservice;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.neopojos.ActionDetails;
import com.inceptai.neoservice.expert.ExpertChannel;
import com.inceptai.neoservice.flatten.FlatViewHierarchy;
import com.inceptai.neoservice.flatten.UiManager;
import com.inceptai.neoservice.uiactions.UIActionController;
import com.inceptai.neoservice.uiactions.model.ScreenInfo;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import static android.view.View.GONE;

/**
 * Created by arunesh on 6/29/17.
 */

public class NeoUiActionsService extends AccessibilityService implements
        ExpertChannel.ExpertChannelCallback,
        UIActionController.UIActionControllerCallback {
    public static final String UUID_INTENT_PARAM = "UUID";
    public static final String SERVER_ADDRESS = "SERVER_ADDRESS";

    private static final String TAG = Utils.TAG;
    private static final String DEFAULT_SERVER_IP = "dobby1743.duckdns.org";
    private static final String NEO_INTENT = "com.inceptai.neo.ACTION";
    private static final int USER_STOP_DELAY_MS = 2500;
    private static final String PREF_UI_STREAMING_ENABLED = "NeoStreaming";
    private static final String PREF_ACCESSIBILITY_ENABLED = "NeoAccessibilityEnabled";
    private static final boolean SUPRESS_SYSTEM_UI_UPDATES = true;
    private static final String SYSTEM_UI_PACKAGE_NAME = "com.android.systemui";


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

    private UIActionController uiActionController;

    private Button stopButton;
    private TextView overlayTitleTv;
    private TextView overlayStatusTv;
    private Handler handler;
    private boolean overlayPermissionGranted;
    private boolean serviceRunning = false;
    private ListenableFuture<ScreenInfo> screenTransitionFuture;
    private String lastPackageNameForTransition;

    public interface UiActionsServiceCallback {
        void onServiceReady();
        void onUiStreamingStoppedByUser();
        void onUiStreamingStoppedByExpert();
        void onServiceDestroy();
        void onRequestAccessibiltySettings();
        void onUIActionsAvailable(List<ActionDetails> actionDetailsList);
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

        //UI Actions
        uiActionController = new UIActionController(this, neoThreadpool.getExecutor());

        handler = new Handler();
        intentReceiver = new NeoCustomIntentReceiver();
        IntentFilter intentFilter = new IntentFilter(NEO_INTENT);
        registerReceiver(intentReceiver, intentFilter);
        NeoService.registerService(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i(TAG, "Got event:" + event);
        if (SUPRESS_SYSTEM_UI_UPDATES && event.getPackageName() != null && event.getPackageName().equals(SYSTEM_UI_PACKAGE_NAME)) {
            return;
        }
        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo != null) {
            Log.v("NeoUIActionsService", "source of event: " + AccessibilityEvent.eventTypeToString(event.getEventType()) + " nodeInfo: " + nodeInfo.toString());
        } else {
            Log.v("NeoUIActionsService", "source of event is null for eventType: "  + AccessibilityEvent.eventTypeToString(event.getEventType()));
        }
        refreshFullUi(event, nodeInfo);
    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "onInterrupt");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getExtras() != null) {
            // The fact that the intent has a user UUID means it was started from NeoService. We
            // should stream UI events to the server at this point even if its disable.
            userUuid = intent.getExtras().getString(UUID_INTENT_PARAM);
            serverAddress = intent.getExtras().getString(SERVER_ADDRESS);
            saveUiStreaming(true /* enabled */);
        }

        if (!isAccessibilityPermissionGranted()) {
            if (uiActionsServiceCallback != null) {
                uiActionsServiceCallback.onRequestAccessibiltySettings();
            } else {
                Log.e(Utils.TAG, "No actions callback instance for sending accessibility settings grant callback.");
            }
        } else if (isUiStreamingEnabled()) {
            startUiStreaming();
            if (uiActionsServiceCallback != null) {
                uiActionsServiceCallback.onServiceReady();
            }
        }

        return START_STICKY;
    }
    
    public void registerUiActionsCallback(UiActionsServiceCallback uiActionsServiceCallback) {
        this.uiActionsServiceCallback = uiActionsServiceCallback;
    }

    public void clearUiActionsCallback() {
        this.uiActionsServiceCallback = null;
    }

    @Override
    public void onDestroy() {
        serviceRunning = false;
        if (isOverlayVisible) {
            hideOverlay();
        }
        unregisterReceiver(intentReceiver);
        expertChannelCleanup();
        uiManagerCleanup();
        if (uiActionsServiceCallback != null) {
            uiActionsServiceCallback.onServiceDestroy();
        }
        uiActionsServiceCallback = null;
        NeoService.unregisterNeoUiActionsService();
        super.onDestroy();
    }

    public boolean isServiceRunning() {
        return serviceRunning;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (uiActionsServiceCallback != null) {
            uiActionsServiceCallback.onServiceReady();
        }
        serviceRunning = true;

        if (isUiStreamingEnabled()) {
            startUiStreaming();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshFullUi(null, null);
                }
            }, 4000);
        }
    }

    private void getDisplayDimensions() {
        primaryDisplayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(primaryDisplayMetrics);
    }

    private void sendViewSnapshot(FlatViewHierarchy flatViewHierarchy) {
        if (expertChannel != null) {
            expertChannel.sendViewHierarchy(flatViewHierarchy.toRenderingJson());
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


    //UIActionController callback
    @Override
    public void onUIActionDetails(List<ActionDetails> actionDetailsList) {
        Log.d(TAG, "In NeoUIActionsService, onUIActionDetails -- got actions");
        if (uiActionsServiceCallback != null) {
            uiActionsServiceCallback.onUIActionsAvailable(actionDetailsList);
        }
        //TODO -- remove the hack for only settings action
        if (actionDetailsList != null && !actionDetailsList.isEmpty()) {
            uiManager.takeUIAction(actionDetailsList.get(0));
        }
    }

    @Override
    public void onUIActionError(String error) {
        Log.d("UIAction", "Error in fetching actions : " + error);
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

    // Resets the overlay views back to an initial state (potentially after being stopped).
    private void resetOverlayViews() {
        if (overlayStatusTv != null) {
            overlayStatusTv.setText(R.string.overlay_status_default);
        }

        if (stopButton != null) {
            stopButton.setVisibility(View.VISIBLE);
        }
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
        stopUIStreaming();
        if (uiActionsServiceCallback != null) {
            uiActionsServiceCallback.onUiStreamingStoppedByUser();
        }
    }

    public void stopServiceByExpert() {
        stopUIStreaming();
        if (uiActionsServiceCallback != null) {
            uiActionsServiceCallback.onUiStreamingStoppedByExpert();
        }
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

    public void refreshFullUi(@Nullable AccessibilityEvent accessibilityEvent, @Nullable AccessibilityNodeInfo eventSourceInfo) {
        if (uiManager != null) {
            if (accessibilityEvent != null) {
                Log.d(TAG, "In RefreshFullUI, processing event: "  + accessibilityEvent);
            }
            FlatViewHierarchy viewHierarchy = uiManager.updateViewHierarchy(getRootInActiveWindow(), accessibilityEvent, eventSourceInfo);
            if (viewHierarchy != null) {
                sendViewSnapshot(viewHierarchy);
            }
            //TODO -- remove
//            if (Build.VERSION.SDK_INT > LOLLIPOP) {
//                AccessibilityNodeInfo rootNode = getRootInActiveWindow();
//                AccessibilityWindowInfo accessibilityWindowInfo = rootNode.getWindow();
//                if (accessibilityWindowInfo != null) {
//                    Log.d("updateViewHierarchy", "window for root " +  accessibilityWindowInfo);
//                }
//                rootNode.recycle();
//            }

        }
    }


    //Public functions for uiActions callback from the app
    public boolean fetchUIActions(final String query, final String appName) {

        if (appName.equalsIgnoreCase(Utils.SETTINGS_APP_NAME)) {
            uiActionController.fetchUIActionsForSettings(query);
            return true;
        }

        final String packageName = Utils.findPackageNameForApp(getApplicationContext(), appName);
        if (Utils.nullOrEmpty(packageName)) {
            //Didn't find the application
            Log.e(TAG, "In fetchUIActions, can't find package for app: " + appName);
            return false;
        }

        //Launch the application with packageName
        //TODO -- launch from recents or home screen
        if (!Utils.nullOrEmpty(lastPackageNameForTransition) &&
                !packageName.equalsIgnoreCase(lastPackageNameForTransition)) {
            return false; //already waiting for transition to another screen, can't process this request
        }

        if (screenTransitionFuture != null && !screenTransitionFuture.isDone()) {
            return true; //already in progress for the right screen transition, return true since already enqueued.
        }

        if (uiManager != null) {
            lastPackageNameForTransition = packageName;
            screenTransitionFuture = uiManager.launchAppAndReturnScreenTitle(getApplicationContext(), packageName);
            screenTransitionFuture.addListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        lastPackageNameForTransition = Utils.EMPTY_STRING;
                        ScreenInfo latestScreenInfo = screenTransitionFuture.get();
                        if (latestScreenInfo != null && !latestScreenInfo.isEmpty()) {
                            uiActionController.fetchUIActionsForApps(packageName.toLowerCase(), latestScreenInfo.getTitle(), query);
                        }
                    } catch (InterruptedException | ExecutionException | CancellationException e) {
                        e.printStackTrace(System.out);
                        Log.e(TAG, "Exception while waiting for screen future" + e.toString());
                    }
                }
            }, neoThreadpool.getExecutor());
            return true;
        }

        return false; //can't transition since uimanager is null
    }


    private String getServerAddress() {
        if (serverAddress == null) {
            String serverIp = BuildConfig.SERVER_IP;
            if (Utils.nullOrEmpty(serverIp)) {
                serverIp = DEFAULT_SERVER_IP;
            }
            serverAddress = "ws://" + serverIp + ":8080/";
            Log.i(Utils.TAG, "serverAddress: " + serverAddress);
        }
        return serverAddress;
    }

    private void startUiStreaming() {
        // Save the fact that UI streaming is enabled.
        // Enable UI streaming.
        saveUiStreaming(true /* enabled */);
        Log.i(Utils.TAG, "userUuid: " + userUuid);
        if (expertChannel != null && uiManager != null) {
            // We are already streaming, so no need to start again.
            Log.i(Utils.TAG, "Dropping startStreaming request, since already streaming.");
            return;
        }
        Log.i(Utils.TAG, "Starting streaming.");
        expertChannel = new ExpertChannel(getServerAddress(), this, this, neoThreadpool, userUuid);
        expertChannel.connect();
        uiManager = new UiManager(this, neoThreadpool, primaryDisplayMetrics);
        if (overlayPermissionGranted) {
            resetOverlayViews();
            showOverlay();
        }
    }

    private void stopUIStreaming() {
        // Save the fact that UI streaming is disabled.
        // Disable UI streaming.
        hideOverlay();
        saveUiStreaming(false /* disabled */);
        expertChannelCleanup();
        uiManagerCleanup();
    }

    private void uiManagerCleanup() {
        if (uiManager != null) {
            uiManager.cleanup();
            uiManager = null;
        }
    }

    private void expertChannelCleanup() {
        if (expertChannel != null) {
            expertChannel.cleanup();
            expertChannel = null;
        }
    }

    private void saveUiStreaming(boolean state) {
        Utils.saveSharedSetting(this, PREF_UI_STREAMING_ENABLED, state);
    }

    private boolean isUiStreamingEnabled() {
        //By default UI streaming is disabled
        return Utils.readSharedSetting(this, PREF_UI_STREAMING_ENABLED, false);
    }

    private boolean isAccessibilityPermissionGranted() {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    this.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(Utils.TAG, "Accessibility settings not found !");
        }

        if (accessibilityEnabled != 1) return false;

        String pkgClassName = this.getPackageName() + "/" + NeoUiActionsService.class.getCanonicalName();
        String enabledServices = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (!Utils.nullOrEmpty(enabledServices)) {
            for (String value : enabledServices.split(":")) {
                if (pkgClassName.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

}
