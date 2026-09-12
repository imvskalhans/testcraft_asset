package com.acc.testcraft_backend.service;

import com.acc.testcraft_backend.client.AiClient;
import com.acc.testcraft_backend.client.JiraClient;
import com.acc.testcraft_backend.client.ZephyrClient;
import com.acc.testcraft_backend.config.JiraProperties;
import com.acc.testcraft_backend.model.AiCoverageAnalysis;
import com.acc.testcraft_backend.model.AiCoverageMapping;
import com.acc.testcraft_backend.model.AiRecommendedTestCase;
import com.acc.testcraft_backend.model.CycleTraceability;
import com.acc.testcraft_backend.model.JiraStory;
import com.acc.testcraft_backend.model.LinkedStory;
import com.acc.testcraft_backend.model.LinkedTestCaseRef;
import com.acc.testcraft_backend.model.ReleaseProcess;
import com.acc.testcraft_backend.model.StoryTraceability;
import com.acc.testcraft_backend.model.TestCycle;
import com.acc.testcraft_backend.model.TestExecutionRef;
import com.acc.testcraft_backend.model.TraceabilityDashboardResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TraceabilityService {

    private static final int MAX_AI_STORIES = 8;

    private final ReleaseProcessService releaseProcessService;
    private final ZephyrClient zephyrClient;
    private final JiraClient jiraClient;
    private final JiraProperties jiraProperties;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    public TraceabilityService(
            ReleaseProcessService releaseProcessService,
            ZephyrClient zephyrClient,
            JiraClient jiraClient,
            JiraProperties jiraProperties,
            AiClient aiClient,
            ObjectMapper objectMapper
    ) {
        this.releaseProcessService = releaseProcessService;
        this.zephyrClient = zephyrClient;
        this.jiraClient = jiraClient;
        this.jiraProperties = jiraProperties;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    public TraceabilityDashboardResponse buildDashboard(String issueKey) {
        if (issueKey == null || issueKey.isBlank()) {
            throw new IllegalArgumentException("Issue key is required");
        }

        String normalizedKey = issueKey.trim().toUpperCase();
        zephyrClient.beginBulkLookup();
        try {
            return buildDashboardInternal(normalizedKey);
        } finally {
            zephyrClient.endBulkLookup();
        }
    }

    private TraceabilityDashboardResponse buildDashboardInternal(String normalizedKey) {
        JiraStory rootIssue = releaseProcessService.fetchIssue(normalizedKey);
        String rootType = rootIssue.getIssueType();
        List<LinkedStory> linkedStories;
        if (isContainerIssue(rootType)) {
            ReleaseProcess release = releaseProcessService.fetchChangeTicket(normalizedKey);
            linkedStories = release.getLinkedStories();
            if (linkedStories == null || linkedStories.isEmpty()) {
                linkedStories = new ArrayList<>(List.of(selfStory(rootIssue)));
            } else {
                linkedStories = new ArrayList<>(linkedStories);
            }
        } else {
            linkedStories = new ArrayList<>();
            linkedStories.add(selfStory(rootIssue));
        }
        mergeChildren(linkedStories, jiraClient.fetchChildWorkItems(normalizedKey));

        TraceabilityDashboardResponse response = new TraceabilityDashboardResponse();
        response.setSuccess(true);
        response.setIssueKey(normalizedKey);
        response.setIssueSummary(rootIssue.getSummary());
        response.setIssueStatus(rootIssue.getStatus());
        response.setIssueType(rootType);
        response.setJiraUrl(jiraBrowseUrl(normalizedKey));

        List<StoryTraceability> stories = new ArrayList<>();
        int storiesWithCoverage = 0;
        int storiesWithCycles = 0;
        int storiesFullyTraced = 0;
        int totalLinkedTestCases = 0;
        int totalCycles = 0;
        int totalExecutions = 0;
        int totalPassed = 0;
        int totalFailed = 0;
        int totalBlocked = 0;
        int totalNotExecuted = 0;
        List<String> releaseGaps = new ArrayList<>();
        int remainingAiStories = MAX_AI_STORIES;

        for (LinkedStory story : linkedStories) {
            StoryTraceability storyTrace = buildStoryTraceability(story, remainingAiStories > 0);
            remainingAiStories--;
            stories.add(storyTrace);

            if (storyTrace.isHasCoverage()) {
                storiesWithCoverage++;
            }
            if (storyTrace.isHasCycles()) {
                storiesWithCycles++;
            }
            if (storyTrace.isFullyTraced()) {
                storiesFullyTraced++;
            }

            totalLinkedTestCases += storyTrace.getLinkedTestCaseCount();
            totalCycles += storyTrace.getCycleCount();
            totalExecutions += storyTrace.getExecutionCount();
            totalPassed += storyTrace.getPassedCount();
            totalFailed += storyTrace.getFailedCount();
            totalBlocked += storyTrace.getBlockedCount();
            totalNotExecuted += storyTrace.getNotExecutedCount();

            for (String gap : storyTrace.getGaps()) {
                releaseGaps.add(story.getKey() + ": " + gap);
            }
            if (storyTrace.getAiCoverage() != null) {
                for (String gap : storyTrace.getAiCoverage().getCriticalGaps()) {
                    if (gap != null && !gap.isBlank()) {
                        releaseGaps.add(story.getKey() + " AI: " + gap);
                    }
                }
            }
        }

        if (storiesWithCoverage < linkedStories.size()) {
            releaseGaps.add(
                    (linkedStories.size() - storiesWithCoverage)
                            + " work item"
                            + (linkedStories.size() - storiesWithCoverage == 1 ? " has" : "s have")
                            + " no linked test cases"
            );
        }
        if (storiesWithCycles < linkedStories.size()) {
            releaseGaps.add(
                    (linkedStories.size() - storiesWithCycles)
                            + " work item"
                            + (linkedStories.size() - storiesWithCycles == 1 ? " has" : "s have")
                            + " no test cycles"
            );
        }

        response.setTotalStories(linkedStories.size());
        response.setStoriesWithCoverage(storiesWithCoverage);
        response.setStoriesWithCycles(storiesWithCycles);
        response.setStoriesFullyTraced(storiesFullyTraced);
        response.setTotalLinkedTestCases(totalLinkedTestCases);
        response.setTotalCycles(totalCycles);
        response.setTotalExecutions(totalExecutions);
        response.setTotalPassed(totalPassed);
        response.setTotalFailed(totalFailed);
        response.setTotalBlocked(totalBlocked);
        response.setTotalNotExecuted(totalNotExecuted);
        response.setGaps(releaseGaps);
        response.setStories(stories);
        response.setMessage(
                "Traceability loaded for "
                        + linkedStories.size()
                        + " work item"
                        + (linkedStories.size() == 1 ? "" : "s")
                        + " — "
                        + storiesFullyTraced
                        + " fully traced"
        );
        return response;
    }

    private StoryTraceability buildStoryTraceability(LinkedStory story, boolean includeAi) {
        StoryTraceability trace = new StoryTraceability();
        trace.setStoryKey(story.getKey());
        trace.setStorySummary(story.getSummary());
        trace.setStoryStatus(story.getStatus());
        trace.setIssueType(story.getIssueType());
        trace.setJiraUrl(jiraBrowseUrl(story.getKey()));

        List<LinkedTestCaseRef> linkedTestCases =
                zephyrClient.getLinkedTestCasesForIssue(story.getKey());
        List<TestCycle> cycles =
                releaseProcessService.getTestCyclesForStory(story.getKey());

        Set<String> executionKeys = new HashSet<>();
        Map<String, String> latestStatusByCase = new LinkedHashMap<>();
        List<CycleTraceability> cycleTraces = new ArrayList<>();
        int executionCount = 0;
        int passedCount = 0;
        int failedCount = 0;
        int blockedCount = 0;
        int notExecutedCount = 0;

        for (TestCycle cycle : cycles) {
            CycleTraceability cycleTrace = new CycleTraceability();
            String cycleKey = cycle.getKey() != null && !cycle.getKey().isBlank()
                    ? cycle.getKey()
                    : cycle.getId();
            cycleTrace.setId(cycle.getId());
            cycleTrace.setKey(cycleKey);
            cycleTrace.setName(cycle.getName());
            cycleTrace.setStatus(cycle.getStatus());
            cycleTrace.setUrl(zephyrClient.buildScaleCloudTestCycleUrl(cycleKey));

            List<TestExecutionRef> executions;
            if (zephyrClient.isScaleCloudMode()) {
                executions = zephyrClient.getScaleCloudExecutionsForCycle(cycleKey, cycle.getId());
            } else {
                executions = mapPluginExecutions(cycle);
            }

            int cyclePassed = 0;
            int cycleFailed = 0;
            int cycleBlocked = 0;
            int cycleNotExecuted = 0;
            for (TestExecutionRef execution : executions) {
                String bucket = executionBucket(execution.getStatus());
                switch (bucket) {
                    case "passed" -> cyclePassed++;
                    case "failed" -> cycleFailed++;
                    case "blocked" -> cycleBlocked++;
                    default -> cycleNotExecuted++;
                }
                recordExecution(execution, executionKeys, latestStatusByCase);
            }

            cycleTrace.setExecutions(executions);
            cycleTrace.setExecutionCount(executions.size());
            cycleTrace.setPassedCount(cyclePassed);
            cycleTrace.setFailedCount(cycleFailed);
            cycleTrace.setBlockedCount(cycleBlocked);
            cycleTrace.setNotExecutedCount(cycleNotExecuted);
            executionCount += executions.size();
            passedCount += cyclePassed;
            failedCount += cycleFailed;
            blockedCount += cycleBlocked;
            notExecutedCount += cycleNotExecuted;
            cycleTraces.add(cycleTrace);
        }

        if (zephyrClient.isScaleCloudMode()) {
            for (TestExecutionRef execution : zephyrClient.getScaleCloudExecutionsLinkedToIssue(story.getKey())) {
                recordExecution(execution, executionKeys, latestStatusByCase);
            }
            for (LinkedTestCaseRef testCase : linkedTestCases) {
                String key = testCase.getKey() != null ? testCase.getKey().toUpperCase(Locale.ROOT) : "";
                if (key.isBlank() || executionKeys.contains(key)) {
                    continue;
                }
                for (TestExecutionRef execution : zephyrClient.getScaleCloudExecutionsForTestCase(testCase.getKey())) {
                    recordExecution(execution, executionKeys, latestStatusByCase);
                }
            }
        }

        for (LinkedTestCaseRef testCase : linkedTestCases) {
            String key = testCase.getKey() != null ? testCase.getKey().toUpperCase(Locale.ROOT) : "";
            testCase.setInCycle(executionKeys.contains(key));
            if (latestStatusByCase.containsKey(key)) {
                testCase.setStatus(latestStatusByCase.get(key));
            }
        }

        JiraStory issue = fetchIssueQuiet(story.getKey());
        List<String> criteria = splitCriteria(issue != null ? issue.getAcceptanceCriteria() : null);
        List<String> uncovered = unmatchedCriteria(criteria, linkedTestCases);
        trace.setAcceptanceCriteria(criteria);
        trace.setUncoveredCriteria(uncovered);

        List<String> gaps = new ArrayList<>();
        boolean hasCoverage = !linkedTestCases.isEmpty();
        boolean hasCycles = !cycleTraces.isEmpty();
        boolean hasExecutions = executionCount > 0 || !executionKeys.isEmpty();

        if (!hasCoverage) {
            gaps.add("No test cases linked to this work item");
        }
        if (!hasCycles) {
            gaps.add("No test cycles found for this work item");
        } else if (!hasExecutions) {
            gaps.add("Cycles exist but no test executions were found");
        }
        if (hasCoverage) {
            long uncoveredCases = linkedTestCases.stream().filter(tc -> !tc.isInCycle()).count();
            if (uncoveredCases > 0) {
                gaps.add(
                        uncoveredCases
                                + " linked test case(s) are not in a Zephyr test cycle (Jira coverage link is separate)"
                );
            }
        }
        if (hasExecutions && failedCount > 0) {
            gaps.add(failedCount + " execution(s) failed");
        }
        if (hasExecutions && blockedCount > 0) {
            gaps.add(blockedCount + " execution(s) blocked");
        }
        if (hasExecutions && passedCount == 0) {
            gaps.add("Executions exist but none have passed");
        }

        trace.setLinkedTestCases(linkedTestCases);
        trace.setTestCycles(cycleTraces);
        trace.setLinkedTestCaseCount(linkedTestCases.size());
        trace.setCycleCount(cycleTraces.size());
        trace.setExecutionCount(executionCount);
        trace.setPassedCount(passedCount);
        trace.setFailedCount(failedCount);
        trace.setBlockedCount(blockedCount);
        trace.setNotExecutedCount(notExecutedCount);
        trace.setHasCoverage(hasCoverage);
        trace.setHasCycles(hasCycles);
        trace.setFullyTraced(hasCoverage && hasCycles && hasExecutions && gaps.isEmpty());
        trace.setGaps(gaps);
        if (includeAi) {
            trace.setAiCoverage(analyzeCoverage(issue, trace));
        } else {
            AiCoverageAnalysis skipped = new AiCoverageAnalysis();
            skipped.setError("AI coverage analysis is limited to the first " + MAX_AI_STORIES + " work items in this report.");
            trace.setAiCoverage(skipped);
        }
        return trace;
    }

    private JiraStory fetchIssueQuiet(String storyKey) {
        try {
            return jiraClient.fetchIssue(storyKey);
        } catch (Exception e) {
            return null;
        }
    }

    private AiCoverageAnalysis analyzeCoverage(JiraStory issue, StoryTraceability trace) {
        if (aiClient == null || aiClient.isMockMode() || !aiClient.isConfigured()) {
            AiCoverageAnalysis mock = mockCoverageAnalysis(issue, trace);
            mock.setMockMode(true);
            if (aiClient != null && !aiClient.isMockMode() && !aiClient.isConfigured()) {
                mock.setError("AI is not configured. Showing a heuristic coverage hint until a provider is connected.");
            }
            return mock;
        }
        try {
            String raw = aiClient.generate(coveragePrompt(issue, trace));
            AiCoverageAnalysis parsed = parseCoverageAnalysis(raw);
            if (parsed.getSummary() == null || parsed.getSummary().isBlank()) {
                parsed.setSummary("AI compared this work item with the linked Zephyr cases.");
            }
            return parsed;
        } catch (Exception e) {
            AiCoverageAnalysis fallback = mockCoverageAnalysis(issue, trace);
            fallback.setError("AI coverage analysis failed: " + e.getMessage());
            return fallback;
        }
    }

    private String coveragePrompt(JiraStory issue, StoryTraceability trace) {
        StringBuilder cases = new StringBuilder();
        int count = 0;
        for (LinkedTestCaseRef testCase : trace.getLinkedTestCases()) {
            if (count++ >= 40) {
                cases.append("- ... additional linked cases omitted\n");
                break;
            }
            cases.append("- ")
                    .append(testCase.getKey() == null ? "" : testCase.getKey())
                    .append(": ")
                    .append(testCase.getName() == null ? "" : testCase.getName())
                    .append(testCase.isInCycle() ? " [in cycle]" : " [not in cycle]")
                    .append('\n');
        }
        if (cases.isEmpty()) {
            cases.append("(none linked)\n");
        }
        String description = issue != null && issue.getDescription() != null ? issue.getDescription() : "";
        String criteria = issue != null && issue.getAcceptanceCriteria() != null ? issue.getAcceptanceCriteria() : "";
        String summary = issue != null && issue.getSummary() != null ? issue.getSummary() : trace.getStorySummary();
        return """
                You are a QA architect in TestCraft. Map linked Zephyr test cases to this Jira work item's description and acceptance criteria.
                Identify missing scenarios that should be added as new test cases.

                Return JSON only, no markdown, with this shape:
                {
                  "summary": "one short paragraph",
                  "mappings": [
                    {
                      "requirement": "AC or implied requirement",
                      "coverage": "covered|partial|missing",
                      "testCases": ["KAN-T1"],
                      "notes": "why"
                    }
                  ],
                  "criticalGaps": ["highest-risk omission"],
                  "recommendedTestCases": [
                    {
                      "title": "one-line test case name",
                      "reason": "why this case is needed",
                      "priority": "High|Normal|Low"
                    }
                  ]
                }

                Rules:
                - Use the actual linked test case keys when a requirement is covered.
                - Mark implied requirements as starting with [Implied].
                - Recommend only useful missing cases. Do not invent exploits, credentials, or attack payloads.
                - If there are no linked cases, every explicit AC is missing.

                Work item: %s
                Summary: %s
                Description:
                %s
                Acceptance criteria:
                %s
                Linked Zephyr test cases:
                %s
                """.formatted(
                trace.getStoryKey(),
                summary == null ? "" : summary,
                truncate(description, 4000),
                truncate(criteria, 2500),
                cases
        );
    }

    private AiCoverageAnalysis parseCoverageAnalysis(String raw) {
        AiCoverageAnalysis analysis = new AiCoverageAnalysis();
        if (raw == null || raw.isBlank()) {
            analysis.setError("AI returned an empty coverage analysis.");
            return analysis;
        }
        try {
            JsonNode root = objectMapper.readTree(extractJsonObject(raw));
            analysis.setSummary(text(root, "summary"));
            if (root.get("criticalGaps") != null && root.get("criticalGaps").isArray()) {
                List<String> gaps = new ArrayList<>();
                for (JsonNode item : root.get("criticalGaps")) {
                    if (item != null && !item.asText("").isBlank()) {
                        gaps.add(item.asText());
                    }
                }
                analysis.setCriticalGaps(gaps);
            }
            if (root.get("mappings") != null && root.get("mappings").isArray()) {
                List<AiCoverageMapping> mappings = new ArrayList<>();
                for (JsonNode item : root.get("mappings")) {
                    AiCoverageMapping mapping = new AiCoverageMapping();
                    mapping.setRequirement(text(item, "requirement"));
                    mapping.setCoverage(normalizeCoverage(text(item, "coverage")));
                    mapping.setNotes(text(item, "notes"));
                    List<String> keys = new ArrayList<>();
                    if (item.get("testCases") != null && item.get("testCases").isArray()) {
                        for (JsonNode key : item.get("testCases")) {
                            if (key != null && !key.asText("").isBlank()) {
                                keys.add(key.asText());
                            }
                        }
                    }
                    mapping.setTestCases(keys);
                    mappings.add(mapping);
                }
                analysis.setMappings(mappings);
            }
            if (root.get("recommendedTestCases") != null && root.get("recommendedTestCases").isArray()) {
                List<AiRecommendedTestCase> recommended = new ArrayList<>();
                for (JsonNode item : root.get("recommendedTestCases")) {
                    AiRecommendedTestCase next = new AiRecommendedTestCase();
                    next.setTitle(text(item, "title"));
                    next.setReason(text(item, "reason"));
                    next.setPriority(text(item, "priority"));
                    if (next.getTitle() != null && !next.getTitle().isBlank()) {
                        recommended.add(next);
                    }
                }
                analysis.setRecommendedTestCases(recommended);
            }
            return analysis;
        } catch (Exception e) {
            analysis.setSummary(truncate(raw, 1200));
            analysis.setError("AI response was not valid JSON. Showing the raw analysis.");
            return analysis;
        }
    }

    private AiCoverageAnalysis mockCoverageAnalysis(JiraStory issue, StoryTraceability trace) {
        AiCoverageAnalysis analysis = new AiCoverageAnalysis();
        List<String> criteria = trace.getAcceptanceCriteria() == null ? List.of() : trace.getAcceptanceCriteria();
        List<LinkedTestCaseRef> cases = trace.getLinkedTestCases() == null ? List.of() : trace.getLinkedTestCases();
        analysis.setSummary(
                cases.isEmpty()
                        ? "No linked Zephyr cases were found, so the story description and acceptance criteria still need dedicated tests."
                        : "Heuristic match of linked case titles against acceptance criteria. Connect a live AI provider for a full requirements matrix."
        );
        List<AiCoverageMapping> mappings = new ArrayList<>();
        for (String criterion : criteria) {
            AiCoverageMapping mapping = new AiCoverageMapping();
            mapping.setRequirement(criterion);
            boolean uncovered = trace.getUncoveredCriteria() != null && trace.getUncoveredCriteria().contains(criterion);
            mapping.setCoverage(cases.isEmpty() ? "missing" : uncovered ? "partial" : "covered");
            mapping.setNotes(uncovered || cases.isEmpty()
                    ? "No strong title match among linked cases."
                    : "Possible title match among linked cases.");
            if (!uncovered && !cases.isEmpty()) {
                mapping.setTestCases(cases.stream().limit(3).map(LinkedTestCaseRef::getKey).toList());
            }
            mappings.add(mapping);
        }
        if (mappings.isEmpty() && issue != null && issue.getDescription() != null && !issue.getDescription().isBlank()) {
            AiCoverageMapping mapping = new AiCoverageMapping();
            mapping.setRequirement("[Implied] Primary flow described in the story");
            mapping.setCoverage(cases.isEmpty() ? "missing" : "partial");
            mapping.setNotes("No explicit acceptance criteria were parsed.");
            mappings.add(mapping);
        }
        analysis.setMappings(mappings);
        List<String> gaps = new ArrayList<>();
        if (cases.isEmpty()) {
            gaps.add("No linked test cases cover this work item yet");
        }
        if (trace.getUncoveredCriteria() != null) {
            gaps.addAll(trace.getUncoveredCriteria().stream().limit(3).toList());
        }
        analysis.setCriticalGaps(gaps);
        List<AiRecommendedTestCase> recommended = new ArrayList<>();
        for (String criterion : criteria) {
            if (trace.getUncoveredCriteria() != null && trace.getUncoveredCriteria().contains(criterion)) {
                AiRecommendedTestCase next = new AiRecommendedTestCase();
                next.setTitle("Verify: " + truncate(criterion, 90));
                next.setReason("Acceptance criterion has no strong match among linked cases.");
                next.setPriority("High");
                recommended.add(next);
            }
            if (recommended.size() >= 5) {
                break;
            }
        }
        if (recommended.isEmpty() && cases.isEmpty()) {
            AiRecommendedTestCase happy = new AiRecommendedTestCase();
            happy.setTitle("Verify the primary story flow succeeds for an authorized user");
            happy.setReason("No linked cases exist yet.");
            happy.setPriority("High");
            recommended.add(happy);
            AiRecommendedTestCase negative = new AiRecommendedTestCase();
            negative.setTitle("Verify invalid or unauthorized input is rejected");
            negative.setReason("Negative coverage is typically missing when no cases are linked.");
            negative.setPriority("Normal");
            recommended.add(negative);
        }
        analysis.setRecommendedTestCases(recommended);
        return analysis;
    }

    private String extractJsonObject(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return "";
        }
        return node.get(field).asText("");
    }

    private String normalizeCoverage(String value) {
        String coverage = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (coverage.startsWith("cover") || coverage.equals("y") || coverage.equals("yes")) {
            return "covered";
        }
        if (coverage.startsWith("part")) {
            return "partial";
        }
        return "missing";
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max) + "…";
    }

    private List<String> splitCriteria(String raw) {
        List<String> items = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return items;
        }
        for (String line : raw.split("\\r?\\n")) {
            String cleaned = line.replaceFirst("^[\\s\\-*•\\d.)]+", "").trim();
            if (cleaned.length() >= 8) {
                items.add(cleaned);
            }
        }
        if (items.isEmpty() && raw.trim().length() >= 8) {
            items.add(raw.trim());
        }
        return items;
    }

    private List<String> unmatchedCriteria(List<String> criteria, List<LinkedTestCaseRef> cases) {
        List<String> unmatched = new ArrayList<>();
        if (criteria.isEmpty() || cases.isEmpty()) {
            if (!criteria.isEmpty() && cases.isEmpty()) {
                unmatched.addAll(criteria);
            }
            return unmatched;
        }
        String haystack = cases.stream()
                .map(item -> ((item.getKey() == null ? "" : item.getKey()) + " "
                        + (item.getName() == null ? "" : item.getName())).toLowerCase(Locale.ROOT))
                .reduce("", (left, right) -> left + " " + right);
        for (String criterion : criteria) {
            boolean matched = false;
            for (String token : criterion.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
                if (token.length() >= 5 && haystack.contains(token)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                unmatched.add(criterion);
            }
        }
        return unmatched;
    }

    private String executionBucket(String status) {
        String value = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        if (value.contains("pass") || value.equals("ok") || value.equals("done")) {
            return "passed";
        }
        if (value.contains("fail")) {
            return "failed";
        }
        if (value.contains("block") || value.contains("wip") || value.contains("abort")) {
            return "blocked";
        }
        return "not_executed";
    }

    private List<TestExecutionRef> mapPluginExecutions(TestCycle cycle) {
        List<TestExecutionRef> executions = new ArrayList<>();
        if (cycle.getTestCases() != null) {
            for (var testCase : cycle.getTestCases()) {
                TestExecutionRef execution = new TestExecutionRef();
                execution.setTestCaseKey(testCase.getKey());
                execution.setTestCaseName(testCase.getTestName());
                execution.setStatus(testCase.getStatus());
                executions.add(execution);
            }
            return executions;
        }

        if (cycle.getId() != null && !cycle.getId().isBlank()) {
            for (var testCase : zephyrClient.getTestCasesInCycle(cycle.getId())) {
                TestExecutionRef execution = new TestExecutionRef();
                execution.setTestCaseKey(testCase.getKey());
                execution.setTestCaseName(testCase.getTestName());
                execution.setStatus(testCase.getStatus());
                executions.add(execution);
            }
        }
        return executions;
    }

    private void recordExecution(
            TestExecutionRef execution,
            Set<String> executionKeys,
            Map<String, String> latestStatusByCase
    ) {
        if (execution == null || execution.getTestCaseKey() == null || execution.getTestCaseKey().isBlank()) {
            return;
        }
        String key = execution.getTestCaseKey().toUpperCase(Locale.ROOT);
        executionKeys.add(key);
        if (execution.getStatus() != null && !execution.getStatus().isBlank()) {
            latestStatusByCase.put(key, execution.getStatus());
        }
    }

    private void mergeChildren(List<LinkedStory> stories, List<LinkedStory> children) {
        Set<String> seen = new HashSet<>();
        for (LinkedStory story : stories) {
            if (story.getKey() != null) {
                seen.add(story.getKey().toUpperCase(Locale.ROOT));
            }
        }
        for (LinkedStory child : children) {
            String key = child.getKey() == null ? "" : child.getKey().toUpperCase(Locale.ROOT);
            if (!key.isBlank() && seen.add(key)) {
                stories.add(child);
            }
        }
    }

    private LinkedStory selfStory(JiraStory issue) {
        LinkedStory self = new LinkedStory();
        self.setId(issue.getId());
        self.setKey(issue.getId());
        self.setSummary(issue.getSummary());
        self.setIssueType(issue.getIssueType());
        self.setStatus(issue.getStatus());
        self.setPriority(issue.getPriority());
        return self;
    }

    private boolean isContainerIssue(String issueType) {
        String type = issueType == null ? "" : issueType.trim().toLowerCase();
        return type.contains("change request") || type.equals("epic") || type.equals("release");
    }

    private String jiraBrowseUrl(String issueKey) {
        return jiraProperties.getBaseUrl().replaceAll("/$", "") + "/browse/" + issueKey;
    }
}
