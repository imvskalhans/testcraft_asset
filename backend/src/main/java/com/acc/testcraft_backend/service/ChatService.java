package com.acc.testcraft_backend.service;

import com.acc.testcraft_backend.client.AiClient;
import com.acc.testcraft_backend.model.ChatRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

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

        String answer = aiClient.isMockMode() ? mockAnswer(message) : aiClient.generate("""
                You are TestCraft's helpful QA assistant. Answer the user's question clearly and briefly.
                Focus on Jira, Zephyr, software testing, test-case design, and using TestCraft.
                If the question needs access to a specific Jira project or account, explain that limitation.

                User question: %s
                """.formatted(message));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("answer", answer);
        response.put("mockMode", aiClient.isMockMode());
        return response;
    }

    private String mockAnswer(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("test case") || lower.contains("testcase")) {
            return "A strong test case includes a clear title, preconditions, data, steps, and expected results. "
                    + "Use Generate Tests for a Jira story, then review and edit the cases before publishing.";
        }
        if (lower.contains("jira")) {
            return "Enter a Jira issue key on Jira Story and choose Fetch issue. TestCraft can then use the story details "
                    + "to generate tests or run an AI review.";
        }
        if (lower.contains("zephyr") || lower.contains("publish")) {
            return "Generate your cases first, select the Zephyr project and folder on Publish & Link, then publish and link "
                    + "the resulting test cases back to the relevant Jira story.";
        }
        return "I’m running in demo mode right now. Ask me about Jira stories, test cases, Zephyr publishing, or QA practices.";
    }
}
