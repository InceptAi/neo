package com.inceptai.neoservice;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Created by arunesh on 7/26/17.
 */

public class NeoService implements NeoUiActionsService.UiActionsServiceCallback {

    public static final int REASON_STOPPED_BY_USER = 0;
    public static final int REASON_STOPPED_BY_EXPERT = 1;
    public static final int REASON_STOPPED_UNABLE_TO_SHOW_SETTINGS = 2;

    private WeakReference<NeoUiActionsService> neoUiActionsServiceWeakReference;
    private String neoServerAddress;
    private String userUuid;
    private Context context;
    private Callback serviceCallback;


    public interface Callback {
        void onStop(int reason);
        void onServiceReady();
    }

    public NeoService(String neoServerAddress, String userUuid, Context context, Callback serviceCallback) {
        this.neoServerAddress = neoServerAddress;
        this.userUuid = userUuid;
        this.context = context;
        this.serviceCallback = serviceCallback;
    }

    public void startService() {
        Intent intent = new Intent(context, NeoUiActionsService.class);
        intent.putExtra(NeoUiActionsService.UUID_INTENT_PARAM, userUuid);
        intent.putExtra(NeoUiActionsService.SERVER_ADDRESS, neoServerAddress);
        NeoUiActionsService.PARENT_INSTANCE = this;
        context.startService(intent);
    }

    public void stopService() {
        if ( neoUiActionsServiceWeakReference != null && neoUiActionsServiceWeakReference.get() != null) {
            NeoUiActionsService service = neoUiActionsServiceWeakReference.get();
            service.stopSelf();
        } else {
            Log.e(Utils.TAG, "Unable to stop NeoUiActionsService");
        }
    }

    public void updateStatus(String status) {

    }

    public void setTitle(String title) {

    }

    public synchronized void registerService(NeoUiActionsService service) {
        neoUiActionsServiceWeakReference = new WeakReference<NeoUiActionsService>(service);
        service.registerUiActionsCallback(this);
    }

    public synchronized NeoUiActionsService getNeoUiActionsService() {
        return neoUiActionsServiceWeakReference.get();
    }

    @Override
    public void onSettingsError() {
        if (serviceCallback != null) {
            serviceCallback.onStop(REASON_STOPPED_UNABLE_TO_SHOW_SETTINGS);
        }
    }

    @Override
    public void onServiceReady() {
        if (serviceCallback != null) {
            serviceCallback.onServiceReady();
        }
    }
}
