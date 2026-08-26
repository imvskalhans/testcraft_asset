package com.acc.testcraft_backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Configuration
@EnableConfigurationProperties({
        JiraProperties.class,
        ZephyrProperties.class,
        AiProperties.class,
        AppProperties.class
})
public class IntegrationConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public IntegrationAuthSupport integrationAuthSupport(
            JiraProperties jira,
            ZephyrProperties zephyr
    ) {
        return new IntegrationAuthSupport(jira, zephyr);
    }

    /**
     * Builds auth headers for Jira and Zephyr (plugin mode uses Jira credentials).
     */
    public static class IntegrationAuthSupport {

        private final JiraProperties jira;
        private final ZephyrProperties zephyr;

        public IntegrationAuthSupport(JiraProperties jira, ZephyrProperties zephyr) {
            this.jira = jira;
            this.zephyr = zephyr;
        }

        public HttpHeaders jiraHeaders() {
            return buildJiraHeaders();
        }

        public HttpHeaders zephyrHeaders() {
            if (zephyr.isScaleCloud()) {
                return scaleCloudHeaders();
            }
            return buildJiraHeaders();
        }

        public HttpHeaders scaleCloudHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String token = zephyr.getScaleCloudApiToken();
            if (token == null || token.isBlank()) {
                throw new IllegalStateException(
                        "zephyr.scale-cloud-api-token is not configured. "
                                + "Create a JWT in Jira → Apps → Zephyr Scale → API keys."
                );
            }
            headers.setBearerAuth(token.trim());
            return headers;
        }

        private HttpHeaders buildJiraHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            if ("bearer".equalsIgnoreCase(jira.getAuthType())) {
                String token = jira.getBearerToken();
                if (token == null || token.isBlank()) {
                    throw new IllegalStateException(
                            "jira.bearer-token is not configured for bearer authentication"
                    );
                }
                headers.setBearerAuth(token.trim());
                return headers;
            }

            String username = jira.getUsername();
            String apiToken = jira.getApiToken();
            if (username == null || username.isBlank()
                    || apiToken == null || apiToken.isBlank()) {
                throw new IllegalStateException(
                        "jira.username and jira.api-token must be configured"
                );
            }

            String encoded = Base64.getEncoder()
                    .encodeToString((username + ":" + apiToken).getBytes());
            headers.set("Authorization", "Basic " + encoded);
            return headers;
        }
    }
}
