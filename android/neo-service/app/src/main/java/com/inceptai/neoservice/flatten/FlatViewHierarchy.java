package com.inceptai.neoservice.flatten;

import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.inceptai.neoservice.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.content.ContentValues.TAG;
import static com.inceptai.neoservice.Utils.EMPTY_STRING;

/**
 * Created by arunesh on 6/30/17.
 */

public class FlatViewHierarchy {

    private AccessibilityNodeInfo rootNode;
    private FlatView rootNodeFlatView;
    private FlatView titleTextView;

    private SparseArray<FlatView> viewDb;
    private DisplayMetrics displayMetrics;
    private SparseArray<FlatView> textViewDb;
    private SparseArray<FlatView> scrollableViews;

    private AccessibilityEvent accessibilityEventTrigger;
    private AccessibilityNodeInfo accessibilityEventSourceInfo;

    //To keep track of last screen
    private boolean firstEventAfterUpdatingScreen;
    private Set<Integer> lastViewIdsDb;
    private int lastRootNodeId;
    private String lastScreenTitle;
    private String lastPackageName;

    public FlatViewHierarchy(AccessibilityNodeInfo rootNode,
                             @Nullable AccessibilityEvent accessibilityEvent,
                             @Nullable AccessibilityNodeInfo accessibilityEventSourceInfo,
                             DisplayMetrics displayMetrics) {
        this.rootNode = rootNode;
        this.viewDb = new SparseArray<>();
        this.textViewDb = new SparseArray<>();
        this.scrollableViews = new SparseArray<>();
        this.lastViewIdsDb = new HashSet<>();
        this.accessibilityEventTrigger = accessibilityEvent;
        this.accessibilityEventSourceInfo = accessibilityEventSourceInfo;
        this.displayMetrics = displayMetrics;
        this.lastScreenTitle = Utils.EMPTY_STRING;
        this.lastPackageName = Utils.EMPTY_STRING;
        this.firstEventAfterUpdatingScreen = false;
    }

    public void flatten() {
        if (rootNode == null) {
            Log.i(TAG, "Flatten failed coz of NULL rootnode.");
            return;
        }
        rootNodeFlatView = addNode(rootNode);
        List<FlatView> nodeQueue = new LinkedList<>();
        nodeQueue.add(rootNodeFlatView);

        while (!nodeQueue.isEmpty()) {
            FlatView flatView = nodeQueue.remove(0);
            addNode(flatView);
            if (titleTextView == null && FlatViewUtils.isTextView(flatView)) {
                titleTextView = flatView;
            }
            traverseChildrenFor(flatView, nodeQueue);
            if (FlatViewUtils.shouldSendViewToServer(flatView)) {
                textViewDb.append(flatView.getHashKey(), flatView);
            } else if (flatView.getNodeInfo() != null && flatView.getNodeInfo().isScrollable()) {
                Log.i(Utils.TAG, "Potential scrollable view: " + flatView.getNodeInfo().toString());
                if (FlatViewUtils.isScrollableView(flatView)) {
                    Log.i(Utils.TAG, "Adding view.");
                    scrollableViews.append(flatView.getHashKey(), flatView);
                }
            } else {
                flatView.recycle();
            }
        }
    }

    private boolean traverseChildrenFor(FlatView parentFlatView, List<FlatView> queue) {
        AccessibilityNodeInfo nodeInfo = parentFlatView.getNodeInfo();
        //AccessibilityNodeInfo.RangeInfo parentRangeInfo = nodeInfo.getRangeInfo();
        // if (parentRangeInfo != null) {
        // Log.i(TAG, "Non null range info");
        //}
        if (nodeInfo == null) {
            Log.i(TAG, "Stopped flattenNode for null nodeInfo.");
            return false;
        }
        boolean isParentOfTextViewChild = false;
        int numChildren = nodeInfo.getChildCount();
        for (int i = 0; i < numChildren; i ++) {
            AccessibilityNodeInfo childInfo = nodeInfo.getChild(i);
            //AccessibilityNodeInfo.RangeInfo chileRangeInfo = childInfo.getRangeInfo();
            if (childInfo != null) {
                FlatView childFlatView = new FlatView(childInfo);
                queue.add(childFlatView);
                parentFlatView.addChild(childFlatView.getHashKey());
                if (FlatViewUtils.shouldSendViewToServer(childFlatView)) {
                    isParentOfTextViewChild = true;
                }
            } else {
                Log.i(Utils.TAG, "Null child info for node: " + nodeInfo.getClassName() + " at index: " + i);
            }
        }
        return isParentOfTextViewChild;
    }

