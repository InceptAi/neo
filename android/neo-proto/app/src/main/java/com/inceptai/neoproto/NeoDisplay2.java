package com.inceptai.neoproto;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY;
import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
import static com.inceptai.neoproto.Common.TAG;

/**
 * Created by arunesh on 6/27/17.
 */

public class NeoDisplay2 {
    public static final String NEO_DISPLAY = "NeoDisplayTWO";
    private static final File FILES_DIR = Environment.getExternalStorageDirectory();
    private Context activityMainContext;
    private DisplayMetrics primaryDisplayMetrics;
    private Surface surface;
    private VirtualDisplay virtualDisplay;
    private Context virtualDisplayContext;
    private WindowManager virtualDisplayWindowManager;
    private NeoPresentation presentation;
    private int fileCount = 1;
    private ImageReader imageReader;
    private int mWidth, mHeight;


    public NeoDisplay2(Context activityMainContext) {
        this.activityMainContext = activityMainContext;
        primaryDisplayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) activityMainContext.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(primaryDisplayMetrics);
        Log.i(TAG, "Display Metrics: " + primaryDisplayMetrics.toString());
        mHeight = primaryDisplayMetrics.heightPixels;
        mWidth = primaryDisplayMetrics.widthPixels;
        imageReader = ImageReader.newInstance(primaryDisplayMetrics.widthPixels, primaryDisplayMetrics.heightPixels, PixelFormat.RGBA_8888, 1);
    }

    public void create() {
        surface = imageReader.getSurface();
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


    public void saveFrame() throws IOException {
        String filename = String.format("frame-%02d.png", fileCount);
        File outputFile = new File(activityMainContext.getFilesDir(), filename);
        fileCount ++;
        Image image = imageReader.acquireLatestImage();
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * mWidth;

        Bitmap bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight,
                Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        // Trim the screenshot to the correct size.
        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, mWidth, mHeight);

        FileOutputStream out = new FileOutputStream(outputFile);

        croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.flush();

        Log.i(TAG, "Written to: " + outputFile.getName());
    }

    public void release() {
    }
}
