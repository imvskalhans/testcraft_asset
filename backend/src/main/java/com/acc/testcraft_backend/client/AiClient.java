package com.acc.testcraft_backend.client;

import com.acc.testcraft_backend.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pluggable AI client — provider selected via ai.provider in config.
 */
@Component
public class AiClient {

    private final AiProperties ai;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiClient(AiProperties ai, RestTemplate restTemplate) {
        this.ai = ai;
        this.restTemplate = restTemplate;
    }

    public boolean isConfigured() {
        return ai.isConfigured();
    }

    public boolean isMockMode() {
        return ai.isMockMode() || !ai.isConfigured();
    }

    public String getProvider() {
        return ai.getProvider();
    }

    public String generate(String prompt) {
        if (ai.isMockMode()) {
            throw new IllegalStateException("AI mock mode — use service-layer mock responses");
        }
        if (ai.isAzureGateway()) {
            return generateViaAzureGateway(prompt);
        }
        if (ai.isOpenAi()) {
            return generateViaOpenAi(prompt);
        }
        throw new IllegalStateException("Unsupported ai.provider: " + ai.getProvider());
    }

    private String generateViaAzureGateway(String prompt) {
        AiProperties.Azure azure = ai.getAzure();

        String endpoint = azure.getOpenAiEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "default";
        }

        String url = azure.getGatewayUrl()
                + "?deploymentId=" + azure.getDeploymentId()
                + "&apiKey=" + azure.getApiKey()
                + "&apiVersion=" + azure.getApiVersion()
                + "&openAiEndPoint=" + endpoint;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (azure.getClientIdHeader() != null && azure.getClientId() != null) {
            headers.set(azure.getClientIdHeader(), azure.getClientId());
        }
        if (azure.getClientSecretHeader() != null && azure.getClientSecret() != null) {
            headers.set(azure.getClientSecretHeader(), azure.getClientSecret());
        }

        Map<String, Object> body = chatBody(prompt, azure.getMaxTokens());
        return postChat(url, headers, body);
    }

    private String generateViaOpenAi(String prompt) {
        AiProperties.OpenAi openai = ai.getOpenai();
        String url = openai.getBaseUrl().replaceAll("/$", "") + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openai.getApiKey());

        Map<String, Object> body = chatBody(prompt, openai.getMaxTokens());
        body.put("model", openai.getModel());

        return postChat(url, headers, body);
    }

    private Map<String, Object> chatBody(String prompt, int maxTokens) {
        Map<String, Object> systemMessage = new LinkedHashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", ai.getSystemPrompt());

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messages", List.of(systemMessage, userMessage));
        body.put("max_completion_tokens", maxTokens);
        body.put("maxCompletionTokens", maxTokens);
        body.put("temperature", ai.getTemperature());
        body.put("frequency_penalty", ai.getFrequencyPenalty());
        body.put("frequencyPenalty", ai.getFrequencyPenalty());
        body.put("presence_penalty", ai.getPresencePenalty());
        body.put("presencePenalty", ai.getPresencePenalty());
        return body;
    }

    @SuppressWarnings("unchecked")
    private String postChat(String url, HttpHeaders headers, Map<String, Object> body) {
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
                    "AI authorization failed — check ai.* settings in application-local.properties"
            );
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("AI API error: " + e.getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException("AI generation failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        if (responseBody == null || !responseBody.containsKey("choices")) {
            throw new RuntimeException("Invalid AI response format");
        }

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) responseBody.get("choices");

        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("Invalid AI response format");
        }

        Map<String, Object> messageObj =
                (Map<String, Object>) choices.get(0).get("message");

        if (messageObj == null || !messageObj.containsKey("content")) {
            throw new RuntimeException("Invalid AI response format");
        }

        String content = (String) messageObj.get("content");
        if (content == null || content.isBlank()) {
            throw new RuntimeException("Empty AI response");
        }

        return content.trim();
    }
}
