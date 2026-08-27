package com.acc.testcraft_backend.service;

import com.acc.testcraft_backend.client.AiClient;
import com.acc.testcraft_backend.config.JenkinsProperties;
import com.acc.testcraft_backend.model.JenkinsLogRequest;
import com.acc.testcraft_backend.model.JenkinsLogResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;

@Service
public class JenkinsLogService {
    private static final int CHUNK_SIZE = 12_000;
    private static final int MAX_CHUNKS = 100;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final AiClient aiClient;
    private final JenkinsProperties properties;

    public JenkinsLogService(RestTemplate restTemplate, ObjectMapper mapper, AiClient aiClient, JenkinsProperties properties) {
        this.restTemplate = restTemplate; this.mapper = mapper; this.aiClient = aiClient; this.properties = properties;
    }

    public JenkinsLogResponse analyze(JenkinsLogRequest request) {
        JenkinsLogResponse response = new JenkinsLogResponse();
        try {
            BuildTarget target = target(request.getUrl());
            response.setJobUrl(target.jobUrl);
            response.setBuildUrl(target.buildUrl);
            JsonNode metadata = getJson(target.buildUrl + "api/json");
            String log = Optional.ofNullable(getText(target.buildUrl + "consoleText")).orElse("");
            response.setJobName(metadata.path("fullDisplayName").asText(target.jobUrl));
            response.setBuildNumber(metadata.path("number").asText(target.buildNumber));
            response.setBuildResult(metadata.path("result").asText("UNKNOWN"));
            response.setBuildTimestamp(metadata.path("timestamp").asText(""));
            response.setDuration(metadata.path("duration").asText(""));
            response.setTotalLines(log.isBlank() ? 0 : log.split("\\R", -1).length);

            List<String> chunks = chunks(log);
            if (chunks.size() > MAX_CHUNKS) throw new IllegalArgumentException("Console output is too large to analyze safely (maximum 1.2 million characters)");
            List<String> notes = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                notes.add(aiClient.isMockMode() ? mockChunk(i + 1, chunks.size(), chunks.get(i))
                        : aiClient.generate(chunkPrompt(i + 1, chunks.size(), chunks.get(i), request.getFocus())));
            }
            response.setChunksAnalyzed(chunks.size());
            String synthesis = aiClient.isMockMode() ? mockSummary(response, log) : aiClient.generate(summaryPrompt(response, notes, latestChanges(metadata), request.getFocus()));
            response.setSummary(synthesis);
            response.setMockMode(aiClient.isMockMode()); response.setSuccess(true);
        } catch (Exception e) { response.setError(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()); }
        return response;
    }

    private BuildTarget target(String raw) {
        if (properties.getBaseUrl().isBlank()) throw new IllegalArgumentException("jenkins.base-url is not configured");
        URI input;
        try { input = URI.create(raw.trim()); } catch (Exception e) { throw new IllegalArgumentException("Invalid Jenkins URL"); }
        URI configured = URI.create(trim(properties.getBaseUrl()));
        if (!Objects.equals(input.getScheme(), configured.getScheme()) || !Objects.equals(input.getHost(), configured.getHost())) {
            throw new IllegalArgumentException("The Jenkins URL must belong to configured jenkins.base-url");
        }
        String path = input.getPath().replaceAll("/+", "/");
        String configuredPath = configured.getPath().replaceAll("/+", "/").replaceFirst("/+$", "");
        if (!configuredPath.isBlank() && !path.equals(configuredPath) && !path.startsWith(configuredPath + "/")) {
            throw new IllegalArgumentException("The Jenkins URL must belong to configured jenkins.base-url");
        }
        if (!configuredPath.isBlank()) path = path.substring(configuredPath.length());
        if (path.isBlank()) path = "/";
        path = path.replaceFirst("/(consoleText|console|api/json)/?$", "").replaceFirst("/+$", "");
        String buildNumber = "lastBuild";
        String[] parts = path.split("/");
        if (parts.length > 0 && parts[parts.length - 1].matches("\\d+")) buildNumber = parts[parts.length - 1];
        else if (parts.length > 1 && parts[parts.length - 1].equals("lastBuild")) buildNumber = "lastBuild";
        else path += "/lastBuild";
        String base = trim(properties.getBaseUrl());
        String buildPath = path.endsWith("/") ? path : path + "/";
        return new BuildTarget(base + path, base + buildPath, buildNumber);
    }

    private JsonNode getJson(String url) throws Exception { return mapper.readTree(exchange(url, MediaType.APPLICATION_JSON)); }
    private String getText(String url) { return exchange(url, MediaType.TEXT_PLAIN); }
    private String exchange(String url, MediaType accept) {
        HttpHeaders headers = new HttpHeaders(); headers.setAccept(List.of(accept));
        if (!properties.getUsername().isBlank() && !properties.getApiToken().isBlank()) headers.setBasicAuth(properties.getUsername(), properties.getApiToken());
        try { return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody(); }
        catch (org.springframework.web.client.HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) throw new IllegalStateException("Jenkins authorization failed — check jenkins.username and jenkins.api-token");
            if (e.getStatusCode().value() == 404) throw new IllegalStateException("Jenkins job/build was not found or is not accessible");
            throw new IllegalStateException("Jenkins request failed with HTTP " + e.getStatusCode().value());
        }
    }

    private List<String> chunks(String log) {
        if (log == null || log.isBlank()) return List.of("(Console output was empty)");
        List<String> result = new ArrayList<>(); StringBuilder current = new StringBuilder();
        for (String line : log.split("\\R", -1)) {
            String remaining = line;
            while (remaining.length() > CHUNK_SIZE) {
                if (current.length() > 0) { result.add(current.toString()); current.setLength(0); }
                result.add(remaining.substring(0, CHUNK_SIZE)); remaining = remaining.substring(CHUNK_SIZE);
            }
            if (current.length() + remaining.length() + 1 > CHUNK_SIZE && current.length() > 0) { result.add(current.toString()); current.setLength(0); }
            current.append(remaining).append('\n');
        }
        if (current.length() > 0) result.add(current.toString()); return result;
    }

    private String chunkPrompt(int n, int total, String chunk, String focus) { return """
            Analyze chunk %d of %d from a Jenkins console log. Extract concrete errors, warnings, failed tests or commands, deployment changes, and useful evidence. Do not guess. Keep the result concise and mention log lines when visible.
            %s
            CONSOLE CHUNK:
            %s
            """.formatted(n, total, focus == null || focus.isBlank() ? "" : "Additional focus: " + focus, chunk); }

    private String summaryPrompt(JenkinsLogResponse r, List<String> notes, String changes, String focus) { return """
            Produce a professional Jenkins build report from the metadata and chunk analyses below. Use Markdown sections: ## Executive summary, ## Build result, ## Latest changes, ## Failure reason, ## Evidence, ## Recommended next steps. State clearly whether the job passed, failed, was aborted, or is unknown. Do not invent details; say unavailable when evidence is missing.
            Job: %s
            Build: #%s
            Result from Jenkins: %s
            Latest changes reported by Jenkins:
            %s
            %s
            Chunk analyses:
            %s
            """.formatted(r.getJobName(), r.getBuildNumber(), r.getBuildResult(), changes, focus == null || focus.isBlank() ? "" : "Reviewer focus: " + focus, String.join("\n\n", notes)); }

    private String latestChanges(JsonNode metadata) {
        List<String> changes = new ArrayList<>();
        for (JsonNode set : metadata.path("changeSets")) for (JsonNode item : set.path("items")) {
            String message = item.path("msg").asText("").replaceAll("\\s+", " ").trim();
            if (!message.isBlank()) changes.add("- " + message);
            if (changes.size() >= 20) return String.join("\n", changes);
        }
        return changes.isEmpty() ? "Unavailable in Jenkins metadata" : String.join("\n", changes);
    }

    private String mockChunk(int n, int total, String chunk) { return "Chunk %d/%d scanned; configure a live AI provider for log-specific findings.".formatted(n, total); }
    private String mockSummary(JenkinsLogResponse r, String log) { return """
            ## Executive summary
            Jenkins build **%s #%s** was fetched successfully in mock AI mode.

            ## Build result
            Jenkins reported: **%s**. Console output contains %d lines across %d analyzed chunk(s).

            ## Latest changes
            Not available from the configured mock analysis.

            ## Failure reason
            Configure a live AI provider for evidence-based failure diagnosis.

            ## Recommended next steps
            Review the console output around the first ERROR/FAILURE and rerun the relevant tests.
            """.formatted(r.getJobName(), r.getBuildNumber(), r.getBuildResult(), r.getTotalLines(), r.getChunksAnalyzed()); }

    private static String trim(String value) { return value.replaceAll("/+$", ""); }
    private record BuildTarget(String jobUrl, String buildUrl, String buildNumber) {}
}
