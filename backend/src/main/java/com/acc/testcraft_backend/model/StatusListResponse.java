package com.acc.testcraft_backend.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class StatusListResponse {

    private boolean success = true;
    private String projectId;
    private Map<String, String> statuses = new LinkedHashMap<>();
    private String error;

    public StatusListResponse(String projectId, Map<String, String> statuses) {
        this.projectId = projectId;
        if (statuses != null) {
            this.statuses = statuses;
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Map<String, String> getStatuses() {
        return statuses;
    }

    public void setStatuses(Map<String, String> statuses) {
        this.statuses = statuses;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
