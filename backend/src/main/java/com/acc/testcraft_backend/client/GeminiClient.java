package com.acc.testcraft_backend.client;

import com.acc.testcraft_backend.config.AiProperties;
import com.acc.testcraft_backend.model.AiAttachment;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Google Gemini REST client. Business services only depend on AiClient. */
public class GeminiClient implements AiClient {
    private static final Pattern RETRY_PATTERN = Pattern.compile(
            "retry\\s+in\\s+([0-9]+(?:\\.[0-9]+)?)\\s*s", Pattern.CASE_INSENSITIVE);
    private static final List<String> FALLBACK_MODELS = List.of(
            "gemini-3.8-flash",
            "gemini-3.7-flash",
            "gemini-3.6-flash",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-3.1-flash-image",
            "gemini-3.1-flash-lite-image",
            "gemini-3-pro-image",
            "gemini-3.1-pro-preview",
            "gemini-3-flash-preview"
    );

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final AiProperties properties;
    private final AiProperties.Gemini config;
    private volatile String lastSuccessfulModel;

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
        return generateWithFallbacks(prompt, attachments, candidateModels(), 2);
    }

    @Override
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", getProvider());
        result.put("configured", isConfigured());
        result.put("mockMode", isMockMode());
        result.put("configuredModel", configuredModel());
        if (isMockMode()) {
            result.put("success", true);
            result.put("message", "Mock AI mode is active. Live Gemini calls are not used.");
            return result;
        }

        ModelListProbe probe = listModels();
        result.put("keyAccepted", probe.keyAccepted);
        if (!probe.names.isEmpty()) {
            result.put("availableModels", probe.names.size() > 15 ? probe.names.subList(0, 15) : probe.names);
        }

        if (!probe.keyAccepted) {
            result.put("success", false);
            result.put("message", probe.error);
            return result;
        }

        try {
            generateWithFallbacks("Reply with the single word OK.", List.of(), candidateModels().stream().limit(3).toList(), 1);
            String used = lastSuccessfulModel != null ? lastSuccessfulModel : configuredModel();
            result.put("success", true);
            result.put("model", used);
            if (!used.equals(configuredModel())) {
                result.put("message", "API key is valid. Configured model '" + configuredModel()
                        + "' was unavailable or overloaded; Gemini responded using '" + used + "'.");
            } else {
                result.put("message", "Gemini responded using " + used + ".");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("model", configuredModel());
            String message = e.getMessage() == null ? "Gemini request failed" : e.getMessage();
            if (probe.keyAccepted) {
                result.put("message", "API key was accepted (models list succeeded). Generation failed: " + message);
            } else {
                result.put("message", message);
            }
        }
        return result;
    }

    private String generateWithFallbacks(String prompt, List<AiAttachment> attachments,
                                        List<String> models, int attemptsPerModel) {
        RuntimeException lastError = null;
        boolean overloaded = false;
        List<String> tried = new ArrayList<>();

        for (String model : models) {
            tried.add(model);
            for (int attempt = 0; attempt < attemptsPerModel; attempt++) {
                try {
                    String text = generateOnce(model, prompt, attachments);
                    lastSuccessfulModel = model;
                    return text;
                } catch (HttpStatusCodeException e) {
                    int status = e.getStatusCode().value();
                    String body = e.getResponseBodyAsString();
                    if (status == 401 || status == 403) {
                        throw apiException(status, body, model);
                    }
                    lastError = apiException(status, body, model);
                    if (status == 404 || status == 400) {
                        break;
                    }
                    if (status == 429 || status == 503) {
                        overloaded |= status == 503;
                        sleep(retryDelayMs(body, attempt));
                        continue;
                    }
                    throw lastError;
                } catch (Exception e) {
                    if (e instanceof RuntimeException runtime) {
                        lastError = runtime;
                    } else {
                        lastError = new RuntimeException("Gemini generation failed: " + e.getMessage(), e);
                    }
                    break;
                }
            }
        }

        if (overloaded) {
            throw new RuntimeException(
                    "Gemini is reachable but overloaded (HTTP 503). This is Google capacity, not an invalid API key. "
                            + "Tried models: " + String.join(", ", tried)
                            + ". Set ai.gemini.model to a stable Flash model such as gemini-2.5-flash, "
                            + "restart the backend, and retry.");
        }
        if (lastError != null) {
            throw lastError;
        }
        throw new RuntimeException("Gemini generation failed");
    }

    private List<String> candidateModels() {
        LinkedHashSet<String> models = new LinkedHashSet<>();
        String configured = configuredModel();
        if (!configured.isBlank()) {
            models.add(configured);
        }
        models.addAll(FALLBACK_MODELS);
        return new ArrayList<>(models);
    }

    private String generateOnce(String model, String prompt, List<AiAttachment> attachments) {
        String url = UriComponentsBuilder
                .fromUriString(config.getBaseUrl().replaceAll("/$", "") + "/models/" + model + ":generateContent")
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
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Gemini generation failed: " + e.getMessage(), e);
        }
    }

    private ModelListProbe listModels() {
        ModelListProbe probe = new ModelListProbe();
        try {
            String url = UriComponentsBuilder
                    .fromUriString(config.getBaseUrl().replaceAll("/$", "") + "/models")
                    .queryParam("key", config.getApiKey().trim())
                    .build().encode().toUriString();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode models = mapper.readTree(response.getBody() == null ? "{}" : response.getBody()).path("models");
            List<String> names = new ArrayList<>();
            if (models.isArray()) {
                for (JsonNode model : models) {
                    String name = stripModelsPrefix(model.path("name").asText(""));
                    if (name.isBlank() || !supportsGenerateContent(model)) {
                        continue;
                    }
                    names.add(name);
                }
            }
            probe.keyAccepted = true;
            probe.names = names;
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            if (status == 401 || status == 403) {
                probe.keyAccepted = false;
                probe.error = "Gemini rejected the API key (HTTP " + status
                        + "). Update ai.gemini.api-key and restart the backend so the new key is loaded.";
            } else {
                probe.keyAccepted = true;
                probe.error = extractErrorMessage(e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            probe.keyAccepted = true;
            probe.error = e.getMessage();
        }
        return probe;
    }

    private static boolean supportsGenerateContent(JsonNode model) {
        JsonNode methods = model.path("supportedGenerationMethods");
        if (!methods.isArray() || methods.isEmpty()) {
            return true;
        }
        for (JsonNode method : methods) {
            if ("generateContent".equals(method.asText())) {
                return true;
            }
        }
        return false;
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

    private RuntimeException apiException(int status, String body, String model) {
        String message = extractErrorMessage(body);
        if (status == 429) {
            long retryAfter = parseRetryAfter(body);
            String hint = retryAfter > 0 ? " Retry in " + retryAfter + "s." : " Try another supported model.";
            return new RateLimitException("Gemini rate limit reached for model '" + model + "'."
                    + hint + (message.isBlank() ? "" : " Server: " + message), retryAfter);
        }
        if (status == 401 || status == 403) {
            return new RuntimeException("Gemini authorization failed — check ai.gemini.api-key and restart the backend");
        }
        if (status == 503) {
            return new RuntimeException("Gemini model '" + model + "' is overloaded (HTTP 503). "
                    + "This is Google capacity, not an invalid API key."
                    + (message.isBlank() ? "" : " Server: " + message));
        }
        return new RuntimeException("Gemini API error " + status
                + " (" + model + ")"
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

    private long retryDelayMs(String body, int attempt) {
        long suggested = parseRetryAfter(body) * 1000;
        long fallback = 700L * (attempt + 1);
        return Math.min(Math.max(suggested, fallback), 2500);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Gemini request interrupted", e);
        }
    }

    private String configuredModel() {
        return stripModelsPrefix(config.getModel() == null ? "" : config.getModel().trim());
    }

    private static String stripModelsPrefix(String name) {
        if (name != null && name.startsWith("models/")) {
            return name.substring("models/".length());
        }
        return name == null ? "" : name;
    }

    @Override public boolean isConfigured() { return config.isConfigured(); }
    @Override public boolean isMockMode() { return properties.isMockMode() || !isConfigured(); }
    @Override public String getProvider() { return "gemini"; }

    private static class ModelListProbe {
        boolean keyAccepted;
        String error = "";
        List<String> names = List.of();
    }

    public static class RateLimitException extends RuntimeException {
        public final long retryAfterSeconds;
        public RateLimitException(String message, long retryAfterSeconds) {
            super(message);
            this.retryAfterSeconds = retryAfterSeconds;
        }
    }
}
