package com.inceptai.neoservice;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
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

/**
 * Created by arunesh on 6/29/17.
 */

public class NeoService extends AccessibilityService {
    private static final String TAG = Utils.TAG;

    private View overlayView;
    private LayoutParams neoOverlayLayout;
    private WindowManager windowManager;
    private DisplayMetrics primaryDisplayMetrics;

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
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i(TAG, "Got event:" + event);
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

    private void getDisplayDimensions() {
        WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = window.getDefaultDisplay();
        windowManager.getDefaultDisplay().getMetrics(primaryDisplayMetrics);
    }
}
