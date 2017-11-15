package com.inceptai.neoservice.uiactions.model;

import android.util.Log;

import com.inceptai.neoservice.Utils;

import static com.inceptai.neoservice.Utils.TAG;

/**
 * Created by vivek on 10/30/17.
 */

public class ScreenInfo {
    public static final String FULL_SCREEN_MODE = "FULL";
    public static final String PARTIAL_SCREEN_MODE = "PARTIAL";
    public static final String UNDEFINED_SCREEN_MODE = "UNDEFINED";
    private String title;
    private String packageName;
    private String screenType;
    private String subTitle;
    private String appVersion;
    private String versionCode;

    public ScreenInfo(String title, String subTitle, String packageName,
                      boolean isFullScreen, String appVersion, String versionCode) {
        this.title = title;
        this.subTitle = subTitle;
        this.packageName = packageName;
        this.screenType = isFullScreen ? FULL_SCREEN_MODE : PARTIAL_SCREEN_MODE;
        this.appVersion = appVersion;
        this.versionCode = versionCode;
    }

    public ScreenInfo() {
        this.title = Utils.EMPTY_STRING;
        this.subTitle = Utils.EMPTY_STRING;
        this.packageName = Utils.EMPTY_STRING;
        this.screenType = UNDEFINED_SCREEN_MODE;
        this.appVersion = Utils.EMPTY_STRING;
        this.versionCode = Utils.EMPTY_STRING;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
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

    public String getScreenType() {
        return screenType;
    }

    public void setScreenType(String screenType) {
        this.screenType = screenType;
    }

    public void setScreenType(boolean isFullScreen) {
        this.screenType = isFullScreen ? FULL_SCREEN_MODE : PARTIAL_SCREEN_MODE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScreenInfo that = (ScreenInfo) o;

        if (!title.equals(that.title)) return false;
        if (!packageName.equals(that.packageName)) return false;
        if (!screenType.equals(that.screenType)) return false;
        return subTitle.equals(that.subTitle);

    }

    @Override
    public int hashCode() {
        int result = title.hashCode();
        result = 31 * result + packageName.hashCode();
        result = 31 * result + screenType.hashCode();
        result = 31 * result + subTitle.hashCode();
        return result;
    }

    public boolean isEmpty() {
        return title.isEmpty() || packageName.isEmpty();
    }

    public static String getScreenType(boolean isFullScreen) {
        return isFullScreen ? FULL_SCREEN_MODE : PARTIAL_SCREEN_MODE;
    }

    @Override
    public String toString() {
        return "ScreenInfo{" +
                "title='" + title + '\'' +
                ", packageName='" + packageName + '\'' +
                ", screenType='" + screenType + '\'' +
                ", subTitle='" + subTitle + '\'' +
                '}';
    }

    public boolean isTransitionScreen() {
        String titleToMatch = Utils.sanitizeText(title);
        String subTitleToMatch = Utils.sanitizeText(subTitle);
        Log.d(TAG, "ScreenInfo, in isTransitionScreen, checking title: " +  titleToMatch);
        return titleToMatch.equalsIgnoreCase("loading") || subTitleToMatch.equalsIgnoreCase("loading");
    }
}
