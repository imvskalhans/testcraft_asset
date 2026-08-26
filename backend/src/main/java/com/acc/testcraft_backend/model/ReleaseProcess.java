package com.acc.testcraft_backend.model;

import java.util.ArrayList;
import java.util.List;

public class ReleaseProcess {

    private String crId;
    private String crKey;
    private String crSummary;
    private String crDescription;
    private String status;
    private String projectId;
    private String productVersionId;
    private int folderId;
    private String createdBy;
    private String createdDate;
    private List<LinkedStory> linkedStories = new ArrayList<>();

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

    public int getFolderId() {
        return folderId;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public List<LinkedStory> getLinkedStories() {
        return linkedStories;
    }

    public void setLinkedStories(List<LinkedStory> linkedStories) {
        this.linkedStories = linkedStories;
    }
}