    public String toJson() {
        FlatViewHierarchySnapshot snapshot = new FlatViewHierarchySnapshot(viewDb,
                displayMetrics.heightPixels, displayMetrics.widthPixels);
        return Utils.gson.toJson(snapshot);
    }

    public String toSimpleJson() {
        int numViews = viewDb.size();
        SimpleViewHierarchySnapshot simpleViewHierarchySnapshot = new SimpleViewHierarchySnapshot();
        for (int i = 0; i < numViews; i ++) {
            FlatView flatView = viewDb.valueAt(i);
            if (flatView.getClassName() != null && flatView.getText() != null) {
                if (FlatViewUtils.shouldSendViewToServer(flatView)) {
                    String displayString = getSimpleViewStringForDemo(flatView);
                    if (!displayString.isEmpty()) {
                        simpleViewHierarchySnapshot.addView(String.valueOf(flatView.getHashKey()), displayString);
                    }
                }
            }
        }
        return Utils.gson.toJson(simpleViewHierarchySnapshot);
    }

    public String toRenderingJson() {
        int numViews = viewDb.size();
        RenderingViewHierarchySnapshot renderingViewHierarchySnapshot =
                new RenderingViewHierarchySnapshot(Utils.convertPixelsToDp(displayMetrics.widthPixels, displayMetrics),
                        Utils.convertPixelsToDp(displayMetrics.heightPixels, displayMetrics));
        for (int i = 0; i < numViews; i ++) {
            FlatView flatView = viewDb.valueAt(i);
            if (flatView.getClassName() != null && flatView.getText() != null) {
                //Adding views with text.
                boolean isLLWithTVChild = isLayoutWithTextViewChild(flatView);
                if (FlatViewUtils.shouldSendViewToServer(flatView) || isLLWithTVChild) {
                    RenderingView renderingView = new RenderingView(flatView, displayMetrics);
                    renderingView.setParentOfClickableView(isLLWithTVChild);
                    renderingViewHierarchySnapshot.addView(String.valueOf(flatView.getHashKey()), renderingView);
                }
            }
        }
        //Set the title of the screen
        if (titleTextView != null) {
            //TODO: remove this for sure
//            String testTitle = titleTextView.getText();
//            char[] testCharArray = testTitle.toCharArray();
//            char[] testCharArray1 = WIFI_ACCESSIBILITY.toCharArray();
//            Log.v("TEST", "Title: " + testTitle);
//            Log.v("TEST", "Title char array: " + testCharArray.toString());
//            Log.v("TEST", "Title char array1: " + testCharArray1.toString());
//            if (testTitle.equals(WIFI_ACCESSIBILITY)) {
//                Log.v("TEST", "WIFI accessibility string match");
//            }
            renderingViewHierarchySnapshot.setRootTitle(titleTextView.getText());
            renderingViewHierarchySnapshot.setRootPackageName(titleTextView.getPackageName());
        }
        //Set the event which triggered it
        if (accessibilityEventTrigger != null) {
            RenderingView lastViewClicked = null;
            String lastClassName = Utils.convertCharSeqToStringSafely(accessibilityEventTrigger.getClassName());
            String lastPackageName = Utils.convertCharSeqToStringSafely(accessibilityEventTrigger.getPackageName());
            String lastContentDescription = Utils.convertCharSeqToStringSafely(accessibilityEventTrigger.getContentDescription());
            String lastText = Utils.EMPTY_STRING;
            if (accessibilityEventTrigger.getText() != null && ! accessibilityEventTrigger.getText().isEmpty()) {
                lastText = Utils.convertCharSeqToStringSafely(accessibilityEventTrigger.getText().get(0));
            }
            try {
                lastViewClicked = new RenderingView(lastClassName, lastPackageName, lastContentDescription, lastText);
            } catch (Exception e) {
                Log.v("NEO", "Exception :" + e.toString());
            }
            renderingViewHierarchySnapshot.setLastViewClicked(lastViewClicked);
            renderingViewHierarchySnapshot.setLastUIAction(AccessibilityEvent.eventTypeToString(accessibilityEventTrigger.getEventType()));
            Log.d("FVH", "Processing eventType: " + AccessibilityEvent.eventTypeToString(accessibilityEventTrigger.getEventType()) + " , text: " + lastText);
        }

        if (accessibilityEventSourceInfo != null) {
            int sourceId = accessibilityEventSourceInfo.hashCode();
            String titleToSet = Utils.EMPTY_STRING;
            String packageNameToSet = Utils.EMPTY_STRING;
            if ((lastViewIdsDb != null && lastViewIdsDb.contains(sourceId)) || firstEventAfterUpdatingScreen) {
                //this trigger is in the last screen, set title/package name accordingly
                titleToSet = lastScreenTitle;
                packageNameToSet = lastPackageName;
            } else {
                Set<Integer> currentViewIdsSet = getViewIdsFromDb(viewDb);
                if (currentViewIdsSet.contains(sourceId)) {
                    titleToSet = titleTextView != null ? titleTextView.getText() : Utils.EMPTY_STRING;
                    packageNameToSet = titleTextView != null ? titleTextView.getPackageName() : Utils.EMPTY_STRING;
                }
            }
            Log.d("FVH", "title/package " + titleToSet + " " + packageNameToSet);
            renderingViewHierarchySnapshot.setLastScreenTitle(titleToSet);
            renderingViewHierarchySnapshot.setLastScreenPackageName(packageNameToSet);
            firstEventAfterUpdatingScreen = false;
        }
//        else if (lastScreenTitleView != null) {
//            renderingViewHierarchySnapshot.setLastScreenTitle(lastScreenTitleView.getText());
//            renderingViewHierarchySnapshot.setLastScreenPackageName(lastScreenTitleView.getPackageName());
//        }


        return Utils.gson.toJson(renderingViewHierarchySnapshot);
    }

