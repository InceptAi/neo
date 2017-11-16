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
            UIActionResultCodes.WAITING_FOR_PREVIOUS_UIACTION_TO_FINISH,
            UIActionResultCodes.EXCEPTION_WHILE_WAITING_FOR_SCREEN_TRANSITION,
            UIActionResultCodes.UI_MANAGER_UNINITIALIZED,
            UIActionResultCodes.ANOTHER_SERVER_REQUEST_IN_FLIGHT,
            UIActionResultCodes.INVALID_ACTION_DETAIL,
            UIActionResultCodes.ROOT_WINDOW_IS_NULL,
            UIActionResultCodes.FINAL_ACTION_SCREEN_MATCH_FAILED,
            UIActionResultCodes.FINAL_ACTION_SUCCESS_CONDITION_NOT_MET,
            UIActionResultCodes.NON_TOGGLE_ACTION,
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
        int WAITING_FOR_PREVIOUS_UIACTION_TO_FINISH = 8;
        int EXCEPTION_WHILE_WAITING_FOR_SCREEN_TRANSITION = 9;
        int UI_MANAGER_UNINITIALIZED = 10;
        int ANOTHER_SERVER_REQUEST_IN_FLIGHT = 11;
        int INVALID_ACTION_DETAIL = 12;
        int ROOT_WINDOW_IS_NULL = 13;
        int FINAL_ACTION_SCREEN_MATCH_FAILED = 14;
        int FINAL_ACTION_SUCCESS_CONDITION_NOT_MET = 15;
        int NON_TOGGLE_ACTION = 16;
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
            case UIActionResultCodes.WAITING_FOR_PREVIOUS_UIACTION_TO_FINISH:
                return "WAITING_FOR_PREVIOUS_UIACTION_TO_FINISH";
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
            default:
                return "UNKNOWN";
        }
    }

    private @UIActionResultCodes int status;
    private String statusString;
    private String packageName;
    private String query;

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
        return  (actionResult != null && actionResult.getStatus() == SUCCESS);
    }
}
