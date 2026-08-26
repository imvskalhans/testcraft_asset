package com.acc.testcraft_backend.model;

public class ReleaseAIRequest {

    private String crKey;
    private String action;
    private String context;

    public String getCrKey() {
        return crKey;
    }

    public void setCrKey(String crKey) {
        this.crKey = crKey;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
