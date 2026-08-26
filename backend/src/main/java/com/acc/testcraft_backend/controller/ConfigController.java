package com.acc.testcraft_backend.controller;

import com.acc.testcraft_backend.client.AiClient;
import com.acc.testcraft_backend.client.JiraClient;
import com.acc.testcraft_backend.config.AiProperties;
import com.acc.testcraft_backend.config.AppProperties;
import com.acc.testcraft_backend.config.JiraProperties;
import com.acc.testcraft_backend.config.ZephyrProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exposes non-sensitive integration status — helps teams verify plug-and-play config.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final JiraProperties jira;
    private final ZephyrProperties zephyr;
    private final AiProperties ai;
    private final AppProperties app;
    private final JiraClient jiraClient;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConfigController(
            JiraProperties jira,
            ZephyrProperties zephyr,
            AiProperties ai,
            AppProperties app,
            JiraClient jiraClient,
            AiClient aiClient
    ) {
        this.jira = jira;
        this.zephyr = zephyr;
        this.ai = ai;
        this.app = app;
        this.jiraClient = jiraClient;
        this.aiClient = aiClient;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> currentUser() {
        try {
            Map<String, Object> user = jiraClient.getCurrentUser();
            user.put("success", true);
            user.put("configuredOwner", app.resolveOwner(jira));
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("displayName", app.resolveOwner(jira));
            response.put("emailAddress", jira.getUsername());
            response.put("owner", app.resolveOwner(jira));
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("configFile", "testcraft/backend/src/main/resources/application-local.properties");
        response.put("jira", jiraStatus());
        response.put("zephyr", zephyrStatus());
        response.put("ai", aiStatus());
        response.put("app", Map.of(
                "commentAuthor", app.getCommentAuthor(),
                "defaultOwner", app.resolveOwner(jira)
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/setup-guide")
    public ResponseEntity<Map<String, Object>> setupGuide() {
        try (InputStream in = new ClassPathResource("setup-guide.json").getInputStream()) {
            Map<String, Object> guide = objectMapper.readValue(
                    in,
                    new TypeReference<LinkedHashMap<String, Object>>() {}
            );
            guide.put("success", true);
            guide.put("markdownPath", "testcraft/config/SETUP.md");
            return ResponseEntity.ok(guide);
        } catch (Exception e) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private Map<String, Object> jiraStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("configured", jira.isConfigured());
        status.put("baseUrl", maskUrl(jira.getBaseUrl()));
        status.put("apiPath", jira.getApiPath());
        status.put("authType", jira.getAuthType());
        status.put("username", jira.getUsername());
        status.put("connected", jira.isConfigured() && jiraClient.testConnection());
        status.put("customFields", Map.of(
                "acceptanceCriteria", jira.getCustomFields().getAcceptanceCriteria(),
                "linkedStories", jira.getCustomFields().getLinkedStories()
        ));
        return status;
    }

    private Map<String, Object> zephyrStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("provider", zephyr.getProvider());
        status.put("configured", zephyr.isConfigured(jira));
        status.put("defaultProjectId", zephyr.getDefaultProjectId());
        status.put("defaultProjectKey", zephyr.getDefaultProjectKey());
        status.put("scaleCloudTokenConfigured", zephyr.hasScaleCloudToken());
        status.put("tm4jBackendBaseUrl", zephyr.getTm4jBackendBaseUrl());
        status.put("defaultTestCaseStatus", zephyr.getDefaultTestCaseStatus());
        status.put("defaultPriority", zephyr.getDefaultPriority());
        if (zephyr.isScaleCloud()) {
            status.put("baseUrl", zephyr.getScaleCloudBaseUrl());
        } else {
            status.put("jiraPluginApiPath", zephyr.getJiraPluginApiPath());
        }
        return status;
    }

    private Map<String, Object> aiStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("provider", ai.getProvider());
        status.put("mockMode", aiClient.isMockMode());
        status.put("configured", aiClient.isConfigured());
        return status;
    }

    private static String maskUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.replaceAll("/$", "");
    }
}
