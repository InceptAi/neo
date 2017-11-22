package com.inceptai.neoservice.uiactions;

/**
 * Created by vivek on 7/5/17.
 */


import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.inceptai.neoservice.uiactions.UIActionResult.UIActionResultCodes.SUCCESS;

/**
 * Represents the result of a FutureAction. One of the possible values of T above.
 */
public class UIActionResult {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SUCCESS,
            UIActionResultCodes.NO_ACTIONS_AVAILABLE,
            UIActionResultCodes.NAVIGATION_FAILURE,
            UIActionResultCodes.FINAL_ACTION_ELEMENT_NOT_FOUND,
            UIActionResultCodes.INVALID_APP_NAME,
            UIActionResultCodes.SERVER_TIMED_OUT,
            UIActionResultCodes.SERVER_ERROR,
            UIActionResultCodes.APP_LAUNCH_FAILURE,
            UIActionResultCodes.WAITING_FOR_PREVIOUS_UI_ACTION_TO_FINISH,
            UIActionResultCodes.EXCEPTION_WHILE_WAITING_FOR_SCREEN_TRANSITION,
            UIActionResultCodes.UI_MANAGER_UNINITIALIZED,
            UIActionResultCodes.ANOTHER_SERVER_REQUEST_IN_FLIGHT,
            UIActionResultCodes.INVALID_ACTION_DETAIL,
            UIActionResultCodes.ROOT_WINDOW_IS_NULL,
            UIActionResultCodes.FINAL_ACTION_SCREEN_MATCH_FAILED,
            UIActionResultCodes.FINAL_ACTION_SUCCESS_CONDITION_NOT_MET,
            UIActionResultCodes.NON_TOGGLE_ACTION,
            UIActionResultCodes.ACCESSIBILITY_PERMISSION_DENIED,
            UIActionResultCodes.ACCESSIBILITY_SERVICE_UNAVAILABLE,
            UIActionResultCodes.INVALID_ACTION_DETAILS,
            UIActionResultCodes.EXCEPTION_WHILE_WAITING_FOR_UI_ACTION_FETCH_FROM_SERVER,
            UIActionResultCodes.ACTION_NOT_PERMITTED,
            UIActionResultCodes.GENERAL_ERROR,
            UIActionResultCodes.UNKNOWN,})
    public @interface UIActionResultCodes {
        int SUCCESS = 0;
        int NO_ACTIONS_AVAILABLE = 1;
        int NAVIGATION_FAILURE = 2;
        int FINAL_ACTION_ELEMENT_NOT_FOUND = 3;
        int INVALID_APP_NAME = 4;
        int SERVER_TIMED_OUT = 5;
        int SERVER_ERROR = 6;
        int APP_LAUNCH_FAILURE = 7;
        int WAITING_FOR_PREVIOUS_UI_ACTION_TO_FINISH = 8;
        int EXCEPTION_WHILE_WAITING_FOR_SCREEN_TRANSITION = 9;
        int UI_MANAGER_UNINITIALIZED = 10;
        int ANOTHER_SERVER_REQUEST_IN_FLIGHT = 11;
        int INVALID_ACTION_DETAIL = 12;
        int ROOT_WINDOW_IS_NULL = 13;
        int FINAL_ACTION_SCREEN_MATCH_FAILED = 14;
        int FINAL_ACTION_SUCCESS_CONDITION_NOT_MET = 15;
        int NON_TOGGLE_ACTION = 16;
        int ACCESSIBILITY_PERMISSION_DENIED = 17;
        int ACCESSIBILITY_SERVICE_UNAVAILABLE = 18;
        int INVALID_ACTION_DETAILS = 19;
        int EXCEPTION_WHILE_WAITING_FOR_UI_ACTION_FETCH_FROM_SERVER = 20;
        int ACTION_NOT_PERMITTED = 21;
        int GENERAL_ERROR = 99;
        int UNKNOWN = 100;
    }

    public static String actionResultCodeToString(@UIActionResultCodes int code) {
        switch (code) {
            case SUCCESS:
                return "SUCCESS";
            case UIActionResultCodes.NO_ACTIONS_AVAILABLE:
                return "NO_ACTIONS_AVAILABLE";
            case UIActionResultCodes.NAVIGATION_FAILURE:
                return "NAVIGATION_FAILURE";
            case UIActionResultCodes.FINAL_ACTION_ELEMENT_NOT_FOUND:
                return "FINAL_ACTION_ELEMENT_NOT_FOUND";
            case UIActionResultCodes.INVALID_APP_NAME:
                return "INVALID_APP_NAME";
            case UIActionResultCodes.SERVER_TIMED_OUT:
                return "SERVER_TIMED_OUT";
            case UIActionResultCodes.SERVER_ERROR:
                return "SERVER_ERROR";
            case UIActionResultCodes.APP_LAUNCH_FAILURE:
                return "APP_LAUNCH_FAILURE";
            case UIActionResultCodes.WAITING_FOR_PREVIOUS_UI_ACTION_TO_FINISH:
                return "WAITING_FOR_PREVIOUS_UI_ACTION_TO_FINISH";
            case UIActionResultCodes.GENERAL_ERROR:
                return "GENERAL_ERROR";
            case UIActionResultCodes.EXCEPTION_WHILE_WAITING_FOR_SCREEN_TRANSITION:
                return "EXCEPTION_WHILE_WAITING_FOR_SCREEN_TRANSITION";
            case UIActionResultCodes.UI_MANAGER_UNINITIALIZED:
                return "UI_MANAGER_UNINITIALIZED";
            case UIActionResultCodes.ANOTHER_SERVER_REQUEST_IN_FLIGHT:
                return "ANOTHER_SERVER_REQUEST_IN_FLIGHT";
            case UIActionResultCodes.INVALID_ACTION_DETAIL:
                return "INVALID_ACTION_DETAIL";
            case UIActionResultCodes.ROOT_WINDOW_IS_NULL:
                return "ROOT_WINDOW_IS_NULL";
            case UIActionResultCodes.FINAL_ACTION_SCREEN_MATCH_FAILED:
                return "FINAL_ACTION_SCREEN_MATCH_FAILED";
            case UIActionResultCodes.FINAL_ACTION_SUCCESS_CONDITION_NOT_MET:
                return "FINAL_ACTION_SUCCESS_CONDITION_NOT_MET";
            case UIActionResultCodes.NON_TOGGLE_ACTION:
                return "NON_TOGGLE_ACTION";
            case UIActionResultCodes.ACCESSIBILITY_PERMISSION_DENIED:
                return "ACCESSIBILITY_PERMISSION_DENIED";
            case UIActionResultCodes.ACCESSIBILITY_SERVICE_UNAVAILABLE:
                return "ACCESSIBILITY_SERVICE_UNAVAILABLE";
            case UIActionResultCodes.INVALID_ACTION_DETAILS:
                return "INVALID_ACTION_DETAILS";
            case UIActionResultCodes.EXCEPTION_WHILE_WAITING_FOR_UI_ACTION_FETCH_FROM_SERVER:
                return "EXCEPTION_WHILE_WAITING_FOR_UI_ACTION_FETCH_FROM_SERVER";
            case UIActionResultCodes.ACTION_NOT_PERMITTED:
                return "ACTION_NOT_PERMITTED";
            default:
                return "UNKNOWN";
        }
    }

    public static String getUserReadableMessage(UIActionResult uiActionResult) {
        int code = UIActionResultCodes.UNKNOWN;
        if (uiActionResult != null) {
            code = uiActionResult.getStatus();
        }
        return getUserReadableMessage(code);
    }

    public static String getUserReadableMessage(@UIActionResultCodes int code) {
        switch (code) {
            case SUCCESS:
                return "SUCCESS";
            case UIActionResultCodes.NO_ACTIONS_AVAILABLE:
                return "No matching actions found for your query";
            case UIActionResultCodes.NAVIGATION_FAILURE:
                return "Can't find the right settings page, sorry !";
            case UIActionResultCodes.FINAL_ACTION_ELEMENT_NOT_FOUND:
                return "Don't know how to take this action";
            case UIActionResultCodes.INVALID_APP_NAME:
                return "Can't take action for this app";
            case UIActionResultCodes.SERVER_TIMED_OUT:
                return "Our server is unreachable right now. Make sure you can reach the Internet.";
            case UIActionResultCodes.SERVER_ERROR:
            case UIActionResultCodes.EXCEPTION_WHILE_WAITING_FOR_UI_ACTION_FETCH_FROM_SERVER:
                return "Oops, sorry our server has an error, try another query.";
            case UIActionResultCodes.APP_LAUNCH_FAILURE:
                return "Couldn't launch settings to take action, try again ?";
            case UIActionResultCodes.WAITING_FOR_PREVIOUS_UI_ACTION_TO_FINISH:
                return "Still waiting for the last action to finish. Patience :)";
            case UIActionResultCodes.GENERAL_ERROR:
                return "Something went wrong for this query, try another query maybe.";
            case UIActionResultCodes.EXCEPTION_WHILE_WAITING_FOR_SCREEN_TRANSITION:
                return "Something went wrong for this query, try another query maybe.";
            case UIActionResultCodes.UI_MANAGER_UNINITIALIZED:
                return "App is in a bad state, try relaunching the app.";
            case UIActionResultCodes.ANOTHER_SERVER_REQUEST_IN_FLIGHT:
                return "Server is busy with another request, try again !";
            case UIActionResultCodes.INVALID_ACTION_DETAILS:
            case UIActionResultCodes.INVALID_ACTION_DETAIL:
                return "Don't know how to take this action";
            case UIActionResultCodes.ROOT_WINDOW_IS_NULL:
                return "Something went wrong for this query, try another query maybe.";
            case UIActionResultCodes.FINAL_ACTION_SCREEN_MATCH_FAILED:
                return "We couldn't find the right settings page for this action. Sorry, you can try another query !";
            case UIActionResultCodes.FINAL_ACTION_SUCCESS_CONDITION_NOT_MET:
                return "Got to the last step, but wasn't able to execute the action. " +
                        "Probably you need to confirm this action to finish.";
            case UIActionResultCodes.NON_TOGGLE_ACTION:
                return "Sorry we don't support this action";
            case UIActionResultCodes.ACCESSIBILITY_PERMISSION_DENIED:
            case UIActionResultCodes.ACCESSIBILITY_SERVICE_UNAVAILABLE:
                return "We don't have accessibility permission, please give us accessibility permission and try again.";
            case UIActionResultCodes.ACTION_NOT_PERMITTED:
                return "We can't take this action as it is grayed out (or not enabled).";
            case UIActionResultCodes.UNKNOWN:
            default:
                return "Oops something went wrong. Try again maybe ?";
        }
    }

    private @UIActionResultCodes int status;
    private String statusString;
    private String packageName;
    private String query;
    private Object payload;


    public UIActionResult(@UIActionResultCodes  int status) {
        this.status = status;
        this.statusString = UIActionResult.actionResultCodeToString(status);
    }

    public UIActionResult(int status, String query, String packageName) {
        this.status = status;
        this.packageName = packageName;
        this.query = query;
        this.statusString = UIActionResult.actionResultCodeToString(status);
    }

    public UIActionResult(String query, String packageName) {
        this.status = UIActionResultCodes.UNKNOWN;
        this.packageName = packageName;
        this.query = query;
        this.statusString = UIActionResult.actionResultCodeToString(status);
    }

    public void setStatus(int status) {
        this.status = status;
        this.statusString = UIActionResult.actionResultCodeToString(status);
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @UIActionResultCodes
    public int getStatus() {
        return status;
    }

    public String getStatusString() {
        return statusString;
    }

    public static boolean isSuccessful(UIActionResult actionResult) {
        return  (actionResult != null && actionResult.getStatus() == UIActionResultCodes.SUCCESS);
    }

    public static boolean failedDueToAccessibilityIssue(@UIActionResultCodes int status) {
        return  (status == UIActionResultCodes.ACCESSIBILITY_PERMISSION_DENIED ||
                        status == UIActionResultCodes.ACCESSIBILITY_SERVICE_UNAVAILABLE);
    }

    public static boolean failedDueToAccessibilityIssue(UIActionResult actionResult) {
        return  (actionResult != null &&
                (actionResult.getStatus() == UIActionResultCodes.ACCESSIBILITY_PERMISSION_DENIED ||
                        actionResult.getStatus() == UIActionResultCodes.ACCESSIBILITY_SERVICE_UNAVAILABLE));
    }
}
