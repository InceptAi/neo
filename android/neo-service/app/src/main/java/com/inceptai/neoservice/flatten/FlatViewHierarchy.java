package com.inceptai.neoservice.flatten;

import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.inceptai.neopojos.CrawlingInput;
import com.inceptai.neopojos.RenderingView;
import com.inceptai.neoservice.Utils;
import com.inceptai.neoservice.uiactions.model.ScreenInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.content.ContentValues.TAG;
import static com.inceptai.neoservice.Utils.EMPTY_STRING;

/**
 * Created by arunesh on 6/30/17.
 */

public class FlatViewHierarchy {
    private static final boolean SHOULD_SEND_LAST_DIFFERENT_SCREEN_TITLE_ALWAYS = false;


    private AccessibilityNodeInfo rootNode;
    private FlatView rootNodeFlatView;
    private ScreenInfo currentScreenInfo;
    private ScreenInfo lastScreenInfo;
    private String appVersion;
    private String versionCode;

    private SparseArray<FlatView> viewDb;
    private DisplayMetrics displayMetrics;
    private SparseArray<FlatView> textViewDb;
    private SparseArray<FlatView> scrollableViews;
    private HashMap<String, ScreenInfo> appPackageNameToLatestScreenInfo;
    private AccessibilityEvent accessibilityEventTrigger;
    private AccessibilityNodeInfo accessibilityEventSourceInfo;
    private SparseArray<EventInfo> accessibilityEventInfoMap;
    private AtomicBoolean rootWindowChanged;
    private int lastRootWindowId;

    private class EventInfo {
        private RenderingView lastRenderingView;
        private long timestamp;
        private String eventTypeString;
        private int windowId;

        EventInfo(AccessibilityEvent accessibilityEvent,
                         AccessibilityNodeInfo accessibilityEventSourceInfo) {
            this.lastRenderingView = getAccessibilityEventTriggerView(accessibilityEvent, accessibilityEventSourceInfo);
            this.windowId = accessibilityEvent.getWindowId();
            this.timestamp = System.currentTimeMillis();
            this.eventTypeString = AccessibilityEvent.eventTypeToString(accessibilityEvent.getEventType());
        }

        @Override
        public String toString() {
            return "EventInfo{" +
                    "lastRenderingView=" + lastRenderingView +
                    ", timestamp=" + timestamp +
                    ", eventTypeString='" + eventTypeString + '\'' +
                    ", windowId=" + windowId +
                    '}';
        }
    }


    private SparseArray<ScreenInfo> windowIdToScreenInfo;


    public FlatViewHierarchy(AccessibilityNodeInfo rootNode,
                             @Nullable AccessibilityEvent accessibilityEvent,
                             @Nullable AccessibilityNodeInfo accessibilityEventSourceInfo,
                             String appVersion, String versionCode,
                             DisplayMetrics displayMetrics) {
        this.rootNode = rootNode;
        this.viewDb = new SparseArray<>();
        this.textViewDb = new SparseArray<>();
        this.scrollableViews = new SparseArray<>();
        this.accessibilityEventTrigger = accessibilityEvent;
        this.accessibilityEventSourceInfo = accessibilityEventSourceInfo;
        this.displayMetrics = displayMetrics;
        this.windowIdToScreenInfo = new SparseArray<>();
        this.appPackageNameToLatestScreenInfo = new HashMap<>();
        this.currentScreenInfo = new ScreenInfo();
        this.lastScreenInfo = new ScreenInfo();
        this.accessibilityEventInfoMap = new SparseArray<>();
        this.rootWindowChanged = new AtomicBoolean(false);
        this.lastRootWindowId = -1;
        this.appVersion = appVersion;
        this.versionCode = versionCode;
    }

