package com.inceptai.neoservice.uiactions.views;


import com.inceptai.neoservice.Utils;

public class RenderingView {
    private static String NULL_TEXT = "null";
    private long parentViewId;
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
    //Important that for boolean getters --- setIsParentOfClickableView -- Is needs to be there.
    private boolean isParentOfClickableView;
    private boolean isChecked;
    private boolean isClickable;
    private boolean isCheckable;

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

    public void setParentViewId(long parentViewId) {
        this.parentViewId = parentViewId;
    }

    public void setFlatViewId(long flatViewId) {
        this.flatViewId = flatViewId;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setContentDescription(String contentDescription) {
        this.contentDescription = contentDescription;
    }

    public void setViewIdResourceName(String viewIdResourceName) {
        this.viewIdResourceName = viewIdResourceName;
    }

    public void setLeftX(int leftX) {
        this.leftX = leftX;
    }

    public void setRightX(int rightX) {
        this.rightX = rightX;
    }

    public void setTopY(int topY) {
        this.topY = topY;
    }

    public void setBottomY(int bottomY) {
        this.bottomY = bottomY;
    }

    public void setIsParentOfClickableView(boolean parentOfClickableView) {
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

    //Empty constructor
    public RenderingView(){}

    //Actual constructor
    public RenderingView(long parentViewId,  long flatViewId, String packageName,
                         String className, String contentDescription, String text,
                         String viewIdResourceName, int leftX, int rightX, int topY,
                         int bottomY, boolean isParentOfClickableView, boolean isChecked,
                         boolean isClickable, boolean isCheckable) {
        this.flatViewId = flatViewId;
        this.viewIdResourceName = viewIdResourceName;
        this.className = className;
        this.packageName = packageName;
        this.text = text;
        this.parentViewId = parentViewId;
        this.contentDescription = contentDescription;
        this.isClickable = isClickable;
        this.isCheckable = isCheckable;
        this.isChecked = isChecked;
        this.topY = topY;
        this.bottomY = bottomY;
        this.leftX = leftX;
        this.rightX = rightX;
        this.isParentOfClickableView = isParentOfClickableView;
    }

    public String getOverallText() {
        String textToReturn = Utils.EMPTY_STRING;
        if (!text.equals(NULL_TEXT)) {
            textToReturn = text;
        } else if (!contentDescription.equals(NULL_TEXT)) {
            textToReturn = contentDescription;
        }
        return textToReturn.trim();
    }
}

