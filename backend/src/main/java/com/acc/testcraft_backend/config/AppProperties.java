package com.acc.testcraft_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * General TestCraft app settings — configure in application-local.properties.
 */
@ConfigurationProperties(prefix = "testcraft")
public class AppProperties {

    private String commentAuthor = "testcraft@local";
    /** Owner for Zephyr test cases/cycles; blank = jira.username */
    private String defaultOwner = "";
    private String corsOrigins = "http://localhost:5173,http://localhost:3000";
    private String supportEmail = "vsk6645@gmail.com";

    public String resolveOwner(JiraProperties jira) {
        if (defaultOwner != null && !defaultOwner.isBlank()) {
            return defaultOwner.trim();
        }
        if (jira.getUsername() != null && !jira.getUsername().isBlank()) {
            return jira.getUsername().trim();
        }
        return "testcraft";
    }

    public String getCommentAuthor() {
        return commentAuthor;
    }

    public void setCommentAuthor(String commentAuthor) {
        this.commentAuthor = commentAuthor;
    }

    public String getDefaultOwner() {
        return defaultOwner;
    }

    public void setDefaultOwner(String defaultOwner) {
        this.defaultOwner = defaultOwner;
    }

    public String getCorsOrigins() {
        return corsOrigins;
    }

    public void setCorsOrigins(String corsOrigins) {
        this.corsOrigins = corsOrigins;
    }

    public String getSupportEmail() { return supportEmail; }

    public void setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; }
}
