package com.inceptai.neoservice.flatten;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.inceptai.neoservice.NeoUiActionsService;
import com.inceptai.neoservice.NeoThreadpool;
import com.inceptai.neoservice.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by arunesh on 7/13/17.
 */

public class UiManager {
    public static final String GLOBAL_ACTION_KEY = "actionName";
    private static final String END_ACTION = "end";
    private static final String BACK_ACTION = "back";
    private static final String HOME_ACTION = "home";
    private static final String SETTINGS_ACTION = "settings";
    private static final String REFRESH_ACTION = "refresh";
    private static final String SCROLLUP_ACTION = "scrollup";
    private static final String SCROLLDOWN_ACTION = "scrolldown";

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

    private boolean performScroll(boolean forward) {
        AccessibilityNodeInfo nodeInfo = flatViewHierarchy.findScrollableFlatView();
        if (nodeInfo == null) {
            Log.e(Utils.TAG, "Could not find scrollable view.");
            return false;
        }

        boolean done = false;
        boolean result = false;
        while (!done) {
            if (nodeInfo.isScrollable()) {
                result = nodeInfo.performAction(forward ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
            }
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

    public FlatViewHierarchy updateViewHierarchy(AccessibilityNodeInfo rootNode) {
        if (flatViewHierarchy == null) {
            flatViewHierarchy = new FlatViewHierarchy(rootNode, primaryDisplayMetrics);
        } else {
            flatViewHierarchy.update(rootNode);
        }
        flatViewHierarchy.flatten();
        return flatViewHierarchy;
    }

    public void takeAction(JSONObject actionJson) {
        String action = Utils.EMPTY_STRING;
        try {
            action = actionJson.getString(GLOBAL_ACTION_KEY);
        } catch (JSONException e) {
            Log.e(Utils.TAG, "Json error: " + e);
            return;
        }
        if (!Utils.nullOrEmpty(action)) {
            if (BACK_ACTION.equals(action)) {
                // back global action.
                neoService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            } else if (END_ACTION.equals(action)) {
                // end global action.
                neoService.stopServiceByExpert();
            } else if (SETTINGS_ACTION.equals(action)) {
                // settings action
                showSettings();
            } else if (HOME_ACTION.equals(action)) {
                neoService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
            } else if (REFRESH_ACTION.equals(action)) {
                neoService.refreshFullUi();
            } else if (SCROLLDOWN_ACTION.equals(action)) {
                performScroll(true /* forward */);
            } else if (SCROLLUP_ACTION.equals(action)) {
                performScroll(false /* backward */);
            }
        }
    }

    public void cleanup () {
        neoService = null;
        neoThreadpool = null;
        primaryDisplayMetrics = null;
        flatViewHierarchy = null;
    }

    private void showSettings() {
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        neoService.startActivity(intent);
    }
}
