package com.acc.testcraft_backend.client;

import com.acc.testcraft_backend.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI client for Groq API (OpenAI-compatible chat completions).
 * Activated when ai.provider=groq.
 */
public class GroqClient implements AiClient {

    private final AiProperties ai;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GroqClient(AiProperties ai, RestTemplate restTemplate) {
        this.ai = ai;
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean isConfigured() {
        return ai.getGroq() != null && ai.getGroq().isConfigured();
    }

    @Override
    public boolean isMockMode() {
        return ai.isMockMode() || !isConfigured();
    }

    @Override
    public String getProvider() {
        return "groq";
    }

    @Override
    public String generate(String prompt) {
        if (isMockMode()) {
            throw new IllegalStateException("AI mock mode — use service-layer mock responses");
        }

        AiProperties.Groq groq = ai.getGroq();
        String url = groq.getBaseUrl().replaceAll("/$", "") + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groq.getApiKey());

        Map<String, Object> body = buildRequestBody(prompt, groq);

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(jsonBody, headers),
                    Map.class
            );
            return extractContent(response.getBody());
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new RuntimeException(
                    "Groq authorization failed — check ai.groq.api-key in application-local.properties"
            );
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Groq API error: " + e.getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException("Groq generation failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildRequestBody(String prompt, AiProperties.Groq groq) {
        Map<String, Object> systemMessage = new LinkedHashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", ai.getSystemPrompt());

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", groq.getModel());
        body.put("messages", List.of(systemMessage, userMessage));
        body.put("max_tokens", groq.getMaxTokens());
        body.put("temperature", ai.getTemperature());
        body.put("frequency_penalty", ai.getFrequencyPenalty());
        body.put("presence_penalty", ai.getPresencePenalty());
        return body;
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        if (responseBody == null || !responseBody.containsKey("choices")) {
            throw new RuntimeException("Invalid Groq response format");
        }

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) responseBody.get("choices");

        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("Invalid Groq response format");
        }

        Map<String, Object> messageObj =
                (Map<String, Object>) choices.get(0).get("message");

        if (messageObj == null || !messageObj.containsKey("content")) {
            throw new RuntimeException("Invalid Groq response format");
        }

        String content = (String) messageObj.get("content");
        if (content == null || content.isBlank()) {
            throw new RuntimeException("Empty Groq response");
        }

        return content.trim();
    }
}
