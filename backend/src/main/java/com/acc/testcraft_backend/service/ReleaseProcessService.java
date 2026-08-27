package com.acc.testcraft_backend.service;

import com.acc.testcraft_backend.client.JiraClient;
import com.acc.testcraft_backend.client.ZephyrClient;
import com.acc.testcraft_backend.config.AppProperties;
import com.acc.testcraft_backend.config.JiraProperties;
import com.acc.testcraft_backend.model.CreateTestCyclesResponse;
import com.acc.testcraft_backend.model.CreatedCycleSummary;
import com.acc.testcraft_backend.model.LinkedStory;
import com.acc.testcraft_backend.model.ReleaseProcess;
import com.acc.testcraft_backend.model.StoryTestCycle;
import com.acc.testcraft_backend.model.TestCase;
import com.acc.testcraft_backend.model.TestCycle;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReleaseProcessService {

    private final JiraClient jiraClient;
    private final ZephyrClient zephyrClient;
    private final AppProperties appProperties;
    private final JiraProperties jiraProperties;

    public ReleaseProcessService(
            JiraClient jiraClient,
            ZephyrClient zephyrClient,
            AppProperties appProperties,
            JiraProperties jiraProperties
    ) {
        this.jiraClient = jiraClient;
        this.zephyrClient = zephyrClient;
        this.appProperties = appProperties;
        this.jiraProperties = jiraProperties;
    }

    public ReleaseProcess fetchChangeTicket(String crKey) {
        return jiraClient.fetchIssueWithLinkedStories(crKey);
    }

    public boolean validateCR(String crKey) {
        try {
            jiraClient.fetchIssueWithLinkedStories(crKey);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public List<StoryTestCycle> getTestCyclesForLinkedStories(String crKey) {
        ReleaseProcess release = fetchChangeTicket(crKey);
        List<StoryTestCycle> result = new ArrayList<>();

        for (LinkedStory story : release.getLinkedStories()) {
            List<TestCycle> cycles = getTestCyclesForStory(story.getKey());
            if (!cycles.isEmpty()) {
                StoryTestCycle stc = new StoryTestCycle();
                stc.setStoryKey(story.getKey());
                stc.setStorySummary(story.getSummary());
                stc.setTestCycles(cycles);
                stc.setTotalTestCycles(cycles.size());
                stc.setTotalTestCases(
                        cycles.stream()
                                .mapToInt(c -> c.getTestCases() != null ? c.getTestCases().size() : 0)
                                .sum()
                );
                result.add(stc);
            }
        }

        return result;
    }

    public List<TestCycle> getTestCyclesForStory(String storyKey) {
        return zephyrClient.searchTestCyclesByIssueKey(storyKey);
    }

    public List<StoryTestCycle> getTestCasesInCycles(String crKey) {
        return getTestCyclesForLinkedStories(crKey);
    }

    public CreateTestCyclesResponse createTestCyclesAndTraceability(
            String crKey,
            boolean createFolders
    ) {
        return createTestCyclesAndTraceability(crKey, createFolders, List.of("Functional"), null);
    }

    public CreateTestCyclesResponse createTestCyclesAndTraceability(
            String crKey,
            boolean createFolders,
            String cycleType,
            String ownerOverride
    ) {
        return createTestCyclesAndTraceability(
                crKey,
                createFolders,
                cycleType == null || cycleType.isBlank() ? List.of("Functional") : List.of(cycleType.trim()),
                ownerOverride
        );
    }

    public CreateTestCyclesResponse createTestCyclesAndTraceability(
            String crKey,
            boolean createFolders,
            List<String> cycleTypes,
            String ownerOverride
    ) {
        return createTestCyclesAndTraceability(
                crKey, createFolders, cycleTypes, ownerOverride, List.of());
    }

    public CreateTestCyclesResponse createTestCyclesAndTraceability(
            String crKey,
            boolean createFolders,
            List<String> cycleTypes,
            String ownerOverride,
            List<String> publishedTestCaseKeys
    ) {
        CreateTestCyclesResponse response = new CreateTestCyclesResponse();
        response.setCrKey(crKey);
        publishedTestCaseKeys = publishedTestCaseKeys == null
                ? List.of()
                : publishedTestCaseKeys;

        List<String> types = new ArrayList<>();
        if (cycleTypes != null) {
            for (String type : cycleTypes) {
                if (type != null && !type.isBlank() && !types.contains(type.trim())) {
                    types.add(type.trim());
                }
            }
        }
        if (types.isEmpty()) {
            types.add("Functional");
        }

        try {
            ReleaseProcess release = fetchChangeTicket(crKey);
            String owner = (ownerOverride != null && !ownerOverride.isBlank())
                    ? ownerOverride
                    : appProperties.resolveOwner(jiraProperties);
            int projectId = Integer.parseInt(release.getProjectId());

            List<LinkedStory> stories = release.getLinkedStories();
            if (stories == null || stories.isEmpty()) {
                LinkedStory self = new LinkedStory();
                self.setId(release.getCrId());
                self.setKey(release.getCrKey());
                self.setSummary(release.getCrSummary());
                self.setStatus(release.getStatus());
                stories = List.of(self);
            }

            List<String> createdIds = new ArrayList<>();
            List<CreatedCycleSummary> createdCycles = new ArrayList<>();
            int newlyCreated = 0;
            int reused = 0;
            int processed = 0;

            for (String type : types) {
                for (LinkedStory story : stories) {
                    String storySummary = story.getSummary() != null ? story.getSummary() : story.getKey();
                    String folderPath = java.time.Year.now().getValue() + " / "
                            + java.time.LocalDate.now().format(
                                    java.time.format.DateTimeFormatter.ofPattern("MMM", java.util.Locale.ENGLISH))
                            + " / " + story.getKey() + " - " + storySummary + " - " + type;
                    int folderId = release.getFolderId();

                    if (createFolders || folderId <= 0) {
                        folderId = zephyrClient.ensureFolderHierarchy(
                                projectId,
                                story.getKey(),
                                story.getSummary() != null ? story.getSummary() : story.getKey(),
                                type,
                                owner
                        );
                    }

                    String cycleName = story.getKey() + " - " + type
                            + " - "
                            + (story.getSummary() != null ? story.getSummary() : "");

                    String existingId = folderId > 0
                            ? zephyrClient.getExistingTestCycleIdBySearch(
                                    cycleName,
                                    projectId,
                                    folderId
                            )
                            : null;

                    String cycleId = existingId;

                    if (cycleId == null || cycleId.isBlank()) {
                        cycleId = zephyrClient.createTestCycle(
                                cycleName,
                                type + " cycle auto-created by TestCraft for " + story.getKey(),
                                folderId,
                                projectId,
                                release.getProductVersionId(),
                                owner,
                                null
                        );
                        newlyCreated++;
                    } else {
                        reused++;
                    }

                    if (cycleId != null && !cycleId.isBlank()) {
                        createdIds.add(cycleId);
                        createdCycles.add(new CreatedCycleSummary(
                                cycleId,
                                cycleName,
                                story.getKey(),
                                jiraProperties.getBaseUrl().replaceAll("/$", "")
                                        + "/browse/" + story.getKey(),
                                jiraProperties.getBaseUrl().replaceAll("/$", "")
                                        + "/jira/software/projects/"
                                        + story.getKey().substring(0, story.getKey().indexOf('-')).toUpperCase()
                                        + "/apps/3feb7ced-1450-4676-aded-099c99bf534b/"
                                        + "2baaeb69-15ac-4955-8eb6-e346aa1567aa#/v2/testCycle/"
                                        + cycleId,
                                folderPath,
                                existingId == null || existingId.isBlank() ? "CREATED" : "REUSED"
                        ));

                        if (zephyrClient.isScaleCloudMode()) {
                            String projectKey = story.getKey() != null && story.getKey().contains("-")
                                    ? story.getKey().substring(0, story.getKey().indexOf('-')).toUpperCase()
                                    : crKey.substring(0, crKey.indexOf('-')).toUpperCase();
                            List<String> linkedTestCases = stories.size() == 1
                                    ? publishedTestCaseKeys.stream()
                                            .filter(key -> key != null && !key.isBlank())
                                            .distinct()
                                            .toList()
                                    : zephyrClient.getScaleCloudTestCaseKeysLinkedToIssue(story.getKey());
                            if (linkedTestCases.isEmpty()) {
                                linkedTestCases = zephyrClient.getScaleCloudTestCaseKeysLinkedToIssue(story.getKey());
                            }
                            for (String testCaseKey : linkedTestCases) {
                                zephyrClient.addTestCaseToScaleCloudCycle(
                                        cycleId, projectKey, testCaseKey);
                            }
                            zephyrClient.linkScaleCloudCycleToIssue(cycleId, story.getKey());
                            zephyrClient.linkScaleCloudCycleToIssue(cycleId, crKey);
                        } else {
                            List<Integer> testCaseIds =
                                    zephyrClient.getTestCaseIdsLinkedToIssue(story.getKey());
                            if (!testCaseIds.isEmpty()) {
                                zephyrClient.addTestCasesToCycle(
                                        cycleId,
                                        testCaseIds,
                                        release.getProductVersionId(),
                                        owner
                                );
                            }

                            String storyNumericId = story.getId();
                            if (storyNumericId == null || storyNumericId.isBlank()) {
                                storyNumericId = jiraClient.getIssueId(story.getKey());
                            }
                            zephyrClient.ensureTestRunHasTraceLinks(
                                    cycleId,
                                    release.getCrId(),
                                    storyNumericId
                            );
                        }
                    }

                    processed++;
                }
            }

            response.setSuccess(true);
            response.setStoriesProcessed(processed);
            response.setCyclesCreated(createdIds.size());
            response.setCreatedCycleIds(createdIds);
            response.setCreatedCycles(createdCycles);
            response.setMessage(
                    "Processed " + processed + " story/type combinations: "
                            + newlyCreated + " created, " + reused + " skipped/reused ("
                            + String.join(", ", types) + ")"
            );
            return response;
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError(e.getMessage());
            return response;
        }
    }

    public int testFolderHierarchy(String crKey, int projectId, String crSummary) {
        return zephyrClient.ensureFolderHierarchy(
                projectId,
                crKey,
                crSummary,
                appProperties.resolveOwner(jiraProperties)
        );
    }
}
