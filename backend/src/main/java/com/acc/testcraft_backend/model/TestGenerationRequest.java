package com.acc.testcraft_backend.model;

import java.util.ArrayList;
import java.util.List;

public class TestGenerationRequest {

    private String issueKey;
    private String testType;
    private int testCount;
    private String jiraDetails;
    private String promptType;
    private String customPrompt;
    private String additionalInstructions;
    private List<AiAttachment> attachments = new ArrayList<>();

    public String getIssueKey() {
        return issueKey;
    }

    public void setIssueKey(String issueKey) {
        this.issueKey = issueKey;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public int getTestCount() {
        return testCount;
    }

    public void setTestCount(int testCount) {
        this.testCount = testCount;
    }

    public String getJiraDetails() {
        return jiraDetails;
    }

    public void setJiraDetails(String jiraDetails) {
        this.jiraDetails = jiraDetails;
    }

    public String getPromptType() {
        return promptType;
    }

    public void setPromptType(String promptType) {
        this.promptType = promptType;
    }

    public String getCustomPrompt() {
        return customPrompt;
    }

    public void setCustomPrompt(String customPrompt) {
        this.customPrompt = customPrompt;
    }

    public String getAdditionalInstructions() {
        return additionalInstructions;
    }

    public void setAdditionalInstructions(String additionalInstructions) {
        this.additionalInstructions = additionalInstructions;
    }

    public List<AiAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<AiAttachment> attachments) { this.attachments = attachments; }
}
