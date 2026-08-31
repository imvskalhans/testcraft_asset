package com.acc.testcraft_backend.service;

import com.acc.testcraft_backend.client.ZephyrClient;
import com.acc.testcraft_backend.config.JiraProperties;
import com.acc.testcraft_backend.model.CycleTraceability;
import com.acc.testcraft_backend.model.LinkedStory;
import com.acc.testcraft_backend.model.LinkedTestCaseRef;
import com.acc.testcraft_backend.model.ReleaseProcess;
import com.acc.testcraft_backend.model.StoryTraceability;
import com.acc.testcraft_backend.model.TestCycle;
import com.acc.testcraft_backend.model.TestExecutionRef;
import com.acc.testcraft_backend.model.TraceabilityDashboardResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TraceabilityService {

    private final ReleaseProcessService releaseProcessService;
    private final ZephyrClient zephyrClient;
    private final JiraProperties jiraProperties;

    public TraceabilityService(
            ReleaseProcessService releaseProcessService,
            ZephyrClient zephyrClient,
            JiraProperties jiraProperties
    ) {
        this.releaseProcessService = releaseProcessService;
        this.zephyrClient = zephyrClient;
        this.jiraProperties = jiraProperties;
    }

    public TraceabilityDashboardResponse buildDashboard(String issueKey) {
        if (issueKey == null || issueKey.isBlank()) {
            throw new IllegalArgumentException("Issue key is required");
        }

        String normalizedKey = issueKey.trim().toUpperCase();
        ReleaseProcess release = releaseProcessService.fetchChangeTicket(normalizedKey);
        List<LinkedStory> linkedStories = release.getLinkedStories();
        if (linkedStories == null || linkedStories.isEmpty()) {
            linkedStories = List.of(selfStory(release));
        }

        TraceabilityDashboardResponse response = new TraceabilityDashboardResponse();
        response.setSuccess(true);
        response.setIssueKey(release.getCrKey());
        response.setIssueSummary(release.getCrSummary());
        response.setIssueStatus(release.getStatus());
        response.setJiraUrl(jiraBrowseUrl(release.getCrKey()));

        List<StoryTraceability> stories = new ArrayList<>();
        int storiesWithCoverage = 0;
        int storiesWithCycles = 0;
        int storiesFullyTraced = 0;
        int totalLinkedTestCases = 0;
        int totalCycles = 0;
        int totalExecutions = 0;
        List<String> releaseGaps = new ArrayList<>();

        for (LinkedStory story : linkedStories) {
            StoryTraceability storyTrace = buildStoryTraceability(story);
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

            for (String gap : storyTrace.getGaps()) {
                releaseGaps.add(story.getKey() + ": " + gap);
            }
        }

        if (storiesWithCoverage < linkedStories.size()) {
            releaseGaps.add(
                    (linkedStories.size() - storiesWithCoverage)
                            + " stor"
                            + (linkedStories.size() - storiesWithCoverage == 1 ? "y has" : "ies have")
                            + " no linked test cases"
            );
        }
        if (storiesWithCycles < linkedStories.size()) {
            releaseGaps.add(
                    (linkedStories.size() - storiesWithCycles)
                            + " stor"
                            + (linkedStories.size() - storiesWithCycles == 1 ? "y has" : "ies have")
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
        response.setGaps(releaseGaps);
        response.setStories(stories);
        response.setMessage(
                "Traceability loaded for "
                        + linkedStories.size()
                        + " stor"
                        + (linkedStories.size() == 1 ? "y" : "ies")
                        + " — "
                        + storiesFullyTraced
                        + " fully traced"
        );
        return response;
    }

    private StoryTraceability buildStoryTraceability(LinkedStory story) {
        StoryTraceability trace = new StoryTraceability();
        trace.setStoryKey(story.getKey());
        trace.setStorySummary(story.getSummary());
        trace.setStoryStatus(story.getStatus());
        trace.setJiraUrl(jiraBrowseUrl(story.getKey()));

        List<LinkedTestCaseRef> linkedTestCases =
                zephyrClient.getLinkedTestCasesForIssue(story.getKey());
        List<TestCycle> cycles =
                releaseProcessService.getTestCyclesForStory(story.getKey());

        Set<String> executionKeys = new HashSet<>();
        List<CycleTraceability> cycleTraces = new ArrayList<>();
        int executionCount = 0;

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
                executions = zephyrClient.getScaleCloudExecutionsForCycle(cycleKey);
            } else {
                executions = mapPluginExecutions(cycle);
            }

            cycleTrace.setExecutions(executions);
            cycleTrace.setExecutionCount(executions.size());
            executionCount += executions.size();
            for (TestExecutionRef execution : executions) {
                if (execution.getTestCaseKey() != null && !execution.getTestCaseKey().isBlank()) {
                    executionKeys.add(execution.getTestCaseKey().toUpperCase());
                }
            }
            cycleTraces.add(cycleTrace);
        }

        for (LinkedTestCaseRef testCase : linkedTestCases) {
            String key = testCase.getKey() != null ? testCase.getKey().toUpperCase() : "";
            testCase.setInCycle(executionKeys.contains(key));
        }

        List<String> gaps = new ArrayList<>();
        boolean hasCoverage = !linkedTestCases.isEmpty();
        boolean hasCycles = !cycleTraces.isEmpty();
        boolean hasExecutions = executionCount > 0;

        if (!hasCoverage) {
            gaps.add("No test cases linked to this story");
        }
        if (!hasCycles) {
            gaps.add("No test cycles found for this story");
        } else if (!hasExecutions) {
            gaps.add("Cycles exist but no test executions were found");
        }
        if (hasCoverage && hasExecutions) {
            long uncovered = linkedTestCases.stream().filter(tc -> !tc.isInCycle()).count();
            if (uncovered > 0) {
                gaps.add(uncovered + " linked test case(s) are not attached to a cycle");
            }
        }

        trace.setLinkedTestCases(linkedTestCases);
        trace.setTestCycles(cycleTraces);
        trace.setLinkedTestCaseCount(linkedTestCases.size());
        trace.setCycleCount(cycleTraces.size());
        trace.setExecutionCount(executionCount);
        trace.setHasCoverage(hasCoverage);
        trace.setHasCycles(hasCycles);
        trace.setFullyTraced(hasCoverage && hasCycles && hasExecutions && gaps.isEmpty());
        trace.setGaps(gaps);
        return trace;
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

    private LinkedStory selfStory(ReleaseProcess release) {
        LinkedStory self = new LinkedStory();
        self.setId(release.getCrId());
        self.setKey(release.getCrKey());
        self.setSummary(release.getCrSummary());
        self.setStatus(release.getStatus());
        return self;
    }

    private String jiraBrowseUrl(String issueKey) {
        return jiraProperties.getBaseUrl().replaceAll("/$", "") + "/browse/" + issueKey;
    }
}
