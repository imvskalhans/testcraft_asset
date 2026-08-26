package com.acc.testcraft_backend.service;

import com.acc.testcraft_backend.client.AiClient;
import com.acc.testcraft_backend.model.LinkedStory;
import com.acc.testcraft_backend.model.ReleaseAIRequest;
import com.acc.testcraft_backend.model.ReleaseAIResponse;
import com.acc.testcraft_backend.model.ReleaseProcess;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReleaseAIService {

    private final AiClient aiClient;
    private final ReleaseProcessService releaseProcessService;

    public ReleaseAIService(
            AiClient aiClient,
            ReleaseProcessService releaseProcessService
    ) {
        this.aiClient = aiClient;
        this.releaseProcessService = releaseProcessService;
    }

    public List<Map<String, String>> getAvailableActions() {
        return List.of(
                action("story-review", "AI review of the fetched story", "Read the Jira details first, then get a QA review of gaps, risks, and missing criteria."),
                action("coverage-gap", "Identify coverage gaps", "Compare the story against typical test coverage and list missing scenarios."),
                action("risk-summary", "Summarize testing risks", "Highlight release risks, dependencies, and what to test first."),
                action("cycle-readiness", "Assess cycle readiness", "Check whether this change is ready for a test cycle."),
                action("story-quality", "Review story quality", "Score the story quality and suggest what to clarify before testing.")
        );
    }

    public ReleaseAIResponse generateReleaseAnalysis(ReleaseAIRequest request) {
        ReleaseAIResponse response = new ReleaseAIResponse();
        response.setCrKey(request.getCrKey());
        response.setAction(request.getAction());

        if (request.getCrKey() == null || request.getCrKey().isBlank()) {
            response.setSuccess(false);
            response.setError("CR key is required");
            return response;
        }

        try {
            ReleaseProcess release =
                    releaseProcessService.fetchChangeTicket(request.getCrKey());

            String context = buildReleaseContext(release, request.getContext());
            String action = request.getAction() != null
                    ? request.getAction()
                    : "risk-summary";

            if (aiClient.isMockMode()) {
                response.setAnalysis(mockAnalysis(release, action));
                response.setMockMode(true);
            } else {
                String prompt = """
                        You are a QA release manager. Action: %s
                        Analyze this change request and linked stories.
                        Provide actionable QA recommendations.

                        %s
                        """.formatted(action, context);

                response.setAnalysis(aiClient.generate(prompt));
            }

            response.setSuccess(true);
            return response;
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError(e.getMessage());
            return response;
        }
    }

    private Map<String, String> action(String id, String label, String description) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("label", label);
        map.put("description", description);
        return map;
    }

    private String buildReleaseContext(ReleaseProcess release, String extra) {
        StringBuilder sb = new StringBuilder();
        sb.append("CR: ").append(release.getCrKey())
                .append(" - ").append(release.getCrSummary()).append('\n');
        sb.append("Status: ").append(release.getStatus()).append('\n');
        sb.append("Linked stories: ").append(release.getLinkedStories().size()).append('\n');

        for (LinkedStory story : release.getLinkedStories()) {
            sb.append("- ").append(story.getKey())
                    .append(": ").append(story.getSummary())
                    .append(" [").append(story.getStatus()).append("]\n");
        }

        if (extra != null && !extra.isBlank()) {
            sb.append("\nAdditional context:\n").append(extra);
        }

        return sb.toString();
    }

    private String mockAnalysis(ReleaseProcess release, String action) {
        return """
                [Mock Release AI — %s]

                Change Request: %s
                Linked stories: %d

                Summary:
                - Prioritize stories in "In Progress" or "Ready for QA" status
                - Ensure each linked story has at least one test cycle
                - Validate regression scope for cross-story dependencies
                - Flag stories missing acceptance criteria before cycle creation

                Next steps:
                1. Run get-test-cycles to audit existing coverage
                2. Generate missing test cases for uncovered stories
                3. Create test cycles and link traceability
                """.formatted(
                action,
                release.getCrKey(),
                release.getLinkedStories().size()
        );
    }
}
