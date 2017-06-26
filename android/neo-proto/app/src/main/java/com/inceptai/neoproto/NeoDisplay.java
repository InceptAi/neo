package com.inceptai.neoproto;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY;
import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;

/**
 * Created by arunesh on 6/24/17.
 */

public class NeoDisplay {
    public static final String NEO_DISPLAY = "NeoDisplay";
    private Context context;
    private DisplayMetrics primaryDisplayMetrics;
    private SurfaceTexture surfaceTexture;
    private OESTexture cameraTexture = new OESTexture();
    private Surface surface;
    private VirtualDisplay virtualDisplay;
    private Context virtualDisplayContext;
    private WindowManager virtualDisplayWindowManager;

    public NeoDisplay(Context context) {
        this.context = context;
        primaryDisplayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(primaryDisplayMetrics);
        Log.i(Common.TAG, "Display Metrics: " + primaryDisplayMetrics.toString());
    }

    public void create() {
        cameraTexture.init();
        surfaceTexture = new SurfaceTexture(cameraTexture.getTextureId());
        surface = new Surface(surfaceTexture);
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        int virtualDisplayFlags = VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY  |  VIRTUAL_DISPLAY_FLAG_PUBLIC;
        virtualDisplay = displayManager.createVirtualDisplay(NEO_DISPLAY, primaryDisplayMetrics.widthPixels,
                primaryDisplayMetrics.heightPixels, primaryDisplayMetrics.densityDpi, surface, virtualDisplayFlags);
        virtualDisplayContext = context.createDisplayContext(virtualDisplay.getDisplay());
        virtualDisplayWindowManager = (WindowManager) virtualDisplayContext.getSystemService(Context.WINDOW_SERVICE);
    }
}

