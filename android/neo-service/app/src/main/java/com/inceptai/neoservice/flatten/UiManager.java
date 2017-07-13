package com.inceptai.neoservice.flatten;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.inceptai.neoservice.NeoThreadpool;
import com.inceptai.neoservice.Utils;

/**
 * Created by arunesh on 7/13/17.
 */

public class UiManager {

    private Context context;
    private NeoThreadpool neoThreadpool;
    private DisplayMetrics primaryDisplayMetrics;
    private FlatViewHierarchy flatViewHierarchy;

    public UiManager(Context context, NeoThreadpool neoThreadpool, DisplayMetrics displayMetrics) {
        this.context = context;
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
        if (!nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            Log.e(Utils.TAG, "Unable to perform action for some reason.");
        }
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
