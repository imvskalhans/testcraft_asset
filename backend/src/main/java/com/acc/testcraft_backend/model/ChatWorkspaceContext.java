package com.acc.testcraft_backend.model;

import java.util.ArrayList;
import java.util.List;

public class ChatWorkspaceContext {

    private String issueKey;
    private String currentPage;
    private String storySummary;
    private String storyStatus;
    private String storyType;
    private String storyDetails;
    private int testCaseCount;
    private String testType;
    private List<String> testCaseSummaries = new ArrayList<>();
    private List<String> publishedKeys = new ArrayList<>();

    public String getIssueKey() {
        return issueKey;
    }

    public void setIssueKey(String issueKey) {
        this.issueKey = issueKey;
    }

    public String getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(String currentPage) {
        this.currentPage = currentPage;
    }

    public String getStorySummary() {
        return storySummary;
    }

    public void setStorySummary(String storySummary) {
        this.storySummary = storySummary;
    }

    public String getStoryStatus() {
        return storyStatus;
    }

    public void setStoryStatus(String storyStatus) {
        this.storyStatus = storyStatus;
    }

    public String getStoryType() {
        return storyType;
    }

    public void setStoryType(String storyType) {
        this.storyType = storyType;
    }

    public String getStoryDetails() {
        return storyDetails;
    }

    public void setStoryDetails(String storyDetails) {
        this.storyDetails = storyDetails;
    }

    public int getTestCaseCount() {
        return testCaseCount;
    }

    public void setTestCaseCount(int testCaseCount) {
        this.testCaseCount = testCaseCount;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public List<String> getTestCaseSummaries() {
        return testCaseSummaries;
    }

    public void setTestCaseSummaries(List<String> testCaseSummaries) {
        this.testCaseSummaries = testCaseSummaries;
    }

    public List<String> getPublishedKeys() {
        return publishedKeys;
    }

    public void setPublishedKeys(List<String> publishedKeys) {
        this.publishedKeys = publishedKeys;
    }
}
