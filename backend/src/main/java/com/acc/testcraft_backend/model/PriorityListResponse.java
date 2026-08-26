package com.acc.testcraft_backend.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class PriorityListResponse {

    private boolean success = true;
    private String projectId;
    private Map<String, String> priorities = new LinkedHashMap<>();
    private String error;

    public PriorityListResponse(String projectId, Map<String, String> priorities) {
        this.projectId = projectId;
        if (priorities != null) {
            this.priorities = priorities;
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

    public Map<String, String> getPriorities() {
        return priorities;
    }

    public void setPriorities(Map<String, String> priorities) {
        this.priorities = priorities;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
