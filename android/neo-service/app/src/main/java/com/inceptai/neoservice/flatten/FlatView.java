package com.inceptai.neoservice.flatten;

import android.graphics.Rect;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import com.inceptai.neoservice.Utils;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by arunesh on 6/30/17.
 */

public class FlatView {

    public static final int UNDEFINED_CONNECTION_ID = -1;
    public static final int UNDEFINED_SELECTION_INDEX = -1;
    public static final int UNDEFINED_ITEM_ID = Integer.MAX_VALUE;
    public static final long INVALID_NODE_ID = -1;
    public static final long UNABLE_TO_FETCH_SOURCE_NODE_ID = -1000;

    public static final int ACTIVE_WINDOW_ID = UNDEFINED_ITEM_ID;
    public static final int ANY_WINDOW_ID = -2;

    private int windowId = UNDEFINED_ITEM_ID;

    private long parentViewId = INVALID_NODE_ID;
    private long labelForId = INVALID_NODE_ID;
    private long labeledById = INVALID_NODE_ID;
    private long traversalBefore = INVALID_NODE_ID;
    private long traversalAfter = INVALID_NODE_ID;

    private int booleanProperties;
    private final Rect boundsInParent = new Rect();
    private final Rect boundsInScreen = new Rect();
    private int drawingOrderInParent;

    private int flatViewId;
    private String packageName;
    private String className;
    private String text;
    private String error;
    private String contentDescription;
    private String viewIdResourceName;

    private int maxTextLength = -1;
    private int movementGranularities;

    private int textSelectionStart = UNDEFINED_SELECTION_INDEX;
    private int textSelectionEnd = UNDEFINED_SELECTION_INDEX;
    private int inputType = InputType.TYPE_NULL;
    private int liveRegion = View.ACCESSIBILITY_LIVE_REGION_NONE;

    private int connectionId = UNDEFINED_CONNECTION_ID;



    private AccessibilityNodeInfo.RangeInfo rangeInfo;
    private AccessibilityNodeInfo.CollectionInfo collectionInfo;
    private AccessibilityNodeInfo.CollectionItemInfo collectionItemInfo;

    private int childCount;
    private long sourceNodeId = INVALID_NODE_ID;
    private static Method sGetSourceNodeIdMethod;

    private ArrayList<Integer> children;

    private boolean isChecked = false;
    private boolean isCheckable = false;
    private boolean isClickable = false;

    // We use "transient" so that this field does not get processed by gson.
    private transient AccessibilityNodeInfo sourceNodeInfo;

    static {
        try {
            sGetSourceNodeIdMethod = AccessibilityNodeInfo.class.getDeclaredMethod("getSourceNodeId");
            sGetSourceNodeIdMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            Log.d(Utils.TAG, "Error setting up fields: " + e.toString());
            e.printStackTrace();
        }
    }

    public FlatView(AccessibilityNodeInfo nodeInfo) {
        this.windowId = nodeInfo.getWindowId();
        this.viewIdResourceName = nodeInfo.getViewIdResourceName();
        this.className = String.valueOf(nodeInfo.getClassName());
        this.packageName = String.valueOf(nodeInfo.getPackageName());
        this.text = String.valueOf(nodeInfo.getText());
        this.flatViewId = nodeInfo.hashCode();
        AccessibilityNodeInfo parent = nodeInfo.getParent();
        if (parent != null) {
            this.parentViewId = parent.hashCode();
        }
        this.error = String.valueOf(nodeInfo.getError());
        this.contentDescription = String.valueOf(nodeInfo.getContentDescription());
        this.maxTextLength = nodeInfo.getMaxTextLength();
        this.movementGranularities = nodeInfo.getMovementGranularities();
        this.textSelectionStart = nodeInfo.getTextSelectionStart();
        this.textSelectionEnd = nodeInfo.getTextSelectionEnd();
        this.inputType = nodeInfo.getInputType();
        this.liveRegion = nodeInfo.getLiveRegion();
        this.rangeInfo = nodeInfo.getRangeInfo();
        this.collectionInfo = nodeInfo.getCollectionInfo();
        this.collectionItemInfo = nodeInfo.getCollectionItemInfo();
        this.childCount = nodeInfo.getChildCount();
        this.isCheckable = nodeInfo.isCheckable();
        this.isChecked = nodeInfo.isChecked();
        this.isClickable = nodeInfo.isClickable();
        try {
            this.sourceNodeId = (long) (sGetSourceNodeIdMethod != null ? sGetSourceNodeIdMethod.invoke(nodeInfo) : UNABLE_TO_FETCH_SOURCE_NODE_ID);
        } catch (Exception e) {
            Log.i(Utils.TAG, "Unable to fetch source node ID.");
        }

        // TODO Get child views if childCount > 0.
        // TODO Get Bundle.

        nodeInfo.getBoundsInParent(boundsInParent);
        nodeInfo.getBoundsInScreen(boundsInScreen);

        this.sourceNodeInfo = nodeInfo;
    }

    public int getHashKey() {
        return flatViewId;
    }

    public void addChild(int childHashKey) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(childHashKey);
    }

    public void recycle() {
        sourceNodeInfo.recycle();
        sourceNodeInfo = null;
    }

    public AccessibilityNodeInfo getNodeInfo() {
        return sourceNodeInfo;
    }

    public String toJson() {
        return Utils.gson.toJson(this);
    }

    public String getClassName() {
        return className;
    }

    public String getText() {
        return text;
    }

    public String getViewIdResourceName() {
        return viewIdResourceName;
    }

    public String getContentDescription() {
        return contentDescription;
    }

    public long getParentViewId() {
        return parentViewId;
    }

    public Rect getBoundsInScreen() {
        return boundsInScreen;
    }

    public int getFlatViewId() {
        return flatViewId;
    }

    public String getPackageName() {
        return packageName;
    }

    public ArrayList<Integer> getChildren() {
        return children;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public boolean isCheckable() {
        return isCheckable;
    }

    public boolean isClickable() {
        return isClickable;
    }
}
