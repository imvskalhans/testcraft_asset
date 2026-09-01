package com.acc.testcraft_backend.client;

import com.acc.testcraft_backend.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/** Azure OpenAI/gateway implementation behind the provider-independent interface. */
public class AzureOpenAiClient extends OpenAiCompatibleClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final AiProperties properties;

    public AzureOpenAiClient(RestTemplate restTemplate, ObjectMapper mapper, AiProperties p) {
        super(restTemplate, mapper, "azure-gateway", endpoint(p.getAzure()), p.getAzure().getApiKey(),
                p.getAzure().getDeploymentId(), p.getAzure().getMaxTokens(), p);
        this.restTemplate = restTemplate;
        this.mapper = mapper;
        this.properties = p;
    }

    @Override
    public String generate(String prompt) {
        AiProperties.Azure azure = properties.getAzure();
        String endpoint = azure.getOpenAiEndpoint();
        if (endpoint == null || endpoint.isBlank()) endpoint = "default";
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
        Map<String, Object> body = Map.of(
                "messages", List.of(
                        Map.of("role", "system", "content", properties.getSystemPrompt()),
                        Map.of("role", "user", "content", prompt)),
                "max_completion_tokens", azure.getMaxTokens(),
                "maxCompletionTokens", azure.getMaxTokens(),
                "temperature", properties.getTemperature(),
                "frequency_penalty", properties.getFrequencyPenalty(),
                "presence_penalty", properties.getPresencePenalty());
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            JsonNode content = mapper.readTree(response.getBody()).path("choices").path(0)
                    .path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) throw new IllegalStateException("Azure response did not contain generated text");
            return content.asText().trim();
        } catch (Exception e) {
            throw new IllegalStateException("Azure AI generation failed: " + e.getMessage(), e);
        }
    }

    @Override public boolean supportsImageInput() { return false; }

    private static String endpoint(AiProperties.Azure a) {
        if (a.getOpenAiEndpoint() != null && !a.getOpenAiEndpoint().isBlank()) return a.getOpenAiEndpoint();
        String base = a.getGatewayUrl().replaceAll("/$", "");
        return base + "/openai/deployments/" + a.getDeploymentId() + "/chat/completions?api-version=" + a.getApiVersion();
    }
}
