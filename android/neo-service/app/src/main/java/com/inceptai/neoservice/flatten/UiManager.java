package com.inceptai.neoservice.flatten;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.inceptai.neoservice.NeoUiActionsService;
import com.inceptai.neoservice.NeoThreadpool;
import com.inceptai.neoservice.Utils;

/**
 * Created by arunesh on 7/13/17.
 */

public class UiManager {

    private NeoUiActionsService neoService;
    private NeoThreadpool neoThreadpool;
    private DisplayMetrics primaryDisplayMetrics;
    private FlatViewHierarchy flatViewHierarchy;

    public UiManager(NeoUiActionsService neoService, NeoThreadpool neoThreadpool, DisplayMetrics displayMetrics) {
        this.neoService = neoService;
        this.neoThreadpool = neoThreadpool;
        this.primaryDisplayMetrics = displayMetrics;
    }

    public void processClick(String viewId) {
        if (flatViewHierarchy == null) {
            Log.e(Utils.TAG, "Unable to process click event, NULL view hierarchy.");
            return;
        }
        FlatView flatView = flatViewHierarchy.getFlatViewFor(viewId);
        AccessibilityNodeInfo nodeInfo = flatView.getNodeInfo();
        if (nodeInfo == null) {
            Log.e(Utils.TAG, "Null NodeInfo for click event.");
            return;
        }
        Log.i(Utils.TAG, "Sending CLICK event for view: " + flatView.getText() + " class: " + flatView.getClassName());
        if (!performHierarchicalClick(nodeInfo)) {
            Log.e(Utils.TAG, "Unable to perform action for some reason.");
        }
    }

    private boolean performHierarchicalClick(AccessibilityNodeInfo nodeInfo) {
        boolean done = false;
        boolean result = false;
        while (!done) {
            result = nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            if (!result) {
                nodeInfo = nodeInfo.getParent();
                if (nodeInfo == null) {
                    done = true;
                }
            } else {
                done = true;
            }
        }
        return result;
    }

    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    public FlatViewHierarchy updateViewHierarchy(AccessibilityNodeInfo rootNode) {
        if (flatViewHierarchy == null) {
            flatViewHierarchy = new FlatViewHierarchy(rootNode, primaryDisplayMetrics);
        } else {
            flatViewHierarchy.update(rootNode);
        }
        flatViewHierarchy.flatten();
        return flatViewHierarchy;
    }
}
