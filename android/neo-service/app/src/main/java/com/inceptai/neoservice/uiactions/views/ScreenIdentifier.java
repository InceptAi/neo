package com.inceptai.neoservice.uiactions.views;

public class ScreenIdentifier {
    private String title;
    private String subTitle;
    private String packageName;
    private String screenType;

    public ScreenIdentifier(String title, String subTitle, String packageName, String screenType) {
        this.title = title;
        this.subTitle = subTitle;
        this.packageName = packageName;
        this.screenType = screenType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public String getScreenType() {
        return screenType;
    }

    public void setScreenType(String screenType) {
        this.screenType = screenType;
    }

    @Override
    public String toString() {
        return "ScreenIdentifier{" +
                "title='" + title + '\'' +
                ", subTitle='" + subTitle + '\'' +
                ", packageName='" + packageName + '\'' +
                ", screenType='" + screenType + '\'' +
                '}';
    }
}