    private void recycleViewDb() {
        if (viewDb != null) {
            for(int viewIndex = 0; viewIndex < viewDb.size(); viewIndex++) {
                int key = viewDb.keyAt(viewIndex);
                // get the object by the key.
                FlatView flatView = viewDb.get(key);
                flatView.recycle();
            }
        }
    }

    private Set<Integer> getViewIdsFromDb(SparseArray<FlatView> flatViewSparseArray) {
        Set<Integer> viewIdSet = new HashSet<>();
        if (flatViewSparseArray != null) {
            for(int viewIndex = 0; viewIndex < flatViewSparseArray.size(); viewIndex++) {
                int key = flatViewSparseArray.keyAt(viewIndex);
                // get the object by the key.
                FlatView flatView = flatViewSparseArray.get(key);
                viewIdSet.add(flatView.getHashKey());
            }
        }
        return viewIdSet;
    }


    public void update(AccessibilityNodeInfo newRootNode,
                       AccessibilityEvent accessibilityEvent,
                       AccessibilityNodeInfo accessibilityEventSourceInfo) {
        String newScreenTitle = Utils.findScreenTitleForNode(newRootNode);
        String newPackageName = newRootNode != null && newRootNode.getPackageName() != null ? newRootNode.getPackageName().toString() : Utils.EMPTY_STRING;
        String currentPackageName = rootNode != null && rootNode.getPackageName() != null ? rootNode.getPackageName().toString() : Utils.EMPTY_STRING;
        String currentScreenTitle = titleTextView != null && titleTextView.getText() != null ? titleTextView.getText() : Utils.EMPTY_STRING;
        if ((!Utils.nullOrEmpty(newScreenTitle) && !newScreenTitle.equals(currentScreenTitle)) ||
                (!Utils.nullOrEmpty(newPackageName) && !newPackageName.equals(currentPackageName))) {
            Log.d("FVH", "updating last node since title changed from " + currentScreenTitle + " -> " +
                    newScreenTitle + " or pkg from " + currentPackageName + " -> " + newPackageName);
            if (rootNode != null) {
                lastRootNodeId = rootNode.hashCode();
            }
            lastRootNodeId = rootNode.hashCode();
            lastViewIdsDb = getViewIdsFromDb(viewDb);
            lastScreenTitle = currentScreenTitle;
            lastPackageName = currentPackageName;
            firstEventAfterUpdatingScreen = true;
        }
        viewDb.clear();
        textViewDb.clear();
        scrollableViews.clear();
        titleTextView = null;
        rootNode = newRootNode;
        accessibilityEventTrigger = accessibilityEvent;
        this.accessibilityEventSourceInfo = accessibilityEventSourceInfo;
    }

