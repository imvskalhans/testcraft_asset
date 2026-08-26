package com.acc.testcraft_backend.client;

import com.acc.testcraft_backend.config.IntegrationConfig.IntegrationAuthSupport;
import com.acc.testcraft_backend.config.JiraProperties;
import com.acc.testcraft_backend.config.ZephyrProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Obtains short-lived Zephyr Scale context JWT from Jira (used by eu.app.tm4j.smartbear.com).
 */
@Component
public class ZephyrContextClient {

    private static final Pattern CONTEXT_JWT =
            Pattern.compile("\"contextJwt\"\\s*:\\s*\"([^\"]+)\"");

    private final RestTemplate restTemplate;
    private final JiraProperties jiraProperties;
    private final ZephyrProperties zephyrProperties;
    private final IntegrationAuthSupport authSupport;

    private String cachedJwt;
    private long cachedJwtExpiryMs;

    public ZephyrContextClient(
            RestTemplate restTemplate,
            JiraProperties jiraProperties,
            ZephyrProperties zephyrProperties,
            IntegrationAuthSupport authSupport
    ) {
        this.restTemplate = restTemplate;
        this.jiraProperties = jiraProperties;
        this.zephyrProperties = zephyrProperties;
        this.authSupport = authSupport;
    }

    public String getContextJwt() {
        if (cachedJwt != null && System.currentTimeMillis() < cachedJwtExpiryMs) {
            return cachedJwt;
        }

        String projectKey = zephyrProperties.getDefaultProjectKey();
        String url = jiraProperties.getBaseUrl().replaceAll("/$", "")
                + "/plugins/servlet/ac/com.kanoah.test-manager/main-project-page";

        if (projectKey != null && !projectKey.isBlank()) {
            url += "?project.key=" + projectKey;
        }

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(authSupport.jiraHeaders()),
                String.class
        );

        String body = response.getBody();
        if (body == null || body.isBlank()) {
            throw new RuntimeException("Zephyr context servlet returned an empty response");
        }

        Matcher matcher = CONTEXT_JWT.matcher(body);
        if (!matcher.find()) {
            throw new RuntimeException(
                    "Could not extract Zephyr context JWT from Jira. "
                            + "Use zephyr.scale-cloud-api-token or zephyr.known-folders instead."
            );
        }

        cachedJwt = matcher.group(1);
        cachedJwtExpiryMs = System.currentTimeMillis() + (12 * 60 * 1000);
        return cachedJwt;
    }

    public void clearCache() {
        cachedJwt = null;
        cachedJwtExpiryMs = 0;
    }
}
