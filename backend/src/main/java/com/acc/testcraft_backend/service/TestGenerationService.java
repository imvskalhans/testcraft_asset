package com.acc.testcraft_backend.service;

import com.acc.testcraft_backend.client.AiClient;
import com.acc.testcraft_backend.client.JiraClient;
import com.acc.testcraft_backend.model.JiraStory;
import com.acc.testcraft_backend.model.TestCase;
import com.acc.testcraft_backend.model.TestGenerationRequest;
import com.acc.testcraft_backend.model.TestGenerationResponse;
import com.acc.testcraft_backend.model.AiAttachment;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

@Service
public class TestGenerationService {

    private final AiClient aiClient;
    private final JiraClient jiraClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TestGenerationService(AiClient aiClient, JiraClient jiraClient) {
        this.aiClient = aiClient;
        this.jiraClient = jiraClient;
    }

    public TestGenerationResponse generate(TestGenerationRequest request) {
        TestGenerationResponse response = new TestGenerationResponse();
        response.setIssueKey(request.getIssueKey());
        response.setTestType(request.getTestType());

        try {
            String context = request.getJiraDetails();
            if (context == null || context.isBlank()) {
                try {
                    JiraStory story = jiraClient.fetchIssue(request.getIssueKey());
                    context = buildStoryContext(story);
                    if (request.getAttachments() == null || request.getAttachments().isEmpty()) {
                        request.setAttachments(story.getAttachments());
                    }
                } catch (Exception e) {
                    context = "Issue key: " + request.getIssueKey()
                            + "\n(Jira details unavailable — using mock context)";
                }
            }

            if (aiClient.isMockMode()) {
                response.setMockMode(true);
                response.setTestCases(mockTestCases(request));
                response.setParseSuccess(true);
                return response;
            }

            List<AiAttachment> attachments = sanitizeAttachments(request.getAttachments());
            String prompt = buildPrompt(request, context);
            String attachmentText = attachmentText(attachments);
            if (!attachmentText.isBlank()) prompt += "\n\nAttached Jira/user text files:\n" + attachmentText;
            if (attachments.stream().anyMatch(a -> a.getMimeType().startsWith("image/")) && !aiClient.supportsImageInput()) {
                throw new IllegalArgumentException("The configured AI provider does not support image attachments. Remove images or switch to a vision-capable provider.");
            }
            String raw = aiClient.generate(prompt, attachments.stream().filter(a -> a.getMimeType().startsWith("image/")).toList());
            response.setRawResponse(raw);

            List<TestCase> cases = parseTestCases(raw, request.getTestType());
            response.setTestCases(cases);
            response.setParseSuccess(!cases.isEmpty());
            return response;
        } catch (Exception e) {
            response.setParseSuccess(false);
            response.setError(e.getMessage());
            response.setTestCases(mockTestCases(request));
            return response;
        }
    }

