package com.acc.testcraft_backend.service;

import com.acc.testcraft_backend.client.AiClient;
import com.acc.testcraft_backend.client.JiraClient;
import com.acc.testcraft_backend.model.AIReviewRequest;
import com.acc.testcraft_backend.model.AIReviewResponse;
import com.acc.testcraft_backend.model.JiraStory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class AIReviewService {

    private final AiClient aiClient;
    private final JiraClient jiraClient;

    public AIReviewService(AiClient aiClient, JiraClient jiraClient) {
        this.aiClient = aiClient;
        this.jiraClient = jiraClient;
    }

    public AIReviewResponse generateReview(AIReviewRequest request) {
        AIReviewResponse response = new AIReviewResponse();
        response.setIssueKey(request.getIssueKey());

        try {
            String details = request.getJiraDetails();

            if (details == null || details.isBlank()) {
                try {
                    JiraStory story = jiraClient.fetchIssue(request.getIssueKey());
                    details = buildStoryContext(story);
                } catch (Exception e) {
                    details = "Issue key: " + request.getIssueKey()
                            + "\n(Jira details unavailable)";
                }
            }

            String reviewText;

            if (aiClient.isMockMode()) {
                reviewText = mockReview(request.getIssueKey(), details);
                response.setMockMode(true);
            } else {
                String prompt = """
                        You are a senior QA engineer reviewing a Jira story.
                        Provide a concise review covering:
                        1. Clarity of requirements
                        2. Testability concerns
                        3. Missing acceptance criteria
                        4. Risk areas
                        5. Recommended test focus

                        Story key: %s
                        Details:
                        %s
                        """.formatted(request.getIssueKey(), details);

                reviewText = aiClient.generate(prompt);
            }

            response.setReview(reviewText);
            response.setFindings(extractFindings(reviewText));
            response.setSuccess(true);
            return response;
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError(e.getMessage());
            return response;
        }
    }

    private String buildStoryContext(JiraStory story) {
        StringBuilder sb = new StringBuilder();
        sb.append("Summary: ").append(story.getSummary()).append('\n');
        sb.append("Type: ").append(story.getIssueType()).append('\n');
        sb.append("Status: ").append(story.getStatus()).append('\n');
        sb.append("Priority: ").append(story.getPriority()).append('\n');
        sb.append("Description: ").append(story.getDescription()).append('\n');
        sb.append("Acceptance Criteria: ").append(story.getAcceptanceCriteria());
        return sb.toString();
    }

    private String mockReview(String issueKey, String details) {
        return """
                [Mock AI Review for %s]

                Overall: Story is testable with minor gaps.

                Findings:
                - Acceptance criteria could be more specific on edge cases
                - Missing non-functional requirements (performance/security)
                - Consider adding negative test scenarios
                - Verify integration points are documented

                Recommended test focus: happy path, validation, authorization, error handling.

                Context preview:
                %s
                """.formatted(issueKey, truncate(details, 500));
    }

    private List<String> extractFindings(String review) {
        return Arrays.stream(review.split("\n"))
                .map(String::trim)
                .filter(line -> line.startsWith("-") || line.startsWith("•"))
                .map(line -> line.replaceFirst("^[-•]\\s*", ""))
                .filter(line -> !line.isBlank())
                .toList();
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
