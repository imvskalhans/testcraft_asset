package com.acc.testcraft_backend.client;

import com.acc.testcraft_backend.model.AiAttachment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider-independent AI client interface.
 * Implementations: AzureOpenAiClient, GeminiClient, GroqClient.
 * <p>
 * Switch provider via {@code ai.provider} in application-local.properties:
 * mock | azure-gateway | openai | gemini | groq
 */
public interface AiClient {

    /** Send a prompt and return the generated text. */
    String generate(String prompt);

    default String generate(String prompt, List<AiAttachment> attachments) {
        if (attachments != null && !attachments.isEmpty()) {
            throw new IllegalArgumentException("The configured AI provider does not support image attachments.");
        }
        return generate(prompt);
    }

    default boolean supportsImageInput() { return false; }

    /** Whether the provider credentials are fully configured. */
    boolean isConfigured();

    /** Whether the client is in mock mode (no real API calls). */
    boolean isMockMode();

    /** Provider identifier, e.g. "gemini", "openai", "azure-gateway", "groq", "mock". */
    String getProvider();

    /**
     * Live health check used by Settings. Implementations must never return secrets.
     * Always returns a map with {@code success}, {@code provider}, and {@code message}.
     */
    default Map<String, Object> testConnection() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", getProvider());
        result.put("configured", isConfigured());
        result.put("mockMode", isMockMode());
        if (isMockMode()) {
            result.put("success", true);
            result.put("message", "Mock AI mode is active. Live generation is not used.");
            return result;
        }
        try {
            generate("Reply with the single word OK.");
            result.put("success", true);
            result.put("message", getProvider() + " responded successfully.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage() == null ? "AI request failed" : e.getMessage());
        }
        return result;
    }
}
