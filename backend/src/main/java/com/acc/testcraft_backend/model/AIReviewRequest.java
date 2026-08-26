package com.acc.testcraft_backend.model;

public class AIReviewRequest {

    private String issueKey;
    private String jiraDetails;
    private String reviewType;

    public String getIssueKey() {
        return issueKey;
    }

    public void setIssueKey(String issueKey) {
        this.issueKey = issueKey;
    }

    public String getJiraDetails() {
        return jiraDetails;
    }

    public void setJiraDetails(String jiraDetails) {
        this.jiraDetails = jiraDetails;
    }

    public String getReviewType() {
        return reviewType;
    }

    public void setReviewType(String reviewType) {
        this.reviewType = reviewType;
    }
}
