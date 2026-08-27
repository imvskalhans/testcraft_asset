package com.acc.testcraft_backend.service;

import com.acc.testcraft_backend.client.AiClient;
import com.acc.testcraft_backend.config.CodeHostProperties;
import com.acc.testcraft_backend.model.PRReviewRequest;
import com.acc.testcraft_backend.model.PRReviewResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;

@Service
public class PRReviewService {
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final AiClient aiClient;
    private final CodeHostProperties properties;

    public PRReviewService(RestTemplate restTemplate, ObjectMapper mapper, AiClient aiClient, CodeHostProperties properties) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
        this.aiClient = aiClient;
        this.properties = properties;
    }

    public PRReviewResponse review(PRReviewRequest request) {
        ParsedPullRequest pr = parse(request.getUrl());
        PRReviewResponse response = new PRReviewResponse();
        response.setProvider(pr.provider);
        response.setUrl(request.getUrl().trim());
        try {
            JsonNode data = get(pr.apiUrl, pr.provider);
            String diff = getDiff(pr, data);
            response.setTitle(text(data, "title", "Pull request #" + pr.number));
            response.setRepository(pr.repository);
            response.setAuthor(author(data));
            response.setStatus(text(data, "state", "unknown"));
            response.setChangedFiles(data.path("changed_files").asInt(data.path("diffstat").path("total").asInt(0)));
            response.setAdditions(data.path("additions").asInt(data.path("diffstat").path("added").asInt(0)));
            response.setDeletions(data.path("deletions").asInt(data.path("diffstat").path("removed").asInt(0)));

            String context = "Provider: " + pr.provider + "\nRepository: " + pr.repository
                    + "\nTitle: " + response.getTitle() + "\nAuthor: " + response.getAuthor()
                    + "\nState: " + response.getStatus() + "\nDiff:\n" + limit(diff, 120_000);
            String review = aiClient.isMockMode() ? mockReview(response) : aiClient.generate(prompt(context, request.getFocus()));
            response.setReview(review);
            response.setFindings(Arrays.stream(review.split("\\R"))
                    .map(String::trim).filter(line -> line.startsWith("-") || line.startsWith("•"))
                    .map(line -> line.replaceFirst("^[-•]\\s*", "")).filter(s -> !s.isBlank()).toList());
            response.setMockMode(aiClient.isMockMode());
            response.setSuccess(true);
            return response;
        } catch (Exception e) {
            if (e instanceof org.springframework.web.client.HttpStatusCodeException http
                    && http.getStatusCode().value() == 404) {
                response.setError(pr.provider + " returned 404. The PR may be private, inaccessible with the configured token, deleted, or the URL may be incorrect.");
            } else {
                response.setError(message(e));
            }
            response.setSuccess(false);
            return response;
        }
    }

    private String prompt(String context, String focus) {
        return """
                You are a senior software engineer performing a pull request review.
                Analyze the supplied PR diff, prioritizing real, actionable issues over style preferences.
                Return Markdown with these sections: ## Summary, ## Findings, ## Testing recommendations, ## Risk.
                For every finding include severity (Blocker, High, Medium, or Low), file/line when inferable, why it matters, and a concrete fix.
                Look for correctness bugs, security/privacy issues, data loss, error handling, performance regressions, and missing tests.
                %s

                Pull request context:
                %s
                """.formatted(focus == null || focus.isBlank() ? "" : "Additional reviewer focus: " + focus.trim(), context);
    }

    private String mockReview(PRReviewResponse response) {
        return """
                ## Summary
                Pull request **%s** was fetched successfully in mock AI mode.

                ## Findings
                - Medium — Add or update automated tests for the changed behavior
                - Low — Confirm error handling and input validation for the new code paths

                ## Testing recommendations
                Run the unit test suite, integration tests, and a focused negative-path check.

                ## Risk
                Review the changed files manually before merging. Configure a live AI provider for code-specific findings.
                """.formatted(response.getTitle());
    }

    private ParsedPullRequest parse(String raw) {
        try {
            URI uri = URI.create(raw.trim());
            String host = Optional.ofNullable(uri.getHost()).orElse("").toLowerCase(Locale.ROOT);
            List<String> parts = Arrays.stream(uri.getPath().split("/"))
                    .filter(s -> !s.isBlank()).toList();
            if (host.equals("github.com") && parts.size() >= 4 && parts.get(2).equals("pull")) {
                String repo = parts.get(0) + "/" + parts.get(1);
                return new ParsedPullRequest("GitHub", repo, parts.get(3),
                        "https://api.github.com/repos/" + repo + "/pulls/" + parts.get(3),
                        "https://api.github.com/repos/" + repo + "/pulls/" + parts.get(3) + ".diff");
            }
            if (host.equals("bitbucket.org") && parts.size() >= 4 && parts.get(2).equals("pull-requests")) {
                String repo = parts.get(0) + "/" + parts.get(1);
                String base = "https://api.bitbucket.org/2.0/repositories/" + repo + "/pullrequests/" + parts.get(3);
                return new ParsedPullRequest("Bitbucket", repo, parts.get(3), base, base + "/diff");
            }
        } catch (IllegalArgumentException ignored) { }
        throw new IllegalArgumentException("Unsupported URL. Use a GitHub /owner/repository/pull/123 or Bitbucket /workspace/repository/pull-requests/123 URL");
    }

    private JsonNode get(String url, String provider) throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers(provider)), String.class);
        return mapper.readTree(response.getBody());
    }

    private String getDiff(ParsedPullRequest pr, JsonNode data) {
        try {
            HttpHeaders headers = headers(pr.provider);
            headers.setAccept(List.of(MediaType.valueOf("text/plain"), MediaType.ALL));
            ResponseEntity<String> response = restTemplate.exchange(pr.diffUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return response.getBody() == null ? "" : response.getBody();
        } catch (Exception ignored) {
            return data.path("description").asText("") + "\n(Diff unavailable; review metadata only)";
        }
    }

    private HttpHeaders headers(String provider) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if ("GitHub".equals(provider)) {
            if (!properties.getGithub().getToken().isBlank()) headers.setBearerAuth(properties.getGithub().getToken().trim());
            headers.set("X-GitHub-Api-Version", "2022-11-28");
        } else if (!properties.getBitbucket().getToken().isBlank()) {
            headers.setBearerAuth(properties.getBitbucket().getToken().trim());
        } else if (!properties.getBitbucket().getUsername().isBlank() && !properties.getBitbucket().getAppPassword().isBlank()) {
            headers.setBasicAuth(properties.getBitbucket().getUsername().trim(), properties.getBitbucket().getAppPassword());
        }
        return headers;
    }

    private static String text(JsonNode node, String field, String fallback) { return node.path(field).asText(fallback); }
    private static String author(JsonNode node) { return node.path("user").path("login").asText(node.path("author").path("raw").asText("unknown")); }
    private static String limit(String value, int max) { return value.length() <= max ? value : value.substring(0, max) + "\n[Diff truncated]"; }
    private static String message(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
    private record ParsedPullRequest(String provider, String repository, String number, String apiUrl, String diffUrl) {}
}
