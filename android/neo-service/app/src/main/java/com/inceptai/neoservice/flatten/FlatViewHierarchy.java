package com.inceptai.neoservice.flatten;

import android.view.accessibility.AccessibilityNodeInfo;

import static com.inceptai.neoservice.Utils.EMPTY_STRING;

/**
 * Created by arunesh on 6/30/17.
 */

public class FlatViewHierarchy {

    private AccessibilityNodeInfo rootNode;


    public FlatViewHierarchy(AccessibilityNodeInfo rootNode) {
        this.rootNode = rootNode;
    }

    public void flatten() {

    }

    public String toJson() {
        return EMPTY_STRING;
    }

    public void update(AccessibilityNodeInfo newRootNode) {

    }
}