    public FlatView getFlatViewFor(String viewId) {
        return viewDb.get(Integer.valueOf(viewId));
    }

    public AccessibilityNodeInfo findScrollableFlatView() {
        for (int i = 0; i < scrollableViews.size(); i ++) {
            FlatView flatView = scrollableViews.valueAt(i);
            if (flatView != null && flatView.getNodeInfo() != null && flatView.getNodeInfo().isScrollable()) {
                return flatView.getNodeInfo();
            }
        }
        return null;
    }

    private String getSimpleViewStringForDemo(FlatView flatView) {
        String className = flatView.getClassName();
        if (FlatViewUtils.isImage(flatView)) {
            Log.i(Utils.TAG, "IMAGEBUTTON: resource: " + flatView.getContentDescription());
            return "Image:" + flatView.getContentDescription();
        }
        if (FlatViewUtils.isTextView(flatView)) {
            return flatView.getText();
        }
        String text = flatView.getText();
        if (text != null && text.length() > 0  && FlatViewUtils.isNotNullValuedString(text)) {
            return text;
        }
        String contentDesc = flatView.getContentDescription();
        if (contentDesc != null && contentDesc.length() > 0 && FlatViewUtils.isNotNullValuedString(contentDesc)) {
            return contentDesc;
        }
        return EMPTY_STRING;
    }

    private FlatView addNode(AccessibilityNodeInfo nodeInfo) {
        FlatView flatView = new FlatView(nodeInfo);
        addNode(flatView);
        return flatView;
    }

    private void addNode(FlatView flatView) {
        viewDb.append(flatView.getHashKey(), flatView);
    }

    private boolean isLayoutWithTextViewChild(FlatView flatView) {
        if (flatView == null || flatView.getChildren() == null || !FlatViewUtils.isLinearRelativeOrFrameLayout(flatView)) {
            return false;
        }
        for (Integer viewId: flatView.getChildren()) {
            FlatView childFlatView = viewDb.get(viewId);
            if (FlatViewUtils.shouldSendViewToServer(childFlatView)) {
                return true;
            }
        }
        return false;
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

    private static class SimpleViewHierarchySnapshot {
        int numViews;
        Map<String, String> viewMap = new HashMap<>();

        SimpleViewHierarchySnapshot() {
            numViews = 0;
        }

        public void addView(String viewId, String title) {
            viewMap.put(viewId, title);
            numViews ++;
        }
    }

    private static class RenderingViewHierarchySnapshot {
        int rootWidth;
        int rootHeight;
        int numViews;
        String rootTitle;
        String lastScreenTitle;
        String lastScreenPackageName;
        String lastUIAction;
        RenderingView lastViewClicked;
        String rootPackageName;
        Map<String, String> deviceInfo = new HashMap<>();
        Map<String, RenderingView> viewMap = new HashMap<>();

        RenderingViewHierarchySnapshot(int rootWidth, int rootHeight) {
            numViews = 0;
            this.rootWidth = rootWidth;
            this.rootHeight = rootHeight;
            this.rootTitle = Utils.EMPTY_STRING;
            this.lastScreenTitle = Utils.EMPTY_STRING;
            this.lastViewClicked = null;
            this.lastUIAction = Utils.EMPTY_STRING;
            this.lastScreenPackageName = Utils.EMPTY_STRING;
            this.rootPackageName = Utils.EMPTY_STRING;
            this.deviceInfo = Utils.getDeviceDetails();
        }

        public void addView(String viewId, RenderingView renderingView) {
            viewMap.put(viewId, renderingView);
            numViews ++;
        }

        public void setRootPackageName(String rootPackageName) {
            this.rootPackageName = rootPackageName;
        }

        public void setRootTitle(String rootTitle) {
            this.rootTitle = rootTitle;
        }

        public void setLastScreenTitle(String lastScreenTitle) {
            this.lastScreenTitle = lastScreenTitle;
        }

        public void setLastUIAction(String lastUIAction) {
            this.lastUIAction = lastUIAction;
        }

        public void setLastViewClicked(RenderingView lastViewClicked) {
            this.lastViewClicked = lastViewClicked;
        }

        public void setLastScreenPackageName(String lastScreenPackageName) {
            this.lastScreenPackageName = lastScreenPackageName;
        }
    }
}
