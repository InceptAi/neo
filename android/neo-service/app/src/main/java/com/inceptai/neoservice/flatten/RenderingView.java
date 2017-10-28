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
    private boolean isScrollable = false;
    private boolean isEnabled = false;
    private boolean isSelected = false;
    private int totalItems = 0;
    private int currentItemIndex = 0;
    private int startItemIndex = 0;
    private int endItemIndex = 0;

    public boolean isSelected() {
        return isSelected;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getCurrentItemIndex() {
        return currentItemIndex;
    }

    public int getStartItemIndex() {
        return startItemIndex;
    }

    public int getEndItemIndex() {
        return endItemIndex;
    }

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

    public boolean isChecked() {
        return isChecked;
    }

    public boolean isClickable() {
        return isClickable;
    }

    public boolean isCheckable() {
        return isCheckable;
    }

    public boolean isScrollable() {
        return isScrollable;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public void setCurrentItemIndex(int currentItemIndex) {
        this.currentItemIndex = currentItemIndex;
    }

    public void setStartItemIndex(int startItemIndex) {
        this.startItemIndex = startItemIndex;
    }

    public void setEndItemIndex(int endItemIndex) {
        this.endItemIndex = endItemIndex;
    }

    public void setParentOfClickableView(boolean parentOfClickableView) {
        isParentOfClickableView = parentOfClickableView;
    }

    public void setIsChecked(boolean checked) {
        isChecked = checked;
    }

    public void setIsClickable(boolean clickable) {
        isClickable = clickable;
    }

    public void setIsCheckable(boolean checkable) {
        isCheckable = checkable;
    }

    public void setIsScrollable(boolean scrollable) {
        isScrollable = scrollable;
    }

    public void setIsEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public void setIsSelected(boolean selected) {
        isSelected = selected;
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
        this.isScrollable = flatView.isScrollable();
        this.isSelected = flatView.isSelected();
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
        this.currentItemIndex = -1;
        this.endItemIndex = -1;
        this.startItemIndex = -1;
        this.totalItems = 0;
    }

    public RenderingView(String className,
                         String packageName,
                         String contentDescription,
                         String text,
                         boolean isClickable,
                         boolean isCheckable,
                         boolean isScrollable,
                         boolean isChecked,
                         boolean isEnabled,
                         boolean isSelected,
                         int totalItems,
                         int currentItemIndex,
                         int startItemIndex,
                         int endItemIndex) {
        this.viewIdResourceName = Utils.EMPTY_STRING;
        this.className = className;
        this.packageName = packageName;
        this.text = text;
        this.contentDescription = contentDescription;
        this.isClickable = isClickable;
        this.isCheckable = isCheckable;
        this.isScrollable = isScrollable;
        this.isChecked = isChecked;
        this.isEnabled = isEnabled;
        this.isSelected = isSelected;
        this.totalItems = totalItems;
        this.currentItemIndex = currentItemIndex;
        this.startItemIndex = startItemIndex;
        this.endItemIndex = endItemIndex;
    }
}
