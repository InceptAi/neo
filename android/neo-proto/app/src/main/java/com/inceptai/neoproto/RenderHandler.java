package com.inceptai.neoproto;

import android.os.Handler;
import android.os.Message;

/**
 * Created by arunesh on 6/26/17.
 */

public class RenderHandler extends Handler {

    private RenderThread renderThread;

    public RenderHandler(RenderThread renderThread) {
        super();
        this.renderThread = renderThread;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
    }
}
