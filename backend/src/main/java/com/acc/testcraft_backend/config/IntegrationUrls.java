package com.acc.testcraft_backend.config;

import org.springframework.stereotype.Component;

/**
 * Central URL builder — all integration URLs derive from config properties.
 */
@Component
public class IntegrationUrls {

    private final JiraProperties jira;
    private final ZephyrProperties zephyr;

    public IntegrationUrls(JiraProperties jira, ZephyrProperties zephyr) {
        this.jira = jira;
        this.zephyr = zephyr;
    }

    public String jira(String path) {
        String suffix = path.startsWith("/") ? path : "/" + path;
        return jira.apiUrl(suffix);
    }

    public String zephyr(String path) {
        String base = zephyr.resolveApiBase(jira);
        String suffix = path.startsWith("/") ? path : "/" + path;
        return base + suffix;
    }

    public String zephyrTests(String path) {
        String base = trimTrailingSlash(jira.getBaseUrl())
                + trimTrailingSlash(zephyr.getTestsApiPath());
        String suffix = path.startsWith("/") ? path : "/" + path;
        return base + suffix;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
