package com.acc.testcraft_backend.controller;

import com.acc.testcraft_backend.model.CreateTestCyclesRequest;
import com.acc.testcraft_backend.model.CreateTestCyclesResponse;
import com.acc.testcraft_backend.model.FetchCRResponse;
import com.acc.testcraft_backend.model.GetTestCasesResponse;
import com.acc.testcraft_backend.model.GetTestCyclesResponse;
import com.acc.testcraft_backend.model.ReleaseAIRequest;
import com.acc.testcraft_backend.model.ReleaseAIResponse;
import com.acc.testcraft_backend.model.ReleaseProcess;
import com.acc.testcraft_backend.model.ReleaseProcessRequest;
import com.acc.testcraft_backend.model.StoryTestCycle;
import com.acc.testcraft_backend.model.TestCycle;
import com.acc.testcraft_backend.service.ReleaseAIService;
import com.acc.testcraft_backend.service.ReleaseProcessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/release-process")
public class ReleaseProcessController {

    private final ReleaseProcessService releaseProcessService;
    private final ReleaseAIService releaseAIService;

    public ReleaseProcessController(
            ReleaseProcessService releaseProcessService,
            ReleaseAIService releaseAIService
    ) {
        this.releaseProcessService = releaseProcessService;
        this.releaseAIService = releaseAIService;
    }

