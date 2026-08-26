package com.acc.testcraft_backend.client;

/** Marker client used by services to select their deterministic demo responses. */
public class MockAiClient implements AiClient {
    @Override public String generate(String prompt) { return prompt; }
    @Override public boolean isConfigured() { return false; }
    @Override public boolean isMockMode() { return true; }
    @Override public String getProvider() { return "mock"; }
}
