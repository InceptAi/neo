package com.inceptai.neoservice.uiactions.views;

public class ElementIdentifier {
    private String className;
    private String packageName;
    private String primaryText;
    private String childText;

    public ElementIdentifier(String className, String packageName, String primaryText, String childText) {
        this.className = className;
        this.packageName = packageName;
        this.primaryText = primaryText;
        this.childText = childText;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPrimaryText() {
        return primaryText;
    }

    public void setPrimaryText(String primaryText) {
        this.primaryText = primaryText;
    }

    public String getChildText() {
        return childText;
    }

    public void setChildText(String childText) {
        this.childText = childText;
    }
}
