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
    private int passedCount;
    private int failedCount;
    private int blockedCount;
    private int notExecutedCount;
    private List<String> acceptanceCriteria = new ArrayList<>();
    private List<String> uncoveredCriteria = new ArrayList<>();
    private List<String> gaps = new ArrayList<>();
    private List<LinkedTestCaseRef> linkedTestCases = new ArrayList<>();
    private List<CycleTraceability> testCycles = new ArrayList<>();
    private AiCoverageAnalysis aiCoverage;

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

    public int getPassedCount() { return passedCount; }
    public void setPassedCount(int passedCount) { this.passedCount = passedCount; }
    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
    public int getBlockedCount() { return blockedCount; }
    public void setBlockedCount(int blockedCount) { this.blockedCount = blockedCount; }
    public int getNotExecutedCount() { return notExecutedCount; }
    public void setNotExecutedCount(int notExecutedCount) { this.notExecutedCount = notExecutedCount; }

    public List<String> getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(List<String> acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }
    public List<String> getUncoveredCriteria() { return uncoveredCriteria; }
    public void setUncoveredCriteria(List<String> uncoveredCriteria) { this.uncoveredCriteria = uncoveredCriteria; }

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

    public AiCoverageAnalysis getAiCoverage() {
        return aiCoverage;
    }

    public void setAiCoverage(AiCoverageAnalysis aiCoverage) {
        this.aiCoverage = aiCoverage;
    }
}
