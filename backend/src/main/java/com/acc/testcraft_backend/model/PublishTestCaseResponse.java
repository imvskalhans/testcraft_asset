package com.acc.testcraft_backend.model;

public class PublishTestCaseResponse {

    private boolean success = true;
    private String testCaseKey;
    private String testName;
    private String projectId;
    private String folderId;
    private String owner;
    private String status;
    private String testCaseUrl;
    private String error;

    public PublishTestCaseResponse() {
    }

    public PublishTestCaseResponse(String testCaseKey, String testName) {
        this.testCaseKey = testCaseKey;
        this.testName = testName;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getTestCaseKey() {
        return testCaseKey;
    }

    public void setTestCaseKey(String testCaseKey) {
        this.testCaseKey = testCaseKey;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTestCaseUrl() {
        return testCaseUrl;
    }

    public void setTestCaseUrl(String testCaseUrl) {
        this.testCaseUrl = testCaseUrl;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
