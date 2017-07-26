package com.inceptai.neoservice;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Created by arunesh on 7/26/17.
 */

public class NeoService {

    private static WeakReference<NeoUiActionsService> INSTANCE;
    private String neoServerAddress;
    private String userUuid;
    private Context context;
    private Callback serviceCallback;


    public interface Callback {
        void onStop();
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
        context.startService(intent);
    }

    public void stopService() {
        if ( INSTANCE != null && INSTANCE.get() != null) {
            NeoUiActionsService service = INSTANCE.get();
            service.stopSelf();
        } else {
            Log.e(Utils.TAG, "Unable to stop NeoUiActionsService");
        }
    }

    public void updateStatus(String status) {

    }

    public void setTitle(String title) {

    }

    public synchronized static void registerService(NeoUiActionsService service) {
        INSTANCE = new WeakReference<NeoUiActionsService>(service);
    }

    public synchronized static NeoUiActionsService getNeoUiActionsService() {
        return INSTANCE.get();
    }

}
