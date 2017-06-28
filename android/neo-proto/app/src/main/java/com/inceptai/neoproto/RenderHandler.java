package com.inceptai.neoproto;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;

import static com.inceptai.neoproto.Common.TAG;

/**
 * Created by arunesh on 6/26/17.
 */

public class RenderHandler extends Handler {
    private static final int MSG_SHUTDOWN_RENDER = 1001;
    private static final int MSG_SURFACE_TEXTURE_FRAME_AVAILABLE = 1002;
    private static final int MSG_SAVE_FRAME = 1003;
    private RenderThread renderThread;

    public RenderHandler(RenderThread renderThread) {
        super();
        this.renderThread = renderThread;
    }

    public void sendShutdown() {
        sendEmptyMessage(MSG_SHUTDOWN_RENDER);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_SHUTDOWN_RENDER:
                renderThread.shutdown();
                break;
            case MSG_SURFACE_TEXTURE_FRAME_AVAILABLE:
                SurfaceTexture surfaceTexture = (SurfaceTexture) msg.obj;
                surfaceTexture.updateTexImage();
                break;
            case MSG_SAVE_FRAME:
                try {
                    renderThread.saveFrame((String) msg.obj);
                } catch (IOException e) {
                    Log.e(TAG, "IOException: " + e);
                }
                break;
        }
    }

    public void onSurfaceTextureFrameAvailable(SurfaceTexture surfaceTexture) {
        Message.obtain(this, MSG_SURFACE_TEXTURE_FRAME_AVAILABLE, surfaceTexture).sendToTarget();
    }

    public void saveFrame(String filename) {
        Message.obtain(this, MSG_SAVE_FRAME, filename).sendToTarget();
    }
}
