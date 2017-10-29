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
    private ScreenTitleAndPackage currentScreenInfo;
    private ScreenTitleAndPackage lastScreenInfo;


    private SparseArray<FlatView> viewDb;
    private DisplayMetrics displayMetrics;
    private SparseArray<FlatView> textViewDb;
    private SparseArray<FlatView> scrollableViews;

    private AccessibilityEvent accessibilityEventTrigger;
    private AccessibilityNodeInfo accessibilityEventSourceInfo;

    private class ScreenTitleAndPackage {
        private String title;
        private String packageName;
        ScreenTitleAndPackage(String title, String packageName) {
            this.title = title;
            this.packageName = packageName;
        }

        public String getTitle() {
            return title;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ScreenTitleAndPackage that = (ScreenTitleAndPackage) o;

            if (!title.equalsIgnoreCase(that.title)) return false;
            return packageName.equalsIgnoreCase(that.packageName);

        }

        @Override
        public int hashCode() {
            int result = title.hashCode();
            result = 31 * result + packageName.hashCode();
            return result;
        }

        public boolean isEmpty() {
            return title.isEmpty() || packageName.isEmpty();
        }
    }

    private SparseArray<ScreenTitleAndPackage> windowIdToScreenInfo;


    public FlatViewHierarchy(AccessibilityNodeInfo rootNode,
                             @Nullable AccessibilityEvent accessibilityEvent,
                             @Nullable AccessibilityNodeInfo accessibilityEventSourceInfo,
                             DisplayMetrics displayMetrics) {
        this.rootNode = rootNode;
        this.viewDb = new SparseArray<>();
        this.textViewDb = new SparseArray<>();
        this.scrollableViews = new SparseArray<>();
        this.accessibilityEventTrigger = accessibilityEvent;
        this.accessibilityEventSourceInfo = accessibilityEventSourceInfo;
        this.displayMetrics = displayMetrics;
        windowIdToScreenInfo = new SparseArray<>();
        this.currentScreenInfo = new ScreenTitleAndPackage(Utils.EMPTY_STRING, Utils.EMPTY_STRING);
        this.lastScreenInfo = new ScreenTitleAndPackage(Utils.EMPTY_STRING, Utils.EMPTY_STRING);
    }


    public void flatten() {
        if (rootNode == null) {
            Log.i(TAG, "Flatten failed coz of NULL rootnode.");
            return;
        }
        rootNodeFlatView = addNode(rootNode);
        List<FlatView> nodeQueue = new LinkedList<>();
        nodeQueue.add(rootNodeFlatView);
        currentScreenInfo = findAndUpdateScreenInfoFromRootNode(rootNode);
        while (!nodeQueue.isEmpty()) {
            FlatView flatView = nodeQueue.remove(0);
            addNode(flatView);
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

    private ScreenTitleAndPackage findAndUpdateScreenInfoFromRootNode(AccessibilityNodeInfo rootNodeInfo) {
        ScreenTitleAndPackage screenTitleAndPackage = null;
        if (rootNodeInfo != null) {
            screenTitleAndPackage = windowIdToScreenInfo.get(rootNodeInfo.getWindowId());
            if (screenTitleAndPackage == null) {
                //Not found, find it using traversal and update
                String currentTitle = Utils.findScreenTitleForNode(rootNodeInfo);
                String currentPackageName = rootNodeInfo.getPackageName() != null ? rootNodeInfo.getPackageName().toString() : Utils.EMPTY_STRING;
                screenTitleAndPackage = new ScreenTitleAndPackage(currentTitle, currentPackageName);
                windowIdToScreenInfo.append(rootNodeInfo.getWindowId(), screenTitleAndPackage);
            }
        }
        return screenTitleAndPackage;
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
        renderingViewHierarchySnapshot.setRootTitle(currentScreenInfo.getTitle());
        renderingViewHierarchySnapshot.setRootPackageName(currentScreenInfo.getPackageName());

        //Set the event which triggered it
        if (accessibilityEventTrigger != null) {
            RenderingView lastViewClicked = getAccessibilityEventTriggerView(accessibilityEventTrigger, accessibilityEventSourceInfo);
            renderingViewHierarchySnapshot.setLastViewClicked(lastViewClicked);
            renderingViewHierarchySnapshot.setLastUIAction(AccessibilityEvent.eventTypeToString(accessibilityEventTrigger.getEventType()));
            Log.d("FVH", "Processing eventType: " + AccessibilityEvent.eventTypeToString(accessibilityEventTrigger.getEventType()));
            //Update last screen info
            //Put a hack that puts in lastScreenInfo instead of event screen info for more options
            //Use either packageName or
            ScreenTitleAndPackage eventScreenInfo = null;
            if (isSubSettingAndWindowStateChangedHack(accessibilityEventTrigger)) {
                eventScreenInfo = lastScreenInfo;
            } else {
                eventScreenInfo = windowIdToScreenInfo.get(accessibilityEventTrigger.getWindowId());

            }
            if (eventScreenInfo != null) {
                renderingViewHierarchySnapshot.setLastScreenTitle(eventScreenInfo.getTitle());
                renderingViewHierarchySnapshot.setLastScreenPackageName(eventScreenInfo.getPackageName());
            } else {
                renderingViewHierarchySnapshot.setLastScreenTitle(Utils.EMPTY_STRING);
                renderingViewHierarchySnapshot.setLastScreenPackageName(Utils.EMPTY_STRING);
            }
        }
        String clickedText = renderingViewHierarchySnapshot.lastViewClicked != null &&
                renderingViewHierarchySnapshot.lastViewClicked.getText() != null ?
                renderingViewHierarchySnapshot.lastViewClicked.getText() : Utils.EMPTY_STRING;
        Log.d(TAG, "JSONXX Sending to server, currentTitle: " +
                renderingViewHierarchySnapshot.rootTitle +
                " lastTitle: " +
                renderingViewHierarchySnapshot.lastScreenTitle +
                " action:" +
                renderingViewHierarchySnapshot.lastUIAction +
                " eventText" +
                clickedText);
        return Utils.gson.toJson(renderingViewHierarchySnapshot);
    }

    private boolean isSubSettingAndWindowStateChangedHack(AccessibilityEvent accessibilityEvent) {
        final String ANDROID_SUB_SETTING_CLASS = "com.android.settings.SubSettings";
        if (accessibilityEvent == null) {
            return false;
        }
        if (accessibilityEvent.getClassName() != null &&
                accessibilityEvent.getClassName().toString().equalsIgnoreCase(ANDROID_SUB_SETTING_CLASS) &&
                accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return true;
        }
        return false;
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

    private RenderingView getAccessibilityEventTriggerView(AccessibilityEvent accessibilityEvent,
                                                           AccessibilityNodeInfo accessibilityEventSourceInfo) {
        return getAccessibilityEventTriggerView(accessibilityEvent, accessibilityEventSourceInfo, Utils.EMPTY_STRING);
    }


    private RenderingView getAccessibilityEventTriggerView(AccessibilityEvent accessibilityEvent,
                                                           AccessibilityNodeInfo accessibilityEventSourceInfo,
                                                           String overWritePackageName) {
        if (accessibilityEvent == null) {
            return null;
        }
        String lastClassName = Utils.convertCharSeqToStringSafely(accessibilityEvent.getClassName());
        String lastPackageName = Utils.nullOrEmpty(overWritePackageName) ? Utils.convertCharSeqToStringSafely(accessibilityEvent.getPackageName()) : overWritePackageName;
        String lastContentDescription = Utils.convertCharSeqToStringSafely(accessibilityEvent.getContentDescription());
        String lastText = Utils.EMPTY_STRING;
        if (accessibilityEvent.getText() != null && ! accessibilityEvent.getText().isEmpty()) {
            lastText = Utils.convertCharSeqToStringSafely(accessibilityEvent.getText().get(0));
        }
        RenderingView renderingView = new RenderingView(lastClassName, lastPackageName, lastContentDescription, lastText);
        if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
                accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_VIEW_SELECTED) {
            renderingView.setTotalItems(accessibilityEvent.getItemCount());
            renderingView.setStartItemIndex(accessibilityEvent.getFromIndex());
            renderingView.setEndItemIndex(accessibilityEvent.getToIndex());
            renderingView.setCurrentItemIndex(accessibilityEvent.getCurrentItemIndex());
        }
        //Setting booleans
        if (accessibilityEventSourceInfo != null) {
            renderingView.setIsCheckable(accessibilityEventSourceInfo.isCheckable());
            renderingView.setIsClickable(accessibilityEventSourceInfo.isClickable());
            renderingView.setIsScrollable(accessibilityEventSourceInfo.isScrollable());
            renderingView.setIsSelected(accessibilityEventSourceInfo.isSelected());
            renderingView.setIsChecked(accessibilityEventSourceInfo.isChecked());
            renderingView.setIsEnabled(accessibilityEventSourceInfo.isEnabled());
        }
        return renderingView;
    }

    public void update(AccessibilityNodeInfo newRootNode,
                       AccessibilityEvent accessibilityEvent,
                       AccessibilityNodeInfo accessibilityEventSourceInfo) {
        if (newRootNode != null && rootNode != null && newRootNode.getWindowId() != rootNode.getWindowId()) {
            ScreenTitleAndPackage newScreenInfo = findAndUpdateScreenInfoFromRootNode(newRootNode);
            if (!newScreenInfo.isEmpty() && !newScreenInfo.equals(currentScreenInfo)) {
                lastScreenInfo = currentScreenInfo;
                Log.d("FVH", "updating last node since title changed from " + currentScreenInfo.toString() + " -> " + newScreenInfo.toString());
                lastScreenInfo = currentScreenInfo;
            }
        }
        viewDb.clear();
        textViewDb.clear();
        scrollableViews.clear();
        rootNode = newRootNode;
        accessibilityEventTrigger = accessibilityEvent;
        this.accessibilityEventSourceInfo = accessibilityEventSourceInfo;
    }

    /*
    public boolean updateNew(AccessibilityNodeInfo newRootNode,
                          AccessibilityEvent accessibilityEvent,
                          AccessibilityNodeInfo accessibilityEventSourceInfo) {
        if (accessibilityEvent == null || newRootNode == null || rootNode == null) {
            return false;
        }

        int accessibilityEventType = accessibilityEvent.getEventType();
        String newScreenTitle = Utils.findScreenTitleForNode(newRootNode);
        String newPackageName = newRootNode.getPackageName() != null ? newRootNode.getPackageName().toString() : Utils.EMPTY_STRING;
        switch (accessibilityEventType) {
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
            case AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED:
                //TODO: Check if this event has any text, if not bail
                //screen change triggers
                //Update screen info
                if ((!Utils.nullOrEmpty(newScreenTitle) && !newScreenTitle.equals(currentTitle)) ||
                        (!Utils.nullOrEmpty(newPackageName) && !newPackageName.equals(currentPackageName))) {
                    Log.d("FVH", "updating last node since title changed from " + currentTitle + " -> " +
                            newScreenTitle + " or pkg from " + currentPackageName + " -> " + newPackageName);
                    lastRootNodeId = rootNode.hashCode();
                    lastViewIdsDb = getViewIdsFromDb(viewDb);
                    lastScreenTitle = currentTitle;
                    lastPackageName = currentPackageName;
                }
                break;
            case AccessibilityEvent.TYPE_VIEW_SELECTED:
                //Seek bar triggers
                //Not handling seek bar for now
                break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                //Handle scrolling related triggers here
                //Scrolling triggers
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                //Edit text triggers
                //Not handling edit text for now
                break;
            default:
                //Ignore all other cases for now
                break;
        }
        viewDb.clear();
        textViewDb.clear();
        scrollableViews.clear();
        currentTitle = Utils.EMPTY_STRING;
        currentPackageName = Utils.EMPTY_STRING;
        rootNode = newRootNode;
        accessibilityEventTrigger = accessibilityEvent;
        this.accessibilityEventSourceInfo = accessibilityEventSourceInfo;
        return true;
    }
    */

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
