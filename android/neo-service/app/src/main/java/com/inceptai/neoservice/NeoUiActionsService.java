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
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.neopojos.ActionDetails;
import com.inceptai.neoservice.expert.ExpertChannel;
import com.inceptai.neoservice.flatten.FlatViewHierarchy;
import com.inceptai.neoservice.flatten.UiManager;
import com.inceptai.neoservice.uiactions.UIActionController;
import com.inceptai.neoservice.uiactions.UIActionResult;
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
        ExpertChannel.ExpertChannelCallback {
    public static final String UUID_INTENT_PARAM = "UUID";
    public static final String SERVER_ADDRESS = "SERVER_ADDRESS";
    public static final String OVERLAY_ENABLED = "OVERLAY_ENABLED";


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
    private SettableFuture<UIActionResult> fetchAndPerformTopUIActionResultFuture;
    private SettableFuture<UIActionResult> takeUIActionResultFuture;
    private SettableFuture<UIActionResult> fetchUIActionsForSettingsFuture;


    private UiManager uiManager;

    private NeoCustomIntentReceiver intentReceiver;
    private boolean isOverlayVisible;
    private boolean isOverlayEnabled;
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
        void onUIActionStarted(String query, String appName);
        void onUIActionFinished(String query, String appName, UIActionResult uiActionResult);
        void onUIActionsAvailable(List<ActionDetails> actionDetailsList);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler();
        isOverlayVisible = false;
        isOverlayEnabled = false;
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
        uiActionController = new UIActionController(neoThreadpool.getExecutor());

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
            isOverlayEnabled = intent.getExtras().getBoolean(OVERLAY_ENABLED);
            saveUiStreaming(true /* enabled */);
        }

        if (!isAccessibilityPermissionGranted(this)) {
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

    public void forceHideOverlay() {
        isOverlayEnabled = false;
        hideOverlay();
    }

    public void forceShowOverlay() {
        isOverlayEnabled = true;
        hideOverlay();
        resetOverlayViews();
        showOverlay();
    }

    //UIActionController callback
    public void waitForResultsFromServer(final SettableFuture<UIActionResult> serverResultsFuture) {
        if (serverResultsFuture == null) {
            return;
        }
        serverResultsFuture.addListener(new Runnable() {
            @Override
            public void run() {
                UIActionResult serverResult;
                try {
                    serverResult = serverResultsFuture.get();
                    if (UIActionResult.isSuccessful(serverResult)) {
                        Log.d(TAG, "In NeoUIActionsService, onUIActionDetails -- got actions");
                        List<ActionDetails> actionDetailsList = (List<ActionDetails>)serverResult.getPayload();
                        if (uiActionsServiceCallback != null) {
                            uiActionsServiceCallback.onUIActionsAvailable(actionDetailsList);
                        }
                        if (actionDetailsList != null && !actionDetailsList.isEmpty()) {
                            forceShowOverlay();
                            fetchAndPerformTopUIActionResultFuture.set(uiManager.takeUIAction(actionDetailsList.get(0), getApplicationContext(), serverResult.getQuery(), serverResult.getPackageName()));
                            forceHideOverlay();
                        } else {
                            //No actions available -- now launch the app and try again.
                            fetchAndPerformTopUIActionResultFuture.set(new UIActionResult(UIActionResult.UIActionResultCodes.NO_ACTIONS_AVAILABLE, serverResult.getQuery(), serverResult.getPackageName()));
                        }
                    } else {
                        fetchAndPerformTopUIActionResultFuture.set(serverResult);
                    }
                } catch (InterruptedException|ExecutionException e) {
                    fetchAndPerformTopUIActionResultFuture.set(new UIActionResult(UIActionResult.UIActionResultCodes.SERVER_ERROR));
                }
            }
        }, neoThreadpool.getExecutor());
    }


    private void showOverlay() {
        if (isOverlayVisible) {
            return;
        }
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
        if (!isOverlayVisible) {
            return;
        }
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
    public SettableFuture performUIAction(final ActionDetails actionDetails, String packageName) {
        if (actionDetails == null ||
                actionDetails.getActionIdentifier() == null ||
                actionDetails.getActionIdentifier().getScreenIdentifier() == null) {
            SettableFuture<UIActionResult> settableFuture = SettableFuture.create();
            settableFuture.set(new UIActionResult(UIActionResult.UIActionResultCodes.INVALID_ACTION_DETAILS));
            return settableFuture;
        }

        final String packageNameForTransition = Utils.nullOrEmpty(packageName) ? actionDetails.getActionIdentifier().getScreenIdentifier().getPackageName() : packageName;
        final String queryDescription = actionDetails.getActionIdentifier().getActionDescription();

        UIActionResult uiActionResult = new UIActionResult(queryDescription, packageNameForTransition);

        if (!isAccessibilityPermissionGranted(this)) {
            SettableFuture<UIActionResult> settableFutureToReturn = SettableFuture.create();
            uiActionResult.setStatus(UIActionResult.UIActionResultCodes.ACCESSIBILITY_PERMISSION_DENIED);
            settableFutureToReturn.set(uiActionResult);
            return settableFutureToReturn;
        }

        if (takeUIActionResultFuture != null && !takeUIActionResultFuture.isDone()) {
            if (packageNameForTransition.equalsIgnoreCase(lastPackageNameForTransition)) {
                return takeUIActionResultFuture;
            } else {
                //Cancel the last one
                takeUIActionResultFuture.cancel(true);
            }
        }

        takeUIActionResultFuture = SettableFuture.create();

        if (Utils.nullOrEmpty(packageNameForTransition)) {
            //Didn't find the application
            Log.e(TAG, "In fetchAndPerformTopUIAction, can't find package for app: " + packageNameForTransition);
            uiActionResult.setStatus(UIActionResult.UIActionResultCodes.INVALID_APP_NAME);
            takeUIActionResultFuture.set(uiActionResult);
            return takeUIActionResultFuture;
        }


        if (uiManager == null) {
            uiActionResult.setStatus(UIActionResult.UIActionResultCodes.UI_MANAGER_UNINITIALIZED);
            takeUIActionResultFuture.set(uiActionResult);
            return takeUIActionResultFuture;
        }

        //Start overlay here
        forceShowOverlay();
        neoThreadpool.getExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                forceHideOverlay();
                takeUIActionResultFuture.set(uiManager.takeUIAction(actionDetails, getApplicationContext(), queryDescription, packageNameForTransition));
            }
        });

        return takeUIActionResultFuture;
    }

    public SettableFuture performUIAction(final ActionDetails actionDetails) {
        return performUIAction(actionDetails, Utils.EMPTY_STRING);
    }

    public SettableFuture<UIActionResult> fetchUIActionsForSettings(final String query) {
        final String packageName = Utils.SETTINGS_PACKAGE_NAME;

        UIActionResult uiActionResult = new UIActionResult(query, packageName);

        if (fetchUIActionsForSettingsFuture != null && !fetchUIActionsForSettingsFuture.isDone()) {
            return fetchUIActionsForSettingsFuture;
        }

        final String appVersion = Utils.findAppVersionForPackageName(getApplicationContext(), packageName);
        final String versionCode = Utils.findVersionCodeForPackageName(getApplicationContext(), packageName);

        fetchUIActionsForSettingsFuture = uiActionController.fetchUIActionsForSettings(appVersion, versionCode, query);
        return fetchUIActionsForSettingsFuture;
    }


    public SettableFuture fetchAndPerformTopUIAction(final String query, final String appName, final boolean forceAppRelaunch) {
        final String packageName = Utils.findPackageNameForApp(getApplicationContext(), appName);
        UIActionResult uiActionResult = new UIActionResult(query, packageName);

        if (!isAccessibilityPermissionGranted(this)) {
            SettableFuture<UIActionResult> settableFutureToReturn = SettableFuture.create();
            uiActionResult.setStatus(UIActionResult.UIActionResultCodes.ACCESSIBILITY_PERMISSION_DENIED);
            settableFutureToReturn.set(uiActionResult);
            return settableFutureToReturn;
        }

        if (fetchAndPerformTopUIActionResultFuture != null && !fetchAndPerformTopUIActionResultFuture.isDone()) {
            if (packageName.equalsIgnoreCase(lastPackageNameForTransition)) {
                return fetchAndPerformTopUIActionResultFuture;
            } else {
                //Cancel the last one
                fetchAndPerformTopUIActionResultFuture.cancel(true);
            }
        }

        fetchAndPerformTopUIActionResultFuture = SettableFuture.create();

        if (Utils.nullOrEmpty(packageName)) {
            //Didn't find the application
            Log.e(TAG, "In fetchAndPerformTopUIAction, can't find package for app: " + appName);
            uiActionResult.setStatus(UIActionResult.UIActionResultCodes.INVALID_APP_NAME);
            fetchAndPerformTopUIActionResultFuture.set(uiActionResult);
            return fetchAndPerformTopUIActionResultFuture;
        }


        final String appVersion = Utils.findAppVersionForPackageName(getApplicationContext(), packageName);
        final String versionCode = Utils.findVersionCodeForPackageName(getApplicationContext(), packageName);


        if (uiManager == null) {
            uiActionResult.setStatus(UIActionResult.UIActionResultCodes.UI_MANAGER_UNINITIALIZED);
            fetchAndPerformTopUIActionResultFuture.set(uiActionResult);
            return fetchAndPerformTopUIActionResultFuture;
        }


        lastPackageNameForTransition = packageName;
        screenTransitionFuture = uiManager.launchAppAndReturnScreenTitle(getApplicationContext(), packageName, forceAppRelaunch);
        screenTransitionFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    lastPackageNameForTransition = Utils.EMPTY_STRING;
                    ScreenInfo latestScreenInfo = screenTransitionFuture.get();
                    if (latestScreenInfo != null && !latestScreenInfo.isEmpty()) {
                        SettableFuture<UIActionResult> fetchActionsFuture = uiActionController.fetchUIActions(
                                packageName.toLowerCase(),
                                latestScreenInfo.getTitle(),
                                latestScreenInfo.getScreenType(),
                                appVersion, versionCode, query);
                        waitForResultsFromServer(fetchActionsFuture);
                    }
                } catch (InterruptedException | ExecutionException | CancellationException e) {
                    e.printStackTrace(System.out);
                    Log.e(TAG, "Exception while waiting for screen future" + e.toString());
                    fetchAndPerformTopUIActionResultFuture.set(new UIActionResult(UIActionResult.UIActionResultCodes.EXCEPTION_WHILE_WAITING_FOR_SCREEN_TRANSITION, query, packageName));
                }
            }
        }, neoThreadpool.getExecutor());
        return fetchAndPerformTopUIActionResultFuture;
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
        if (overlayPermissionGranted && isOverlayEnabled) {
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

    public static boolean isAccessibilityPermissionGranted(Context context) {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    context.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(Utils.TAG, "Accessibility settings not found !");
        }

        if (accessibilityEnabled != 1) return false;

        String pkgClassName = context.getPackageName() + "/" + NeoUiActionsService.class.getCanonicalName();
        String enabledServices = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
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
