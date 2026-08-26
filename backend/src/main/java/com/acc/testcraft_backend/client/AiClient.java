package com.acc.testcraft_backend.client;

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

    /** Whether the provider credentials are fully configured. */
    boolean isConfigured();

    /** Whether the client is in mock mode (no real API calls). */
    boolean isMockMode();

    /** Provider identifier, e.g. "gemini", "openai", "azure-gateway", "groq", "mock". */
    String getProvider();
}
