package com.inceptai.neoservice;

import android.app.Presentation;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Display;

/**
 * Created by arunesh on 6/26/17.
 */

public class NeoPresentation extends Presentation {
    private static int[] colors  = new int[] {
                    ((int) (Math.random() * Integer.MAX_VALUE)) | 0xFF000000,
                    ((int) (Math.random() * Integer.MAX_VALUE)) | 0xFF000000 };

    public NeoPresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    public NeoPresentation(Context outerContext, Display display, int theme) {
        super(outerContext, display, theme);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the resources for the context of the presentation.
        // Notice that we are getting the resources from the context of the presentation.
        Resources r = getContext().getResources();

        // Inflate the layout.
        setContentView(R.layout.presentation_content);

        final Display display = getDisplay();
        final int displayId = display.getDisplayId();
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);

        // Set the background to a random gradient.
        Point p = new Point();
        getDisplay().getSize(p);
        drawable.setGradientRadius(Math.max(p.x, p.y) / 2);
        drawable.setColors(colors);
        findViewById(android.R.id.content).setBackground(drawable);
    }
}
