package com.inceptai.neoservice.uiactions.views;

public class ScreenIdentifier {
    private String title;
    private String packageName;

    public ScreenIdentifier(String title, String packageName) {
        this.title = title;
        this.packageName = packageName;
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
}