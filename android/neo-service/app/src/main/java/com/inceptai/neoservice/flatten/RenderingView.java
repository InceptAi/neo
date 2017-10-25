package com.inceptai.neoservice.flatten;

import android.graphics.Rect;
import android.util.DisplayMetrics;

import com.inceptai.neoservice.Utils;

import static com.inceptai.neoservice.flatten.FlatView.INVALID_NODE_ID;

/**
 * Created by arunesh on 6/30/17.
 */

public class RenderingView {

    private long parentViewId = INVALID_NODE_ID;
    private long flatViewId;
    private String packageName;
    private String className;
    private String text;
    private String contentDescription;
    private String viewIdResourceName;
    private int leftX;
    private int rightX;
    private int topY;
    private int bottomY;
    private boolean isParentOfClickableView = false;
    private boolean isChecked = false;
    private boolean isClickable = false;
    private boolean isCheckable = false;

    public long getParentViewId() {
        return parentViewId;
    }

    public long getFlatViewId() {
        return flatViewId;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getText() {
        return text;
    }

    public String getContentDescription() {
        return contentDescription;
    }

    public String getViewIdResourceName() {
        return viewIdResourceName;
    }

    public int getLeftX() {
        return leftX;
    }

    public int getRightX() {
        return rightX;
    }

    public int getTopY() {
        return topY;
    }

    public int getBottomY() {
        return bottomY;
    }

    public boolean isParentOfClickableView() {
        return isParentOfClickableView;
    }

    public void setParentOfClickableView(boolean parentOfClickableView) {
        isParentOfClickableView = parentOfClickableView;
    }

    public RenderingView(FlatView flatView, DisplayMetrics displayMetrics) {
        this.flatViewId = flatView.getHashKey();
        this.viewIdResourceName = flatView.getViewIdResourceName();
        this.className = flatView.getClassName();
        this.packageName = flatView.getPackageName();
        this.text = flatView.getText();
        this.parentViewId = flatView.getParentViewId();
        this.contentDescription = flatView.getContentDescription();
        this.isClickable = flatView.isClickable();
        this.isCheckable = flatView.isCheckable();
        this.isChecked = flatView.isChecked();
        Rect bounds = flatView.getBoundsInScreen();
        if (bounds != null) {
            this.topY = Utils.convertPixelsToDp(bounds.top, displayMetrics);
            this.bottomY = Utils.convertPixelsToDp(bounds.bottom, displayMetrics);
            this.leftX = Utils.convertPixelsToDp(bounds.left, displayMetrics);
            this.rightX = Utils.convertPixelsToDp(bounds.right, displayMetrics);
        }
    }


    public RenderingView(String className,
                         String packageName,
                         String contentDescription,
                         String text) {
        this.viewIdResourceName = Utils.EMPTY_STRING;
        this.className = className;
        this.packageName = packageName;
        this.text = text;
        this.contentDescription = contentDescription;
    }
}
