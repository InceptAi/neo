package com.inceptai.neoproto;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import com.inceptai.neoservice.NeoPresentation;

import java.io.File;
import java.io.IOError;
import java.io.IOException;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY;
import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
import static com.inceptai.neoproto.Common.TAG;

/**
 * Created by arunesh on 6/24/17.
 */

public class NeoDisplay {
    public static final String NEO_DISPLAY = "NeoDisplay";
    private static final File FILES_DIR = Environment.getExternalStorageDirectory();
    private Context activityMainContext;
    private DisplayMetrics primaryDisplayMetrics;
    private SurfaceTexture surfaceTexture;
    private OESTexture oesTexture;
    private Surface surface;
    private VirtualDisplay virtualDisplay;
    private Context virtualDisplayContext;
    private WindowManager virtualDisplayWindowManager;
    private RenderThread renderThread;
    private NeoPresentation presentation;
    private int fileCount = 1;


    public NeoDisplay(Context activityMainContext) {
        this.activityMainContext = activityMainContext;
        primaryDisplayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) activityMainContext.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(primaryDisplayMetrics);
        Log.i(TAG, "Display Metrics: " + primaryDisplayMetrics.toString());
        oesTexture = new OESTexture(primaryDisplayMetrics.widthPixels, primaryDisplayMetrics.heightPixels);
        renderThread = new RenderThread(oesTexture);
        renderThread.start();
    }

    public void postCreate() {
        renderThread.waitUntilReady();
        renderThread.getHandler().post(new Runnable() {
            @Override
            public void run() {
                create();
            }
        });
    }

    public void create() {
        surfaceTexture = new SurfaceTexture(oesTexture.getTextureId());
        surfaceTexture.setOnFrameAvailableListener(renderThread);
        surface = new Surface(surfaceTexture);
        DisplayManager displayManager = (DisplayManager) activityMainContext.getSystemService(Context.DISPLAY_SERVICE);
        int virtualDisplayFlags = VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | VIRTUAL_DISPLAY_FLAG_PUBLIC;
        virtualDisplay = displayManager.createVirtualDisplay(NEO_DISPLAY, primaryDisplayMetrics.widthPixels,
                primaryDisplayMetrics.heightPixels, primaryDisplayMetrics.densityDpi, surface, virtualDisplayFlags);
        virtualDisplayContext = activityMainContext.createDisplayContext(virtualDisplay.getDisplay());
        virtualDisplayWindowManager = (WindowManager) virtualDisplayContext.getSystemService(Context.WINDOW_SERVICE);
        presentation = new NeoPresentation(activityMainContext, virtualDisplay.getDisplay());
        presentation.show();
    }


    public void showSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        presentation.getContext().startActivity(intent, null);
    }


    public void saveFrame() {
        String filename = String.format("frame-%02d.png", fileCount);
        File outputFile = new File(activityMainContext.getFilesDir(), filename);
        fileCount ++;
        renderThread.getHandler().saveFrame(outputFile.toString());
        Log.i(TAG, "Written to: " + outputFile.getName());
    }

    public void release() {
        oesTexture.release();
    }
}

