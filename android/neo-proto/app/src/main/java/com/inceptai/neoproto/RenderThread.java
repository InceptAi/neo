package com.inceptai.neoproto;

import android.graphics.SurfaceTexture;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;

import static com.inceptai.neoproto.Common.TAG;

/**
 * Created by arunesh on 6/26/17.
 */

public class RenderThread extends Thread implements SurfaceTexture.OnFrameAvailableListener {

    private OESTexture oesTexture;
    private RenderHandler renderHandler;

    // Used to wait for the thread to start.
    private Object mStartLock = new Object();
    private boolean mReady = false;

    public RenderThread(OESTexture oesTexture) {
        this.oesTexture = oesTexture;
    }


    @Override
    public void run() {
        super.run();
        Looper.prepare();
        renderHandler = new RenderHandler(this);
        initOesEgl();

        synchronized (mStartLock) {
            mReady = true;
            mStartLock.notify();    // signal waitUntilReady()
        }

        Looper.loop();

        Log.d(TAG, "RenderThread: looper quit");
        oesTexture.release();
        //releaseGl();
        //mEglCore.release();
    }

    public RenderHandler getHandler() {
        return renderHandler;
    }

    /**
     * Waits until the render thread is ready to receive messages.
     * <p>
     * Call from the UI thread.
     */
    public void waitUntilReady() {
        synchronized (mStartLock) {
            while (!mReady) {
                try {
                    mStartLock.wait();
                } catch (InterruptedException ie) { /* not expected */ }
            }
        }
    }

    private void initOesEgl() {
        oesTexture.initEgl();
        oesTexture.makeCurrent();
        oesTexture.initOESTexture();
    }

    public void shutdown() {
        Log.d(TAG, "RenderThread: shutdown");
        Looper.myLooper().quit();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        renderHandler.onSurfaceTextureFrameAvailable(surfaceTexture);
    }

    public void saveFrame(String filename) throws IOException {
        oesTexture.saveFrame(filename);
    }
}
