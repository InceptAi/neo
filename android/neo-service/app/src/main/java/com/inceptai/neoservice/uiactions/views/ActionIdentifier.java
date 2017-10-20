package com.inceptai.neoservice.uiactions.views;

public class ActionIdentifier {
    private ScreenIdentifier screenIdentifier;
    private ElementIdentifier elementIdentifier;
    private String actionDescription;
    private String actionToTake;

    public ActionIdentifier(ScreenIdentifier screenIdentifier,
                            ElementIdentifier elementIdentifier,
                            String actionDescription,
                            String actionToTake) {
        this.screenIdentifier = screenIdentifier;
        this.elementIdentifier = elementIdentifier;
        this.actionDescription = actionDescription;
        this.actionToTake = actionToTake;
    }

    public ScreenIdentifier getScreenIdentifier() {
        return screenIdentifier;
    }

    public void setScreenIdentifier(ScreenIdentifier screenIdentifier) {
        this.screenIdentifier = screenIdentifier;
    }

    public ElementIdentifier getElementIdentifier() {
        return elementIdentifier;
    }

    public void setElementIdentifier(ElementIdentifier elementIdentifier) {
        this.elementIdentifier = elementIdentifier;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public void setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription;
    }

    public String getActionToTake() {
        return actionToTake;
    }

    public void setActionToTake(String actionToTake) {
        this.actionToTake = actionToTake;
    }
}