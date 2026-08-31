package com.acc.testcraft_backend.service;

import com.acc.testcraft_backend.client.AiClient;
import com.acc.testcraft_backend.model.ChatRequest;
import com.acc.testcraft_backend.model.ChatWorkspaceContext;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private final AiClient aiClient;

    public ChatService(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public Map<String, Object> answer(ChatRequest request) {
        String message = request.getMessage() == null ? "" : request.getMessage().trim();
        if (message.isBlank()) throw new IllegalArgumentException("message is required");
        if (message.length() > 2000) throw new IllegalArgumentException("message must be 2000 characters or fewer");

        ChatWorkspaceContext context = request.getContext();
        String contextBlock = buildContextBlock(context);
        String answer = aiClient.isMockMode()
                ? mockAnswer(message, context)
                : aiClient.generate("""
                        You are TestCraft's helpful QA assistant embedded in a Jira + Zephyr test workflow tool.
                        Answer the user's question clearly and briefly using the workspace context below when it is relevant.
                        Prefer concrete guidance about the current story, generated test cases, and published Zephyr keys.
                        If the user asks about data you do not have, say what is missing and suggest the next TestCraft step.
                        Do not invent Jira keys, Zephyr keys, or test cases that are not in the context.

                        WORKSPACE CONTEXT:
                        %s

                        USER QUESTION:
                        %s
                        """.formatted(contextBlock, message));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("answer", answer);
        response.put("mockMode", aiClient.isMockMode());
        response.put("contextAware", context != null && hasContext(context));
        return response;
    }

    private String buildContextBlock(ChatWorkspaceContext context) {
        if (context == null) {
            return "No active workspace context was provided.";
        }

        StringBuilder builder = new StringBuilder();
        appendLine(builder, "Current page", context.getCurrentPage());
        appendLine(builder, "Active issue key", context.getIssueKey());
        appendLine(builder, "Story summary", context.getStorySummary());
        appendLine(builder, "Story status", context.getStoryStatus());
        appendLine(builder, "Story type", context.getStoryType());
        appendLine(builder, "Story details", truncate(context.getStoryDetails(), 1800));
        appendLine(builder, "Generated test case count", String.valueOf(context.getTestCaseCount()));
        appendLine(builder, "Selected test type", context.getTestType());
        appendLine(builder, "Generated test cases", joinList(context.getTestCaseSummaries()));
        appendLine(builder, "Published Zephyr keys", joinList(context.getPublishedKeys()));
        return builder.toString().trim();
    }

    private boolean hasContext(ChatWorkspaceContext context) {
        return isPresent(context.getIssueKey())
                || isPresent(context.getStorySummary())
                || context.getTestCaseCount() > 0
                || (context.getPublishedKeys() != null && !context.getPublishedKeys().isEmpty());
    }

    private String mockAnswer(String message, ChatWorkspaceContext context) {
        String lower = message.toLowerCase();
        String issueKey = context != null ? safe(context.getIssueKey()) : "";
        int caseCount = context != null ? context.getTestCaseCount() : 0;
        List<String> published = context != null ? context.getPublishedKeys() : List.of();

        if (lower.contains("publish") || lower.contains("zephyr")) {
            if (!published.isEmpty()) {
                return "You already published "
                        + String.join(", ", published)
                        + " for "
                        + (issueKey.isBlank() ? "the current story" : issueKey)
                        + ". Next, link those keys to the Jira story on Publish & Link, then create or update test cycles.";
            }
            if (caseCount > 0) {
                return "You have "
                        + caseCount
                        + " generated test case(s) ready, but none are published yet. Go to Publish & Link, choose the Zephyr project and folder, then publish them.";
            }
            return "No test cases are loaded yet. Fetch a Jira story, generate or import cases, then publish them from Publish & Link.";
        }

        if (lower.contains("story") || lower.contains("jira") || lower.contains(issueKey.toLowerCase())) {
            if (context != null && isPresent(context.getStorySummary())) {
                return "Current story "
                        + issueKey
                        + ": "
                        + context.getStorySummary()
                        + (isPresent(context.getStoryStatus()) ? " (status: " + context.getStoryStatus() + ")" : "")
                        + ". "
                        + (caseCount > 0
                        ? "You already have " + caseCount + " generated case(s) for this workflow."
                        : "Generate or import test cases next if coverage is still missing.");
            }
            return "No Jira story is loaded in the workspace yet. Open Jira Story, enter an issue key, and fetch it first.";
        }

        if (lower.contains("test case") || lower.contains("coverage") || lower.contains("cases")) {
            if (caseCount > 0 && context != null && context.getTestCaseSummaries() != null && !context.getTestCaseSummaries().isEmpty()) {
                String preview = context.getTestCaseSummaries().stream().limit(3).collect(Collectors.joining("; "));
                return "There are "
                        + caseCount
                        + " test case(s) in the editor"
                        + (issueKey.isBlank() ? "" : " for " + issueKey)
                        + ". Examples: "
                        + preview
                        + (caseCount > 3 ? "; and more." : ".")
                        + (published.isEmpty()
                        ? " They are not published yet."
                        : " Published keys: " + String.join(", ", published) + ".");
            }
            return "No generated test cases are loaded right now. Use Generate Tests or import JSON/CSV/Excel to add cases.";
        }

        if (context != null && hasContext(context)) {
            return "I'm in demo mode, but I can see "
                    + (issueKey.isBlank() ? "your current workspace" : issueKey)
                    + " with "
                    + caseCount
                    + " generated case(s)"
                    + (published.isEmpty() ? "" : " and published keys " + String.join(", ", published))
                    + ". Ask me about coverage gaps, publishing, linking, or the next QA step.";
        }

        return "I'm running in demo mode right now. Fetch a Jira story first, then ask me about its test coverage, publishing, or Zephyr linking.";
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (!isPresent(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(label).append(": ").append(value.trim());
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(this::isPresent)
                .collect(Collectors.joining("; "));
    }

    private String truncate(String value, int maxLength) {
        if (!isPresent(value) || value.length() <= maxLength) {
            return safe(value);
        }
        return value.substring(0, maxLength).trim() + "...";
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
