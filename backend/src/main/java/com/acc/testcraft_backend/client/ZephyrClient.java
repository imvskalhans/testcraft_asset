package com.acc.testcraft_backend.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.acc.testcraft_backend.config.IntegrationConfig.IntegrationAuthSupport;
import com.acc.testcraft_backend.config.IntegrationUrls;
import com.acc.testcraft_backend.config.JiraProperties;
import com.acc.testcraft_backend.config.ZephyrProperties;
import com.acc.testcraft_backend.model.TestCase;
import com.acc.testcraft_backend.model.TestCycle;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ZephyrClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JiraProperties jiraProperties;
    private final ZephyrProperties zephyrProperties;
    private final IntegrationAuthSupport authSupport;
    private final IntegrationUrls urls;
    private final JiraClient jiraClient;
    private final ZephyrContextClient zephyrContextClient;
    private String lastFolderWarning = "";

    public ZephyrClient(
            RestTemplate restTemplate,
            JiraProperties jiraProperties,
            ZephyrProperties zephyrProperties,
            IntegrationAuthSupport authSupport,
            IntegrationUrls urls,
            JiraClient jiraClient,
            ZephyrContextClient zephyrContextClient
    ) {
        this.restTemplate = restTemplate;
        this.jiraProperties = jiraProperties;
        this.zephyrProperties = zephyrProperties;
        this.authSupport = authSupport;
        this.urls = urls;
        this.jiraClient = jiraClient;
        this.zephyrContextClient = zephyrContextClient;
    }

    private HttpHeaders authHeaders() {
        return authSupport.zephyrHeaders();
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getProjects() {
        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/project";

        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    List.class
            );

            Map<String, String> projectMap = new LinkedHashMap<>();
            List<Map<String, Object>> projects = response.getBody();

            if (projects != null) {
                for (Object projectObj : projects) {
                    if (projectObj instanceof Map) {
                        Map<String, Object> project =
                                (Map<String, Object>) projectObj;

                        String name = (String) project.get("name");
                        Object id = project.get("id");

                        if (name != null && id != null) {
                            projectMap.put(name, String.valueOf(id));
                        }
                    }
                }
            }

            return projectMap;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to fetch Zephyr projects: " + e.getMessage(),
                    e
            );
        }
    }

    public String getLastFolderWarning() {
        return lastFolderWarning;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getProjectFolders(String projectId) {
        Map<String, String> folderMap = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();

        String projectKey = "";
        try {
            projectKey = jiraClient.resolveProjectKey(projectId);
        } catch (Exception e) {
            projectKey = zephyrProperties.getDefaultProjectKey();
            warnings.add("Could not resolve Jira project key: " + e.getMessage());
        }

        tryAtmFolderTree(projectId, folderMap, warnings);
        if (projectKey != null && !projectKey.isBlank() && !projectKey.equals(projectId)) {
            tryAtmFolderTree(projectKey, folderMap, warnings);
        }

        tryTm4jFolderTree(projectId, folderMap, warnings);

        boolean scaleCloudWorked = false;
        if (zephyrProperties.hasScaleCloudToken() && projectKey != null && !projectKey.isBlank()) {
            try {
                Map<String, String> scaleFolders = getScaleCloudFolders(projectKey);
                for (Map.Entry<String, String> entry : scaleFolders.entrySet()) {
                    folderMap.putIfAbsent(entry.getKey(), entry.getValue());
                }
                scaleCloudWorked = !scaleFolders.isEmpty();
            } catch (Exception e) {
                warnings.add("Zephyr Scale Cloud folder list failed: " + e.getMessage());
            }
        } else if (folderMap.isEmpty()) {
            warnings.add(
                    "Live Zephyr folder APIs need either a Scale Cloud JWT "
                            + "(zephyr.scale-cloud-api-token) or TM4J context access. "
                            + "Until then, configure zephyr.known-folders with name:numericId pairs."
            );
        }

        for (Map.Entry<String, String> known : zephyrProperties.parsedKnownFolders().entrySet()) {
            boolean alreadyListed = folderMap.containsValue(known.getValue())
                    || folderMap.keySet().stream().anyMatch(
                            name -> name.equalsIgnoreCase(known.getKey())
                    );
            if (!alreadyListed) {
                folderMap.put(known.getKey(), known.getValue());
            }
        }

        lastFolderWarning = scaleCloudWorked
                ? ""
                : String.join(" ", warnings);
        if (!scaleCloudWorked && !folderMap.isEmpty() && !zephyrProperties.parsedKnownFolders().isEmpty()) {
            lastFolderWarning =
                    "Folders loaded from zephyr.known-folders. "
                            + "Check zephyr.scale-cloud-api-token and EU base URL if live sync fails.";
        }
        if (folderMap.isEmpty()) {
            throw new RuntimeException(
                    lastFolderWarning.isBlank()
                            ? "No Zephyr folders found for this project"
                            : lastFolderWarning
            );
        }
        return folderMap;
    }

    @SuppressWarnings("unchecked")
    private void tryAtmFolderTree(
            String projectIdOrKey,
            Map<String, String> folderMap,
            List<String> warnings
    ) {
        String url = jiraProperties.getBaseUrl().replaceAll("/$", "")
                + zephyrProperties.getJiraPluginApiPath()
                + "/project/"
                + projectIdOrKey
                + "/foldertree/testcase";

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authSupport.jiraHeaders()),
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                List<Map<String, Object>> children =
                        (List<Map<String, Object>>) responseBody.get("children");
                if (children != null) {
                    addFoldersRecursively(children, folderMap);
                }
            }
        } catch (Exception e) {
            warnings.add("ATM foldertree (" + projectIdOrKey + ") unavailable.");
        }
    }

    @SuppressWarnings("unchecked")
    private void tryTm4jFolderTree(
            String projectId,
            Map<String, String> folderMap,
            List<String> warnings
    ) {
        if (projectId == null || projectId.isBlank() || !projectId.matches("\\d+")) {
            return;
        }

        try {
            String jwt = zephyrContextClient.getContextJwt();
            String base = trimTrailingSlash(zephyrProperties.getTm4jBackendBaseUrl());
            String url = base
                    + "/rest/tests/2.0/project/"
                    + projectId
                    + "/foldertree/testcase";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "JWT " + jwt);
            headers.set("jira-project-id", projectId);
            headers.set("Accept", "application/json");

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body != null) {
                List<Map<String, Object>> children =
                        (List<Map<String, Object>>) body.get("children");
                if (children != null) {
                    addTm4jFoldersRecursively(children, folderMap, "");
                }
            }
        } catch (Exception e) {
            warnings.add("TM4J foldertree unavailable: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void addTm4jFoldersRecursively(
            List<Map<String, Object>> folders,
            Map<String, String> folderMap,
            String parentPath
    ) {
        for (Map<String, Object> folder : folders) {
            String name = folder.get("name") != null ? String.valueOf(folder.get("name")) : "";
            Object id = folder.get("id");
            if (name.isBlank() || id == null) {
                continue;
            }

            String path = parentPath.isBlank() ? name : parentPath + " / " + name;
            folderMap.put(path, String.valueOf(id));

            List<Map<String, Object>> children =
                    (List<Map<String, Object>>) folder.get("children");
            if (children != null && !children.isEmpty()) {
                addTm4jFoldersRecursively(children, folderMap, path);
            }
        }
    }

    private String resolvedScaleCloudBaseUrl;

    private String scaleCloudBaseUrl() {
        if (resolvedScaleCloudBaseUrl != null && !resolvedScaleCloudBaseUrl.isBlank()) {
            return resolvedScaleCloudBaseUrl;
        }

        String primary = trimTrailingSlash(zephyrProperties.getScaleCloudBaseUrl());
        String eu = primary.replace(
                "https://api.zephyrscale.smartbear.com",
                "https://eu.api.zephyrscale.smartbear.com"
        );

        List<String> candidates = new ArrayList<>();
        candidates.add(primary);
        if (!eu.equals(primary)) {
            candidates.add(eu);
        }

        String projectKey = zephyrProperties.getDefaultProjectKey();
        for (String base : candidates) {
            if (base == null || base.isBlank()) {
                continue;
            }
            try {
                String probe = base
                        + "/folders?maxResults=1&projectKey="
                        + projectKey
                        + "&folderType=TEST_CASE";
                ResponseEntity<Map> response = restTemplate.exchange(
                        probe,
                        HttpMethod.GET,
                        new HttpEntity<>(authSupport.scaleCloudHeaders()),
                        Map.class
                );
                if (response.getStatusCode().is2xxSuccessful()) {
                    resolvedScaleCloudBaseUrl = base;
                    return base;
                }
            } catch (Exception ignored) {
                // Try next region
            }
        }

        resolvedScaleCloudBaseUrl = primary;
        return primary;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getScaleCloudFolders(String projectKey) {
        Map<String, Object> byId = new LinkedHashMap<>();
        int startAt = 0;
        int maxResults = 100;
        String baseUrl = scaleCloudBaseUrl();

        while (true) {
            String url = baseUrl
                    + "/folders?maxResults="
                    + maxResults
                    + "&startAt="
                    + startAt
                    + "&projectKey="
                    + projectKey
                    + "&folderType=TEST_CASE";

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authSupport.scaleCloudHeaders()),
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                break;
            }

            Object values = body.get("values");
            int batch = 0;
            if (values instanceof List<?> list) {
                batch = list.size();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> folder) {
                        Object id = folder.get("id");
                        if (id != null) {
                            byId.put(String.valueOf(id), (Map<String, Object>) folder);
                        }
                    }
                }
            }

            Object isLast = body.get("isLast");
            if (Boolean.TRUE.equals(isLast) || batch == 0) {
                break;
            }
            startAt += maxResults;
            if (startAt > 5000) {
                break;
            }
        }

        Map<String, String> folderMap = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : byId.entrySet()) {
            Map<String, Object> folder = (Map<String, Object>) entry.getValue();
            String name = folder.get("name") != null
                    ? String.valueOf(folder.get("name"))
                    : folderPath(folder, byId);
            if (name != null && !name.isBlank()) {
                folderMap.put(name, entry.getKey());
            }
        }
        return folderMap;
    }

    @SuppressWarnings("unchecked")
    private String folderPath(Map<String, Object> folder, Map<String, Object> byId) {
        String name = folder.get("name") != null ? String.valueOf(folder.get("name")) : "";
        Object parentId = folder.get("parentId");
        if (parentId == null) {
            return name;
        }
        Object parent = byId.get(String.valueOf(parentId));
        if (parent instanceof Map<?, ?> parentMap) {
            String parentPath = folderPath((Map<String, Object>) parentMap, byId);
            if (parentPath != null && !parentPath.isBlank()) {
                return parentPath + " / " + name;
            }
        }
        return name;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @SuppressWarnings("unchecked")
    private void addFoldersRecursively(
            List<Map<String, Object>> folders,
            Map<String, String> folderMap
    ) {
        for (Map<String, Object> folder : folders) {
            String name = (String) folder.get("name");
            Object id = folder.get("id");

            if (name != null && id != null) {
                folderMap.put(name + " [" + id + "]", String.valueOf(id));
            }

            List<Map<String, Object>> children =
                    (List<Map<String, Object>>) folder.get("children");

            if (children != null && !children.isEmpty()) {
                addFoldersRecursively(children, folderMap);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getStatusNames(String projectId) {
        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/project/"
                + projectId
                + "/testcasestatus";

        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    List.class
            );

            List<Map<String, Object>> statuses = response.getBody();
            List<String> statusNames = new ArrayList<>();

            if (statuses != null) {
                for (Object statusObj : statuses) {
                    if (statusObj instanceof Map<?, ?> status) {
                        String name = (String) status.get("name");

                        if (name != null) {
                            statusNames.add(name);
                        }
                    }
                }
            }

            return statusNames;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to fetch statuses: " + e.getMessage(),
                    e
            );
        }
    }

    @SuppressWarnings("unchecked")
    public String getStatusIdByName(String projectId, String statusName) {
        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/project/"
                + projectId
                + "/testcasestatus";

        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    List.class
            );

            List<Map<String, Object>> statuses = response.getBody();

            if (statuses != null) {
                for (Map<String, Object> status : statuses) {
                    String name = (String) status.get("name");
                    Object id = status.get("id");

                    if (name != null
                            && name.equalsIgnoreCase(statusName)
                            && id != null) {
                        return String.valueOf(id);
                    }
                }

                if (!statuses.isEmpty()) {
                    return String.valueOf(statuses.get(0).get("id"));
                }
            }

            throw new RuntimeException(
                    "No statuses found for project: " + projectId
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to get status ID: " + e.getMessage(),
                    e
            );
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getPriorityNames(String projectId) {
        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/project/"
                + projectId
                + "/testcasepriority";

        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    List.class
            );

            List<Map<String, Object>> priorities = response.getBody();
            Map<String, String> priorityMap = new LinkedHashMap<>();

            if (priorities != null) {
                for (Map<String, Object> priority : priorities) {
                    String name = (String) priority.get("name");
                    Object id = priority.get("id");

                    if (name != null && id != null) {
                        priorityMap.put(name, String.valueOf(id));
                    }
                }
            }

            return priorityMap;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to fetch priorities: " + e.getMessage(),
                    e
            );
        }
    }

    @SuppressWarnings("unchecked")
    public String getPriorityIdByName(
            String projectId,
            String priorityName
    ) {
        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/project/"
                + projectId
                + "/testcasepriority";

        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    List.class
            );

            List<Map<String, Object>> priorities = response.getBody();

            if (priorities != null) {
                for (Map<String, Object> priority : priorities) {
                    String name = (String) priority.get("name");
                    Object id = priority.get("id");

                    if (name != null
                            && name.equalsIgnoreCase(priorityName)
                            && id != null) {
                        return String.valueOf(id);
                    }
                }

                for (Map<String, Object> priority : priorities) {
                    String name = (String) priority.get("name");
                    Object id = priority.get("id");

                    if (name != null
                            && name.equalsIgnoreCase("Normal")
                            && id != null) {
                        return String.valueOf(id);
                    }
                }

                if (!priorities.isEmpty()) {
                    return String.valueOf(priorities.get(0).get("id"));
                }
            }

            throw new RuntimeException(
                    "No priorities found for project: " + projectId
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to get priority ID: " + e.getMessage(),
                    e
            );
        }
    }

    public String postTestCase(
            TestCase testCase,
            String projectId,
            String folderId,
            String ownerKey,
            String statusId,
            String priorityId
    ) {
        if (zephyrProperties.useScaleCloudApi() && zephyrProperties.hasScaleCloudToken()) {
            return postScaleCloudTestCase(
                    testCase,
                    projectId,
                    folderId,
                    ownerKey,
                    statusId,
                    priorityId
            );
        }
        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/testcase";

        Map<String, Object> payload = buildTestCasePayload(
                testCase,
                projectId,
                folderId,
                ownerKey,
                statusId,
                priorityId
        );

        try {
            String requestBody = objectMapper.writeValueAsString(payload);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, authHeaders()),
                    Map.class
            );

            if (response.getBody() != null) {
                return (String) response.getBody().get("key");
            }

            throw new RuntimeException("Invalid response from Zephyr API");
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to post test case to Zephyr: " + e.getMessage(),
                    e
            );
        }
    }

    @SuppressWarnings("unchecked")
    private String postScaleCloudTestCase(
            TestCase testCase,
            String projectId,
            String folderId,
            String ownerKey,
            String statusId,
            String priorityId
    ) {
        String projectKey = jiraClient.resolveProjectKey(projectId);
        if (projectKey == null || projectKey.isBlank()) {
            projectKey = zephyrProperties.getDefaultProjectKey();
        }

        String resolvedFolderId = resolveFolderId(projectKey, folderId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectKey", projectKey);
        payload.put("name", testCase.getTestName());
        payload.put("objective", testCase.getObjective() != null ? testCase.getObjective() : "");
        payload.put(
                "precondition",
                testCase.getPreCondition() != null ? testCase.getPreCondition() : ""
        );
        if (resolvedFolderId != null && resolvedFolderId.matches("\\d+")) {
            payload.put("folderId", Long.parseLong(resolvedFolderId));
        }
        payload.put(
                "statusName",
                testCase.getStatus() != null && !testCase.getStatus().isBlank()
                        ? testCase.getStatus()
                        : zephyrProperties.getDefaultTestCaseStatus()
        );
        payload.put(
                "priorityName",
                mapScalePriority(
                        testCase.getPriority() != null ? testCase.getPriority() : zephyrProperties.getDefaultPriority()
                )
        );
        if (ownerKey != null && !ownerKey.isBlank()) {
            payload.put("ownerId", ownerKey);
        }

        String url = scaleCloudBaseUrl() + "/testcases";
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(payload), authSupport.scaleCloudHeaders()),
                    Map.class
            );
            if (response.getBody() != null && response.getBody().get("key") != null) {
                return String.valueOf(response.getBody().get("key"));
            }
            throw new RuntimeException("Scale Cloud did not return a test case key");
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish test case to Zephyr Scale Cloud: " + e.getMessage(), e);
        }
    }

    private String resolveFolderId(String projectKey, String folderIdOrName) {
        if (folderIdOrName == null || folderIdOrName.isBlank()) {
            return folderIdOrName;
        }
        if (folderIdOrName.matches("\\d+")) {
            return folderIdOrName;
        }
        try {
            Map<String, String> folders = getScaleCloudFolders(projectKey);
            for (Map.Entry<String, String> entry : folders.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(folderIdOrName)
                        || entry.getKey().toLowerCase().endsWith(folderIdOrName.toLowerCase())) {
                    return entry.getValue();
                }
            }
        } catch (Exception ignored) {
            // Fall through to the original value
        }
        return folderIdOrName;
    }

    private static String mapScalePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return "Normal";
        }
        return switch (priority.trim().toLowerCase()) {
            case "high", "critical", "highest" -> "High";
            case "low", "lowest" -> "Low";
            case "medium", "normal" -> "Normal";
            default -> priority;
        };
    }

    private Map<String, Object> buildTestCasePayload(
            TestCase testCase,
            String projectId,
            String folderId,
            String ownerKey,
            String statusId,
            String priorityId
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("projectId", Integer.parseInt(projectId));
        payload.put("name", testCase.getTestName());
        payload.put("statusId", statusId);
        payload.put("priorityId", priorityId);
        payload.put("folderId", Integer.parseInt(folderId));
        payload.put("owner", ownerKey);
        payload.put("objective", testCase.getObjective());
        payload.put(
                "precondition",
                testCase.getPreCondition() != null
                        ? testCase.getPreCondition()
                        : ""
        );

        List<Map<String, Object>> stepsList = new ArrayList<>();

        if (testCase.getSteps() != null) {
            for (int i = 0; i < testCase.getSteps().size(); i++) {
                Map<String, Object> step = new LinkedHashMap<>();
                step.put("description", testCase.getSteps().get(i));
                step.put("testData", "");
                step.put(
                        "expectedResult",
                        testCase.getExpectedResult() != null
                                ? testCase.getExpectedResult()
                                : ""
                );
                step.put("index", i);
                stepsList.add(step);
            }
        }

        Map<String, Object> stepByStepScript = new LinkedHashMap<>();
        stepByStepScript.put("steps", stepsList);

        Map<String, Object> testScript = new LinkedHashMap<>();
        testScript.put("stepByStepScript", stepByStepScript);

        payload.put("testScript", testScript);

        return payload;
    }

    public String getTestCaseIdFromTestCaseKey(String testKey) {
        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/testcase/"
                + testKey;

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url + "?fields=id",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null) {
                Object id = response.getBody().get("id");

                if (id != null) {
                    return String.valueOf(id);
                }
            }

            return null;
        } catch (Exception e) {
            System.err.println(
                    "Error getting test case ID: " + e.getMessage()
            );
            return null;
        }
    }

    public String getIssueIdFromJiraKey(String jiraId) {
        String url = jiraProperties.getBaseUrl().replaceAll("/$", "") + jiraProperties.getApiPath()
                + "/issue/"
                + jiraId;

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url + "?fields=id",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null) {
                Object id = response.getBody().get("id");

                if (id != null) {
                    return String.valueOf(id);
                }
            }

            return null;
        } catch (Exception e) {
            System.err.println(
                    "Error getting issue ID: " + e.getMessage()
            );
            return null;
        }
    }

    public void linkTestIdToJiraStory(String testCaseId, String issueId) {
        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/tracelink/bulk/create";

        try {
            List<Map<String, Object>> linkPayload = new ArrayList<>();
            Map<String, Object> linkItem = new LinkedHashMap<>();

            linkItem.put("testCaseId", Long.parseLong(testCaseId));
            linkItem.put("issueId", issueId);
            linkItem.put("typeId", 1);

            linkPayload.add(linkItem);

            String requestBody =
                    objectMapper.writeValueAsString(linkPayload);

            HttpHeaders headers = authHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );
        } catch (NumberFormatException e) {
            throw new RuntimeException(
                    "Invalid test case ID format: " + e.getMessage(),
                    e
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to link test case to issue: " + e.getMessage(),
                    e
            );
        }
    }

    public void linkTestToIssue(String testCaseKey, String issueKey) {
        if (zephyrProperties.useScaleCloudApi() && zephyrProperties.hasScaleCloudToken()) {
            linkScaleCloudTestToIssue(testCaseKey, issueKey);
            return;
        }
        try {
            String testCaseId =
                    getTestCaseIdFromTestCaseKey(testCaseKey);

            if (testCaseId == null) {
                throw new RuntimeException(
                        "Could not resolve test case ID from key: "
                                + testCaseKey
                );
            }

            String issueId = getIssueIdFromJiraKey(issueKey);

            if (issueId == null) {
                throw new RuntimeException(
                        "Could not resolve issue ID from key: " + issueKey
                );
            }

            linkTestIdToJiraStory(testCaseId, issueId);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to link test case to issue: " + e.getMessage(),
                    e
            );
        }
    }

    public static String mapAiPriorityToZephyrPriority(String aiPriority) {
        if (aiPriority == null) {
            return "Normal";
        }

        return switch (aiPriority.toLowerCase()) {
            case "critical", "high", "urgent", "immediate", "highest" ->
                    "High";
            case "medium", "moderate", "normal" -> "Normal";
            case "low", "minor", "trivial" -> "Low";
            default -> "Normal";
        };
    }

    @SuppressWarnings("unchecked")
    public List<TestCase> getTestCasesInCycle(String cycleId) {
        if (cycleId == null || cycleId.isBlank()) {
            return new ArrayList<>();
        }

        String url = urls.zephyrTests("/testrunitem?testRunId=" + cycleId);

        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    List.class
            );

            List<TestCase> testCases = new ArrayList<>();

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null) {
                List<?> items = response.getBody();

                for (Object itemObj : items) {
                    if (itemObj instanceof Map) {
                        Map<String, Object> itemData =
                                (Map<String, Object>) itemObj;
                        testCases.add(mapToTestCase(itemData));
                    }
                }
            }

            return testCases;
        } catch (Exception e) {
            System.err.println(
                    "Error fetching test cases for cycle "
                            + cycleId
                            + ": "
                            + e.getMessage()
            );
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private TestCase mapToTestCase(Map<String, Object> caseData) {
        TestCase testCase = new TestCase();

        testCase.setTestName((String) caseData.get("name"));
        testCase.setObjective((String) caseData.get("objective"));
        testCase.setPreCondition((String) caseData.get("precondition"));
        testCase.setExpectedResult(
                (String) caseData.get("expectedResult")
        );
        testCase.setPriority((String) caseData.get("priority"));

        Object stepsObj = caseData.get("steps");

        if (stepsObj instanceof List<?> stepList) {
            List<String> steps = new ArrayList<>();

            for (Object stepObj : stepList) {
                if (stepObj instanceof String) {
                    steps.add((String) stepObj);
                } else if (stepObj instanceof Map) {
                    steps.add(
                            (String) ((Map<String, Object>) stepObj)
                                    .get("description")
                    );
                }
            }

            testCase.setSteps(steps);
        }

        return testCase;
    }

    public List<TestCycle> searchTestCyclesByIssueKey(String issueKey) {
        if (issueKey == null || issueKey.isBlank()) {
            return new ArrayList<>();
        }

        try {
            return jiraClient.getTestRunsLinkedToIssue(issueKey);
        } catch (Exception e) {
            System.err.println(
                    "Failed to search test cycles: " + e.getMessage()
            );
            return new ArrayList<>();
        }
    }

    public String createTestCycle(
            String name,
            String description,
            int folderId,
            int projectId,
            String projectVersionId,
            String owner,
            String statusId
    ) {
        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/testrun";

        try {
            Long now = System.currentTimeMillis();
            String isoDate = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            ).format(new Date(now));

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put(
                    "description",
                    description != null ? description : ""
            );
            payload.put("folderId", folderId);
            payload.put("name", name);
            payload.put("owner", owner);
            payload.put("plannedEndDate", isoDate);
            payload.put("plannedStartDate", isoDate);
            payload.put("projectId", projectId);

            int versionId = 1;

            try {
                versionId = Integer.parseInt(projectVersionId);
            } catch (NumberFormatException e) {
                System.out.println(
                        "DEBUG: Invalid project version ID. Using default."
                );
            }

            payload.put("projectVersionId", versionId);
            if (zephyrProperties.getDefaultTestRunStatusId() > 0) {
                payload.put("statusId", zephyrProperties.getDefaultTestRunStatusId());
            }

            String jsonBody = objectMapper.writeValueAsString(payload);

            System.out.println(
                    "DEBUG: Creating test cycle with payload: " + jsonBody
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(jsonBody, authHeaders()),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null) {
                Object idObj = response.getBody().get("id");

                if (idObj != null) {
                    System.out.println(
                            "Test cycle created: "
                                    + name
                                    + " (ID: "
                                    + idObj
                                    + ")"
                    );
                    return String.valueOf(idObj);
                }
            }

            System.err.println(
                    "ERROR: Failed to create test cycle. Status: "
                            + response.getStatusCode()
            );
            System.err.println("Response body: " + response.getBody());
            System.err.println("Request payload was: " + jsonBody);

            return null;
        } catch (Exception e) {
            System.err.println(
                    "Error creating test cycle: " + e.getMessage()
            );
            e.printStackTrace();
            return null;
        }
    }

    public boolean addTestCasesToCycle(
            String testCycleId,
            List<Integer> testCaseIds,
            String jiraVersionId,
            String owner
    ) {
        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/testrunitem/bulk/save";

        try {
            Long now = System.currentTimeMillis();
            String isoDate = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            ).format(new Date(now));

            List<Map<String, Object>> addedItems = new ArrayList<>();

            for (int i = 0; i < testCaseIds.size(); i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("index", i);

                Map<String, Object> testResult = new LinkedHashMap<>();
                testResult.put("testCaseId", testCaseIds.get(i));
                testResult.put(
                        "jiraVersionId",
                        Integer.parseInt(jiraVersionId)
                );

                System.out.println(
                        "DEBUG: Adding test case with assignedTo: " + owner
                );

                testResult.put("assignedTo", owner);
                testResult.put("plannedStartDate", isoDate);
                testResult.put("plannedEndDate", isoDate);

                item.put("lastTestResult", testResult);
                addedItems.add(item);
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("testRunId", Integer.parseInt(testCycleId));
            payload.put("addedTestRunItems", addedItems);
            payload.put("updatedTestRunItems", new ArrayList<>());
            payload.put("updatedTestRunItemsIndexes", new ArrayList<>());
            payload.put("deletedTestRunItems", new ArrayList<>());
            payload.put("autoReorder", false);

            String jsonBody = objectMapper.writeValueAsString(payload);

            System.out.println(
                    "DEBUG: Adding test cases payload: " + jsonBody
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(jsonBody, authHeaders()),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Test cases added to cycle");
                return true;
            }

            System.err.println(
                    "ERROR: Failed to add test cases. Status: "
                            + response.getStatusCode()
            );
            System.err.println("Response: " + response.getBody());
            return false;
        } catch (Exception e) {
            System.err.println(
                    "Error adding test cases to cycle: " + e.getMessage()
            );
            e.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public int getTestCaseIdFromKey(String testCaseKey) {
        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/testcase/bulk/get?fields=id";

        try {
            List<String> keys = new ArrayList<>();
            keys.add(testCaseKey);

            String jsonBody = objectMapper.writeValueAsString(keys);

            ResponseEntity<List> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(jsonBody, authHeaders()),
                    List.class
            );

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null) {
                List<?> results = response.getBody();

                if (!results.isEmpty()) {
                    Map<String, Object> testCase =
                            (Map<String, Object>) results.get(0);

                    Object idObj = testCase.get("id");

                    if (idObj != null) {
                        if (idObj instanceof Number number) {
                            return number.intValue();
                        }

                        return Integer.parseInt(idObj.toString());
                    }
                }
            }

            return 0;
        } catch (Exception e) {
            System.err.println(
                    "Error getting test case ID from key: " + e.getMessage()
            );
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Integer> getTestCaseIdsLinkedToIssue(
            String issueNumericId
    ) {
        List<Integer> testCaseIds = new ArrayList<>();

        if (issueNumericId == null || issueNumericId.isBlank()) {
            return testCaseIds;
        }

        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/issue/"
                + issueNumericId
                + "/tracelinks";

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url + "?maxResults=15&startAt=0",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    Object.class
            );

            Object body = response.getBody();

            if (body instanceof List<?> items) {
                for (Object item : items) {
                    if (item instanceof Map) {
                        Map<String, Object> map =
                                (Map<String, Object>) item;

                        Object testCaseObj = map.get("testCase");

                        if (testCaseObj instanceof Map<?, ?> testCase) {
                            addNumericId(
                                    testCase.get("id"),
                                    testCaseIds
                            );
                        }

                        Object testCaseCoverage = map.get("testCaseId");

                        if (testCaseCoverage != null) {
                            addNumericId(
                                    testCaseCoverage,
                                    testCaseIds
                            );
                        }
                    }
                }
            } else if (body instanceof Map) {
                Map<String, Object> mapBody =
                        (Map<String, Object>) body;

                Object testCaseData = mapBody.get("testCase");

                if (testCaseData instanceof Map<?, ?> tcMap) {
                    Object coverage = tcMap.get("coverage");

                    if (coverage instanceof Map<?, ?> coverageMap) {
                        Object traceLinks = coverageMap.get("traceLinks");

                        if (traceLinks instanceof List<?> links) {
                            for (Object traceLink : links) {
                                if (traceLink instanceof Map<?, ?> link) {
                                    Object nestedTestCase =
                                            link.get("testCase");

                                    if (nestedTestCase instanceof Map<?, ?> tc) {
                                        addNumericId(
                                                tc.get("id"),
                                                testCaseIds
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(
                    "Error fetching testCase tracelinks for issue "
                            + issueNumericId
                            + ": "
                            + e.getMessage()
            );
        }

        return new ArrayList<>(new LinkedHashSet<>(testCaseIds));
    }

    @SuppressWarnings("unchecked")
    public Set<Integer> getLastTestResultCaseIds(String testRunId) {
        Set<Integer> existing = new LinkedHashSet<>();

        if (testRunId == null || testRunId.isBlank()) {
            return existing;
        }

        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/testrun/"
                + testRunId
                + "/testrunitems/lasttestresults";

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    Object.class
            );

            Object body = response.getBody();

            if (body instanceof List<?> items) {
                for (Object item : items) {
                    if (item instanceof Map) {
                        Map<String, Object> itemMap =
                                (Map<String, Object>) item;

                        Object last = itemMap.get("lastTestResult");

                        if (last instanceof Map<?, ?> result) {
                            addNumericId(result.get("testCaseId"), existing);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(
                    "Error fetching last test results for run "
                            + testRunId
                            + ": "
                            + e.getMessage()
            );
        }

        return existing;
    }

    public int addMissingTestCasesToTestRun(
            String testRunId,
            List<Integer> candidateTestCaseIds,
            String jiraVersionId,
            String owner
    ) {
        if (testRunId == null
                || testRunId.isBlank()
                || candidateTestCaseIds == null
                || candidateTestCaseIds.isEmpty()) {
            return 0;
        }

        try {
            Set<Integer> existing = getLastTestResultCaseIds(testRunId);
            List<Integer> missing = new ArrayList<>();

            for (Integer id : candidateTestCaseIds) {
                if (id != null && !existing.contains(id)) {
                    missing.add(id);
                }
            }

            if (missing.isEmpty()) {
                System.out.println(
                        "No missing test cases to add for testRun "
                                + testRunId
                );
                return 0;
            }

            Long now = System.currentTimeMillis();
            String isoDate = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            ).format(new Date(now));

            List<Map<String, Object>> addedItems = new ArrayList<>();

            for (int i = 0; i < missing.size(); i++) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("index", i);

                Map<String, Object> lastTestResult =
                        new LinkedHashMap<>();

                lastTestResult.put("testCaseId", missing.get(i));

                try {
                    lastTestResult.put(
                            "jiraVersionId",
                            Integer.parseInt(jiraVersionId)
                    );
                } catch (Exception e) {
                    lastTestResult.put("jiraVersionId", 1);
                }

                lastTestResult.put("assignedTo", owner);
                lastTestResult.put("plannedStartDate", isoDate);
                lastTestResult.put("plannedEndDate", isoDate);

                item.put("lastTestResult", lastTestResult);
                addedItems.add(item);
            }

            Map<String, Object> payload = new LinkedHashMap<>();

            try {
                payload.put("testRunId", Integer.parseInt(testRunId));
            } catch (Exception e) {
                payload.put("testRunId", testRunId);
            }

            payload.put("addedTestRunItems", addedItems);
            payload.put("updatedTestRunItems", new ArrayList<>());
            payload.put("updatedTestRunItemsIndexes", new ArrayList<>());
            payload.put("deletedTestRunItems", new ArrayList<>());
            payload.put("autoReorder", false);

            String jsonBody = objectMapper.writeValueAsString(payload);

            System.out.println(
                    "DEBUG: Adding missing test cases to run payload: "
                            + jsonBody
            );

            String url = zephyrProperties.resolveApiBase(jiraProperties)
                    + "/testrunitem/bulk/save";

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(jsonBody, authHeaders()),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println(
                        "Added "
                                + missing.size()
                                + " missing test cases to testRun "
                                + testRunId
                );
                return missing.size();
            }

            System.err.println(
                    "Failed to add test cases, status: "
                            + response.getStatusCode()
            );
            return 0;
        } catch (Exception e) {
            System.err.println(
                    "Error adding missing test cases to run "
                            + testRunId
                            + ": "
                            + e.getMessage()
            );
            e.printStackTrace();
            return 0;
        }
    }

    private void addNumericId(Object value, List<Integer> target) {
        if (value == null) {
            return;
        }

        try {
            target.add(value instanceof Number number
                    ? number.intValue()
                    : Integer.parseInt(value.toString()));
        } catch (Exception ignored) {
            // Preserve the original behavior: ignore malformed IDs.
        }
    }

    private void addNumericId(Object value, Set<Integer> target) {
        if (value == null) {
            return;
        }

        try {
            target.add(value instanceof Number number
                    ? number.intValue()
                    : Integer.parseInt(value.toString()));
        } catch (Exception ignored) {
            // Preserve the original behavior: ignore malformed IDs.
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTestRunIssueTraceLinks(
            String testRunId
    ) {
        List<Map<String, Object>> links = new ArrayList<>();

        if (testRunId == null || testRunId.isBlank()) {
            return links;
        }

        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/testrun/"
                + testRunId
                + "/tracelinks/issue?fields=id,issueId,typeId";

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    Object.class
            );

            Object body = response.getBody();

            if (body instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?>) {
                        links.add((Map<String, Object>) item);
                    }
                }
            } else if (body instanceof Map<?, ?>) {
                links.add((Map<String, Object>) body);
            }
        } catch (Exception e) {
            System.err.println(
                    "Error fetching tracelinks for test run "
                            + testRunId
                            + ": "
                            + e.getMessage()
            );
        }

        return links;
    }

    public int ensureTestRunHasTraceLinks(
            String testRunId,
            String crNumericId,
            String storyNumericId
    ) {
        if (testRunId == null || testRunId.isBlank()) {
            return 0;
        }

        int created = 0;

        try {
            List<Map<String, Object>> existing =
                    getTestRunIssueTraceLinks(testRunId);

            boolean hasCr = false;
            boolean hasStory = false;

            for (Map<String, Object> link : existing) {
                Object issueIdObj = link.get("issueId");

                if (issueIdObj != null) {
                    String issueId = issueIdObj.toString();

                    if (!hasCr
                            && crNumericId != null
                            && crNumericId.equals(issueId)) {
                        hasCr = true;
                    }

                    if (!hasStory
                            && storyNumericId != null
                            && storyNumericId.equals(issueId)) {
                        hasStory = true;
                    }
                }
            }

            List<Map<String, Object>> createList = new ArrayList<>();

            if (!hasCr && crNumericId != null && !crNumericId.isBlank()) {
                createList.add(buildTestRunTraceLink(
                        testRunId,
                        crNumericId
                ));
            }

            if (!hasStory
                    && storyNumericId != null
                    && !storyNumericId.isBlank()) {
                createList.add(buildTestRunTraceLink(
                        testRunId,
                        storyNumericId
                ));
            }

            if (!createList.isEmpty()) {
                String url = urls.zephyr("/tracelink/bulk/create");

                String jsonBody = objectMapper.writeValueAsString(createList);

                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        new HttpEntity<>(jsonBody, authHeaders()),
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    created = createList.size();

                    System.out.println(
                            "Created "
                                    + created
                                    + " tracelink(s) for test run "
                                    + testRunId
                    );
                } else {
                    System.err.println(
                            "Failed to create tracelinks. Status: "
                                    + response.getStatusCode()
                    );
                }
            }
        } catch (Exception e) {
            System.err.println(
                    "Error ensuring tracelinks for test run "
                            + testRunId
                            + ": "
                            + e.getMessage()
            );
        }

        return created;
    }

    private Map<String, Object> buildTestRunTraceLink(
            String testRunId,
            String issueId
    ) {
        Map<String, Object> item = new LinkedHashMap<>();

        try {
            item.put("testRunId", Integer.parseInt(testRunId));
        } catch (Exception e) {
            item.put("testRunId", testRunId);
        }

        item.put("issueId", issueId);
        item.put("typeId", 2);

        return item;
    }

    @SuppressWarnings("unchecked")
    public String getExistingTestCycleIdBySearch(
            String cycleName,
            int projectId,
            int folderId
    ) {
        try {
            String tql = "testRun.projectId IN (" + projectId + ") "
                    + "AND testRun.folderTreeId IN (" + folderId + ") "
                    + "ORDER BY testRun.name ASC";

            String fields = "id,key,name,folderId,status.name";

            String url = zephyrProperties.resolveApiBase(jiraProperties)
                    + "/testrun/search?fields="
                    + fields
                    + "&query="
                    + tql;

            System.out.println("DEBUG URL = " + url);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null) {
                return null;
            }

            Map<String, Object> body = response.getBody();
            Object resultsObj = body.get("results");

            if (!(resultsObj instanceof List<?> results)) {
                return null;
            }

            System.out.println(
                    "DEBUG: Found "
                            + results.size()
                            + " cycles inside folder "
                            + folderId
            );

            for (Object result : results) {
                if (!(result instanceof Map<?, ?> run)) {
                    continue;
                }

                String existingName = String.valueOf(run.get("name"));

                if (cycleName.equals(existingName)) {
                    System.out.println(
                            "DEBUG: Existing cycle found -> "
                                    + existingName
                                    + " ID="
                                    + run.get("id")
                    );

                    return String.valueOf(run.get("id"));
                }
            }

            System.out.println(
                    "DEBUG: No existing cycle found for " + cycleName
            );

            return null;
        } catch (Exception e) {
            System.err.println(
                    "ERROR searching existing cycle: " + e.getMessage()
            );
            e.printStackTrace();
            return null;
        }
    }

    public int createFolder(
            String name,
            int parentId,
            int projectId,
            int index,
            String owner
    ) {
        validateFolderCreateInput(name, parentId, projectId, owner);

        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/folder/testrun";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("index", Math.max(index, 1));
        payload.put("name", name.trim());
        payload.put("parentId", parentId);
        payload.put("projectId", projectId);
        payload.put("owner", owner.trim());

        try {
            String jsonBody = objectMapper.writeValueAsString(payload);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(jsonBody, authHeaders()),
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                        "Folder create API returned HTTP status "
                                + response.getStatusCode()
                );
            }

            validateFolderCreateResponse(name, response.getBody());

            int actualFolderId = resolveCreatedFolderId(
                    projectId,
                    parentId,
                    name.trim()
            );

            if (actualFolderId <= 0) {
                throw new RuntimeException(
                        "Zephyr accepted creation of folder "
                                + name
                                + ", but the folder could not be resolved."
                );
            }

            return actualFolderId;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create and resolve folder "
                            + name
                            + ": "
                            + e.getMessage(),
                    e
            );
        }
    }

    private void validateFolderCreateInput(
            String name,
            int parentId,
            int projectId,
            String owner
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Folder name cannot be null or blank"
            );
        }

        if (parentId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid parent folder ID: " + parentId
            );
        }

        if (projectId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid project ID: " + projectId
            );
        }

        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException(
                    "Folder owner cannot be null or blank"
            );
        }
    }

    @SuppressWarnings("unchecked")
    private void validateFolderCreateResponse(
            String folderName,
            Map<String, Object> responseBody
    ) {
        if (responseBody == null) {
            return;
        }

        Object validObject = responseBody.get("valid");

        if (validObject instanceof Boolean valid && !valid) {
            throw new RuntimeException(
                    "Zephyr rejected folder creation for " + folderName
            );
        }

        Object validationResultObject =
                responseBody.get("validationResult");

        if (validationResultObject instanceof Map<?, ?> map) {
            Map<String, Object> validationResult =
                    (Map<String, Object>) map;

            Object validationValidObject =
                    validationResult.get("valid");

            if (validationValidObject instanceof Boolean valid && !valid) {
                throw new RuntimeException(
                        "Zephyr validation rejected folder creation for "
                                + folderName
                );
            }
        }

        Object statusCodeObject = responseBody.get("statusCode");

        if (statusCodeObject instanceof Number number
                && number.intValue() != 0) {
            throw new RuntimeException(
                    "Zephyr folder creation returned statusCode="
                            + number.intValue()
                            + " for folder "
                            + folderName
            );
        }
    }

    public int ensureFolderHierarchy(
            int projectId,
            String crKey,
            String crSummary,
            String owner
    ) {
        return ensureFolderHierarchy(projectId, crKey, crSummary, "Functional", owner);
    }

    @SuppressWarnings("unchecked")
    public int ensureFolderHierarchy(
            int projectId,
            String crKey,
            String crSummary,
            String cycleType,
            String owner
    ) {
        try {
            validateFolderHierarchyInput(
                    projectId,
                    crKey,
                    crSummary,
                    owner
            );

            Map<String, Object> folderTree = refreshFolderTree(projectId);
            Map<String, Object> rootFolder = getFirstRootFolder(folderTree);

            if (rootFolder == null) {
                throw new RuntimeException(
                        "No root folder was found for project " + projectId
                );
            }

            int rootFolderId = getNodeId(rootFolder);

            if (rootFolderId <= 0) {
                throw new RuntimeException(
                        "Root folder does not contain a valid ID"
                );
            }

            String yearName = String.valueOf(
                    java.time.Year.now().getValue()
            );

            String monthName = java.time.LocalDate.now().format(
                    java.time.format.DateTimeFormatter.ofPattern(
                            "MMM",
                            java.util.Locale.ENGLISH
                    )
            );

            String type = (cycleType == null || cycleType.isBlank())
                    ? "Functional"
                    : cycleType.trim();

            String ticketFolderName = sanitizeFolderName(
                    crKey.trim() + " - " + crSummary.trim() + " - " + type
            );

            int yearFolderId = findOrCreateChildFolder(
                    projectId,
                    rootFolderId,
                    yearName,
                    owner,
                    "Year"
            );

            int monthFolderId = findOrCreateChildFolder(
                    projectId,
                    yearFolderId,
                    monthName,
                    owner,
                    "Month"
            );

            return findOrCreateChildFolder(
                    projectId,
                    monthFolderId,
                    ticketFolderName,
                    owner,
                    "Ticket"
            );
        } catch (Exception e) {
            System.err.println(
                    "Error ensuring folder hierarchy: " + e.getMessage()
            );
            e.printStackTrace();
            return 0;
        }
    }

    private String sanitizeFolderName(String name) {
        if (name == null) {
            return "Untitled";
        }
        String cleaned = name.replaceAll("[\\\\/:*?\"<>|]", " ").replaceAll("\\s+", " ").trim();
        return cleaned.length() > 80 ? cleaned.substring(0, 80).trim() : cleaned;
    }

    private void validateFolderHierarchyInput(
            int projectId,
            String crKey,
            String crSummary,
            String owner
    ) {
        if (projectId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid project ID: " + projectId
            );
        }

        if (crKey == null || crKey.isBlank()) {
            throw new IllegalArgumentException(
                    "CR key cannot be null or blank"
            );
        }

        if (crSummary == null || crSummary.isBlank()) {
            throw new IllegalArgumentException(
                    "CR summary cannot be null or blank"
            );
        }

        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException(
                    "Owner cannot be null or blank"
            );
        }
    }

    @SuppressWarnings("unchecked")
    private int findOrCreateChildFolder(
            int projectId,
            int parentId,
            String folderName,
            String owner,
            String folderLevel
    ) {
        Map<String, Object> folderTree = refreshFolderTree(projectId);

        Map<String, Object> parentFolder =
                findFolderById(folderTree, parentId);

        if (parentFolder == null) {
            throw new RuntimeException(
                    "Parent folder with ID "
                            + parentId
                            + " was not found."
            );
        }

        List<Map<String, Object>> childFolders = getChildren(parentFolder);

        Map<String, Object> existingFolder =
                findFolderByName(childFolders, folderName);

        if (existingFolder != null) {
            int existingFolderId = getNodeId(existingFolder);

            if (existingFolderId <= 0) {
                throw new RuntimeException(
                        folderLevel
                                + " folder "
                                + folderName
                                + " exists but has an invalid ID"
                );
            }

            return existingFolderId;
        }

        return createFolder(
                folderName,
                parentId,
                projectId,
                calculateNextFolderIndex(childFolders),
                owner
        );
    }

    private int calculateNextFolderIndex(
            List<Map<String, Object>> folders
    ) {
        if (folders == null || folders.isEmpty()) {
            return 1;
        }

        int highestIndex = 0;

        for (Map<String, Object> folder : folders) {
            if (folder == null) {
                continue;
            }

            Object indexObject = folder.get("index");

            if (indexObject instanceof Number number) {
                highestIndex = Math.max(highestIndex, number.intValue());
            } else if (indexObject != null) {
                try {
                    highestIndex = Math.max(
                            highestIndex,
                            Integer.parseInt(indexObject.toString())
                    );
                } catch (NumberFormatException ignored) {
                    // Ignore malformed index values.
                }
            }
        }

        return highestIndex + 1;
    }

    private int resolveCreatedFolderId(
            int projectId,
            int parentId,
            String folderName
    ) {
        final int maxAttempts = 6;
        final long retryDelayMilliseconds = 500L;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Map<String, Object> folderTree = refreshFolderTree(projectId);

            Map<String, Object> parentFolder =
                    findFolderById(folderTree, parentId);

            if (parentFolder != null) {
                Map<String, Object> createdFolder = findFolderByName(
                        getChildren(parentFolder),
                        folderName
                );

                if (createdFolder != null) {
                    int actualFolderId = getNodeId(createdFolder);

                    if (actualFolderId > 0) {
                        return actualFolderId;
                    }
                }
            }

            if (attempt < maxAttempts) {
                sleepForFolderResolution(
                        retryDelayMilliseconds,
                        folderName
                );
            }
        }

        return 0;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> refreshFolderTree(int projectId) {
        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/project/"
                + projectId
                + "/foldertree/testrun";

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                        "Folder-tree API returned HTTP status "
                                + response.getStatusCode()
                );
            }

            if (response.getBody() == null) {
                throw new RuntimeException(
                        "Folder-tree API returned an empty response body"
                );
            }

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to refresh Zephyr folder tree for project "
                            + projectId
                            + ": "
                            + e.getMessage(),
                    e
            );
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getFirstRootFolder(
            Map<String, Object> folderTree
    ) {
        if (folderTree == null || folderTree.isEmpty()) {
            return null;
        }

        Object childrenObject = folderTree.get("children");

        if (!(childrenObject instanceof List<?> children)
                || children.isEmpty()
                || !(children.get(0) instanceof Map<?, ?>)) {
            return null;
        }

        return (Map<String, Object>) children.get(0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findFolderById(
            Map<String, Object> node,
            int targetFolderId
    ) {
        if (node == null || node.isEmpty()) {
            return null;
        }

        if (getNodeId(node) == targetFolderId) {
            return node;
        }

        for (Map<String, Object> child : getChildren(node)) {
            Map<String, Object> found =
                    findFolderById(child, targetFolderId);

            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private Map<String, Object> findFolderByName(
            List<Map<String, Object>> folders,
            String name
    ) {
        if (folders == null || folders.isEmpty() || name == null) {
            return null;
        }

        for (Map<String, Object> folder : folders) {
            if (folder == null) {
                continue;
            }

            Object folderNameObject = folder.get("name");

            if (folderNameObject != null
                    && name.equals(folderNameObject.toString())) {
                return folder;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getChildren(
            Map<String, Object> node
    ) {
        if (node == null) {
            return new ArrayList<>();
        }

        Object childrenObject = node.get("children");

        return childrenObject instanceof List<?>
                ? (List<Map<String, Object>>) childrenObject
                : new ArrayList<>();
    }

    private int getNodeId(Map<String, Object> node) {
        if (node == null) {
            return 0;
        }

        Object idObject = node.get("id");

        if (idObject instanceof Number number) {
            return number.intValue();
        }

        try {
            return idObject != null
                    ? Integer.parseInt(idObject.toString())
                    : 0;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void sleepForFolderResolution(
            long milliseconds,
            String folderName
    ) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Interrupted while waiting for folder "
                            + folderName
                            + " to appear in the folder tree",
                    e
            );
        }
    }

    @SuppressWarnings("unchecked")
    public String getTestCycleKeyById(String testCycleId) {
        if (testCycleId == null || testCycleId.isBlank()) {
            return null;
        }

        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/testrun/"
                + testCycleId
                + "?fields=id,key,name";

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null) {
                return null;
            }

            Object keyObject = response.getBody().get("key");

            if (keyObject == null) {
                return null;
            }

            return keyObject.toString().trim();
        } catch (Exception e) {
            System.err.println(
                    "Failed to resolve test cycle key for ID "
                            + testCycleId
                            + ": "
                            + e.getMessage()
            );
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Set<String> getTestCaseIssueTraceLinkIds(int testCaseId) {
        Set<String> issueIds = new LinkedHashSet<>();

        if (testCaseId <= 0) {
            return issueIds;
        }

        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/testcase/"
                + testCaseId
                + "/tracelinks/issue?fields=id,issueId,typeId";

        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    Object.class
            );

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null) {
                return issueIds;
            }

            Object responseBody = response.getBody();

            if (responseBody instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        Object issueIdObject = map.get("issueId");

                        if (issueIdObject != null) {
                            issueIds.add(issueIdObject.toString());
                        }
                    }
                }
            } else if (responseBody instanceof Map<?, ?> map) {
                Object issueIdObject = map.get("issueId");

                if (issueIdObject != null) {
                    issueIds.add(issueIdObject.toString());
                }
            }

            return issueIds;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to fetch issue trace links for test case ID "
                            + testCaseId
                            + ": "
                            + e.getMessage(),
                    e
            );
        }
    }

    public boolean ensureTestCaseLinkedToIssue(
            int testCaseId,
            String issueNumericId
    ) {
        if (testCaseId <= 0) {
            throw new IllegalArgumentException(
                    "Valid numeric test case ID is required"
            );
        }

        if (issueNumericId == null || issueNumericId.isBlank()) {
            throw new IllegalArgumentException(
                    "Valid numeric Jira issue ID is required"
            );
        }

        Set<String> existingIssueIds =
                getTestCaseIssueTraceLinkIds(testCaseId);

        if (existingIssueIds.contains(issueNumericId)) {
            System.out.println(
                    "Test case trace link already exists: testCaseId="
                            + testCaseId
                            + ", issueId="
                            + issueNumericId
            );

            return false;
        }

        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/tracelink/bulk/create";

        Map<String, Object> linkItem = new LinkedHashMap<>();
        linkItem.put("issueId", issueNumericId);
        linkItem.put("testCaseId", testCaseId);
        linkItem.put("typeId", 1);

        List<Map<String, Object>> payload = new ArrayList<>();
        payload.add(linkItem);

        try {
            String requestBody = objectMapper.writeValueAsString(payload);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, authHeaders()),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                        "Trace-link API returned HTTP "
                                + response.getStatusCode()
                );
            }

            System.out.println(
                    "Created test case trace link: testCaseId="
                            + testCaseId
                            + ", issueId="
                            + issueNumericId
            );

            return true;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to link test case ID "
                            + testCaseId
                            + " to issue ID "
                            + issueNumericId
                            + ": "
                            + e.getMessage(),
                    e
            );
        }
    }

    @SuppressWarnings("unchecked")
    private void linkScaleCloudTestToIssue(String testCaseKey, String issueKey) {
        String issueId = jiraClient.getIssueId(issueKey);
        if (issueId == null || issueId.isBlank()) {
            throw new RuntimeException("Could not resolve Jira issue ID for " + issueKey);
        }

        String url = scaleCloudBaseUrl()
                + "/testcases/"
                + testCaseKey
                + "/links/issues";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("issueId", Long.parseLong(issueId));
        payload.put("type", "COVERAGE");

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(payload), authSupport.scaleCloudHeaders()),
                    Map.class
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to link " + testCaseKey + " to " + issueKey + ": " + e.getMessage(),
                    e
            );
        }
    }

    @SuppressWarnings("unchecked")
    public List<TestCycle> listProjectTestCycles(String projectId) {
        String projectKey = jiraClient.resolveProjectKey(projectId);
        if (projectKey == null || projectKey.isBlank()) {
            projectKey = zephyrProperties.getDefaultProjectKey();
        }
        if (zephyrProperties.hasScaleCloudToken()) {
            return listScaleCloudTestCycles(projectKey);
        }
        return searchTestCyclesByIssueKey(projectKey);
    }

    @SuppressWarnings("unchecked")
    private List<TestCycle> listScaleCloudTestCycles(String projectKey) {
        List<TestCycle> cycles = new ArrayList<>();
        int startAt = 0;
        int maxResults = 50;
        while (true) {
            String url = scaleCloudBaseUrl()
                    + "/testcycles?maxResults="
                    + maxResults
                    + "&startAt="
                    + startAt
                    + "&projectKey="
                    + projectKey;
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authSupport.scaleCloudHeaders()),
                    Map.class
            );
            Map<String, Object> body = response.getBody();
            if (body == null) {
                break;
            }
            Object values = body.get("values");
            int batch = 0;
            if (values instanceof List<?> list) {
                batch = list.size();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        TestCycle cycle = new TestCycle();
                        cycle.setId(map.get("id") != null ? String.valueOf(map.get("id")) : "");
                        cycle.setKey(map.get("key") != null ? String.valueOf(map.get("key")) : "");
                        cycle.setName(map.get("name") != null ? String.valueOf(map.get("name")) : "");
                        Object status = map.get("status");
                        if (status instanceof Map<?, ?> statusMap && statusMap.get("name") != null) {
                            cycle.setStatus(String.valueOf(statusMap.get("name")));
                        }
                        cycles.add(cycle);
                    }
                }
            }
            if (Boolean.TRUE.equals(body.get("isLast")) || batch == 0) {
                break;
            }
            startAt += maxResults;
            if (startAt > 2000) {
                break;
            }
        }
        return cycles;
    }
}