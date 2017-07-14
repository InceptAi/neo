package com.inceptai.neoservice.flatten;

import com.inceptai.neoservice.Utils;

import org.json.JSONObject;

/**
 * Created by arunesh on 7/12/17.
 */

public class FlatViewUtils {
    private static final String TEXTVIEW_CLASSNAME = "android.widget.TextView";
    private FlatViewUtils() {}

    public static boolean isTextView(FlatView flatView) {
        return TEXTVIEW_CLASSNAME.equals(flatView.getClassName()) || !Utils.nullOrEmpty(flatView.getText());
    }
}
