package com.acc.testcraft_backend.service;

import com.acc.testcraft_backend.client.JiraClient;
import com.acc.testcraft_backend.config.AppProperties;
import com.acc.testcraft_backend.config.JiraProperties;
import com.acc.testcraft_backend.model.JiraStory;
import org.springframework.stereotype.Service;

@Service
public class JiraService {

    private final JiraClient jiraClient;
    private final AppProperties appProperties;
    private final JiraProperties jiraProperties;
    private String lastResolvedCommentAuthor;

    public JiraService(
            JiraClient jiraClient,
            AppProperties appProperties,
            JiraProperties jiraProperties
    ) {
        this.jiraClient = jiraClient;
        this.appProperties = appProperties;
        this.jiraProperties = jiraProperties;
    }

    public boolean testConnection() {
        return jiraClient.testConnection();
    }

    public JiraStory fetchStory(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Issue key is required");
        }
        return jiraClient.fetchIssue(key.trim().toUpperCase());
    }

    public void postComment(String issueKey, String comment) {
        if (issueKey == null || issueKey.isBlank()) {
            throw new IllegalArgumentException("Issue key is required");
        }
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Comment is required");
        }

        lastResolvedCommentAuthor = appProperties.resolveOwner(jiraProperties);
        jiraClient.postComment(issueKey.trim().toUpperCase(), comment);
    }

    public String getLastResolvedCommentAuthor() {
        return lastResolvedCommentAuthor != null
                ? lastResolvedCommentAuthor
                : appProperties.getCommentAuthor();
    }

    public String getCommentAuthor() {
        return appProperties.getCommentAuthor();
    }
}
