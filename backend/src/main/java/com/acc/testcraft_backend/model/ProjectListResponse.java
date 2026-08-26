package com.acc.testcraft_backend.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProjectListResponse {

    private boolean success = true;
    private Map<String, String> projects = new LinkedHashMap<>();
    private String error;

    public ProjectListResponse(Map<String, String> projects) {
        if (projects != null) {
            this.projects = projects;
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Map<String, String> getProjects() {
        return projects;
    }

    public void setProjects(Map<String, String> projects) {
        this.projects = projects;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
