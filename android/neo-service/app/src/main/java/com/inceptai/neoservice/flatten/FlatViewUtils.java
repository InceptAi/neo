package com.inceptai.neoservice.flatten;

import com.inceptai.neoservice.Utils;

import org.json.JSONObject;

/**
 * Created by arunesh on 7/12/17.
 */

public class FlatViewUtils {
    private static final String TEXTVIEW_CLASSNAME = "android.widget.TextView";
    private static final String IMAGE_CLASSNAME = "android.widget.ImageButton";
    private static final String VIEWPAGER_CLASS = "ViewPager";
    private static final String NULL_STRING = "null";

    private FlatViewUtils() {}

    public static boolean isTextView(FlatView flatView) {
        return TEXTVIEW_CLASSNAME.equals(flatView.getClassName());
    }

    public static boolean hasText(FlatView flatView) {
        return IMAGE_CLASSNAME.equals(flatView.getClassName()) || TEXTVIEW_CLASSNAME.equals(flatView.getClassName()) || !Utils.nullOrEmpty(flatView.getText())
        || !Utils.nullOrEmpty(flatView.getContentDescription());
    }

    public static boolean isImage(FlatView flatView) {
        return IMAGE_CLASSNAME.equals(flatView.getClassName());
    }

    public static boolean isNotNullValuedString(String target) {
        return !NULL_STRING.equals(target);
    }

    public static boolean isScrollableView(FlatView flatView) {
        if (flatView.getNodeInfo() != null && flatView.getNodeInfo().isScrollable()) {
            String className = String.valueOf(flatView.getNodeInfo().getClassName());
            return (!className.contains(VIEWPAGER_CLASS));
        }
        return false;
    }
}
