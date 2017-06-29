package com.inceptai.neoservice;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.support.annotation.IntDef;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;

/**
 * Created by arunesh on 6/29/17.
 */

public class NeoService extends AccessibilityService {
    private static final String TAG = Utils.TAG;

    private View overlayView;
    private LayoutParams neoOverlayLayout;
    private WindowManager windowManager;

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
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "onInterrupt");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (windowManager != null) {
            windowManager.addView(overlayView, neoOverlayLayout);
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
}
