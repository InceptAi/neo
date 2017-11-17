package com.inceptai.neoservice;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.neopojos.ActionDetails;
import com.inceptai.neoservice.uiactions.UIActionResult;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by arunesh on 7/26/17.
 */

public class NeoService implements NeoUiActionsService.UiActionsServiceCallback {
    private static WeakReference<NeoUiActionsService> neoUiActionsServiceWeakReference;
    private static NeoService MY_INSTANCE = null;

    private String neoServerAddress;
    private String userUuid;
    private Context context;
    private Callback serviceCallback;

    public interface Callback {
        void onServiceStopped();
        void onServiceReady();
        void onUiStreamingStoppedByUser();
        void onUiStreamingStoppedByExpert();
        void onRequestAccessibilitySettings();
        void onUIActionsAvailable(List<ActionDetails> actionDetailsList);
        void onUIActionStarted(String query, String appName);
        void onUIActionFinished(String query, String appName, UIActionResult uiActionResult);
    }

    public NeoService(String neoServerAddress, String userUuid, Context context, Callback serviceCallback) {
        this.neoServerAddress = neoServerAddress;
        this.userUuid = userUuid;
        this.context = context;
        this.serviceCallback = serviceCallback;
        MY_INSTANCE = this;
        checkInWithNeoUiActionsService();
    }

    public void startService() {
        Intent intent = new Intent(context, NeoUiActionsService.class);
        intent.putExtra(NeoUiActionsService.UUID_INTENT_PARAM, userUuid);
        intent.putExtra(NeoUiActionsService.SERVER_ADDRESS, neoServerAddress);
        context.startService(intent);
    }

    public void stopService() {
        if (isNeoUiActionsServiceAvailable()) {
            NeoUiActionsService service = neoUiActionsServiceWeakReference.get();
            service.stopServiceByExpert();
        } else {
            Log.e(Utils.TAG, "Unable to stop NeoUiActionsService");
        }
    }

    public void updateStatus(String status) {
        if (isNeoUiActionsServiceAvailable()) {
            neoUiActionsServiceWeakReference.get().setStatus(status);
        }
    }

    public void setTitle(String title) {
        if (isNeoUiActionsServiceAvailable()) {
            neoUiActionsServiceWeakReference.get().setTitle(title);
        }
    }

    public static synchronized void registerService(NeoUiActionsService service) {
        neoUiActionsServiceWeakReference = new WeakReference<NeoUiActionsService>(service);
        if (MY_INSTANCE != null) {
            MY_INSTANCE.checkInWithNeoUiActionsService();
        }
    }

    public static synchronized void unregisterNeoUiActionsService() {
        if (neoUiActionsServiceWeakReference != null) {
            neoUiActionsServiceWeakReference.clear();
            neoUiActionsServiceWeakReference = null;
        }
    }

    public void toggleOverlay() {
        if (isNeoUiActionsServiceAvailable()) {
            neoUiActionsServiceWeakReference.get().toggleOverlay();
        }
    }

    public boolean isServiceRunning() {
        return isNeoUiActionsServiceAvailable() && neoUiActionsServiceWeakReference.get().isServiceRunning();
    }

    public SettableFuture fetchUIActions(String query, String appName, boolean forceAppRelaunch) {
        if(isNeoUiActionsServiceAvailable()) {
            return neoUiActionsServiceWeakReference.get().fetchUIActions(query, appName, forceAppRelaunch);
        }
        return null;
    }


    @Override
    public void onServiceReady() {
        if (serviceCallback != null) {
            serviceCallback.onServiceReady();
        }
    }

    @Override
    public void onUiStreamingStoppedByUser() {
       if (serviceCallback != null) {
           serviceCallback.onUiStreamingStoppedByUser();
       }
    }

    @Override
    public void onUiStreamingStoppedByExpert() {
        if (serviceCallback != null) {
            serviceCallback.onUiStreamingStoppedByExpert();
        }
    }

    @Override
    public void onUIActionsAvailable(List<ActionDetails> actionDetailsList) {
        if (serviceCallback != null) {
            serviceCallback.onUIActionsAvailable(actionDetailsList);
        }
    }

    @Override
    public void onUIActionStarted(String query, String appName) {
        if (serviceCallback != null) {
            serviceCallback.onUIActionStarted(query, appName);
        }
    }

    @Override
    public void onUIActionFinished(String query, String appName, UIActionResult uiActionResult) {
        if (serviceCallback != null) {
            serviceCallback.onUIActionFinished(query, appName, uiActionResult);
        }
    }

    public void cleanup() {
        MY_INSTANCE = null;
        if (isNeoUiActionsServiceAvailable()) {
            neoUiActionsServiceWeakReference.get().clearUiActionsCallback();
        }
    }

    private void checkInWithNeoUiActionsService() {
        if (isNeoUiActionsServiceAvailable()) {
            // service is available.
            neoUiActionsServiceWeakReference.get().registerUiActionsCallback(this);
        }
    }

    private static boolean isNeoUiActionsServiceAvailable() {
        return neoUiActionsServiceWeakReference != null && neoUiActionsServiceWeakReference.get() != null;
    }

    @Override
    public void onServiceDestroy() {
        if (serviceCallback != null) {
            serviceCallback.onServiceStopped();
        }
    }

    @Override
    public void onRequestAccessibiltySettings() {
        if (serviceCallback != null) {
            serviceCallback.onRequestAccessibilitySettings();
        }
    }

    public static boolean showAccessibilitySettings(Context context) {
        Intent settingsIntent = new Intent(
                Settings.ACTION_ACCESSIBILITY_SETTINGS);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        boolean isOk = true;
        try {
            context.startActivity(settingsIntent);
        } catch (ActivityNotFoundException e) {
            isOk = false;
        }
        return isOk;
    }


}
