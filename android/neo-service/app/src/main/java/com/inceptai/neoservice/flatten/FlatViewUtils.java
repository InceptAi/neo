package com.inceptai.neoservice.flatten;

import com.inceptai.neoservice.Utils;

/**
 * Created by arunesh on 7/12/17.
 */

public class FlatViewUtils {
    public static final String TEXT_VIEW_CLASSNAME = "android.widget.TextView";
    public static final String IMAGE_CLASSNAME = "android.widget.ImageButton";
    public static final String LINEAR_LAYOUT_CLASSNAME = "android.widget.LinearLayout";
    public static final String FRAME_LAYOUT_CLASSNAME = "android.widget.FrameLayout";
    public static final String RELATIVE_LAYOUT_CLASSNAME = "android.widget.RelativeLayout";
    public static final String CHECK_BOX_CLASSNAME = "android.widget.CheckBox";
    public static final String CHECKED_TEXT_VIEW_CLASS_NAME = "android.widget.CheckedTextView";
    public static final String SWITCH_CLASSNAME = "android.widget.Switch";
    public static final String SEEK_BAR_CLASS_NAME = "android.widget.SeekBar";
    public static final String EDIT_TEXT_VIEW_CLASS_NAME = "android.widget.EditText";
    public static final String ON_TEXT = "ON";
    public static final String OFF_TEXT = "OFF";


    private static final String VIEWPAGER_CLASS = "ViewPager";
    private static final String NULL_STRING = "null";

    private FlatViewUtils() {}

    public static boolean isTextView(FlatView flatView) {
        return TEXT_VIEW_CLASSNAME.equals(flatView.getClassName());
    }

    public static boolean shouldSendViewToServer(FlatView flatView) {
        return IMAGE_CLASSNAME.equals(flatView.getClassName())
                || CHECK_BOX_CLASSNAME.equals(flatView.getClassName())
                || TEXT_VIEW_CLASSNAME.equals(flatView.getClassName())
                || !Utils.nullOrEmpty(flatView.getText())
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

    public static boolean isLinearRelativeOrFrameLayout(FlatView flatView) {
        String className = flatView.getClassName();
        return LINEAR_LAYOUT_CLASSNAME.equals(className) || RELATIVE_LAYOUT_CLASSNAME.equals(className) || FRAME_LAYOUT_CLASSNAME.equals(className);
    }

    public static boolean isCheckBox(FlatView flatView) {
        return CHECK_BOX_CLASSNAME.equals(flatView.getClassName());
    }

}
