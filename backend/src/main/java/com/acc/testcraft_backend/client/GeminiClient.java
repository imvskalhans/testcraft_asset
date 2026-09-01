package com.acc.testcraft_backend.client;

import com.acc.testcraft_backend.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.acc.testcraft_backend.model.AiAttachment;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Google Gemini REST client. Business services only depend on AiClient. */
public class GeminiClient implements AiClient {
    private static final Pattern RETRY_PATTERN = Pattern.compile(
            "retry\\s+in\\s+([0-9]+(?:\\.[0-9]+)?)\\s*s", Pattern.CASE_INSENSITIVE);
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final AiProperties properties;
    private final AiProperties.Gemini config;

    public GeminiClient(RestTemplate restTemplate, ObjectMapper mapper, AiProperties properties) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
        this.properties = properties;
        this.config = properties.getGemini();
    }

    @Override
    public String generate(String prompt) {
        return generate(prompt, List.of());
    }

    @Override
    public String generate(String prompt, List<AiAttachment> attachments) {
        if (isMockMode()) {
            throw new IllegalStateException(
                    "Gemini is not configured — set ai.gemini.api-key in application-local.properties");
        }

        String url = UriComponentsBuilder
                .fromUriString(config.getBaseUrl().replaceAll("/$", "") + "/models/" + config.getModel()
                        + ":generateContent")
                .queryParam("key", config.getApiKey().trim())
                .build().encode().toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String requestJson = mapper.writeValueAsString(buildRequestBody(prompt, attachments));
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(requestJson, headers), String.class);
            return extractResponseText(response.getBody());
        } catch (HttpStatusCodeException e) {
            throw apiException(e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Gemini generation failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildRequestBody(String prompt, List<AiAttachment> attachments) {
        Map<String, Object> systemInstruction = new LinkedHashMap<>();
        systemInstruction.put("parts", List.of(Map.of("text", properties.getSystemPrompt())));
        Map<String, Object> userContent = new LinkedHashMap<>();
        userContent.put("role", "user");
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt == null ? "" : prompt));
        for (AiAttachment attachment : attachments == null ? List.<AiAttachment>of() : attachments) {
            if (attachment.getMimeType() == null || !attachment.getMimeType().startsWith("image/")) continue;
            parts.add(Map.of("inlineData", Map.of("mimeType", attachment.getMimeType(), "data", attachment.getData())));
        }
        userContent.put("parts", parts);
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", properties.getTemperature());
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", config.getMaxTokens());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", systemInstruction);
        body.put("contents", List.of(userContent));
        body.put("generationConfig", generationConfig);
        return body;
    }

    @Override public boolean supportsImageInput() { return true; }

    private String extractResponseText(String body) {
        try {
            JsonNode root = mapper.readTree(body == null ? "" : body);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                String reason = root.path("promptFeedback").path("blockReason").asText("");
                throw new IllegalStateException(reason.isBlank()
                        ? "Gemini response did not contain candidates"
                        : "Gemini blocked the prompt: " + reason);
            }
            List<String> parts = new ArrayList<>();
            for (JsonNode part : candidates.get(0).path("content").path("parts")) {
                if (part.has("text")) parts.add(part.get("text").asText());
            }
            String text = String.join("", parts);
            if (text.isBlank()) {
                String reason = candidates.get(0).path("finishReason").asText("");
                throw new IllegalStateException("Gemini returned no text"
                        + (reason.isBlank() ? "" : " (finish reason: " + reason + ")"));
            }
            return text;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Gemini returned an invalid response", e);
        }
    }

    private RuntimeException apiException(int status, String body) {
        String message = extractErrorMessage(body);
        if (status == 429) {
            long retryAfter = parseRetryAfter(body);
            String hint = retryAfter > 0 ? " Retry in " + retryAfter + "s." : " Try another supported model.";
            return new RateLimitException("Gemini rate limit reached for model '" + config.getModel() + "'."
                    + hint + (message.isBlank() ? "" : " Server: " + message), retryAfter);
        }
        if (status == 401 || status == 403) {
            return new RuntimeException("Gemini authorization failed — check ai.gemini.api-key");
        }
        return new RuntimeException("Gemini API error " + status
                + (message.isBlank() ? "" : ": " + message));
    }

    private String extractErrorMessage(String body) {
        try {
            return mapper.readTree(body == null ? "" : body).path("error").path("message").asText("");
        } catch (Exception ignored) {
            return "";
        }
    }

    private long parseRetryAfter(String body) {
        Matcher matcher = RETRY_PATTERN.matcher(body == null ? "" : body);
        if (!matcher.find()) return 0;
        try { return (long) Math.ceil(Double.parseDouble(matcher.group(1))); }
        catch (NumberFormatException ignored) { return 0; }
    }
    @Override public boolean isConfigured() { return config.isConfigured(); }
    @Override public boolean isMockMode() { return properties.isMockMode() || !isConfigured(); }
    @Override public String getProvider() { return "gemini"; }

    public static class RateLimitException extends RuntimeException {
        public final long retryAfterSeconds;
        public RateLimitException(String message, long retryAfterSeconds) {
            super(message);
            this.retryAfterSeconds = retryAfterSeconds;
        }
    }
}
