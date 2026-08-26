package com.acc.testcraft_backend.model;

import java.util.ArrayList;
import java.util.List;

public class FetchCRResponse {

    private boolean success;
    private String crId;
    private String crKey;
    private String projectId;
    private String productVersionId;
    private String crSummary;
    private String crDescription;
    private String status;
    private List<LinkedStory> linkedStories = new ArrayList<>();
    private String message;
    private String error;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCrId() {
        return crId;
    }

    public void setCrId(String crId) {
        this.crId = crId;
    }

    public String getCrKey() {
        return crKey;
    }

    public void setCrKey(String crKey) {
        this.crKey = crKey;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProductVersionId() {
        return productVersionId;
    }

    public void setProductVersionId(String productVersionId) {
        this.productVersionId = productVersionId;
    }

    public String getCrSummary() {
        return crSummary;
    }

    public void setCrSummary(String crSummary) {
        this.crSummary = crSummary;
    }

    public String getCrDescription() {
        return crDescription;
    }

    public void setCrDescription(String crDescription) {
        this.crDescription = crDescription;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<LinkedStory> getLinkedStories() {
        return linkedStories;
    }

    public void setLinkedStories(List<LinkedStory> linkedStories) {
        this.linkedStories = linkedStories;
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
