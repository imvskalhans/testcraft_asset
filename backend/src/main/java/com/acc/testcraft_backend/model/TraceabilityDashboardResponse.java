package com.acc.testcraft_backend.model;

import java.util.ArrayList;
import java.util.List;

public class TraceabilityDashboardResponse {

    private boolean success;
    private String issueKey;
    private String issueSummary;
    private String issueStatus;
    private String issueType;
    private String jiraUrl;
    private int totalStories;
    private int storiesWithCoverage;
    private int storiesWithCycles;
    private int storiesFullyTraced;
    private int totalLinkedTestCases;
    private int totalCycles;
    private int totalExecutions;
    private List<String> gaps = new ArrayList<>();
    private List<StoryTraceability> stories = new ArrayList<>();
    private String message;
    private String error;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public void setIssueKey(String issueKey) {
        this.issueKey = issueKey;
    }

    public String getIssueSummary() {
        return issueSummary;
    }

    public void setIssueSummary(String issueSummary) {
        this.issueSummary = issueSummary;
    }

    public String getIssueStatus() {
        return issueStatus;
    }

    public void setIssueStatus(String issueStatus) {
        this.issueStatus = issueStatus;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getJiraUrl() {
        return jiraUrl;
    }

    public void setJiraUrl(String jiraUrl) {
        this.jiraUrl = jiraUrl;
    }

    public int getTotalStories() {
        return totalStories;
    }

    public void setTotalStories(int totalStories) {
        this.totalStories = totalStories;
    }

    public int getStoriesWithCoverage() {
        return storiesWithCoverage;
    }

    public void setStoriesWithCoverage(int storiesWithCoverage) {
        this.storiesWithCoverage = storiesWithCoverage;
    }

    public int getStoriesWithCycles() {
        return storiesWithCycles;
    }

    public void setStoriesWithCycles(int storiesWithCycles) {
        this.storiesWithCycles = storiesWithCycles;
    }

    public int getStoriesFullyTraced() {
        return storiesFullyTraced;
    }

    public void setStoriesFullyTraced(int storiesFullyTraced) {
        this.storiesFullyTraced = storiesFullyTraced;
    }

    public int getTotalLinkedTestCases() {
        return totalLinkedTestCases;
    }

    public void setTotalLinkedTestCases(int totalLinkedTestCases) {
        this.totalLinkedTestCases = totalLinkedTestCases;
    }

    public int getTotalCycles() {
        return totalCycles;
    }

    public void setTotalCycles(int totalCycles) {
        this.totalCycles = totalCycles;
    }

    public int getTotalExecutions() {
        return totalExecutions;
    }

    public void setTotalExecutions(int totalExecutions) {
        this.totalExecutions = totalExecutions;
    }

    public List<String> getGaps() {
        return gaps;
    }

    public void setGaps(List<String> gaps) {
        this.gaps = gaps;
    }

    public List<StoryTraceability> getStories() {
        return stories;
    }

    public void setStories(List<StoryTraceability> stories) {
        this.stories = stories;
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
