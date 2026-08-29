package com.acc.testcraft_backend.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class AiActionRunRequest {

    private String actionId;
    private String promptTemplate;
    private boolean includeJiraContext;
    private boolean includeCrContext;
    private String issueKey;
    private String jiraDetails;
    private String crKey;
    private String crDetails;
    private Map<String, String> inputs = new LinkedHashMap<>();

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public boolean isIncludeJiraContext() {
        return includeJiraContext;
    }

    public void setIncludeJiraContext(boolean includeJiraContext) {
        this.includeJiraContext = includeJiraContext;
    }

    public boolean isIncludeCrContext() {
        return includeCrContext;
    }

    public void setIncludeCrContext(boolean includeCrContext) {
        this.includeCrContext = includeCrContext;
    }

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

    public String getCrKey() {
        return crKey;
    }

    public void setCrKey(String crKey) {
        this.crKey = crKey;
    }

    public String getCrDetails() {
        return crDetails;
    }

    public void setCrDetails(String crDetails) {
        this.crDetails = crDetails;
    }

    public Map<String, String> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, String> inputs) {
        this.inputs = inputs;
    }
}
