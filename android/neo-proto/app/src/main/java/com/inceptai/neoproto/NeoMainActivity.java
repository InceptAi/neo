package com.inceptai.neoproto;

import android.annotation.TargetApi;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.inceptai.neoservice.NeoDisplay2;
import com.inceptai.neoservice.NeoUiActionsService;
import com.inceptai.neoservice.Utils;

import java.io.IOException;
import java.util.UUID;

public class NeoMainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_MEDIA_PROJECTION_PERMISSION = 1001;
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 2001;

    private final Handler handler = new Handler();
    private View mContentView;
    private View mControlsView;

    private NeoDisplay2 neoDisplay2;
    private Button startButton;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private boolean askedForOverlayPermission = false;
    private String userUuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userUuid = UUID.randomUUID().toString();
        setContentView(R.layout.activity_neo_main);
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.

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
       // startActivityForResult(intent, REQUEST_CODE_MEDIA_PROJECTION_PERMISSION);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
               startNeoService();
            }
        }, 2000);
    }

    private void setFullScreen() {
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.i(Utils.TAG, "onActivityResult:" + String.valueOf(requestCode));

        if (requestCode == REQUEST_CODE_MEDIA_PROJECTION_PERMISSION) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            startNeoService();
            return;
        } else if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION && isAndroidMOrLater()) {
            askedForOverlayPermission = false;
            if (Settings.canDrawOverlays(this)) {
                continueStartNeoService();
            } else {
                Toast.makeText(this, "ACTION_MANAGE_OVERLAY_PERMISSION Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void askForOverlayPermission() {
        askedForOverlayPermission = true;
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
    }

    private boolean isAndroidMOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    private void startNeoService() {
        if (isAndroidMOrLater()) {
            if (!askedForOverlayPermission && !Settings.canDrawOverlays(this)) {
                askForOverlayPermission();
                return;
            }
        }
        continueStartNeoService();
    }

    private void continueStartNeoService() {
        Intent intent = new Intent(this, NeoUiActionsService.class);
        intent.putExtra(NeoUiActionsService.UUID_INTENT_PARAM, userUuid);
        startService(intent);
    }
}