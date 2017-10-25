package com.inceptai.neoservice;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.gson.Gson;
import com.inceptai.neoservice.flatten.FlatViewUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
        AccessibilityNodeInfo screenTitleNodeInfo =
                findFirstChildWithGivenClassNames(rootNodeInfo, Collections.singletonList(FlatViewUtils.TEXT_VIEW_CLASSNAME));
        if (screenTitleNodeInfo == null || screenTitleNodeInfo.getText() == null) {
            return Utils.EMPTY_STRING;
        }

        return screenTitleNodeInfo.getText().toString();

    }

    public static boolean matchScreenWithRootNode(String screenTitle,
                                                  String screenPackageName,
                                                  AccessibilityNodeInfo nodeInfo) {
        return (findUIElement(FlatViewUtils.TEXT_VIEW_CLASSNAME,
                screenPackageName,
                Arrays.asList(screenTitle.split(" ")),
                nodeInfo,
                false) != null);
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

    private static HashMap<String, AccessibilityNodeInfo> searchForKeyword(String keyWord,
                                                                           String matchingClassName,
                                                                           List<AccessibilityNodeInfo> accessibilityNodeInfoList,
                                                                           boolean isClickable) {
        HashMap<String, AccessibilityNodeInfo> overallCandidates = new HashMap<>();
        for (AccessibilityNodeInfo accessibilityNodeInfo: accessibilityNodeInfoList) {
            HashMap<String, AccessibilityNodeInfo> candidates = searchForKeyword(keyWord,
                    matchingClassName, accessibilityNodeInfo, isClickable);
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

    private static HashMap<String, AccessibilityNodeInfo> searchForKeyword(String keyWord,
                                                                           String matchingClassName,
                                                                           AccessibilityNodeInfo rootNodeInfo,
                                                                           boolean isClickable) {

        List<AccessibilityNodeInfo> matchingNodes = new ArrayList<>();
        List<String> wordsToMatch = Arrays.asList(keyWord.split(MULTIPLE_WORD_MATCH_DELIMITER));
        for (String word: wordsToMatch) {
            matchingNodes.addAll(rootNodeInfo.findAccessibilityNodeInfosByText(translateWordForAccessibilitySearch(word)));
        }
        HashMap<String, AccessibilityNodeInfo> nodesWithKeyWord = new HashMap<>();
        for (AccessibilityNodeInfo currentNode: matchingNodes) {
            if ((!isClickable || currentNode.isClickable()) && currentNode.getClassName().equals(matchingClassName)) {
                nodesWithKeyWord.put(String.valueOf(currentNode.hashCode()),currentNode);
            } else {
                AccessibilityNodeInfo parentInfo = findFirstParentWithTargetClassName(currentNode, matchingClassName, isClickable);
                if (parentInfo != null) {
                    nodesWithKeyWord.put(String.valueOf(parentInfo.hashCode()), parentInfo);
                }
            }
//            AccessibilityNodeInfo parentNode = currentNode.getParent();
//            //Use the element class name to do this better
//            if (parentNode != null && parentNode.isClickable()) {
//                clickableNodes.put(String.valueOf(parentNode.hashCode()), parentNode);
//            } else {
//                clickableNodes.put(String.valueOf(currentNode.hashCode()),currentNode);
//            }
        }
        return nodesWithKeyWord;
    }


    public static AccessibilityNodeInfo findUIElement(String elementClassName,
                                                      String elementPackageName,
                                                      List<String> keyWords,
                                                      AccessibilityNodeInfo nodeInfo,
                                                      boolean isClickable) {
        if (keyWords == null || nodeInfo == null) {
            return null;
        }
        HashMap<String, AccessibilityNodeInfo> candidateNodes = new HashMap<>();
        List<AccessibilityNodeInfo> accessibilityNodeInfoListToSearch = new ArrayList<>();
        accessibilityNodeInfoListToSearch.add(nodeInfo);
        for (String keyWord: keyWords) {
            //Need to recycle the views
            candidateNodes = searchForKeyword(keyWord, elementClassName, accessibilityNodeInfoListToSearch, isClickable);
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
            int maxCount = 0;
            AccessibilityNodeInfo accessibilityNodeInfoToReturn = null;
            for (AccessibilityNodeInfo finalMatchInfo: candidateNodes.values()) {
                //Find the string for this node -- text + child text -- sorted and separated by spaces
                int matchingCount  = getMatchingWordCount(keyWords, finalMatchInfo);
                if (matchingCount > maxCount) {
                    accessibilityNodeInfoToReturn = finalMatchInfo;
                    maxCount = matchingCount;
                }
            }
            return accessibilityNodeInfoToReturn;
        }
        return null;
    }

    private static int getMatchingWordCount(List<String> wordList, AccessibilityNodeInfo nodeInfo) {
        List<String> wordListForNode = getWordsForNodeInfo(nodeInfo);
        return getMatchingWordCount(wordList, wordListForNode);
    }

    private static int getMatchingWordCount(AccessibilityNodeInfo nodeInfo1, AccessibilityNodeInfo nodeInfo2) {
        List<String> wordList1 = getWordsForNodeInfo(nodeInfo1);
        List<String> wordList2 = getWordsForNodeInfo(nodeInfo2);
        return getMatchingWordCount(wordList1, wordList2);
    }

    private static List<String> getWordsForNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        List<String> wordList = new ArrayList<>();
        if (accessibilityNodeInfo == null) {
            return wordList;
        }
        if (accessibilityNodeInfo.getText() != null) {
            wordList.addAll(Arrays.asList(accessibilityNodeInfo.getText().toString().split(" ")));
        }
        for (int childIndex = 0; childIndex < accessibilityNodeInfo.getChildCount(); childIndex++) {
            AccessibilityNodeInfo childInfo = accessibilityNodeInfo.getChild(childIndex);
            wordList.addAll(getWordsForNodeInfo(childInfo));
        }
        return wordList;
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
            wordHashMap.put(word.trim().toLowerCase(), false);
        }

        for (String word: longerList) {
            word = word.trim().toLowerCase();
            Boolean alreadyCounted = wordHashMap.get(word);
            if (alreadyCounted != null && !alreadyCounted) {
                matchingCount++;
            }
            wordHashMap.put(word, true);
        }
        return matchingCount;
    }

    public static AccessibilityNodeInfo findFirstChildWithGivenClassNames(AccessibilityNodeInfo accessibilityNodeInfo,
                                                                          List<String> classNames) {
        if (accessibilityNodeInfo == null) {
            return null;
        }
        for (String className: classNames) {
            if (accessibilityNodeInfo.getClassName() != null && accessibilityNodeInfo.getClassName().equals(className)) {
                return accessibilityNodeInfo;
            }
        }
        for (int childIndex=0; childIndex < accessibilityNodeInfo.getChildCount(); childIndex++) {
            AccessibilityNodeInfo childInfo = findFirstChildWithGivenClassNames(accessibilityNodeInfo.getChild(childIndex), classNames);
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
                        FlatViewUtils.CHECKED_TEXT_VIEW_CLASS_NAME));
        if (switchNodeInfo != null) {
            //Found switch element. //Check its state based on input textToMatch
            if (textToMatch.equalsIgnoreCase(FlatViewUtils.ON_TEXT)) {
                return switchNodeInfo.isEnabled() && switchNodeInfo.isChecked();
            } else {
                return !switchNodeInfo.isChecked();
            }
        }
        //Non switch nodes -- TODO handle non switch elements like SEEK, EDIT TEXT etc.
        return false;
    }


    public static AccessibilityNodeInfo findUIElementFlattenHierarchy(String elementClassName,
                                                                      String elementPackageName,
                                                                      List<String> keyWords,
                                                                      AccessibilityNodeInfo nodeInfo) {
        return null;
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

}
