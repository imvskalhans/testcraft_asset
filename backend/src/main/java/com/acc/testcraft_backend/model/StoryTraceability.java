package com.acc.testcraft_backend.model;

import java.util.ArrayList;
import java.util.List;

public class StoryTraceability {

    private String storyKey;
    private String storySummary;
    private String storyStatus;
    private String issueType;
    private String jiraUrl;
    private boolean hasCoverage;
    private boolean hasCycles;
    private boolean fullyTraced;
    private int linkedTestCaseCount;
    private int cycleCount;
    private int executionCount;
    private List<String> gaps = new ArrayList<>();
    private List<LinkedTestCaseRef> linkedTestCases = new ArrayList<>();
    private List<CycleTraceability> testCycles = new ArrayList<>();

    public String getStoryKey() {
        return storyKey;
    }

    public void setStoryKey(String storyKey) {
        this.storyKey = storyKey;
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

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public String getJiraUrl() {
        return jiraUrl;
    }

    public void setJiraUrl(String jiraUrl) {
        this.jiraUrl = jiraUrl;
    }

    public boolean isHasCoverage() {
        return hasCoverage;
    }

    public void setHasCoverage(boolean hasCoverage) {
        this.hasCoverage = hasCoverage;
    }

    public boolean isHasCycles() {
        return hasCycles;
    }

    public void setHasCycles(boolean hasCycles) {
        this.hasCycles = hasCycles;
    }

    public boolean isFullyTraced() {
        return fullyTraced;
    }

    public void setFullyTraced(boolean fullyTraced) {
        this.fullyTraced = fullyTraced;
    }

    public int getLinkedTestCaseCount() {
        return linkedTestCaseCount;
    }

    public void setLinkedTestCaseCount(int linkedTestCaseCount) {
        this.linkedTestCaseCount = linkedTestCaseCount;
    }

    public int getCycleCount() {
        return cycleCount;
    }

    public void setCycleCount(int cycleCount) {
        this.cycleCount = cycleCount;
    }

    public int getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(int executionCount) {
        this.executionCount = executionCount;
    }

    public List<String> getGaps() {
        return gaps;
    }

    public void setGaps(List<String> gaps) {
        this.gaps = gaps;
    }

    public List<LinkedTestCaseRef> getLinkedTestCases() {
        return linkedTestCases;
    }

    public void setLinkedTestCases(List<LinkedTestCaseRef> linkedTestCases) {
        this.linkedTestCases = linkedTestCases;
    }

    public List<CycleTraceability> getTestCycles() {
        return testCycles;
    }

    public void setTestCycles(List<CycleTraceability> testCycles) {
        this.testCycles = testCycles;
    }
}
