package com.acc.testcraft_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Jira connection settings — configure in application-local.properties.
 */
@ConfigurationProperties(prefix = "jira")
public class JiraProperties {

    private String baseUrl = "";
    private String apiPath = "/rest/api/3";
    private String authType = "basic";
    private String username = "";
    private String apiToken = "";
    private String bearerToken = "";
    private CustomFields customFields = new CustomFields();

    public String apiUrl(String suffix) {
        return trimTrailingSlash(baseUrl) + apiPath + suffix;
    }

    public boolean isConfigured() {
        if ("bearer".equalsIgnoreCase(authType)) {
            return baseUrl != null && !baseUrl.isBlank()
                    && bearerToken != null && !bearerToken.isBlank();
        }
        return baseUrl != null && !baseUrl.isBlank()
                && username != null && !username.isBlank()
                && apiToken != null && !apiToken.isBlank();
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiPath() {
        return apiPath;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public CustomFields getCustomFields() {
        return customFields;
    }

    public void setCustomFields(CustomFields customFields) {
        this.customFields = customFields;
    }

    public static class CustomFields {
        /** Jira custom field id for acceptance criteria, e.g. customfield_10101 */
        private String acceptanceCriteria = "";
        /** Jira custom field id for linked stories on CRs, e.g. customfield_11314 */
        private String linkedStories = "";

        public String getAcceptanceCriteria() {
            return acceptanceCriteria;
        }

        public void setAcceptanceCriteria(String acceptanceCriteria) {
            this.acceptanceCriteria = acceptanceCriteria;
        }

        public String getLinkedStories() {
            return linkedStories;
        }

        public void setLinkedStories(String linkedStories) {
            this.linkedStories = linkedStories;
        }
    }
}
