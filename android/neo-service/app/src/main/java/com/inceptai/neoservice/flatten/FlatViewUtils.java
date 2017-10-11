package com.inceptai.neoservice.flatten;

import com.inceptai.neoservice.Utils;

/**
 * Created by arunesh on 7/12/17.
 */

public class FlatViewUtils {
    private static final String TEXTVIEW_CLASSNAME = "android.widget.TextView";
    private static final String IMAGE_CLASSNAME = "android.widget.ImageButton";
    private static final String LINEAR_LAYOUT_CLASSNAME = "android.widget.LinearLayout";
    private static final String FRAME_LAYOUT_CLASSNAME = "android.widget.FrameLayout";
    private static final String RELATIVE_LAYOUT_CLASSNAME = "android.widget.RelativeLayout";
    private static final String CHECK_BOX_CLASSNAME = "android.widget.CheckBox";

    private static final String VIEWPAGER_CLASS = "ViewPager";
    private static final String NULL_STRING = "null";

    private FlatViewUtils() {}

    public static boolean isTextView(FlatView flatView) {
        return TEXTVIEW_CLASSNAME.equals(flatView.getClassName());
    }

    public static boolean shouldSendViewToServer(FlatView flatView) {
        return IMAGE_CLASSNAME.equals(flatView.getClassName())
                || CHECK_BOX_CLASSNAME.equals(flatView.getClassName())
                || TEXTVIEW_CLASSNAME.equals(flatView.getClassName())
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