    public void flatten() {
        if (rootNode == null) {
            Log.i(TAG, "Flatten failed coz of NULL rootnode.");
            return;
        }
        rootNodeFlatView = addNode(rootNode);
        List<FlatView> nodeQueue = new LinkedList<>();
        nodeQueue.add(rootNodeFlatView);
        Log.d(TAG, "In flatten with root node with window id: " + rootNode.getWindowId());
        currentScreenInfo = findAndUpdateScreenInfoFromRootNode(rootNode, appVersion, versionCode);
        while (!nodeQueue.isEmpty()) {
            FlatView flatView = nodeQueue.remove(0);
            addNode(flatView);
            traverseChildrenFor(flatView, nodeQueue);
            if (FlatViewUtils.shouldSendViewToServer(flatView)) {
                textViewDb.append(flatView.getHashKey(), flatView);
            } else if (flatView.getNodeInfo() != null && flatView.getNodeInfo().isScrollable()) {
                Log.i(Utils.TAG, "Potential scrollable view: " + flatView.getNodeInfo().toString());
                if (FlatViewUtils.isScrollableView(flatView)) {
                    Log.i(Utils.TAG, "SCROLLNEO Adding scrollable view .");
                    scrollableViews.append(flatView.getHashKey(), flatView);
                }
            } else {
                flatView.recycle();
            }
        }
    }

