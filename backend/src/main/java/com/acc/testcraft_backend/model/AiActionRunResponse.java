package com.acc.testcraft_backend.model;

public class AiActionRunResponse {

    private boolean success;
    private String actionId;
    private String result;
    private String resolvedPrompt;
    private boolean mockMode;
    private String error;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResolvedPrompt() {
        return resolvedPrompt;
    }

    public void setResolvedPrompt(String resolvedPrompt) {
        this.resolvedPrompt = resolvedPrompt;
    }

    public boolean isMockMode() {
        return mockMode;
    }

    public void setMockMode(boolean mockMode) {
        this.mockMode = mockMode;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
