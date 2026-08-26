package com.acc.testcraft_backend.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class FolderListResponse {

    private boolean success = true;
    private String projectId;
    private Map<String, String> folders = new LinkedHashMap<>();
    private String error;
    private String warning;

    public FolderListResponse(String projectId, Map<String, String> folders) {
        this.projectId = projectId;
        if (folders != null) {
            this.folders = folders;
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

    public Map<String, String> getFolders() {
        return folders;
    }

    public void setFolders(Map<String, String> folders) {
        this.folders = folders;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}
