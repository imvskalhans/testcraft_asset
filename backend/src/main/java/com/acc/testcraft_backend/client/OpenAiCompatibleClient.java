package com.acc.testcraft_backend.client;

import com.acc.testcraft_backend.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import com.acc.testcraft_backend.model.AiAttachment;

/** Shared implementation for OpenAI-compatible chat-completions providers. */
public class OpenAiCompatibleClient implements AiClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final String provider;
    private final String url;
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final AiProperties properties;

    public OpenAiCompatibleClient(RestTemplate restTemplate, ObjectMapper mapper, String provider,
                                  String url, String apiKey, String model, int maxTokens,
                                  AiProperties properties) {
        this.restTemplate = restTemplate; this.mapper = mapper; this.provider = provider;
        this.url = url; this.apiKey = apiKey; this.model = model; this.maxTokens = maxTokens;
        this.properties = properties;
    }

    @Override public String generate(String prompt) {
        return generate(prompt, List.of());
    }

    @Override public String generate(String prompt, List<AiAttachment> attachments) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        List<Object> contentParts = new ArrayList<>();
        contentParts.add(Map.of("type", "text", "text", prompt));
        for (AiAttachment attachment : attachments == null ? List.<AiAttachment>of() : attachments) {
            if (attachment.getMimeType() == null || !attachment.getMimeType().startsWith("image/")) continue;
            contentParts.add(Map.of("type", "image_url", "image_url", Map.of(
                    "url", "data:" + attachment.getMimeType() + ";base64," + attachment.getData())));
        }
        Map<String, Object> body = Map.of("model", model, "messages", new Object[]{
                Map.of("role", "system", "content", properties.getSystemPrompt()),
                Map.of("role", "user", "content", contentParts)
        }, "temperature", properties.getTemperature(), "max_tokens", maxTokens);
        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        try {
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) throw new IllegalStateException("AI response did not contain generated text");
            return content.asText();
        } catch (Exception e) {
            throw new IllegalStateException(provider + " returned an invalid response", e);
        }
    }
    @Override public boolean supportsImageInput() { return true; }
    @Override public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }
    @Override public boolean isMockMode() { return false; }
    @Override public String getProvider() { return provider; }
}
