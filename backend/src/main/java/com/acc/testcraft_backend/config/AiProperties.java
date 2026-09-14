package com.acc.testcraft_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI provider settings — configure in application-local.properties.
 * Supported providers: mock, azure-gateway, openai, gemini, groq
 */
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** mock | azure-gateway | openai | gemini | groq */
    private String provider = "mock";

    private Azure azure = new Azure();
    private OpenAi openai = new OpenAi();
    private Gemini gemini = new Gemini();
    private Groq groq = new Groq();
    private double temperature = 0.7;
    private double frequencyPenalty = 0.0;
    private double presencePenalty = 0.0;
    private String systemPrompt = "You are an AI assistant that helps QA teams review stories and generate test cases.";

    public boolean isMockMode() {
        return "mock".equalsIgnoreCase(provider);
    }

    public boolean isAzureGateway() {
        return "azure-gateway".equalsIgnoreCase(provider);
    }

    public boolean isOpenAi() {
        return "openai".equalsIgnoreCase(provider);
    }

    public boolean isGemini() {
        return "gemini".equalsIgnoreCase(provider);
    }

    public boolean isGroq() {
        return "groq".equalsIgnoreCase(provider);
    }

    public boolean isConfigured() {
        if (isMockMode()) {
            return false;
        }
        if (isAzureGateway()) {
            return azure.isConfigured();
        }
        if (isOpenAi()) {
            return openai.isConfigured();
        }
        if (isGemini()) {
            return gemini.isConfigured();
        }
        if (isGroq()) {
            return groq.isConfigured();
        }
        return false;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Azure getAzure() {
        return azure;
    }

    public void setAzure(Azure azure) {
        this.azure = azure;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAi openai) {
        this.openai = openai;
    }

    public Gemini getGemini() {
        return gemini;
    }

    public void setGemini(Gemini gemini) {
        this.gemini = gemini;
    }

    public Groq getGroq() {
        return groq;
    }

    public void setGroq(Groq groq) {
        this.groq = groq;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public void setFrequencyPenalty(double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }

    public double getPresencePenalty() {
        return presencePenalty;
    }

    public void setPresencePenalty(double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public static class Azure {
        private String gatewayUrl = "";
        private String deploymentId = "";
        private String apiKey = "";
        private String apiVersion = "2024-02-15-preview";
        private int maxTokens = 4096;
        private String clientIdHeader = "client_id";
        private String clientId = "";
        private String clientSecretHeader = "client_secret";
        private String clientSecret = "";
        private String openAiEndpoint = "";

        public boolean isConfigured() {
            return gatewayUrl != null && !gatewayUrl.isBlank()
                    && deploymentId != null && !deploymentId.isBlank()
                    && apiKey != null && !apiKey.isBlank();
        }

        public String getGatewayUrl() {
            return gatewayUrl;
        }

        public void setGatewayUrl(String gatewayUrl) {
            this.gatewayUrl = gatewayUrl;
        }

        public String getDeploymentId() {
            return deploymentId;
        }

        public void setDeploymentId(String deploymentId) {
            this.deploymentId = deploymentId;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiVersion() {
            return apiVersion;
        }

        public void setApiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public String getClientIdHeader() {
            return clientIdHeader;
        }

        public void setClientIdHeader(String clientIdHeader) {
            this.clientIdHeader = clientIdHeader;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecretHeader() {
            return clientSecretHeader;
        }

        public void setClientSecretHeader(String clientSecretHeader) {
            this.clientSecretHeader = clientSecretHeader;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getOpenAiEndpoint() {
            return openAiEndpoint;
        }

        public void setOpenAiEndpoint(String openAiEndpoint) {
            this.openAiEndpoint = openAiEndpoint;
        }
    }

    public static class OpenAi {
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey = "";
        private String model = "gpt-4o-mini";
        private int maxTokens = 4096;

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank();
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
    }

    public static class Gemini {
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        private String apiKey = "";
        private String model = "gemini-2.5-flash";
        private int maxTokens = 4096;

        public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    }

    public static class Groq {
        private String baseUrl = "https://api.groq.com/openai/v1";
        private String apiKey = "";
        private String model = "llama-3.3-70b-versatile";
        private int maxTokens = 4096;

        public boolean isConfigured() { return apiKey != null && !apiKey.isBlank(); }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    }
}
