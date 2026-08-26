package com.acc.testcraft_backend.controller;

import com.acc.testcraft_backend.model.FolderListResponse;
import com.acc.testcraft_backend.model.LinkRequest;
import com.acc.testcraft_backend.model.PriorityListResponse;
import com.acc.testcraft_backend.model.ProjectListResponse;
import com.acc.testcraft_backend.model.PublishRequest;
import com.acc.testcraft_backend.model.PublishTestCaseResponse;
import com.acc.testcraft_backend.model.StatusListResponse;
import com.acc.testcraft_backend.service.ZephyrService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/zephyr")
public class ZephyrController {

    private final ZephyrService zephyrService;

    public ZephyrController(ZephyrService zephyrService) {
        this.zephyrService = zephyrService;
    }

    @GetMapping("/projects")
    public ResponseEntity<?> getProjects() {
        try {
            Map<String, String> projects = zephyrService.getProjects();
            ProjectListResponse response =
                    new ProjectListResponse(projects);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ProjectListResponse response =
                    new ProjectListResponse(null);

            response.setSuccess(false);
            response.setError(e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/folders/{projectId}")
    public ResponseEntity<?> getProjectFolders(
            @PathVariable String projectId
    ) {
        try {
            Map<String, String> folders =
                    zephyrService.getProjectFolders(projectId);

            FolderListResponse response =
                    new FolderListResponse(projectId, folders);
            String warning = zephyrService.getLastFolderWarning();
            if (warning != null && !warning.isBlank()) {
                response.setWarning(warning);
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            FolderListResponse response =
                    new FolderListResponse(projectId, null);

            response.setSuccess(false);
            response.setError(e.getMessage());

            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            FolderListResponse response =
                    new FolderListResponse(projectId, null);

            response.setSuccess(false);
            response.setError(e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/statuses/{projectId}")
    public ResponseEntity<?> getProjectStatuses(
            @PathVariable String projectId
    ) {
        try {
            Map<String, String> statuses =
                    zephyrService.getProjectStatuses(projectId);

            StatusListResponse response =
                    new StatusListResponse(projectId, statuses);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            StatusListResponse response =
                    new StatusListResponse(projectId, null);

            response.setSuccess(false);
            response.setError(e.getMessage());

            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            StatusListResponse response =
                    new StatusListResponse(projectId, null);

            response.setSuccess(false);
            response.setError(e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/priorities/{projectId}")
    public ResponseEntity<?> getProjectPriorities(
            @PathVariable String projectId
    ) {
        try {
            Map<String, String> priorities =
                    zephyrService.getProjectPriorities(projectId);

            PriorityListResponse response =
                    new PriorityListResponse(projectId, priorities);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            PriorityListResponse response =
                    new PriorityListResponse(projectId, null);

            response.setSuccess(false);
            response.setError(e.getMessage());

            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            PriorityListResponse response =
                    new PriorityListResponse(projectId, null);

            response.setSuccess(false);
            response.setError(e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/validate-user")
    public ResponseEntity<?> validateUser() {
        try {
            Map<String, Object> user = zephyrService.getCurrentUser();
            Map<String, Object> response = new HashMap<>(user);
            response.put("username", user.get("emailAddress"));
            response.put("email", user.get("emailAddress"));
            response.put("accountId", user.get("accountId"));
            response.put("isValid", true);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("isValid", false);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/publish")
    public ResponseEntity<?> publishTestCase(
            @RequestBody PublishRequest request
    ) {
        try {
            if (request.getTestCase() == null) {
                return ResponseEntity.badRequest()
                        .body("Test case cannot be null");
            }

            if (request.getProjectId() == null
                    || request.getProjectId().isBlank()) {
                return ResponseEntity.badRequest()
                        .body("Project ID is required");
            }

            if (request.getFolderId() == null
                    || request.getFolderId().isBlank()) {
                return ResponseEntity.badRequest()
                        .body("Folder ID is required");
            }

            String testCaseKey = zephyrService.publishTestCase(
                    request.getTestCase(),
                    request.getProjectId(),
                    request.getFolderId(),
                    request.getOwner(),
                    request.getStatusId()
            );

            String resolvedOwner =
                    zephyrService.getLastResolvedOwner();

            PublishTestCaseResponse response =
                    new PublishTestCaseResponse(
                            testCaseKey,
                            request.getTestCase().getTestName()
                    );

            response.setProjectId(request.getProjectId());
            response.setFolderId(request.getFolderId());
            response.setOwner(resolvedOwner);
            response.setStatus("Published");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            PublishTestCaseResponse response =
                    new PublishTestCaseResponse();

            response.setSuccess(false);
            response.setError(e.getMessage());

            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            PublishTestCaseResponse response =
                    new PublishTestCaseResponse();

            response.setSuccess(false);
            response.setError(e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/link")
    public ResponseEntity<?> linkTestCase(
            @RequestBody LinkRequest request
    ) {
        try {
            if (request.getTestCaseKey() == null
                    || request.getTestCaseKey().isBlank()) {
                return ResponseEntity.badRequest()
                        .body("Test case key is required");
            }

            if (request.getIssueKey() == null
                    || request.getIssueKey().isBlank()) {
                return ResponseEntity.badRequest()
                        .body("Issue key is required");
            }

            zephyrService.linkTestCaseToIssue(
                    request.getTestCaseKey(),
                    request.getIssueKey()
            );

            Map<String, Object> response = Map.of(
                    "testCaseKey", request.getTestCaseKey(),
                    "issueKey", request.getIssueKey(),
                    "linked", true,
                    "success", true
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                    "error", e.getMessage(),
                    "success", false
            );

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/test-cycles/{projectId}")
    public ResponseEntity<?> listProjectTestCycles(@PathVariable String projectId) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "projectId", projectId,
                    "cycles", zephyrService.listProjectTestCycles(projectId)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/link-bulk")
    public ResponseEntity<?> linkBulk(@RequestBody Map<String, Object> body) {
        List<String> testCaseKeys = stringList(body.get("testCaseKeys"));
        List<String> issueKeys = stringList(body.get("issueKeys"));
        if (testCaseKeys.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "testCaseKeys are required"
            ));
        }
        if (issueKeys.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "issueKeys are required"
            ));
        }

        List<Map<String, Object>> linked = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (String testCaseKey : testCaseKeys) {
            for (String issueKey : issueKeys) {
                try {
                    zephyrService.linkTestCaseToIssue(testCaseKey, issueKey);
                    linked.add(Map.of("testCaseKey", testCaseKey, "issueKey", issueKey));
                } catch (Exception e) {
                    errors.add(testCaseKey + " → " + issueKey + ": " + e.getMessage());
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", errors.isEmpty());
        response.put("linked", linked);
        response.put("errors", errors);
        return ResponseEntity.ok(response);
    }

    private static List<String> stringList(Object value) {
        List<String> items = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && !item.toString().isBlank()) {
                    items.add(item.toString().trim());
                }
            }
        } else if (value instanceof String text && !text.isBlank()) {
            for (String part : text.split("[,\\s]+")) {
                if (!part.isBlank()) {
                    items.add(part.trim());
                }
            }
        }
        return items;
    }
}