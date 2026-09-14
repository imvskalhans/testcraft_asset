package com.acc.testcraft_backend.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gemini generateContent models from Google AI Studio / Gemini API docs.
 * https://ai.google.dev/gemini-api/docs/models
 * Image, live, TTS, and transcribe endpoints are omitted — TestCraft needs text generation.
 */
public final class GeminiModelCatalog {

    public static final String DEFAULT_MODEL = "gemini-3.5-flash-lite";
    public static final String DOCS_URL = "https://ai.google.dev/gemini-api/docs/models";

    private GeminiModelCatalog() {}

    public static List<Map<String, Object>> options() {
        List<Map<String, Object>> options = new ArrayList<>();
        add(options, "gemini-3.8-flash", "Gemini 3.8 Flash", "Most capable Flash model");
        add(options, "gemini-3.7-flash", "Gemini 3.7 Flash", "Complex coding and agent workflows");
        add(options, "gemini-3.6-flash", "Gemini 3.6 Flash", "Balanced speed and multimodal work");
        add(options, "gemini-3.5-flash", "Gemini 3.5 Flash", "High-throughput Flash");
        add(options, "gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite", "Fastest / lowest cost (recommended)");
        add(options, "gemini-3.1-flash-lite", "Gemini 3.1 Flash-Lite", "Cost-efficient Flash-Lite");
        add(options, "gemini-3.1-flash-image", "Nano Banana 2", "Vision-capable model");
        add(options, "gemini-3.1-flash-lite-image", "Nano Banana 2 Lite", "Vision-capable lite model");
        add(options, "gemini-3-pro-image", "Nano Banana Pro", "Advanced vision reasoning");
        add(options, "gemini-3.1-pro-preview", "Gemini 3.1 Pro", "Advanced reasoning (preview)");
        add(options, "gemini-3-flash-preview", "Gemini 3 Flash", "Preview Flash — may 503 under load");
        return options;
    }

    public static List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> option : options()) {
            ids.add((String) option.get("id"));
        }
        return ids;
    }

    public static boolean isKnown(String model) {
        if (model == null || model.isBlank()) {
            return false;
        }
        String id = stripPrefix(model.trim());
        return ids().contains(id);
    }

    /** After the configured model, try other generateContent Flash models. */
    public static List<String> fallbackModels() {
        return List.of(
                DEFAULT_MODEL,
                "gemini-3.1-flash-lite",
                "gemini-3.5-flash",
                "gemini-3.6-flash",
                "gemini-3.7-flash",
                "gemini-3.8-flash",
                "gemini-3.1-flash-image",
                "gemini-3.1-flash-lite-image"
        );
    }

    public static String stripPrefix(String name) {
        if (name != null && name.startsWith("models/")) {
            return name.substring("models/".length());
        }
        return name == null ? "" : name;
    }

    private static void add(List<Map<String, Object>> options, String id, String label, String hint) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("id", id);
        option.put("label", label);
        option.put("hint", hint);
        option.put("recommended", DEFAULT_MODEL.equals(id));
        options.add(option);
    }
}
