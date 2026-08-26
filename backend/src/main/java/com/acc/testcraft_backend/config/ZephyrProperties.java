package com.acc.testcraft_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Zephyr / test management settings — configure in application-local.properties.
 */
@ConfigurationProperties(prefix = "zephyr")
public class ZephyrProperties {

    /**
     * jira-plugin = Zephyr API on Jira instance (/rest/zapi/latest or /rest/atm/1.0)
     * scale-cloud = Zephyr Scale Cloud API (api.zephyrscale.smartbear.com)
     */
    private String provider = "jira-plugin";

    /** Path appended to jira.base-url when provider=jira-plugin */
    private String jiraPluginApiPath = "/rest/atm/1.0";

    /** Alternate Zephyr tests API path on the Jira host (testrun items, etc.) */
    private String testsApiPath = "/rest/tests/1.0";

    /** Base URL when provider=scale-cloud */
    private String scaleCloudBaseUrl = "https://api.zephyrscale.smartbear.com/v2";

    /** API token for scale-cloud (separate from Jira token) */
    private String scaleCloudApiToken = "";

    /**
     * Zephyr Scale Cloud TM4J private API host (browser UI uses this for foldertree).
     * Example: https://eu.app.tm4j.smartbear.com/backend
     */
    private String tm4jBackendBaseUrl = "https://eu.app.tm4j.smartbear.com/backend";

    private String defaultProjectId = "";
    private String defaultProjectKey = "";
    private int defaultTestRunStatusId = 0;
    private String defaultTestCaseStatus = "Approved";
    private String defaultPriority = "Normal";

    /**
     * Comma-separated folders used when live Zephyr APIs cannot list them.
     * Each entry is {@code name} or {@code name:numericId}.
     */
    private String knownFolders = "";

    public String resolveApiBase(JiraProperties jira) {
        if (isScaleCloud()) {
            return trimTrailingSlash(scaleCloudBaseUrl);
        }
        return jira.getBaseUrl().replaceAll("/$", "")
                + jiraPluginApiPath;
    }

    public boolean isScaleCloud() {
        return "scale-cloud".equalsIgnoreCase(provider);
    }

    public boolean hasScaleCloudToken() {
        return scaleCloudApiToken != null && !scaleCloudApiToken.isBlank();
    }

    public boolean useScaleCloudApi() {
        return isScaleCloud() || hasScaleCloudToken();
    }

    public boolean isConfigured(JiraProperties jira) {
        if (isScaleCloud()) {
            return scaleCloudBaseUrl != null && !scaleCloudBaseUrl.isBlank()
                    && hasScaleCloudToken();
        }
        return jira.isConfigured();
    }

    public Map<String, String> parsedKnownFolders() {
        Map<String, String> folders = new LinkedHashMap<>();
        if (knownFolders == null || knownFolders.isBlank()) {
            return folders;
        }
        for (String part : knownFolders.split(",")) {
            String item = part.trim();
            if (item.isEmpty()) {
                continue;
            }
            int colon = item.lastIndexOf(':');
            if (colon > 0 && item.substring(colon + 1).trim().matches("\\d+")) {
                folders.put(item.substring(0, colon).trim(), item.substring(colon + 1).trim());
            } else {
                folders.put(item, item);
            }
        }
        return folders;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getJiraPluginApiPath() {
        return jiraPluginApiPath;
    }

    public void setJiraPluginApiPath(String jiraPluginApiPath) {
        this.jiraPluginApiPath = jiraPluginApiPath;
    }

    public String getTestsApiPath() {
        return testsApiPath;
    }

    public void setTestsApiPath(String testsApiPath) {
        this.testsApiPath = testsApiPath;
    }

    public String getScaleCloudBaseUrl() {
        return scaleCloudBaseUrl;
    }

    public void setScaleCloudBaseUrl(String scaleCloudBaseUrl) {
        this.scaleCloudBaseUrl = scaleCloudBaseUrl;
    }

    public String getScaleCloudApiToken() {
        return scaleCloudApiToken;
    }

    public void setScaleCloudApiToken(String scaleCloudApiToken) {
        this.scaleCloudApiToken = scaleCloudApiToken;
    }

    public String getTm4jBackendBaseUrl() {
        return tm4jBackendBaseUrl;
    }

    public void setTm4jBackendBaseUrl(String tm4jBackendBaseUrl) {
        this.tm4jBackendBaseUrl = tm4jBackendBaseUrl;
    }

    public String getDefaultProjectId() {
        return defaultProjectId;
    }

    public void setDefaultProjectId(String defaultProjectId) {
        this.defaultProjectId = defaultProjectId;
    }

    public String getDefaultProjectKey() {
        return defaultProjectKey;
    }

    public void setDefaultProjectKey(String defaultProjectKey) {
        this.defaultProjectKey = defaultProjectKey;
    }

    public int getDefaultTestRunStatusId() {
        return defaultTestRunStatusId;
    }

    public void setDefaultTestRunStatusId(int defaultTestRunStatusId) {
        this.defaultTestRunStatusId = defaultTestRunStatusId;
    }

    public String getDefaultTestCaseStatus() {
        return defaultTestCaseStatus;
    }

    public void setDefaultTestCaseStatus(String defaultTestCaseStatus) {
        this.defaultTestCaseStatus = defaultTestCaseStatus;
    }

    public String getDefaultPriority() {
        return defaultPriority;
    }

    public void setDefaultPriority(String defaultPriority) {
        this.defaultPriority = defaultPriority;
    }

    public String getKnownFolders() {
        return knownFolders;
    }

    public void setKnownFolders(String knownFolders) {
        this.knownFolders = knownFolders;
    }
}