    public ScreenInfo findLatestScreenInfoForPackageName(String appPackageName) {
        return appPackageNameToLatestScreenInfo.get(appPackageName);
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

    private ScreenInfo findAndUpdateScreenInfoFromRootNode(AccessibilityNodeInfo rootNodeInfo,
                                                           String appVersion, String versionCode) {
        ScreenInfo screenInfo = null;
        if (rootNodeInfo != null) {
            screenInfo = windowIdToScreenInfo.get(rootNodeInfo.getWindowId());
            if (screenInfo == null) {
                //Not found, find it using traversal and update
                screenInfo = Utils.findScreenInfoForNode(rootNodeInfo, appVersion, versionCode, displayMetrics);
                if (!screenInfo.isTransitionScreen()) {
                    windowIdToScreenInfo.append(rootNodeInfo.getWindowId(), screenInfo);
                }
                Log.d(TAG, "JSONXX CREATED screen info in windowToScreenInfo for window ID: " + rootNodeInfo.getWindowId() + " , " + screenInfo.toString());
            } else {
                Log.d(TAG, "JSONXX FOUND screen info in windowToScreenInfo for window ID: " + rootNodeInfo.getWindowId() + " , " + screenInfo.toString());
            }
        }
        return screenInfo;
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
        CrawlingInput renderingViewHierarchySnapshot =
                new CrawlingInput(
                        Utils.convertPixelsToDp(displayMetrics.widthPixels, displayMetrics),
                        Utils.convertPixelsToDp(displayMetrics.heightPixels, displayMetrics),
                        Utils.createDeviceInfo());
        for (int i = 0; i < numViews; i ++) {
            FlatView flatView = viewDb.valueAt(i);
            if (flatView.getClassName() != null && flatView.getText() != null) {
                //Adding views with text.
                boolean isLLWithTVChild = isLayoutWithTextViewChild(flatView);
                if (FlatViewUtils.shouldSendViewToServer(flatView) || isLLWithTVChild) {
                    RenderingView renderingView = createRenderingView(flatView, displayMetrics, isLLWithTVChild);
                    renderingViewHierarchySnapshot.addView(String.valueOf(flatView.getHashKey()), renderingView);
                }
            }
        }
        //Set the crawling input properties
        renderingViewHierarchySnapshot.setRootTitle(currentScreenInfo.getTitle());
        renderingViewHierarchySnapshot.setRootSubTitle(currentScreenInfo.getSubTitle());
        renderingViewHierarchySnapshot.setRootPackageName(currentScreenInfo.getPackageName());
        renderingViewHierarchySnapshot.setCurrentScreenType(currentScreenInfo.getScreenType());
        renderingViewHierarchySnapshot.setAppVersion(currentScreenInfo.getAppVersion());
        renderingViewHierarchySnapshot.setVersionCode(currentScreenInfo.getVersionCode());


        //Set the event which triggered it
        if (accessibilityEventTrigger != null) {

            ScreenInfo eventScreenInfo;
            EventInfo eventInfo = getEventInfoForWindow(lastRootWindowId);
            //TODO: Don't suppress events here -- send up two events when this happens
            if (!hasText(accessibilityEventTrigger) && eventInfo != null && rootWindowChanged.getAndSet(false)) {
                renderingViewHierarchySnapshot.setLastViewClicked(eventInfo.lastRenderingView);
                renderingViewHierarchySnapshot.setLastUIAction(eventInfo.eventTypeString);
                Log.d("FVH", "Root window changed and found an event with text in last 500 ms: " + eventInfo.toString());
                eventScreenInfo = windowIdToScreenInfo.get(lastRootWindowId);
            } else {
                RenderingView lastViewClicked = getAccessibilityEventTriggerView(accessibilityEventTrigger, accessibilityEventSourceInfo);
                renderingViewHierarchySnapshot.setLastViewClicked(lastViewClicked);
                renderingViewHierarchySnapshot.setLastUIAction(AccessibilityEvent.eventTypeToString(accessibilityEventTrigger.getEventType()));
                Log.d("FVH", "Processing eventTypeString: " + AccessibilityEvent.eventTypeToString(accessibilityEventTrigger.getEventType()));
                //Update last screen info
                if (SHOULD_SEND_LAST_DIFFERENT_SCREEN_TITLE_ALWAYS || isWindowStateChangedHack(accessibilityEventTrigger)) {
                    eventScreenInfo = lastScreenInfo;
                } else {
                    eventScreenInfo = windowIdToScreenInfo.get(accessibilityEventTrigger.getWindowId());
                }
            }

            if (eventScreenInfo == null) {
                eventScreenInfo = new ScreenInfo();
            }

            renderingViewHierarchySnapshot.setLastScreenTitle(eventScreenInfo.getTitle());
            renderingViewHierarchySnapshot.setLastScreenSubTitle(eventScreenInfo.getSubTitle());
            renderingViewHierarchySnapshot.setLastScreenPackageName(eventScreenInfo.getPackageName());
            renderingViewHierarchySnapshot.setLastScreenType(eventScreenInfo.getScreenType());
        }

        String clickedText = renderingViewHierarchySnapshot.getLastViewClicked() != null &&
                renderingViewHierarchySnapshot.getLastViewClicked().getText() != null ?
                renderingViewHierarchySnapshot.getLastViewClicked().getText() : Utils.EMPTY_STRING;
        Log.d(TAG, "JSONXX Sending to server, currentTitle: " +
                renderingViewHierarchySnapshot.getRootTitle() +
                " current subTitle:" +
                renderingViewHierarchySnapshot.getRootSubTitle() +
                " current screenType:" +
                renderingViewHierarchySnapshot.getCurrentScreenType() +
                " lastTitle: " +
                renderingViewHierarchySnapshot.getLastScreenTitle() +
                " last subTitle:" +
                renderingViewHierarchySnapshot.getLastScreenSubTitle() +
                " last screenType:" +
                renderingViewHierarchySnapshot.getLastScreenType() +
                " action:" +
                renderingViewHierarchySnapshot.getLastUIAction() +
                " eventText" +
                clickedText);
        return Utils.gson.toJson(renderingViewHierarchySnapshot);
    }

    private EventInfo getEventInfoForWindow(int windowId) {
        final long MAX_DURATION_FOR_CHECKING_EVENTS_MS = 500;
        EventInfo eventInfo = accessibilityEventInfoMap.get(windowId);
        if (eventInfo != null && System.currentTimeMillis() -  eventInfo.timestamp <= MAX_DURATION_FOR_CHECKING_EVENTS_MS) {
            return eventInfo;
        } else {
            return null;
        }
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

    private boolean isWindowStateChangedHack(AccessibilityEvent accessibilityEvent) {
        if (accessibilityEvent == null) {
            return false;
        }
        if (accessibilityEvent.getClassName() != null &&
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
            //TODO: Add all the text from type view clicked here
            lastText = Utils.convertCharSeqToStringSafely(accessibilityEvent.getText().get(0));
        } else if (accessibilityEvent.getContentDescription() != null) {
            lastText = Utils.convertCharSeqToStringSafely(accessibilityEvent.getContentDescription().toString());
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
                       AccessibilityNodeInfo accessibilityEventSourceInfo,
                       String appVersion,
                       String versionCode) {
        ScreenInfo newScreenInfo = new ScreenInfo();
        if (newRootNode != null) {
            //TODO remove this hack
            newScreenInfo = findAndUpdateScreenInfoFromRootNode(newRootNode, appVersion, versionCode);
            Log.d(TAG, "In update, newRoot ID is " + newRootNode.getWindowId() + " , new screen info is " +  newScreenInfo.toString());
            if (accessibilityEvent != null) {
                Log.d(TAG, "In update, accessibility event is " + accessibilityEvent);
            }
        }
        if (newRootNode != null && rootNode != null && newRootNode.getWindowId() != rootNode.getWindowId()) {
            //TODO update current screen info here to prevent two traversals
            //ScreenInfo newScreenInfo = findAndUpdateScreenInfoFromRootNode(newRootNode);
            Log.d(TAG, "In update, new screen info is " + newScreenInfo.toString());
            if (!newScreenInfo.isEmpty() && !newScreenInfo.isTransitionScreen() && !newScreenInfo.equals(currentScreenInfo)) {
                Log.d("FVH", "JSONXX updating last node since title changed from " + currentScreenInfo.toString() + " -> " + newScreenInfo.toString());
                lastScreenInfo = currentScreenInfo;
                appPackageNameToLatestScreenInfo.put(newScreenInfo.getPackageName(), newScreenInfo);
                rootWindowChanged.set(true);
                lastRootWindowId = rootNode.getWindowId();
            } else {
                Log.d("FVH", "JSONXX Not updating last screenInfo since newScreenInfo is same as currentScreenInfo: " + currentScreenInfo.toString() + " -> " + newScreenInfo.toString());
            }
        } else {
            if (newRootNode != null && rootNode != null) {
                Log.d("FVH", "JSONXX Not updating last node since newRootWindowId is same as OldRootWindowId = " + rootNode.getWindowId());
            } else {
                Log.d("FVH", "JSONXX newRootNode is null or rootNode is null");
            }
        }

        //Add the event info -- if it contains text to eventInfoMap
        if (accessibilityEvent != null &&
                accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED &&
                accessibilityEvent.getText() != null) {
            EventInfo eventInfo = new EventInfo(accessibilityEvent, accessibilityEventSourceInfo);
            accessibilityEventInfoMap.put(accessibilityEvent.getWindowId(), eventInfo);
            Log.d("FVH", "JSONXX Adding eventInfo  " + eventInfo.toString() + " ,for windowId: " + eventInfo.windowId);
        }

        if (newRootNode == null) {
            Log.d("FVH", "JSONXX rootNode is null, so returning");
            return;
        }

        if (newScreenInfo.isTransitionScreen()) {
            Log.d("FVH", "In update, JSONXX new screen info title is loading..., so returning null ");
            return;
        }


        viewDb.clear();
        textViewDb.clear();
        scrollableViews.clear();
        rootNode = newRootNode;
        accessibilityEventTrigger = accessibilityEvent;
        this.appVersion = appVersion;
        this.versionCode = versionCode;
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
            Log.i(Utils.TAG, "IMAGE BUTTON: resource: " + flatView.getContentDescription());
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


    private static String getKeyForEventHashMap(int eventType, int windowId) {
        return AccessibilityEvent.eventTypeToString(eventType) + ":" + String.valueOf(windowId);
    }

    private static boolean hasText(AccessibilityEvent accessibilityEvent) {
        if (accessibilityEvent == null) {
            return false;
        }
        if (accessibilityEvent.getText() != null && !accessibilityEvent.getText().isEmpty()) {
            return true;
        }
        if (accessibilityEvent.getContentDescription() != null) {
            return true;
        }
        return false;
    }

    private static RenderingView createRenderingView(FlatView flatView, DisplayMetrics displayMetrics, boolean isParentOfClickableView) {
        Rect bounds = flatView.getBoundsInScreen();
        int leftX = 0, topY = 0, bottomY = 0, rightX = 0;
        if (bounds != null) {
            topY = Utils.convertPixelsToDp(bounds.top, displayMetrics);
            bottomY = Utils.convertPixelsToDp(bounds.bottom, displayMetrics);
            leftX = Utils.convertPixelsToDp(bounds.left, displayMetrics);
            rightX = Utils.convertPixelsToDp(bounds.right, displayMetrics);
        }

        return new RenderingView(
                String.valueOf(flatView.getParentViewId()), //Parent view Id
                String.valueOf(flatView.getHashKey()),     //flat view Id
                flatView.getPackageName(), //pkgname
                flatView.getClassName(),
                flatView.getContentDescription(),
                flatView.getText(),
                flatView.getViewIdResourceName(),
                leftX,
                rightX,
                topY,
                bottomY,
                isParentOfClickableView,
                flatView.isChecked(),
                flatView.isClickable(),
                flatView.isCheckable(),
                flatView.isScrollable(),
                flatView.isSelected());
    }

}
