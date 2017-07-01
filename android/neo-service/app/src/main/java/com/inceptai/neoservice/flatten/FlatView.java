package com.inceptai.neoservice.flatten;

import android.graphics.Rect;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;

/**
 * Created by arunesh on 6/30/17.
 */

public class FlatView {

    public static final int UNDEFINED_CONNECTION_ID = -1;
    public static final int UNDEFINED_SELECTION_INDEX = -1;
    public static final int UNDEFINED_ITEM_ID = Integer.MAX_VALUE;
    public static final long ROOT_NODE_ID = -1;

    public static final int ACTIVE_WINDOW_ID = UNDEFINED_ITEM_ID;
    public static final int ANY_WINDOW_ID = -2;

    private AccessibilityNodeInfo nodeInfo;

    private int windowId = UNDEFINED_ITEM_ID;

    private long parentViewId = ROOT_NODE_ID;
    private long labelForId = ROOT_NODE_ID;
    private long labeledById = ROOT_NODE_ID;
    private long traversalBefore = ROOT_NODE_ID;
    private long traversalAfter = ROOT_NODE_ID;

    private int booleanProperties;
    private final Rect boundsInParent = new Rect();
    private final Rect boundsInScreen = new Rect();
    private int drawingOrderInParent;

    private int viewId;
    private String packageName;
    private String className;
    private String text;
    private String error;
    private String contentDescription;
    private String viewIdResourceName;

    private LongArray mChildNodeIds;
    private ArrayList<AccessibilityNodeInfo.AccessibilityAction> mActions;

    private int maxTextLength = -1;
    private int movementGranularities;

    private int textSelectionStart = UNDEFINED_SELECTION_INDEX;
    private int textSelectionEnd = UNDEFINED_SELECTION_INDEX;
    private int inputType = InputType.TYPE_NULL;
    private int liveRegion = View.ACCESSIBILITY_LIVE_REGION_NONE;

    private Bundle extras;

    private int connectionId = UNDEFINED_CONNECTION_ID;

    private AccessibilityNodeInfo.RangeInfo rangeInfo;
    private AccessibilityNodeInfo.CollectionInfo collectionInfo;
    private AccessibilityNodeInfo.CollectionItemInfo collectionItemInfo;
    private int childCount;

    public FlatView(AccessibilityNodeInfo nodeInfo) {
        this.windowId = nodeInfo.getWindowId();
        this.viewIdResourceName = nodeInfo.getViewIdResourceName();
        this.className = String.valueOf(nodeInfo.getClassName());
        this.packageName = String.valueOf(nodeInfo.getPackageName());
        this.text = String.valueOf(nodeInfo.getText());
        this.viewId = nodeInfo.hashCode();
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
        // TODO Get child views if childCount > 0.
        // TODO Get Bundle.

        nodeInfo.getBoundsInParent(boundsInParent);
        nodeInfo.getBoundsInScreen(boundsInScreen);
    }

    private void getChildren() {

    }
}
