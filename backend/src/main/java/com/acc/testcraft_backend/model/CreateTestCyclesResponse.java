package com.acc.testcraft_backend.model;

import java.util.ArrayList;
import java.util.List;

public class CreateTestCyclesResponse {

    private boolean success;
    private String crKey;
    private int cyclesCreated;
    private int storiesProcessed;
    private List<String> createdCycleIds = new ArrayList<>();
    private String message;
    private String error;

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

    public int getCyclesCreated() {
        return cyclesCreated;
    }

    public void setCyclesCreated(int cyclesCreated) {
        this.cyclesCreated = cyclesCreated;
    }

    public int getStoriesProcessed() {
        return storiesProcessed;
    }

    public void setStoriesProcessed(int storiesProcessed) {
        this.storiesProcessed = storiesProcessed;
    }

    public List<String> getCreatedCycleIds() {
        return createdCycleIds;
    }

    public void setCreatedCycleIds(List<String> createdCycleIds) {
        this.createdCycleIds = createdCycleIds;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
