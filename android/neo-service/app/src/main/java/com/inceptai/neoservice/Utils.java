package com.inceptai.neoservice;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.common.collect.MinMaxPriorityQueue;
import com.google.gson.Gson;
import com.inceptai.neoservice.flatten.FlatViewUtils;
import com.inceptai.neoservice.uiactions.model.ScreenInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by arunesh on 6/29/17.
 */

public class Utils {
    public static final String TAG = "NeoUiActionsService";
    public static final String PREFERENCES_FILE = "NeoWifiExpert_preferences";
    public static final String EMPTY_STRING = "";
    public static final String MULTIPLE_WORD_MATCH_DELIMITER = "#";
    public static final String WIFI_SANITIZED = "wifi";
    public static final char WIFI_ACCESSIBILITY_HYPHEN = (char)8209;
    public static final String WIFI_ACCESSIBILITY = "Wi" + WIFI_ACCESSIBILITY_HYPHEN + "Fi";
    public static final String SETTINGS_APP_NAME = "SETTINGS";
    public static final String SETTINGS_PACKAGE_NAME = "com.android.settings";


    public static Gson gson = new Gson();

    private Utils() {}

    public static void logNodeHierarchy(AccessibilityNodeInfo nodeInfo, int depth) {
        Rect bounds = new Rect();
        nodeInfo.getBoundsInScreen(bounds);

        StringBuilder sb = new StringBuilder();
        if (depth > 0) {
            for (int i=0; i<depth; i++) {
                sb.append("  ");
            }
            sb.append("\u2514 ");
        }
        sb.append(nodeInfo.getClassName());
        sb.append(" (" + nodeInfo.getChildCount() +  ")");
        sb.append(" " + bounds.toString());
        if (nodeInfo.getText() != null) {
            sb.append(" - \"" + nodeInfo.getText() + "\"");
        }
        Log.v(TAG, sb.toString());

        for (int i=0; i<nodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = nodeInfo.getChild(i);
            if (childNode != null) {
                logNodeHierarchy(childNode, depth + 1);
            }
        }
    }

    private static AccessibilityNodeInfo findSmallestNodeAtPoint(AccessibilityNodeInfo sourceNode, int x, int y) {
        Rect bounds = new Rect();
        sourceNode.getBoundsInScreen(bounds);

        if (!bounds.contains(x, y)) {
            return null;
        }

        for (int i=0; i<sourceNode.getChildCount(); i++) {
            AccessibilityNodeInfo nearestSmaller = findSmallestNodeAtPoint(sourceNode.getChild(i), x, y);
            if (nearestSmaller != null) {
                return nearestSmaller;
            }
        }
        return sourceNode;
    }

