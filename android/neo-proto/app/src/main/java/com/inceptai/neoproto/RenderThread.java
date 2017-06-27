package com.inceptai.neoproto;

import android.os.Looper;

/**
 * Created by arunesh on 6/26/17.
 */

public class RenderThread extends Thread {

    private OESTexture oesTexture;

    public RenderThread(OESTexture oesTexture) {
        this.oesTexture = oesTexture;
    }

    @Override
    public void run() {
        super.run();
        Looper.prepare();

    }

    private void initOesEgl() {
        oesTexture.initEgl();
        oesTexture.initOESTexture();
    }
}