    private List<AiAttachment> sanitizeAttachments(List<AiAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) return List.of();
        if (attachments.size() > 5) throw new IllegalArgumentException("Attach up to 5 files at a time");
        for (AiAttachment attachment : attachments) {
            if (attachment == null || attachment.getData() == null || attachment.getData().isBlank()) throw new IllegalArgumentException("Each attachment must contain file data");
            String mime = attachment.getMimeType() == null ? "" : attachment.getMimeType().toLowerCase();
            if (!mime.startsWith("image/") && !mime.startsWith("text/") && !mime.equals("application/json") && !mime.equals("text/csv")) throw new IllegalArgumentException("Unsupported attachment type. Use an image or text/JSON/CSV file.");
            if (attachment.getData().length() > 8_000_000) throw new IllegalArgumentException("Attachment is too large (maximum 6 MB)");
            attachment.setMimeType(mime);
        }
        return attachments;
    }

    private String attachmentText(List<AiAttachment> attachments) {
        StringBuilder text = new StringBuilder();
        for (AiAttachment attachment : attachments) {
            if (attachment.getMimeType().startsWith("image/")) continue;
            try {
                String data = attachment.getData().contains(",") ? attachment.getData().substring(attachment.getData().indexOf(',') + 1) : attachment.getData();
                text.append("\n--- ").append(attachment.getName()).append(" ---\n").append(new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8));
            } catch (Exception e) { throw new IllegalArgumentException("Could not read attachment " + attachment.getName()); }
        }
        return text.toString();
    }

    private String buildPrompt(TestGenerationRequest request, String context) {
        String outputContract = """
                OUTPUT CONTRACT (required for Zephyr publish — do not break this):
                Return ONLY a JSON array. No markdown. No commentary.
                Each object MUST contain exactly these keys:
                {
                  "testName": "short unique name, max 80 chars, no quotes or newlines",
                  "objective": "one sentence",
                  "preCondition": "setup required",
                  "steps": ["step 1", "step 2", "step 3"],
                  "expectedResult": "observable expected outcome",
                  "priority": "High" | "Medium" | "Low",
                  "testType": "%s"
                }
                steps must be a JSON array of strings with at least 2 items.
                """.formatted(request.getTestType());

        String type = request.getPromptType() == null
                ? "default"
                : request.getPromptType().trim().toLowerCase();

        String style = switch (type) {
            case "advanced" -> """
                    Use an advanced QA prompt:
                    Cover happy path, negative path, boundary, authorization, and data validation.
                    Make each test independently executable.
                    """;
            case "smoke" -> """
                    Use a smoke-test prompt:
                    Focus on the fastest critical-path checks needed to confirm the story is basically working.
                    Prioritize login/access, primary user flow, and one key failure guardrail per test.
                    Keep steps short and avoid deep edge-case exploration.
                    """;
            case "security" -> """
                    Use a security-focused QA prompt:
                    Cover authentication, authorization, session handling, input validation, injection risks,
                    sensitive data exposure, and unsafe defaults.
                    Include negative tests for unauthorized access and malformed input.
                    """;
            case "api" -> """
                    Use an API-focused QA prompt:
                    Cover request/response contracts, required headers, status codes, validation errors,
                    pagination or filtering behavior, idempotency where relevant, and backward compatibility.
                    Make steps explicit about endpoint, method, payload, and expected response.
                    """;
            case "mobile" -> """
                    Use a mobile-focused QA prompt:
                    Cover responsive layout, touch interactions, orientation changes, offline or flaky-network
                    behavior, permissions, and platform-specific UX expectations where relevant.
                    Keep steps practical for manual mobile execution.
                    """;
            case "template", "custom" -> (request.getCustomPrompt() == null || request.getCustomPrompt().isBlank())
                    ? "Follow standard QA coverage."
                    : request.getCustomPrompt().trim();
            default -> """
                    Use a default QA prompt:
                    Cover the main happy path and the most important negative case.
                    Keep steps short and executable.
                    """;
        };

        if (request.getAdditionalInstructions() != null && !request.getAdditionalInstructions().isBlank()) {
            style += "\n\nAdditional instructions from the QA engineer:\n"
                    + request.getAdditionalInstructions().trim();
        }

        return """
                You are a QA engineer generating test cases that will be published to Zephyr Scale.
                Generate exactly %d %s test cases for Jira story %s.

                %s

                %s

                Story details:
                %s
                """.formatted(
                request.getTestCount(),
                request.getTestType(),
                request.getIssueKey(),
                style,
                outputContract,
                context
        );
    }

    private List<TestCase> mockTestCases(TestGenerationRequest request) {
        List<TestCase> cases = new ArrayList<>();
        int count = Math.max(1, request.getTestCount());

        for (int i = 1; i <= count; i++) {
            TestCase tc = new TestCase();
            tc.setTestName("Verify " + request.getTestType() + " scenario " + i + " for " + request.getIssueKey());
            tc.setObjective("Validate core behavior for scenario " + i);
            tc.setPreCondition("User has access and test data is prepared");
            tc.setSteps(List.of(
                    "Navigate to the feature",
                    "Perform action for scenario " + i,
                    "Observe system response"
            ));
            tc.setExpectedResult("System behaves as specified in acceptance criteria");
            tc.setPriority(i == 1 ? "High" : "Medium");
            tc.setTestType(request.getTestType());
            tc.setStatus("Draft");
            cases.add(tc);
        }

        return cases;
    }

    private List<TestCase> parseTestCases(String raw, String defaultType) {
        try {
            String json = extractJsonArray(raw);
            List<Map<String, Object>> items = objectMapper.readValue(
                    json,
                    new TypeReference<>() {}
            );

            List<TestCase> cases = new ArrayList<>();

            for (Map<String, Object> item : items) {
                TestCase tc = new TestCase();
                tc.setTestName(sanitizeName(stringValue(item.get("testName"))));
                tc.setObjective(stringValue(item.get("objective")));
                tc.setPreCondition(stringValue(item.get("preCondition")));
                tc.setExpectedResult(stringValue(item.get("expectedResult")));
                tc.setPriority(normalizePriority(stringValue(item.get("priority"))));
                tc.setTestType(stringValue(item.get("testType"), defaultType));
                tc.setStatus("Draft");

                Object stepsObj = item.get("steps");
                if (stepsObj instanceof List<?> list) {
                    tc.setSteps(list.stream().map(Object::toString).toList());
                }
                if (tc.getSteps() == null || tc.getSteps().isEmpty()) {
                    tc.setSteps(List.of("Execute the scenario", "Verify the result"));
                }

                cases.add(tc);
            }

            return cases;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String extractJsonArray(String raw) {
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    private String buildStoryContext(JiraStory story) {
        return "Summary: " + story.getSummary()
                + "\nDescription: " + story.getDescription()
                + "\nAcceptance Criteria: " + story.getAcceptanceCriteria();
    }

    private String sanitizeName(String name) {
        if (name == null) {
            return "Untitled test";
        }
        String cleaned = name.replaceAll("[\\r\\n\"\\\\]", " ").replaceAll("\\s+", " ").trim();
        return cleaned.length() > 80 ? cleaned.substring(0, 80).trim() : cleaned;
    }

    private String normalizePriority(String priority) {
        if (priority == null) {
            return "Medium";
        }
        String p = priority.trim().toLowerCase();
        if (p.startsWith("high") || p.startsWith("crit")) {
            return "High";
        }
        if (p.startsWith("low")) {
            return "Low";
        }
        return "Medium";
    }

    private String stringValue(Object value) {
        return stringValue(value, "");
    }

    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }
}
