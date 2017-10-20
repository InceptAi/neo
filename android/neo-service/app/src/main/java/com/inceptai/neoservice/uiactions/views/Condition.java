package com.inceptai.neoservice.uiactions.views;

public class Condition {
    private String textToMatch;
    private int minValue = 0;
    private int maxValue = 0;

    public Condition() {}

    public Condition(String textToMatch) {
        this.textToMatch = textToMatch;
    }

    public Condition(int minValue, int maxValue) {
        this.maxValue = maxValue;
        this.minValue = minValue;
    }

    public String getTextToMatch() {
        return textToMatch;
    }

    public void setTextToMatch(String textToMatch) {
        this.textToMatch = textToMatch;
    }

    public int getMinValue() {
        return minValue;
    }

    public void setMinValue(int minValue) {
        this.minValue = minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }
}