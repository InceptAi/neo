package com.inceptai.neoservice.flatten;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.neopojos.ActionDetails;
import com.inceptai.neopojos.ElementIdentifier;
import com.inceptai.neopojos.NavigationIdentifier;
import com.inceptai.neopojos.ScreenIdentifier;
import com.inceptai.neoservice.NeoThreadpool;
import com.inceptai.neoservice.NeoUiActionsService;
import com.inceptai.neoservice.Utils;
import com.inceptai.neoservice.uiactions.UIActionResult;
import com.inceptai.neoservice.uiactions.model.ScreenInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import static com.inceptai.neoservice.Utils.SETTINGS_PACKAGE_NAME;
import static com.inceptai.neoservice.Utils.TAG;
import static com.inceptai.neoservice.Utils.findUIElement;

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

    //Different UI actions
    private static final String TOGGLE_ACTION = "TOGGLE";
    private static final String SEEK_ACTION = "SEEK";
    private static final String SUBMIT_ACTION = "SUBMIT";

    //Delay
    private static final int SCREEN_TRANSITION_DELAY_MS = 500;
    private static final int SCROLL_TRANSITION_DELAY_MS = 400;
    private static final boolean SCROLL_WHEN_FINDING_UI_ELEMENTS_FOR_ACTION = true;
    private static final boolean SCROLL_WHEN_FINDING_UI_SCREEN_FOR_NAVIGATION = true;
    private static final boolean CHECK_CONDITION_AFTER_CLICKING = false;
    private static final boolean FINALIZE_ACTION_BY_ACTING_ON_PARTIAL_SCREEN = true;
    public static final String[] FINALIZE_ACTION_KEYWORDS = {
            "ok",
            "sure",
            "agree",
            "yes"
    };
    private NeoUiActionsService neoService;
    private NeoThreadpool neoThreadpool;
    private DisplayMetrics primaryDisplayMetrics;
    private FlatViewHierarchy flatViewHierarchy;
    private SettableFuture<ScreenInfo> screenTitleFuture;

    public UiManager(NeoUiActionsService neoService, NeoThreadpool neoThreadpool, DisplayMetrics displayMetrics) {
        this.neoService = neoService;
        this.neoThreadpool = neoThreadpool;
        this.primaryDisplayMetrics = displayMetrics;
    }

    public void processClick(String viewId) {
        if (flatViewHierarchy == null) {
            Log.e(TAG, "Unable to process click event, NULL view hierarchy.");
            return;
        }
        FlatView flatView = flatViewHierarchy.getFlatViewFor(viewId);
        AccessibilityNodeInfo nodeInfo = flatView.getNodeInfo();
        if (nodeInfo == null) {
            Log.e(TAG, "Null NodeInfo for click event.");
            return;
        }
        Log.i(TAG, "Sending CLICK event for view: " + flatView.getText() + " class: " + flatView.getClassName());
        if (!performHierarchicalClick(nodeInfo)) {
            Log.e(TAG, "Unable to perform action for some reason.");
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

    private boolean performScroll(boolean forward) {
        Log.i(TAG, "SCROLLNEO Attempting scroll: " + (forward ? "UP" : "DOWN"));
        AccessibilityNodeInfo nodeInfo = flatViewHierarchy.findScrollableFlatView();
        if (nodeInfo == null) {
            Log.e(TAG, "SCROLLNEO Could not find scrollable view.");
            return false;
        }
        boolean result = false;
        if (nodeInfo.isScrollable()) {
            Log.i(TAG, "SCROLLNEO Trying scroll for: " + nodeInfo.toString());
            result = nodeInfo.performAction(forward ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        }
        if (!result) {
            Log.i(TAG, "SCROLLNEO Scroll failed.");
        }
        return result;
    }

    public FlatViewHierarchy updateViewHierarchy(AccessibilityNodeInfo rootNode,
                                                 AccessibilityEvent accessibilityEvent,
                                                 AccessibilityNodeInfo eventSourceInfo) {
        if (accessibilityEvent != null) {
            Log.d(TAG, "In updateViewHierarchy, processing event: "  + accessibilityEvent);
        }


        String rootPackageName = rootNode != null && rootNode.getPackageName() != null ?
                rootNode.getPackageName().toString() : Utils.EMPTY_STRING;
        String appVersion = Utils.findAppVersionForPackageName(neoService.getApplicationContext(), rootPackageName);
        String versionCode = Utils.findVersionCodeForPackageName(neoService.getApplicationContext(), rootPackageName);

        if (flatViewHierarchy == null) {
            flatViewHierarchy = new FlatViewHierarchy(
                    rootNode,
                    accessibilityEvent,
                    eventSourceInfo,
                    appVersion,
                    versionCode,
                    primaryDisplayMetrics);
            Log.d(TAG, "In updateViewHierarchy, initializing new FVH");
        } else {
            Log.d(TAG, "In updateViewHierarchy, Calling update on FVH");
            flatViewHierarchy.update(rootNode, accessibilityEvent, eventSourceInfo, appVersion, versionCode);
        }

        if (rootNode == null) {
            Log.d(TAG, "In updateViewHierarchy, returning because rootNode is null");
            return null;
        }

        flatViewHierarchy.flatten();
        return flatViewHierarchy;
    }

    public ListenableFuture<ScreenInfo> launchAppAndReturnScreenTitle(
            Context context,
            final String appPackageName,
            boolean forceAppRelaunch) {
        //Navigate to the app
        if (screenTitleFuture != null && !screenTitleFuture.isDone()) {
            return screenTitleFuture;
        }
        screenTitleFuture = SettableFuture.create();
        boolean isCurrentScreenForTargetPackage =
                flatViewHierarchy != null && flatViewHierarchy.checkIfCurrentScreenBelongsToPackage(appPackageName);
        if (isCurrentScreenForTargetPackage && !forceAppRelaunch) {
            //already on target screen, set the future here.
            Log.v(TAG, "ESXXX Already on the right app, so returning current screen info: " + flatViewHierarchy.findCurrentScreenInfo().toString());
            screenTitleFuture.set(flatViewHierarchy.findCurrentScreenInfo());
        } else {
            boolean launched = Utils.launchAppIfInstalled(context, appPackageName);
            if (!launched) {
                Log.v(TAG, "ESXXX Unable to launch app, so empty screen info");
                screenTitleFuture.set(new ScreenInfo());
            } else {
                //Wait for screen delay transition and check
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        // this code will be executed after 2 seconds
                        checkScreenTransitionState(appPackageName);
                    }
                }, SCREEN_TRANSITION_DELAY_MS);
            }
        }
        return screenTitleFuture;
    }


    private void checkScreenTransitionState(String appPackageName) {
        Log.v(TAG, "ESXXX Coming in checkTransitionState for pkg " + appPackageName);
        if (flatViewHierarchy != null) {
            ScreenInfo appScreenInfo = flatViewHierarchy.findLatestScreenInfoForPackageName(appPackageName);
            if (screenTitleFuture != null) {
                if (appScreenInfo != null && !screenTitleFuture.isDone()) {
                    Log.v(TAG, "ESXXX setting screenTitleFuture with info: " +  appScreenInfo.toString() + " pkg: " + appPackageName);
                    screenTitleFuture.set(appScreenInfo);
                } else {
                    Log.v(TAG, "ESXXX App screeninfo is " + (appScreenInfo == null ? "null" : "not null"));
                    Log.v(TAG, "ESXXX Screen title future is  " + (screenTitleFuture.isDone() ? "Done" : "not done"));
                    Log.v(TAG, "ESXXX Setting screen title future to empty");
                    screenTitleFuture.set(new ScreenInfo());
                }
            }
        } else {
            Log.v(TAG, "ESXXX Null flat view hierarchy, so returning empty screen info");
            screenTitleFuture.set(new ScreenInfo());
        }
    }

    public UIActionResult takeUIAction(ActionDetails actionDetails, Context context, String query, String packageName) {
        UIActionResult uiActionResult = new UIActionResult(query, packageName);
        Log.d(TAG, "SCROLLNEO In UIManager, takeActions");
        if (actionDetails == null || actionDetails.getActionIdentifier() == null) {
            uiActionResult.setStatus(UIActionResult.UIActionResultCodes.NO_ACTIONS_AVAILABLE);
            return uiActionResult;
        }

        String packageNameForAction = Utils.EMPTY_STRING;
        if (actionDetails.getNavigationIdentifierList() != null && !actionDetails.getNavigationIdentifierList().isEmpty()) {
            NavigationIdentifier firstStep = actionDetails.getNavigationIdentifierList().get(0);
            if (firstStep != null) {
                packageNameForAction = firstStep.getSrcScreenIdentifier().getPackageName();
            }
        } else {
            packageNameForAction = actionDetails.getActionIdentifier().getScreenIdentifier().getPackageName();
        }

        if (Utils.nullOrEmpty(packageNameForAction)) {
            uiActionResult.setStatus(UIActionResult.UIActionResultCodes.INVALID_ACTION_DETAIL);
            return uiActionResult;
        }

        //Perform navigation
        //first check if the current package name is same as packageNameForAction, if not launch the package
        if (packageName.equalsIgnoreCase(SETTINGS_PACKAGE_NAME)) {
            //Settings package -- launch settings
            ListenableFuture<ScreenInfo> screenLaunchFuture = launchAppAndReturnScreenTitle(context, packageName, true);
            try {
                screenLaunchFuture.get();
            } catch (InterruptedException|ExecutionException e) {
                uiActionResult.setStatus(UIActionResult.UIActionResultCodes.EXCEPTION_WHILE_WAITING_FOR_SCREEN_TRANSITION);
                return uiActionResult;
            }
        }

        boolean navigationResult = navigateToScreen(actionDetails.getNavigationIdentifierList());
        if (!navigationResult) {
            uiActionResult.setStatus(UIActionResult.UIActionResultCodes.NAVIGATION_FAILURE);
            return uiActionResult;
        }

        //Navigation went fine -- now execute the UI Action
        ScreenIdentifier screenIdentifier = actionDetails.getActionIdentifier().getScreenIdentifier();
        ElementIdentifier elementIdentifier = actionDetails.getActionIdentifier().getElementIdentifier();
        String actionToTake = actionDetails.getActionIdentifier().getActionToTake();
        String successConditionText = actionDetails.getSuccessCondition() != null ?
                actionDetails.getSuccessCondition().getTextToMatch() : Utils.EMPTY_STRING;
        return executeUIAction(
                screenIdentifier.getTitle(),
                screenIdentifier.getSubTitle(),
                screenIdentifier.getPackageName(),
                elementIdentifier.getClassName(),
                elementIdentifier.getPackageName(),
                elementIdentifier.getKeywordList(),
                actionToTake,
                successConditionText,
                query,
                packageName);
    }

    public boolean matchScreenWithScrolling(String screenTitle,
                                            String screenSubTitle,
                                            String screenPackageName,
                                            AccessibilityNodeInfo nodeInfo,
                                            boolean checkSubtitle) {
        Log.d(TAG, "SCROLLNEO matchScreenWithScrolling for title: " + screenTitle);
        //Just scroll up if possible and then do the match
        boolean matchInCurrentState = Utils.matchScreenWithRootNode(
                screenTitle, screenSubTitle, screenPackageName, nodeInfo, checkSubtitle);

        if (matchInCurrentState) {
            return true;
        } else if (!SCROLL_WHEN_FINDING_UI_SCREEN_FOR_NAVIGATION) {
            return false;
        }

        //Perform backward scroll till possible
        Log.d(TAG, "SCROLLNEO matchScreenWithScrolling for title: " + screenTitle + " trying backward scroll");
        int numScroll = 1;
        AccessibilityNodeInfo scrolledNodeInfo = null;
        while(performScroll(false)) {
            Log.d(TAG, "SCROLLNEO matchScreenWithScrolling for title: " + screenTitle + " backward scroll num " + numScroll++);
            waitForScreenTransition(SCROLL_TRANSITION_DELAY_MS);
            scrolledNodeInfo = neoService.getRootInActiveWindow();
            if (scrolledNodeInfo == null) {
                //Found null scrollable view -- break;
                Log.d(TAG, "SCROLLNEO matchScreenWithScrolling for title: " + screenTitle + " found null scrollable view so bailing");
                break;
            }
        }
        return Utils.matchScreenWithRootNode(
                screenTitle,
                screenSubTitle,
                screenPackageName,
                scrolledNodeInfo,
                checkSubtitle);
    }

    private UIActionResult executeUIAction(String screenTitle, String screenSubTitle,
                                    String screenPackageName, String elementClassName,
                                    String elementPackageName, List<String> keyWordList,
                                    String actionName, String textAfterSuccessfulAction,
                                    String originalQuery, String packageName) {
        UIActionResult uiActionResult = new UIActionResult(originalQuery, packageName);
        AccessibilityNodeInfo currentNodeInfo = neoService.getRootInActiveWindow();
        if (currentNodeInfo == null) {
            uiActionResult.setStatus(UIActionResult.UIActionResultCodes.ROOT_WINDOW_IS_NULL);
            return uiActionResult;
        }
        //Check if we are on the right screen
        if (!matchScreenWithScrolling(
                screenTitle,
                screenSubTitle,
                screenPackageName,
                currentNodeInfo,
                false)) {
            uiActionResult.setStatus(UIActionResult.UIActionResultCodes.FINAL_ACTION_SCREEN_MATCH_FAILED);
            return uiActionResult;
        }

        //TODO: Handle all types of UIActions
        if (!actionName.equalsIgnoreCase(TOGGLE_ACTION)) {
            uiActionResult.setStatus(UIActionResult.UIActionResultCodes.NON_TOGGLE_ACTION);
            return uiActionResult;
        }

        //Find the clickable element info for action
        AccessibilityNodeInfo elementInfo = findUIElementWithScrolling(
                elementClassName,
                elementPackageName,
                keyWordList,
                currentNodeInfo,
                true,
                SCROLL_WHEN_FINDING_UI_ELEMENTS_FOR_ACTION);
//        AccessibilityNodeInfo elementInfo = findUIElement(elementClassName,
//                elementPackageName, keyWordList, currentNodeInfo, true);
        if (elementInfo == null) {
            uiActionResult.setStatus(UIActionResult.UIActionResultCodes.FINAL_ACTION_ELEMENT_NOT_FOUND);
            return uiActionResult;
        }

        if (!elementInfo.isCheckable() && !elementInfo.isClickable()) {
            uiActionResult.setStatus(UIActionResult.UIActionResultCodes.ACTION_NOT_PERMITTED);
            return uiActionResult;
        }

        //Check success condition to see if we even need to take an action
        if (Utils.checkCondition(textAfterSuccessfulAction, elementInfo)) {
            uiActionResult.setStatus(UIActionResult.UIActionResultCodes.SUCCESS);
            return uiActionResult;
        }

        performHierarchicalClick(elementInfo);
        waitForScreenTransition();

        //Never leave the user on a partial screen -- see if the new screen on transition is partial -- click on yes/agree/ok button
        if (FINALIZE_ACTION_BY_ACTING_ON_PARTIAL_SCREEN) {
            currentNodeInfo.recycle();
            currentNodeInfo = neoService.getRootInActiveWindow();
            ScreenInfo currentScreenInfo = Utils.findScreenInfoForNode(currentNodeInfo, Utils.EMPTY_STRING, Utils.EMPTY_STRING, primaryDisplayMetrics);
            //if current node is partial
            if (currentScreenInfo.isPartialScreen() && !currentScreenInfo.getTitle().equalsIgnoreCase(screenTitle)) {
                for (String finalizeCommand: FINALIZE_ACTION_KEYWORDS) {
                    elementInfo = findUIElementWithScrolling(
                            Arrays.asList(
                                    FlatViewUtils.BUTTON_CLASS_NAME,
                                    FlatViewUtils.IMAGE_BUTTON_CLASSNAME,
                                    FlatViewUtils.TEXT_VIEW_CLASSNAME,
                                    FlatViewUtils.IMAGE_VIEW_CLASSNAME),
                            elementPackageName,
                            Arrays.asList(finalizeCommand),
                            currentNodeInfo,
                            true,
                            false);
                    if (elementInfo != null) {
                        performHierarchicalClick(elementInfo);
                        waitForScreenTransition();
                        break;
                    }
                }
            }
        }

        //Refresh the views --
        if (CHECK_CONDITION_AFTER_CLICKING) {
            //currentScreenInfo.refresh(); //TODO switch to API level 18 so we can just use refresh

            currentNodeInfo.recycle();
            currentNodeInfo = neoService.getRootInActiveWindow();
            if (currentNodeInfo == null) {
                uiActionResult.setStatus(UIActionResult.UIActionResultCodes.ROOT_WINDOW_IS_NULL);
                return uiActionResult;
            }
            //elementInfo = findUIElement(elementClassName, elementPackageName, keyWordList, currentNodeInfo, true);
            elementInfo = findUIElementWithScrolling(
                    elementClassName,
                    elementPackageName,
                    keyWordList,
                    currentNodeInfo,
                    true,
                    SCROLL_WHEN_FINDING_UI_ELEMENTS_FOR_ACTION);

            //Check condition again to see if it worked
            // TODO make sure the element info is still relevant -- do we need to get another one ?
            //Unable to get success condition even after taking the action -- mark as failure\
            if (!Utils.checkCondition(textAfterSuccessfulAction, elementInfo)) {
                uiActionResult.setStatus(UIActionResult.UIActionResultCodes.FINAL_ACTION_SUCCESS_CONDITION_NOT_MET);
                return uiActionResult;
            }
        }
        currentNodeInfo.recycle();
        uiActionResult.setStatus(UIActionResult.UIActionResultCodes.SUCCESS);
        return uiActionResult;
    }

    private AccessibilityNodeInfo performScrollAndElementSearch(List<String> elementClassNames,
                                                                String elementPackageName,
                                                                List<String> keyWords,
                                                                boolean isClickable,
                                                                boolean forwardScroll) {
        AccessibilityNodeInfo scrolledNodeInfo;
        Log.d(TAG, "SCROLLNEO performScrollAndElementSearch with forwardScroll: " + forwardScroll);
        int scrollNumber = 1;
        while(performScroll(forwardScroll)) {
            Log.d(TAG, "SCROLLNEO performScrollAndElementSearch Scroll : " + (forwardScroll ? "forward" : "backward") + " scroll num" + scrollNumber++);
            waitForScreenTransition(SCROLL_TRANSITION_DELAY_MS);
            scrolledNodeInfo = neoService.getRootInActiveWindow();
            if (scrolledNodeInfo != null) {
                AccessibilityNodeInfo elementInfoAfterScrolling = findUIElement(
                        elementClassNames,
                        elementPackageName,
                        keyWords,
                        scrolledNodeInfo,
                        isClickable);
                if (elementInfoAfterScrolling != null) {
                    //Found element -- return
                    return elementInfoAfterScrolling;
                }
                scrolledNodeInfo.recycle();
            } else {
                //Found null scrollable view -- break;
                Log.d(TAG, "SCROLLNEO performScrollAndElementSearch breaking because root window is null, scroll type " + forwardScroll);
                break;
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findUIElementWithScrolling(String elementClassName,
                                                             String elementPackageName,
                                                             List<String> keyWords,
                                                             AccessibilityNodeInfo nodeInfo,
                                                             boolean isClickable,
                                                             boolean shouldScroll) {
        return findUIElementWithScrolling(Arrays.asList(elementClassName), elementPackageName, keyWords, nodeInfo, isClickable, shouldScroll);
    }


    private AccessibilityNodeInfo findUIElementWithScrolling(List<String> elementClassNames,
                                                             String elementPackageName,
                                                             List<String> keyWords,
                                                             AccessibilityNodeInfo nodeInfo,
                                                             boolean isClickable,
                                                             boolean shouldScroll) {
        Log.d(TAG, "SCROLLNEO findUIElementWithScrolling with keywords " + (keyWords != null && !keyWords.isEmpty() ? keyWords.get(0) : " emtpy"));
        AccessibilityNodeInfo elementInfo = findUIElement(elementClassNames, elementPackageName, keyWords, nodeInfo, isClickable);
        if (elementInfo != null || !shouldScroll) {
            //Found element or scrolling is disabled -- return
            return elementInfo;
        }

        //Didn't find element -- scroll to see if we can find it.
        //First scroll down -- till you can't anymore
        Log.d(TAG, "SCROLLNEO findUIElementWithScrolling couldn't find element, trying forward scroll");
        elementInfo = performScrollAndElementSearch(elementClassNames, elementPackageName, keyWords, isClickable, true);
        if (elementInfo != null) {
            //Not a scrollable element.
            return elementInfo;
        }

        Log.d(TAG, "SCROLLNEO findUIElementWithScrolling couldn't find element, trying reverse scroll");
        elementInfo = performScrollAndElementSearch(elementClassNames, elementPackageName, keyWords, isClickable, false);
        if (elementInfo != null) {
            //Not a scrollable element.
            return elementInfo;
        }

        return null;

    }


    private boolean navigateToScreen(List<NavigationIdentifier> navigationIdentifierList) {
        if (navigationIdentifierList == null) {
            return true;
        }
        //Navigate to the right
        AccessibilityNodeInfo currentScreenInfo = neoService.getRootInActiveWindow();
        if (currentScreenInfo == null) {
            return false;
        }

        for (NavigationIdentifier navigationIdentifier: navigationIdentifierList) {

            //match the starting screen with current root node info
            if (!matchScreenWithScrolling(
                    navigationIdentifier.getSrcScreenIdentifier().getTitle(),
                    navigationIdentifier.getSrcScreenIdentifier().getSubTitle(),
                    navigationIdentifier.getSrcScreenIdentifier().getPackageName(),
                    currentScreenInfo, false)) {
                currentScreenInfo.recycle();
                return false;
            }

//            if (!Utils.matchScreenWithRootNode(
//                    navigationIdentifier.getSrcScreenIdentifier().getTitle(),
//                    navigationIdentifier.getSrcScreenIdentifier().getSubTitle(),
//                    navigationIdentifier.getSrcScreenIdentifier().getPackageName(),
//                    currentScreenInfo, false)) {
//                currentScreenInfo.recycle();
//                return false;
//            }
            //Find the element to click for navigation
//            AccessibilityNodeInfo elementInfo = findUIElement(
//                    navigationIdentifier.getElementIdentifier().getClassName(),
//                    navigationIdentifier.getElementIdentifier().getPackageName(),
//                    navigationIdentifier.getElementIdentifier().getKeywordList(),
//                    currentScreenInfo,
//                    true);
            AccessibilityNodeInfo elementInfo = findUIElementWithScrolling(
                    navigationIdentifier.getElementIdentifier().getClassName(),
                    navigationIdentifier.getElementIdentifier().getPackageName(),
                    navigationIdentifier.getElementIdentifier().getKeywordList(),
                    currentScreenInfo,
                    true,
                    SCROLL_WHEN_FINDING_UI_ELEMENTS_FOR_ACTION);


            //Only 1 should match -- //TODO: handle cases where more than 1 elements match text
            if (elementInfo == null) {
                currentScreenInfo.recycle();
                return false;
            }

            //Perform the click
            performHierarchicalClick(elementInfo);
            waitForScreenTransition();

            // /Update the screen info
            currentScreenInfo.recycle();
            currentScreenInfo = neoService.getRootInActiveWindow();
            if (currentScreenInfo == null) {
                return false;
            }

            //Check if arrived at the right destination

            if (!matchScreenWithScrolling(
                    navigationIdentifier.getDstScreenIdentifier().getTitle(),
                    navigationIdentifier.getDstScreenIdentifier().getSubTitle(),
                    navigationIdentifier.getDstScreenIdentifier().getPackageName(),
                    currentScreenInfo, false)) {
                currentScreenInfo.recycle();
                return false;
            }

//            if (!Utils.matchScreenWithRootNode(
//                    navigationIdentifier.getDstScreenIdentifier().getTitle(),
//                    navigationIdentifier.getDstScreenIdentifier().getSubTitle(),
//                    navigationIdentifier.getDstScreenIdentifier().getPackageName(),
//                    currentScreenInfo, false)) {
//                currentScreenInfo.recycle();
//                return false;
//            }
        }

        //Navigation went fine
        currentScreenInfo.recycle();
        return true;

    }


    public void takeAction(JSONObject actionJson) {
        String action = Utils.EMPTY_STRING;
        try {
            action = actionJson.getString(GLOBAL_ACTION_KEY);
        } catch (JSONException e) {
            Log.e(TAG, "Json error: " + e);
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
                neoService.refreshFullUi(null, null);
            } else if (SCROLLDOWN_ACTION.equals(action)) {
                performScroll(false /* down */);
            } else if (SCROLLUP_ACTION.equals(action)) {
                performScroll(true /* up */);
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

    private void waitForScreenTransition() {
        waitForScreenTransition(SCREEN_TRANSITION_DELAY_MS);
    }

    private void waitForScreenTransition(int delayMs) {
        try {
            Thread.sleep(delayMs);
        }catch (InterruptedException e) {
            Log.e("UIManager", "Exception while waiting");
        }
    }
}
