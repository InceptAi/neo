package com.inceptai.neoexpert;

/**
 * Created by arunesh on 7/13/17.
 */

public class ViewEntry {

    private String viewId;
    private String text;

    public ViewEntry(String viewId, String text) {
        this.viewId = viewId;
        this.text = text;
    }

    public String getViewId() {
        return viewId;
    }

    public String getText() {
        return text;
    }
}
