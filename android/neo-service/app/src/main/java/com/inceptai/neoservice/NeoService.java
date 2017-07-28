package com.inceptai.neoservice;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.common.base.Preconditions;

import java.lang.ref.WeakReference;

/**
 * Created by arunesh on 7/26/17.
 */

public class NeoService implements NeoUiActionsService.UiActionsServiceCallback {
    public static final int REASON_STOPPED_BY_USER = 0;
    public static final int REASON_STOPPED_BY_EXPERT = 1;
    public static final int REASON_STOPPED_UNABLE_TO_SHOW_SETTINGS = 2;

    private static WeakReference<NeoUiActionsService> neoUiActionsServiceWeakReference;
    private static NeoService MY_INSTANCE = null;

    private String neoServerAddress;
    private String userUuid;
    private Context context;
    private Callback serviceCallback;
    private boolean isServiceRunning = false;


    public interface Callback {
        void onServiceStopped(int reason);
        void onServiceReady();
        void onStopClickedByUser();
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
        isServiceRunning = false;
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
        return isServiceRunning;
    }

    @Override
    public void onSettingsError() {
        if (serviceCallback != null) {
            serviceCallback.onServiceStopped(REASON_STOPPED_UNABLE_TO_SHOW_SETTINGS);
            Preconditions.checkArgument(!isServiceRunning, "Service should not be running");
        }
    }

    @Override
    public void onServiceReady() {
        if (serviceCallback != null) {
            serviceCallback.onServiceReady();
        }
        isServiceRunning = true;
    }

    @Override
    public void onStopClickedByUser() {
       if (serviceCallback != null) {
           serviceCallback.onStopClickedByUser();
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
}
