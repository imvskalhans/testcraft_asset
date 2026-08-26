package com.acc.testcraft_backend.model;

public class ReleaseAIResponse {

    private boolean success = true;
    private String crKey;
    private String action;
    private String analysis;
    private String error;
    private boolean mockMode;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

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

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isMockMode() {
        return mockMode;
    }

    public void setMockMode(boolean mockMode) {
        this.mockMode = mockMode;
    }
}
