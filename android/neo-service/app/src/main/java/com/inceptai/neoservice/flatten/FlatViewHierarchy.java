package com.inceptai.neoservice.flatten;

import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityNodeInfo;

import com.inceptai.neoservice.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static android.content.ContentValues.TAG;
import static com.inceptai.neoservice.Utils.EMPTY_STRING;

/**
 * Created by arunesh on 6/30/17.
 */

public class FlatViewHierarchy {

    private AccessibilityNodeInfo rootNode;
    private FlatView rootNodeFlatView;

    private SparseArray<FlatView> viewDb;
    private DisplayMetrics displayMetrics;

    public FlatViewHierarchy(AccessibilityNodeInfo rootNode, DisplayMetrics displayMetrics) {
        this.rootNode = rootNode;
        this.viewDb = new SparseArray<>();
        this.displayMetrics = displayMetrics;
    }

    public void flatten() {
        rootNodeFlatView = addNode(rootNode);
        List<FlatView> nodeQueue = new LinkedList<>();
        nodeQueue.add(rootNodeFlatView);

        while (!nodeQueue.isEmpty()) {
            FlatView nodeInfo = nodeQueue.remove(0);
            addNode(nodeInfo);
            traverseChildrenFor(nodeInfo, nodeQueue);
            nodeInfo.recycle();
        }
    }

    private void traverseChildrenFor(FlatView parentFlatView, List<FlatView> queue) {
        AccessibilityNodeInfo nodeInfo = parentFlatView.getNodeInfo();
        if (nodeInfo == null) {
            Log.i(TAG, "Stopped flattenNode for null nodeInfo.");
            return;
        }
        int numChildren = nodeInfo.getChildCount();
        for (int i = 0; i < numChildren; i ++) {
            AccessibilityNodeInfo childInfo = nodeInfo.getChild(i);
            FlatView childFlatView = new FlatView(childInfo);
            queue.add(childFlatView);
            parentFlatView.addChild(childFlatView.getHashKey());
        }
    }

    public String toJson() {
        FlatViewHierarchySnapshot snapshot = new FlatViewHierarchySnapshot(viewDb,
                displayMetrics.heightPixels, displayMetrics.widthPixels);
        return Utils.gson.toJson(snapshot);
    }

    public void update(AccessibilityNodeInfo newRootNode) {

    }

    private FlatView addNode(AccessibilityNodeInfo nodeInfo) {
        FlatView flatView = new FlatView(nodeInfo);
        addNode(flatView);
        return flatView;
    }

    private void addNode(FlatView flatView) {
        viewDb.append(flatView.getHashKey(), flatView);
    }

    private static class FlatViewHierarchySnapshot {

        List<FlatView> viewList;
        String timestamp;
        int height;
        int width;

        FlatViewHierarchySnapshot(SparseArray<FlatView> flatViewSparseArray, int height, int width) {
            viewList = new ArrayList<>(flatViewSparseArray.size());
            for (int i = 0; i < flatViewSparseArray.size(); i++) {
                viewList.add(flatViewSparseArray.valueAt(i));
            }
            timestamp = new Date().toString();
            this.height = height;
            this.width = width;
        }
    }
}
