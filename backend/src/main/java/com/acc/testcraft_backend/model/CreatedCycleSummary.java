package com.acc.testcraft_backend.model;

public class CreatedCycleSummary {
    private String id;
    private String name;
    private String storyKey;
    private String jiraUrl;
    private String zephyrUrl;
    private String folderPath;
    private String action;

    public CreatedCycleSummary() {
    }

    public CreatedCycleSummary(String id, String name, String storyKey, String jiraUrl, String zephyrUrl,
                               String folderPath, String action) {
        this.id = id;
        this.name = name;
        this.storyKey = storyKey;
        this.jiraUrl = jiraUrl;
        this.zephyrUrl = zephyrUrl;
        this.folderPath = folderPath;
        this.action = action;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStoryKey() { return storyKey; }
    public void setStoryKey(String storyKey) { this.storyKey = storyKey; }
    public String getJiraUrl() { return jiraUrl; }
    public void setJiraUrl(String jiraUrl) { this.jiraUrl = jiraUrl; }
    public String getZephyrUrl() { return zephyrUrl; }
    public void setZephyrUrl(String zephyrUrl) { this.zephyrUrl = zephyrUrl; }
    public String getFolderPath() { return folderPath; }
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