    @PostMapping("/fetch-cr")
    public ResponseEntity<?> fetchChangeTicket(
            @RequestBody ReleaseProcessRequest request
    ) {
        try {
            if (request.getCrKey() == null || request.getCrKey().isBlank()) {
                FetchCRResponse response = new FetchCRResponse();
                response.setSuccess(false);
                response.setError("CR Key is required");

                return ResponseEntity.badRequest().body(response);
            }

            ReleaseProcess releaseProcess =
                    releaseProcessService.fetchChangeTicket(
                            request.getCrKey()
                    );

            FetchCRResponse response = new FetchCRResponse();
            response.setCrId(releaseProcess.getCrId());
            response.setCrKey(releaseProcess.getCrKey());
            response.setProjectId(releaseProcess.getProjectId());
            response.setProductVersionId(
                    releaseProcess.getProductVersionId()
            );
            response.setCrSummary(releaseProcess.getCrSummary());
            response.setCrDescription(releaseProcess.getCrDescription());
            response.setStatus(releaseProcess.getStatus());
            response.setLinkedStories(releaseProcess.getLinkedStories());
            response.setMessage(
                    "CR fetched successfully with "
                            + releaseProcess.getLinkedStories().size()
                            + " linked stories"
            );
            response.setSuccess(true);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            FetchCRResponse response = new FetchCRResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());

            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            FetchCRResponse response = new FetchCRResponse();
            response.setSuccess(false);
            response.setError("Failed to fetch CR: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/validate/{crKey}")
    public ResponseEntity<?> validateCR(@PathVariable String crKey) {
        try {
            boolean isValid = releaseProcessService.validateCR(crKey);

            return ResponseEntity.ok(
                    Map.of(
                            "crKey", crKey,
                            "exists", isValid,
                            "success", true
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of(
                            "crKey", crKey,
                            "exists", false,
                            "error", e.getMessage(),
                            "success", false
                    )
            );
        }
    }

    @PostMapping("/get-test-cycles")
    public ResponseEntity<?> getTestCyclesForLinkedStories(
            @RequestBody ReleaseProcessRequest request
    ) {
        try {
            if (request.getCrKey() == null || request.getCrKey().isBlank()) {
                GetTestCyclesResponse response =
                        new GetTestCyclesResponse();

                response.setSuccess(false);
                response.setError("CR Key is required");

                return ResponseEntity.badRequest().body(response);
            }

            ReleaseProcess releaseProcess =
                    releaseProcessService.fetchChangeTicket(
                            request.getCrKey()
                    );

            List<StoryTestCycle> storyTestCycles =
                    releaseProcessService.getTestCyclesForLinkedStories(
                            request.getCrKey()
                    );

            GetTestCyclesResponse response =
                    new GetTestCyclesResponse();

            response.setCrKey(request.getCrKey());
            response.setTotalLinkedStories(
                    releaseProcess.getLinkedStories().size()
            );
            response.setStoryTestCycles(storyTestCycles);
            response.setStoriesWithCycles(storyTestCycles.size());
            response.setMessage(
                    "Found "
                            + storyTestCycles.size()
                            + " stories with "
                            + storyTestCycles.stream()
                            .mapToInt(
                                    StoryTestCycle::getTotalTestCycles
                            )
                            .sum()
                            + " test cycles"
            );
            response.setSuccess(true);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            GetTestCyclesResponse response =
                    new GetTestCyclesResponse();

            response.setSuccess(false);
            response.setError(e.getMessage());

            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            GetTestCyclesResponse response =
                    new GetTestCyclesResponse();

            response.setSuccess(false);
            response.setError(
                    "Failed to get test cycles: " + e.getMessage()
            );

            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/get-cycles-for-story")
    public ResponseEntity<?> getTestCyclesForStory(
            @RequestBody ReleaseProcessRequest request
    ) {
        try {
            if (request.getCrKey() == null || request.getCrKey().isBlank()) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "error", "Story Key is required"
                        )
                );
            }

            List<TestCycle> cycles =
                    releaseProcessService.getTestCyclesForStory(
                            request.getCrKey()
                    );

            return ResponseEntity.ok(
                    Map.of(
                            "storyKey", request.getCrKey(),
                            "totalCycles", cycles.size(),
                            "cycles", cycles,
                            "success", true
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "error", e.getMessage()
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of(
                            "success", false,
                            "error",
                            "Failed to get test cycles: "
                                    + e.getMessage()
                    )
            );
        }
    }

    @PostMapping("/get-test-cases")
    public ResponseEntity<?> getTestCasesInCycles(
            @RequestBody ReleaseProcessRequest request
    ) {
        try {
            if (request.getCrKey() == null || request.getCrKey().isBlank()) {
                GetTestCasesResponse response =
                        new GetTestCasesResponse();

                response.setSuccess(false);
                response.setError("CR Key is required");

                return ResponseEntity.badRequest().body(response);
            }

            ReleaseProcess releaseProcess =
                    releaseProcessService.fetchChangeTicket(
                            request.getCrKey()
                    );

            List<StoryTestCycle> storyTestCycles =
                    releaseProcessService.getTestCasesInCycles(
                            request.getCrKey()
                    );

            GetTestCasesResponse response =
                    new GetTestCasesResponse();

            response.setCrKey(request.getCrKey());
            response.setTotalLinkedStories(
                    releaseProcess.getLinkedStories().size()
            );
            response.setStoriesWithCycles(storyTestCycles.size());
            response.setStoryTestCycles(storyTestCycles);

            int totalCycles = storyTestCycles.stream()
                    .mapToInt(StoryTestCycle::getTotalTestCycles)
                    .sum();

            int totalCases = storyTestCycles.stream()
                    .mapToInt(StoryTestCycle::getTotalTestCases)
                    .sum();

            response.setTotalTestCycles(totalCycles);
            response.setTotalTestCases(totalCases);
            response.setMessage(
                    "Found "
                            + storyTestCycles.size()
                            + " stories with "
                            + totalCycles
                            + " test cycles and "
                            + totalCases
                            + " test cases"
            );
            response.setSuccess(true);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            GetTestCasesResponse response =
                    new GetTestCasesResponse();

            response.setSuccess(false);
            response.setError(e.getMessage());

            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            GetTestCasesResponse response =
                    new GetTestCasesResponse();

            response.setSuccess(false);
            response.setError(
                    "Failed to get test cases: " + e.getMessage()
            );

            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/create-test-cycles")
    public ResponseEntity<?> createTestCycles(
            @RequestBody CreateTestCyclesRequest request
    ) {
        try {
            if (request.getCrKey() == null || request.getCrKey().isBlank()) {
                CreateTestCyclesResponse response =
                        new CreateTestCyclesResponse();

                response.setSuccess(false);
                response.setError("CR Key is required");

                return ResponseEntity.badRequest().body(response);
            }

            CreateTestCyclesResponse response =
                    releaseProcessService.createTestCyclesAndTraceability(
                            request.getCrKey(),
                            request.isCreateFolders(),
                            request.resolveCycleTypes(),
                            request.getOwner(),
                            request.getTestCaseKeys()
                    );

            return response.isSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(500).body(response);
        } catch (Exception e) {
            CreateTestCyclesResponse response =
                    new CreateTestCyclesResponse();

            response.setSuccess(false);
            response.setError(
                    "Failed to create test cycles: " + e.getMessage()
            );

            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/test-folder-hierarchy")
    public ResponseEntity<?> testFolderHierarchy(
            @RequestBody Map<String, String> request
    ) {
        try {
            String crKey = request.get("crKey");
            String projectId = request.get("projectId");
            String crSummary = request.get("crSummary");

            if (crKey == null || crKey.isBlank()) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "error", "crKey is required"
                        )
                );
            }

            if (projectId == null || projectId.isBlank()) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "success", false,
                                "error", "projectId is required"
                        )
                );
            }

            int folderId = releaseProcessService.testFolderHierarchy(
                    crKey,
                    Integer.parseInt(projectId),
                    crSummary
            );

            return ResponseEntity.ok(
                    Map.of(
                            "crKey", crKey,
                            "projectId", projectId,
                            "folderId", folderId,
                            "success", folderId > 0
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of(
                            "success", false,
                            "error", e.getMessage()
                    )
            );
        }
    }

    @PostMapping("/ai-analysis")
    public ResponseEntity<ReleaseAIResponse> generateReleaseAIAnalysis(
            @RequestBody ReleaseAIRequest request
    ) {
        try {
            ReleaseAIResponse response =
                    releaseAIService.generateReleaseAnalysis(request);

            return response.isSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR
            ).body(response);
        } catch (IllegalArgumentException e) {
            ReleaseAIResponse response = new ReleaseAIResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());

            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            ReleaseAIResponse response = new ReleaseAIResponse();
            response.setSuccess(false);
            response.setError(
                    "Failed to process release AI request: "
                            + e.getMessage()
            );

            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR
            ).body(response);
        }
    }

    @GetMapping("/ai-actions")
    public ResponseEntity<?> getReleaseAIActions() {
        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "actions",
                        releaseAIService.getAvailableActions()
                )
        );
    }
}
