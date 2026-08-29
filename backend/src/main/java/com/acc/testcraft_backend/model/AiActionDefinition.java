package com.acc.testcraft_backend.model;

import java.util.ArrayList;
import java.util.List;

public class AiActionDefinition {

    private String id;
    private String label;
    private String description;
    private String category;
    private boolean supportsJiraContext;
    private boolean supportsCrContext;
    private String defaultPromptTemplate;
    private List<AiActionInputField> inputFields = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isSupportsJiraContext() {
        return supportsJiraContext;
    }

    public void setSupportsJiraContext(boolean supportsJiraContext) {
        this.supportsJiraContext = supportsJiraContext;
    }

    public boolean isSupportsCrContext() {
        return supportsCrContext;
    }

    public void setSupportsCrContext(boolean supportsCrContext) {
        this.supportsCrContext = supportsCrContext;
    }

    public String getDefaultPromptTemplate() {
        return defaultPromptTemplate;
    }

    public void setDefaultPromptTemplate(String defaultPromptTemplate) {
        this.defaultPromptTemplate = defaultPromptTemplate;
    }

    public List<AiActionInputField> getInputFields() {
        return inputFields;
    }

    public void setInputFields(List<AiActionInputField> inputFields) {
        this.inputFields = inputFields;
    }
}
