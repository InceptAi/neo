package com.inceptai.neoproto;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

public class NeoMainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_MEDIA_PROJECTION_PERMISSION = 1001;


    private final Handler handler = new Handler();
    private View mContentView;
    private View mControlsView;


    private NeoDisplay2 neoDisplay2;
    private Button startButton;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_neo_main);

        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        startButton = (Button) findViewById(R.id.dummy_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                neoDisplay2 = new NeoDisplay2(NeoMainActivity.this);
                neoDisplay2.createImageReaderSurface();
                neoDisplay2.createVirtualDisplayUsingMediaProjection(mediaProjection);
                neoDisplay2.createPresentation();
                // neoDisplay2.showSettings();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            neoDisplay2.showSettings();
                            neoDisplay2.saveFrame();
                        } catch (IOException e) {
                            Log.e(Common.TAG, "IOException: " + e);
                        }
                    }
                }, 2000L);

            }
        });
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE_MEDIA_PROJECTION_PERMISSION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_MEDIA_PROJECTION_PERMISSION) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
