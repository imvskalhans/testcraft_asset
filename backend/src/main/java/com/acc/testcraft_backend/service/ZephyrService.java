package com.acc.testcraft_backend.service;

import com.acc.testcraft_backend.client.JiraClient;
import com.acc.testcraft_backend.client.ZephyrClient;
import com.acc.testcraft_backend.config.AppProperties;
import com.acc.testcraft_backend.config.JiraProperties;
import com.acc.testcraft_backend.config.ZephyrProperties;
import com.acc.testcraft_backend.model.TestCase;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ZephyrService {

    private final ZephyrClient zephyrClient;
    private final JiraClient jiraClient;
    private final AppProperties appProperties;
    private final JiraProperties jiraProperties;
    private final ZephyrProperties zephyrProperties;
    private String lastResolvedOwner;

    public ZephyrService(
            ZephyrClient zephyrClient,
            JiraClient jiraClient,
            AppProperties appProperties,
            JiraProperties jiraProperties,
            ZephyrProperties zephyrProperties
    ) {
        this.zephyrClient = zephyrClient;
        this.jiraClient = jiraClient;
        this.appProperties = appProperties;
        this.jiraProperties = jiraProperties;
        this.zephyrProperties = zephyrProperties;
    }

    public Map<String, String> getProjects() {
        try {
            Map<String, String> zephyrProjects = zephyrClient.getProjects();
            if (zephyrProjects != null && !zephyrProjects.isEmpty()) {
                return zephyrProjects;
            }
        } catch (Exception e) {
            System.err.println(
                    "Zephyr project list failed, falling back to Jira: "
                            + e.getMessage()
            );
        }

        return jiraClient.listProjects();
    }

    public Map<String, String> getProjectFolders(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("Project ID is required");
        }
        return zephyrClient.getProjectFolders(projectId);
    }

    public String getLastFolderWarning() {
        return zephyrClient.getLastFolderWarning();
    }

    public List<com.acc.testcraft_backend.model.TestCycle> listProjectTestCycles(String projectId) {
        return zephyrClient.listProjectTestCycles(projectId);
    }

    public Map<String, String> getProjectStatuses(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("Project ID is required");
        }

        List<String> names = zephyrClient.getStatusNames(projectId);
        Map<String, String> statuses = new LinkedHashMap<>();

        for (String name : names) {
            statuses.put(name, zephyrClient.getStatusIdByName(projectId, name));
        }

        return statuses;
    }

    public Map<String, String> getProjectPriorities(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("Project ID is required");
        }
        return zephyrClient.getPriorityNames(projectId);
    }

    public boolean isValidSystemUser(String username) {
        return jiraClient.isValidJiraUser(username);
    }

    public Map<String, Object> getCurrentUser() {
        return jiraClient.getCurrentUser();
    }

    public String publishTestCase(
            TestCase testCase,
            String projectId,
            String folderId,
            String owner,
            String statusId
    ) {
        if (testCase == null) {
            throw new IllegalArgumentException("Test case is required");
        }

        String resolvedOwner = resolveOwner(owner);
        lastResolvedOwner = resolvedOwner;

        // Scale Cloud accepts statusName/priorityName directly. The numeric
        // status and priority endpoints below belong to the Jira-plugin API.
        if (zephyrProperties.useScaleCloudApi() && zephyrProperties.hasScaleCloudToken()) {
            return zephyrClient.postTestCase(
                    testCase,
                    projectId,
                    folderId,
                    resolvedOwner,
                    null,
                    null
            );
        }

        String resolvedStatusId = statusId;
        if (resolvedStatusId == null || resolvedStatusId.isBlank()) {
            resolvedStatusId = zephyrClient.getStatusIdByName(
                    projectId,
                    zephyrProperties.getDefaultTestCaseStatus()
            );
        }

        String priorityName = testCase.getPriority() != null && !testCase.getPriority().isBlank()
                ? testCase.getPriority()
                : zephyrProperties.getDefaultPriority();
        String priorityId = zephyrClient.getPriorityIdByName(projectId, priorityName);

        return zephyrClient.postTestCase(
                testCase,
                projectId,
                folderId,
                resolvedOwner,
                resolvedStatusId,
                priorityId
        );
    }

    public void linkTestCaseToIssue(String testCaseKey, String issueKey) {
        zephyrClient.linkTestToIssue(testCaseKey, issueKey);
    }

    public String getLastResolvedOwner() {
        return lastResolvedOwner;
    }

    public String getConfiguredOwner() {
        return appProperties.resolveOwner(jiraProperties);
    }

    private String resolveOwner(String owner) {
        if (owner != null && !owner.isBlank()) {
            String resolved = jiraClient.getZephyrOwnerId(owner);
            return resolved != null ? resolved : owner;
        }

        try {
            Map<String, Object> current = jiraClient.getCurrentUser();
            Object accountId = current.get("accountId");
            if (accountId != null && !accountId.toString().isBlank()) {
                return accountId.toString();
            }
            Object configured = current.get("owner");
            if (configured != null && !configured.toString().isBlank()) {
                return configured.toString();
            }
        } catch (Exception ignored) {
            // Fall through to configured owner
        }

        String fallback = appProperties.resolveOwner(jiraProperties);
        String resolved = jiraClient.getZephyrOwnerId(fallback);
        return resolved != null ? resolved : fallback;
    }
}