    public static AccessibilityNodeInfo findRootNode(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return null;
        }
        if (nodeInfo.getParent() == null) {
            return nodeInfo;
        }
        //        try {
//            if (nodeInfo.getParent() == null) {
//                return nodeInfo;
//            }
//        } catch (IllegalStateException e) {
//            Log.e(TAG, "Illegal state exception while trying to find the parent of root node");
//            return null;
//        }
//
        return findRootNode(nodeInfo.getParent());
    }




    public static String findScreenTitleForNode(AccessibilityNodeInfo nodeInfo) {
        //Check for correct input
        if (nodeInfo == null) {
            return Utils.EMPTY_STRING;
        }

        //Check for root node
        AccessibilityNodeInfo rootNodeInfo = findRootNode(nodeInfo);
        if (rootNodeInfo == null) {
            return Utils.EMPTY_STRING;
        }

        //Found root node
//        AccessibilityNodeInfo screenTitleNodeInfo =
//                findFirstChildWithGivenClassNames(rootNodeInfo, Collections.singletonList(FlatViewUtils.TEXT_VIEW_CLASSNAME), false);
        AccessibilityNodeInfo screenTitleNodeInfo = findTopMostTextView(rootNodeInfo, null);

        if (screenTitleNodeInfo == null || screenTitleNodeInfo.getText() == null) {
            return Utils.EMPTY_STRING;
        }

        return screenTitleNodeInfo.getText().toString();
    }

    public static ScreenInfo findScreenInfoForNode(AccessibilityNodeInfo nodeInfo, DisplayMetrics displayMetrics) {
        final int MAX_VIEWS_FOR_SCREEN_INFO = 2;
        MinMaxPriorityQueue<AccessibilityNodeInfo> bestNodesSoFar = MinMaxPriorityQueue.orderedBy(new AccessibilityNodeBoundsComparator())
                .maximumSize(MAX_VIEWS_FOR_SCREEN_INFO)
                .create();

        //Check for correct input
        if (nodeInfo == null) {
            return new ScreenInfo();
        }

        //Check for root node
        AccessibilityNodeInfo rootNodeInfo = findRootNode(nodeInfo);
        if (rootNodeInfo == null) {
            return new ScreenInfo();
        }

        findTopNTextView(rootNodeInfo, bestNodesSoFar);

        AccessibilityNodeInfo topMostTextView = null;
        AccessibilityNodeInfo secondTopMostTextView = null;
        if (bestNodesSoFar.size() > 0) {
            topMostTextView = bestNodesSoFar.removeFirst();
        }
        if (bestNodesSoFar.size() > 0) {
            secondTopMostTextView = bestNodesSoFar.removeFirst();
        }

        ScreenInfo screenInfo = new ScreenInfo();
        if (topMostTextView != null) {
            screenInfo.setTitle(topMostTextView.getText().toString());
        }
        if (secondTopMostTextView != null) {
            screenInfo.setSubTitle(secondTopMostTextView.getText().toString());
        }

        if (rootNodeInfo.getPackageName() != null) {
            screenInfo.setPackageName(rootNodeInfo.getPackageName().toString());
        }

        screenInfo.setScreenType(isFullScreenNode(rootNodeInfo, displayMetrics));
        return screenInfo;
    }

    public static boolean isFullScreenNode(AccessibilityNodeInfo nodeInfo, @NonNull DisplayMetrics displayMetrics) {
        AccessibilityNodeInfo rootNodeInfo = Utils.findRootNode(nodeInfo);
        if (rootNodeInfo == null) {
            return false;
        }
        final double MIN_SIZE_FRACTION_FOR_FULL_SCREEN = 0.7;
        Rect rootNodeBounds = new Rect();
        rootNodeInfo.getBoundsInScreen(rootNodeBounds);
        int currentRootHeight = rootNodeBounds.height();
        int currentRootWidth = rootNodeBounds.width();
        int nodeSize = currentRootWidth * currentRootHeight;
        int totalSize =  displayMetrics.widthPixels * displayMetrics.heightPixels;
        double fraction = (double) nodeSize / (double) totalSize;
        return fraction > MIN_SIZE_FRACTION_FOR_FULL_SCREEN;
    }

    public static boolean matchScreenWithRootNode(String screenTitle,
                                                  String screenSubTitle,
                                                  String screenPackageName,
                                                  AccessibilityNodeInfo nodeInfo,
                                                  boolean checkSubtitle) {
        AccessibilityNodeInfo titleTextView = findUIElement(
                Arrays.asList(FlatViewUtils.CLASS_NAMES_FOR_TITLE_VIEW),
                screenPackageName,
                Arrays.asList(screenTitle.split(" ")),
                nodeInfo,
                false);


        boolean found = titleTextView != null;
        if (found && checkSubtitle) {
            AccessibilityNodeInfo subTitleTextView = findUIElement(
                    Arrays.asList(FlatViewUtils.CLASS_NAMES_FOR_TITLE_VIEW),
                    screenPackageName,
                    Arrays.asList(screenSubTitle.split(" ")),
                    nodeInfo,
                    false);
            found = subTitleTextView != null;
        }
        return found;
    }


    private static boolean matchNodeInfoWithGivenClassNames(AccessibilityNodeInfo accessibilityNodeInfo, List<String> classNames) {
        if (accessibilityNodeInfo == null || accessibilityNodeInfo.getClassName() == null || classNames == null) {
            return false;
        }
        for (String className: classNames) {
            if (accessibilityNodeInfo.getClassName().toString().equalsIgnoreCase(className)) {
                return true;
            }
        }
        return false;
    }

    private static AccessibilityNodeInfo findFirstParentWithTargetClassName(AccessibilityNodeInfo accessibilityNodeInfo,
                                                                            String targetClassName,
                                                                            boolean isClickable) {
        if (accessibilityNodeInfo == null) {
            return null;
        }
        if ((!isClickable || accessibilityNodeInfo.isClickable()) && accessibilityNodeInfo.getClassName().equals(targetClassName)) {
            return accessibilityNodeInfo;
        }
        return findFirstParentWithTargetClassName(accessibilityNodeInfo.getParent(), targetClassName, isClickable);
    }

    private static AccessibilityNodeInfo findFirstParentWithTargetClassNames(AccessibilityNodeInfo accessibilityNodeInfo,
                                                                            List<String> targetClassNames,
                                                                            boolean isClickable) {
        if (accessibilityNodeInfo == null) {
            return null;
        }
        if ((!isClickable || accessibilityNodeInfo.isClickable()) && matchNodeInfoWithGivenClassNames(accessibilityNodeInfo, targetClassNames)) {
            return accessibilityNodeInfo;
        }
        return findFirstParentWithTargetClassNames(accessibilityNodeInfo.getParent(), targetClassNames, isClickable);
    }


    private static HashMap<String, AccessibilityNodeInfo> searchForKeyword(String keyWord,
                                                                           List<String> matchingClassNames,
                                                                           List<AccessibilityNodeInfo> accessibilityNodeInfoList,
                                                                           boolean isClickable) {
        HashMap<String, AccessibilityNodeInfo> overallCandidates = new HashMap<>();
        for (AccessibilityNodeInfo accessibilityNodeInfo: accessibilityNodeInfoList) {
            HashMap<String, AccessibilityNodeInfo> candidates = searchForKeyword(keyWord,
                    matchingClassNames, accessibilityNodeInfo, isClickable);
            overallCandidates.putAll(candidates);
        }
        return overallCandidates;
    }

    private static String translateWordForAccessibilitySearch(String word) {
        if (Utils.nullOrEmpty(word)) {
            return Utils.EMPTY_STRING;
        }
        if (word.equalsIgnoreCase(WIFI_SANITIZED)) {
            return WIFI_ACCESSIBILITY;
        }
        return word;
    }

    private static List<String> getWordsForAccessibilitySearch(String word) {
        if (Utils.nullOrEmpty(word)) {
            return new ArrayList<>();
        }

        List<String> wordList = new ArrayList<>();
        wordList.add(word);

        if (word.equalsIgnoreCase(WIFI_SANITIZED)) {
            wordList.add(WIFI_ACCESSIBILITY);
        }

        return wordList;
    }

    public static String sanitizeText(String input) {
        if (nullOrEmpty(input)) {
            return input;
        }
        return input.replaceAll("[^\\w\\s]","").trim().toLowerCase();
    }

    private static List<AccessibilityNodeInfo> searchAccessibilityNodeInfoByText(String inputText, AccessibilityNodeInfo accessibilityNodeInfo) {
        if (accessibilityNodeInfo == null) {
            return new ArrayList<>();
        }
        List<AccessibilityNodeInfo> accessibilityNodeInfoList = new ArrayList<>();
        String nodeText = Utils.EMPTY_STRING;
        if (accessibilityNodeInfo.getText() != null) {
            nodeText += nodeText + accessibilityNodeInfo.getText().toString() + " ";
        }
        if (accessibilityNodeInfo.getContentDescription() != null) {
            nodeText += nodeText + accessibilityNodeInfo.getContentDescription().toString();
        }
        nodeText = sanitizeText(nodeText);
        inputText = sanitizeText(inputText);
        if (nodeText.contains(inputText)) {
            accessibilityNodeInfoList.add(accessibilityNodeInfo);
        }
        for (int childIndex=0; childIndex < accessibilityNodeInfo.getChildCount(); childIndex++) {
            accessibilityNodeInfoList.addAll(searchAccessibilityNodeInfoByText(inputText, accessibilityNodeInfo.getChild(childIndex)));
        }
        return accessibilityNodeInfoList;
    }

    private static HashMap<String, AccessibilityNodeInfo> searchForKeyword(String keyWord,
                                                                           List<String> matchingClassNames,
                                                                           AccessibilityNodeInfo rootNodeInfo,
                                                                           boolean isClickable) {

        List<AccessibilityNodeInfo> matchingNodes = new ArrayList<>();
        List<String> wordsToMatch = Arrays.asList(keyWord.split(MULTIPLE_WORD_MATCH_DELIMITER));
        for (String word: wordsToMatch) {
            List<String> translatedWords = Utils.getWordsForAccessibilitySearch(word);
            //For matching both Wi-Fi and WiFi
            for (String translatedWord: translatedWords) {
                matchingNodes.addAll(searchAccessibilityNodeInfoByText(translatedWord, rootNodeInfo));
                //matchingNodes.addAll(rootNodeInfo.findAccessibilityNodeInfosByText(translatedWord));
            }
            //matchingNodes.addAll(rootNodeInfo.findAccessibilityNodeInfosByText(translateWordForAccessibilitySearch(word)));
        }
        HashMap<String, AccessibilityNodeInfo> nodesWithKeyWord = new HashMap<>();
        for (AccessibilityNodeInfo currentNode: matchingNodes) {
            if ((!isClickable || currentNode.isClickable()) && matchNodeInfoWithGivenClassNames(currentNode, matchingClassNames)) {
                nodesWithKeyWord.put(String.valueOf(currentNode.hashCode()),currentNode);
            } else {
                AccessibilityNodeInfo parentMatch = findFirstParentWithTargetClassNames(currentNode, matchingClassNames, isClickable);
                if (parentMatch != null) {
                    nodesWithKeyWord.put(String.valueOf(parentMatch.hashCode()), parentMatch);
                } else {
                    //We couldn't find suitable parent, look for suitable child
                    AccessibilityNodeInfo childMatch = findFirstChildWithGivenClassNames(currentNode, matchingClassNames, isClickable);
                    if (childMatch != null) {
                        nodesWithKeyWord.put(String.valueOf(childMatch.hashCode()), childMatch);
                    }
                }
            }
        }
        return nodesWithKeyWord;
    }

    public static AccessibilityNodeInfo findUIElement(String elementClassName,
                                                      String elementPackageName,
                                                      List<String> keyWords,
                                                      AccessibilityNodeInfo nodeInfo,
                                                      boolean isClickable) {
        return findUIElement(Arrays.asList(elementClassName), elementPackageName,
                keyWords, nodeInfo, isClickable);

    }

    public static AccessibilityNodeInfo findUIElement(List<String> elementClassNames,
                                                      String elementPackageName,
                                                      List<String> keyWords,
                                                      AccessibilityNodeInfo nodeInfo,
                                                      boolean isClickable) {
        if (keyWords == null || nodeInfo == null) {
            return null;
        }
        //TODO: FIX this -- search should begin at root node
        HashMap<String, AccessibilityNodeInfo> candidateNodes = new HashMap<>();
        List<AccessibilityNodeInfo> accessibilityNodeInfoListToSearch = new ArrayList<>();
        accessibilityNodeInfoListToSearch.add(nodeInfo);
        for (String keyWord: keyWords) {
            //Need to recycle the views
            if (Utils.nullOrEmpty(keyWord)) {
                continue;
            }
            candidateNodes = searchForKeyword(keyWord, elementClassNames, accessibilityNodeInfoListToSearch, isClickable);
            if (candidateNodes.size() <= 1) {
                break;
            }
            accessibilityNodeInfoListToSearch = new ArrayList<>(candidateNodes.values());
        }

        if(candidateNodes.size() == 1) {
            List<AccessibilityNodeInfo> finalMatchingInfo = new ArrayList<>(candidateNodes.values());
            AccessibilityNodeInfo finalMatch = finalMatchingInfo.get(0);
            if (finalMatch.getPackageName().equals(elementPackageName)) {
                return finalMatch;
            }
        } else if (candidateNodes.size() > 1) {
            //Find the best matching one, match the keywords string sorted with the node string
            double bestMatchingMetric = 0;
            AccessibilityNodeInfo accessibilityNodeInfoToReturn = null;
            for (AccessibilityNodeInfo finalMatchInfo: candidateNodes.values()) {
                //Find the string for this node -- text + child text -- sorted and separated by spaces
                double matchingMetric = getMatchingMetricForKeywordsWithRegex(keyWords, finalMatchInfo);
                if (matchingMetric > bestMatchingMetric) {
                    accessibilityNodeInfoToReturn = finalMatchInfo;
                    bestMatchingMetric = matchingMetric;
                }
            }
            return accessibilityNodeInfoToReturn;
        }
        return null;
    }

    private static double getMatchingMetricForKeywordsWithRegex(List<String> keywordList,
                                                                AccessibilityNodeInfo nodeInfo) {
        if (keywordList == null) {
            return 0;
        }
        Set<String> wordSetForNode = getWordsForNodeInfo(nodeInfo);
        Set<String> keyWordSet = new HashSet<>(keywordList);

        //Break input text into words, and see how many words occur in reference text
        int matchingCount = 0;

        for (String keyWord: keyWordSet) {
            boolean doesKeyWordMatch = false;
            List<String> wordsToMatch = Arrays.asList(keyWord.split(MULTIPLE_WORD_MATCH_DELIMITER));
            for (String word: wordsToMatch) {
                List<String> translatedWords = Utils.getWordsForAccessibilitySearch(word);
                //For matching both Wi-Fi and WiFi
                for (String translatedWord : translatedWords) {
                    if (wordSetForNode.contains(translatedWord)) {
                        doesKeyWordMatch = true;
                        break;
                    }
                }
            }
            matchingCount = doesKeyWordMatch ? matchingCount + 1 : matchingCount;
        }

        return (double)matchingCount / (wordSetForNode.size() + keyWordSet.size());
    }

    private static Set<String> getWordsForNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        Set<String> wordList = new HashSet<>();
        if (accessibilityNodeInfo == null) {
            return wordList;
        }
        if (accessibilityNodeInfo.getText() != null) {
            String nodeText = Utils.sanitizeText(accessibilityNodeInfo.getText().toString());
            wordList.addAll(Arrays.asList(nodeText.split(" ")));
        }
        if (accessibilityNodeInfo.getContentDescription() != null) {
            String contentDescriptionText = Utils.sanitizeText(accessibilityNodeInfo.getContentDescription().toString());
            wordList.addAll(Arrays.asList(contentDescriptionText.split(" ")));
        }
        for (int childIndex = 0; childIndex < accessibilityNodeInfo.getChildCount(); childIndex++) {
            AccessibilityNodeInfo childInfo = accessibilityNodeInfo.getChild(childIndex);
            wordList.addAll(getWordsForNodeInfo(childInfo));
        }
        return wordList;
    }

    public static double getMatchingMetric(List<String> inputList1, List<String> inputList2) {
        if (inputList1 == null || inputList1.isEmpty() || inputList2 == null || inputList2.isEmpty()) {
            return 0;
        }
        //Break input text into words, and see how many words occur in reference text
        int matchingCount = getMatchingWordCount(inputList1, inputList2);
        return (double)matchingCount / (inputList1.size() + inputList2.size());
    }


    private static int getMatchingWordCount(List<String> inputList1, List<String> inputList2) {
        if (inputList1 == null || inputList2 == null) {
            return 0;
        }

        int matchingCount = 0;
        int length1 = inputList1.size();
        int length2 = inputList2.size();
        List<String> shorterList;
        List<String> longerList;
        if (length1 < length2) {
            shorterList = inputList1;
            longerList = inputList2;
        } else {
            shorterList = inputList2;
            longerList = inputList1;
        }

        HashMap<String, Boolean> wordHashMap = new HashMap<>();
        for (String word: shorterList) {
            //wordHashMap.put(word.trim().toLowerCase(), false);
            wordHashMap.put(Utils.sanitizeText(word), false);
        }

        for (String word: longerList) {
            word = Utils.sanitizeText(word);
            //word = word.trim().toLowerCase();
            Boolean alreadyCounted = wordHashMap.get(word);
            if (alreadyCounted != null && !alreadyCounted) {
                matchingCount++;
            }
            wordHashMap.put(word, true);
        }
        return matchingCount;
    }


    private static AccessibilityNodeInfo getTopLeftNode(AccessibilityNodeInfo nodeInfo1, AccessibilityNodeInfo nodeInfo2) {
        if (nodeInfo1 == null && nodeInfo2 == null) {
            return null;
        } else if (nodeInfo1 == null) {
            return nodeInfo2;
        } else if (nodeInfo2 == null) {
            return nodeInfo1;
        }
        Rect bounds1 = new Rect();
        Rect bounds2 = new Rect();
        nodeInfo1.getBoundsInScreen(bounds1);
        nodeInfo2.getBoundsInScreen(bounds2);
        int comparisonResult = compareBounds(bounds1, bounds2);
        if (comparisonResult < 0) {
            return nodeInfo1;
        } else if (comparisonResult > 0) {
            return nodeInfo2;
        } else {
            return nodeInfo1;
        }
    }

    private static int compareBounds(Rect bounds1, Rect bounds2) {
        //0 if equal
        //-1 if bound1 < bound2
        //1 if bound1 > bound2
        int bounds1Smaller = -1;
        int bounds2Smaller = 1;
        int boundsEqual = 0;
        if (bounds1.top < bounds2.top) {
            return bounds1Smaller;
        } else if (bounds1.top > bounds2.top) {
            return bounds2Smaller;
        } else if (bounds1.left < bounds2.left) {
            return bounds1Smaller;
        } else if (bounds1.left > bounds2.left) {
            return bounds2Smaller;
        } else if (bounds1.centerY() < bounds2.centerY()) {
            return bounds1Smaller;
        } else if (bounds1.centerY() > bounds2.centerY()) {
            return bounds2Smaller;
        } else if (bounds1.centerX() < bounds2.centerX()) {
            return bounds1Smaller;
        } else if (bounds1.centerX() > bounds2.centerX()) {
            return bounds2Smaller;
        } else {
            //both are equal, return the current best
            return boundsEqual;
        }
    }

    private static boolean isTextViewClass(String className) {
        if (Utils.nullOrEmpty(className)) {
            return false;
        }
        for (String textViewClassName: FlatViewUtils.CLASS_NAMES_FOR_TITLE_VIEW) {
            if (className.equalsIgnoreCase(textViewClassName)) {
                return true;
            }
        }
        return false;
    }

    private static void findTopNTextView(AccessibilityNodeInfo accessibilityNodeInfo,
                                         MinMaxPriorityQueue<AccessibilityNodeInfo> bestNodesSoFar) {


        if (accessibilityNodeInfo == null) {
            return;
        }

        int sizeBestNodes = bestNodesSoFar.size();

        if (accessibilityNodeInfo.getClassName() != null &&
                isTextViewClass(accessibilityNodeInfo.getClassName().toString()) &&
                accessibilityNodeInfo.getText() != null &&
                !accessibilityNodeInfo.getText().toString().equals(Utils.EMPTY_STRING)) {
            //Log.d(TAG, "subsettingXX found textview with bounds: " + accessibilityNodeInfo.toString());
            bestNodesSoFar.add(accessibilityNodeInfo);
            if (bestNodesSoFar.size() != sizeBestNodes) {
                //Log.d(TAG, "subsettingXX adding to best node: " + accessibilityNodeInfo.toString());
            }
        }

        for (int childIndex=0; childIndex < accessibilityNodeInfo.getChildCount(); childIndex++) {
           findTopNTextView(accessibilityNodeInfo.getChild(childIndex), bestNodesSoFar);
        }
    }

    public static AccessibilityNodeInfo findTopMostTextView(AccessibilityNodeInfo accessibilityNodeInfo,
                                                            @Nullable AccessibilityNodeInfo bestNodeSoFar) {
        if (accessibilityNodeInfo == null) {
            return null;
        }

        AccessibilityNodeInfo oldBestNode = bestNodeSoFar;

        if (accessibilityNodeInfo.getClassName() != null &&
                    accessibilityNodeInfo.getClassName().equals(FlatViewUtils.TEXT_VIEW_CLASSNAME) &&
                accessibilityNodeInfo.getText() != null &&
                !accessibilityNodeInfo.getText().toString().equals(Utils.EMPTY_STRING)) {
            //Log.d(TAG, "subsettingXX found textview with bounds: " + accessibilityNodeInfo.toString());
            bestNodeSoFar = getTopLeftNode(bestNodeSoFar, accessibilityNodeInfo);
            if (bestNodeSoFar != oldBestNode) {
                //Log.d(TAG, "subsettingXX updating best node to " + bestNodeSoFar.toString());
            }
        }

        for (int childIndex=0; childIndex < accessibilityNodeInfo.getChildCount(); childIndex++) {
            bestNodeSoFar = findTopMostTextView(accessibilityNodeInfo.getChild(childIndex), bestNodeSoFar);
        }

        return bestNodeSoFar;
    }

    public static AccessibilityNodeInfo findFirstChildWithGivenClassNames(AccessibilityNodeInfo accessibilityNodeInfo,
                                                                          List<String> classNames,
                                                                          boolean isClickable) {
        if (accessibilityNodeInfo == null) {
            return null;
        }
        for (String className: classNames) {
            if (accessibilityNodeInfo.getClassName() != null &&
                    accessibilityNodeInfo.getClassName().equals(className) &&
                    (!isClickable || accessibilityNodeInfo.isClickable())) {
                return accessibilityNodeInfo;
            }
        }
        for (int childIndex=0; childIndex < accessibilityNodeInfo.getChildCount(); childIndex++) {
            AccessibilityNodeInfo childInfo = findFirstChildWithGivenClassNames(accessibilityNodeInfo.getChild(childIndex), classNames, isClickable);
            if (childInfo != null) {
                return childInfo;
            }
        }
        return null;
    }

    public static boolean checkCondition(String textToMatch, AccessibilityNodeInfo elementInfo) {
        //Check isClickable, isCheckable, isChecked
        //First look for switch like elements
        AccessibilityNodeInfo switchNodeInfo = findFirstChildWithGivenClassNames(
                elementInfo,
                Arrays.asList(FlatViewUtils.SWITCH_CLASSNAME,
                        FlatViewUtils.CHECK_BOX_CLASSNAME,
                        FlatViewUtils.CHECKED_TEXT_VIEW_CLASS_NAME,
                        FlatViewUtils.TOGGLE_BUTTON_CLASSNAME,
                        FlatViewUtils.RADIO_BUTTON_CLASSNAME), false);
        if (switchNodeInfo != null) {
            //Found switch element. //Check its state based on input textToMatch
            if (textToMatch.equalsIgnoreCase(FlatViewUtils.OFF_TEXT)) {
                return !switchNodeInfo.isChecked();
            } else {
                return switchNodeInfo.isEnabled() && switchNodeInfo.isChecked();
            }
       }
        //Non switch nodes -- TODO handle non switch elements like SEEK, EDIT TEXT etc.
        return false;
    }

    public static boolean nullOrEmpty(String target) {
        return target == null || target.isEmpty() || target.equals("null");
    }

    public static boolean readSharedSetting(Context ctx, String settingName, boolean defaultValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(settingName, defaultValue);
    }

    public static void saveSharedSetting(Context ctx, String settingName, boolean settingValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(settingName, settingValue);
        editor.apply();
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param displayMetrics DisplayMetrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static int convertDpToPixel(float dp, DisplayMetrics displayMetrics){
        float px = dp * ((float)displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return (int)px;
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param displayMetrics DisplayMetrics
     * @return A float value to represent dp equivalent to px value
     */
    public static int convertPixelsToDp(float px, DisplayMetrics displayMetrics) {
        float dp = px / ((float) displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return (int)dp;
    }

    public static String convertCharSeqToStringSafely(CharSequence charSequence) {
        if (charSequence == null) {
            return Utils.EMPTY_STRING;
        }
        return charSequence.toString();
    }

    public static HashMap<String, String> getDeviceDetails() {
        HashMap<String, String> phoneInfo = new HashMap<>();
        phoneInfo.put("manufacturer", Build.MANUFACTURER);
        phoneInfo.put("model", Build.MODEL);
        phoneInfo.put("release", Build.VERSION.RELEASE);
        phoneInfo.put("sdk", Integer.toString(Build.VERSION.SDK_INT));
        phoneInfo.put("hardware", Build.HARDWARE);
        phoneInfo.put("product", Build.PRODUCT);
        return phoneInfo;
    }

    public static String findPackageNameForApp(Context context, String appName) {
        final PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            Log.d(TAG, "Installed package :" + packageInfo.packageName);
            Log.d(TAG, "Source dir : " + packageInfo.sourceDir);
            Log.d(TAG, "Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName));
            if (packageInfo.packageName.toLowerCase().contains(appName.toLowerCase())) {
                //Found the app
                return packageInfo.packageName;
            }
        }
        return Utils.EMPTY_STRING;
    }

    public static boolean searchAndLaunchApp(Context context, String appName) {
        final PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo packageInfo : packages) {
            Log.d(TAG, "Installed package :" + packageInfo.packageName);
            Log.d(TAG, "Source dir : " + packageInfo.sourceDir);
            Log.d(TAG, "Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName));
            if (packageInfo.packageName.toLowerCase().contains(appName.toLowerCase())) {
                //Found the app
                launchApp(context, packageInfo.packageName);
                return true;
            }
        }
        return false;
    }

    private static void launchApp(Context context, String packageName) {
        Intent launchIntentForPackage = context.getPackageManager()
                .getLaunchIntentForPackage(packageName);
        context.startActivity(launchIntentForPackage);
    }

    public static boolean launchAppIfInstalled(Context context, String packageName) {
        boolean isInstalled = isAppInstalled(context, packageName);
        if (isInstalled) {
           launchApp(context, packageName);
            return true;
        }
        return false;
    }

    private static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        }
        catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static class AccessibilityNodeBoundsComparator implements Comparator<AccessibilityNodeInfo> {
        @Override
        public int compare(AccessibilityNodeInfo nodeInfo1, AccessibilityNodeInfo nodeInfo2) {
            //0 if equal
            //-1 if nodeInfo1 is smaller
            //1 if nodeInfo2 is smaller
            if (nodeInfo1 == null && nodeInfo2 == null) {
                return 0;
            } else if (nodeInfo1 == null) {
                return 1;
            } else if (nodeInfo2 == null) {
                return -1;
            } else {
                Rect bounds1 = new Rect();
                Rect bounds2 = new Rect();
                nodeInfo1.getBoundsInScreen(bounds1);
                nodeInfo2.getBoundsInScreen(bounds2);
                return compareBounds(bounds1, bounds2);
            }
        }
    }


}
