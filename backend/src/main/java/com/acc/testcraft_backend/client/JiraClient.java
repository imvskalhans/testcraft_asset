package com.acc.testcraft_backend.client;

import com.acc.testcraft_backend.config.AppProperties;
import com.acc.testcraft_backend.config.IntegrationConfig.IntegrationAuthSupport;
import com.acc.testcraft_backend.config.IntegrationUrls;
import com.acc.testcraft_backend.config.JiraProperties;
import com.acc.testcraft_backend.config.ZephyrProperties;
import com.acc.testcraft_backend.model.JiraStory;
import com.acc.testcraft_backend.model.AiAttachment;
import com.acc.testcraft_backend.model.LinkedStory;
import com.acc.testcraft_backend.model.ReleaseProcess;
import com.acc.testcraft_backend.model.TestCase;
import com.acc.testcraft_backend.model.TestCycle;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class JiraClient {

    private final RestTemplate restTemplate;
    private final JiraProperties jiraProperties;
    private final ZephyrProperties zephyrProperties;
    private final AppProperties appProperties;
    private final IntegrationAuthSupport authSupport;
    private final IntegrationUrls urls;
    private volatile String resolvedAcceptanceCriteriaField;

    public JiraClient(
            RestTemplate restTemplate,
            JiraProperties jiraProperties,
            ZephyrProperties zephyrProperties,
            AppProperties appProperties,
            IntegrationAuthSupport authSupport,
            IntegrationUrls urls
    ) {
        this.restTemplate = restTemplate;
        this.jiraProperties = jiraProperties;
        this.zephyrProperties = zephyrProperties;
        this.appProperties = appProperties;
        this.authSupport = authSupport;
        this.urls = urls;
    }

    private HttpHeaders authHeaders() {
        return authSupport.jiraHeaders();
    }

    @SuppressWarnings("unchecked")
    public JiraStory fetchIssue(String issueKey) {
        String url = jiraProperties.getBaseUrl().replaceAll("/$", "") + jiraProperties.getApiPath()
                + "/issue/"
                + issueKey
                + "?fields=*all";

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    Map.class
            );

            Map<String, Object> body = response.getBody();

            if (body == null) {
                throw new RuntimeException("Empty response from Jira API");
            }

            Map<String, Object> fields =
                    (Map<String, Object>) body.get("fields");

            if (fields == null) {
                throw new RuntimeException(
                        "Missing 'fields' in Jira API response"
                );
            }

            JiraStory story = new JiraStory();
            story.setId(issueKey);
            story.setSummary((String) fields.get("summary"));
            story.setStatus(nestedName(fields, "status"));
            story.setPriority(nestedName(fields, "priority"));
            story.setIssueType(nestedName(fields, "issuetype"));

            String acceptanceField = resolveAcceptanceCriteriaField();
            if (acceptanceField != null && !acceptanceField.isBlank()) {
                story.setAcceptanceCriteria(jiraText(fields.get(acceptanceField)));
            }

            story.setDescription(jiraText(fields.get("description")));

            Object lbls = fields.get("labels");
            if (lbls instanceof List<?> list) {
                story.setLabels((List<String>) list);
            }

            Map<String, Object> commentData =
                    (Map<String, Object>) fields.get("comment");

            if (commentData != null) {
                story.setComments(extractComments(commentData));
            }
            if (story.getComments().isEmpty()) {
                story.setComments(fetchComments(issueKey));
            }
            story.setAttachments(fetchAttachments(fields));

            return story;
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException("Issue not found: " + issueKey);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new RuntimeException(
                    "Unauthorized: Check your Jira API token/credentials"
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error fetching issue from Jira: " + e.getMessage(),
                    e
            );
        }
    }

    @SuppressWarnings("unchecked")
    private List<AiAttachment> fetchAttachments(Map<String, Object> fields) {
        Object raw = fields.get("attachment");
        if (!(raw instanceof List<?> list)) return new ArrayList<>();
        List<AiAttachment> attachments = new ArrayList<>();
        int totalBytes = 0;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> rawMap)) continue;
            Map<String, Object> map = (Map<String, Object>) rawMap;
            String name = String.valueOf(map.getOrDefault("filename", "Jira attachment"));
            String mime = String.valueOf(map.getOrDefault("mimeType", "application/octet-stream"));
            String contentUrl = String.valueOf(map.getOrDefault("content", ""));
            AiAttachment attachment = new AiAttachment();
            attachment.setName(name);
            attachment.setMimeType(mime);
            attachment.setUrl(contentUrl);
            Object sizeValue = map.get("size");
            int size = sizeValue instanceof Number ? ((Number) sizeValue).intValue() : 0;
            if (size > 6_000_000 || totalBytes + size > 20_000_000 || contentUrl.isBlank()) {
                attachments.add(attachment);
                continue;
            }
            try {
                ResponseEntity<byte[]> download = restTemplate.exchange(
                        contentUrl, HttpMethod.GET, new HttpEntity<>(authHeaders()), byte[].class);
                byte[] bytes = download.getBody();
                if (bytes != null && bytes.length <= 6_000_000) {
                    attachment.setData(Base64.getEncoder().encodeToString(bytes));
                    totalBytes += bytes.length;
                }
            } catch (Exception e) {
                System.err.println("Could not download Jira attachment " + name + ": " + e.getMessage());
            }
            attachments.add(attachment);
            if (attachments.size() >= 5) break;
        }
        return attachments;
    }

    /**
     * Jira does not expose custom-field names in an issue payload. Resolve
     * the configured field when available, otherwise find the field whose
     * Jira name is Acceptance Criteria.
     */
    @SuppressWarnings("unchecked")
    private String resolveAcceptanceCriteriaField() {
        String configured = jiraProperties.getCustomFields().getAcceptanceCriteria();
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        if (resolvedAcceptanceCriteriaField != null) {
            return resolvedAcceptanceCriteriaField;
        }

        try {
            String url = jiraProperties.getBaseUrl().replaceAll("/$", "")
                    + jiraProperties.getApiPath() + "/field";
            ResponseEntity<List> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders()), List.class);
            if (response.getBody() != null) {
                for (Object item : response.getBody()) {
                    if (item instanceof Map<?, ?> field) {
                        Object name = field.get("name");
                        Object id = field.get("id");
                        if (name != null && id != null
                                && "acceptance criteria".equalsIgnoreCase(name.toString().trim())) {
                            resolvedAcceptanceCriteriaField = id.toString();
                            return resolvedAcceptanceCriteriaField;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not resolve Jira Acceptance Criteria field: " + e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<JiraStory.Comment> fetchComments(String issueKey) {
        try {
            String url = jiraProperties.getBaseUrl().replaceAll("/$", "")
                    + jiraProperties.getApiPath() + "/issue/" + issueKey + "/comment";
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
            return response.getBody() == null
                    ? new ArrayList<>()
                    : extractComments(response.getBody());
        } catch (Exception e) {
            System.err.println("Could not fetch Jira comments for " + issueKey + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean testConnection() {
        try {
            getCurrentUser();
            return true;
        } catch (Exception e) {
            System.err.println(
                    "Jira connection test failed: " + e.getMessage()
            );
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getCurrentUser() {
        String url = jiraProperties.getBaseUrl().replaceAll("/$", "")
                + jiraProperties.getApiPath()
                + "/myself";

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        if (response.getBody() == null) {
            throw new RuntimeException("Empty /myself response from Jira");
        }

        Map<String, Object> body = response.getBody();
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("accountId", stringOrEmpty(body.get("accountId")));
        user.put("key", stringOrEmpty(body.get("key")));
        user.put("name", stringOrEmpty(body.get("name")));
        user.put("displayName", stringOrEmpty(body.get("displayName")));
        user.put("emailAddress", stringOrEmpty(body.get("emailAddress")));
        user.put("active", body.get("active") instanceof Boolean b && b);
        user.put("owner", resolveOwnerKey(body));
        return user;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> listProjects() {
        Map<String, String> projects = new LinkedHashMap<>();

        String searchUrl = jiraProperties.getBaseUrl().replaceAll("/$", "")
                + jiraProperties.getApiPath()
                + "/project/search?maxResults=100";

        try {
            ResponseEntity<Map> search = restTemplate.exchange(
                    searchUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    Map.class
            );

            Object values = search.getBody() != null
                    ? search.getBody().get("values")
                    : null;

            if (values instanceof List<?> list) {
                addJiraProjects(list, projects);
            }
        } catch (Exception ignored) {
            // Fall through to classic /project
        }

        if (!projects.isEmpty()) {
            return projects;
        }

        String url = jiraProperties.getBaseUrl().replaceAll("/$", "")
                + jiraProperties.getApiPath()
                + "/project";

        ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                List.class
        );

        if (response.getBody() != null) {
            addJiraProjects(response.getBody(), projects);
        }

        return projects;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getProject(String projectKey) {
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("Project key is required");
        }
        String url = jiraProperties.getBaseUrl().replaceAll("/$", "")
                + jiraProperties.getApiPath()
                + "/project/"
                + URLEncoder.encode(projectKey.trim().toUpperCase(), StandardCharsets.UTF_8);
        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
        Map<String, Object> body = response.getBody();
        if (body == null || body.get("id") == null) {
            throw new IllegalStateException("Jira returned no project details");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", String.valueOf(body.get("id")));
        result.put("key", String.valueOf(body.getOrDefault("key", projectKey.trim().toUpperCase())));
        result.put("name", String.valueOf(body.getOrDefault("name", "")));
        return result;
    }

    @SuppressWarnings("unchecked")
    public String resolveProjectKey(String projectIdOrKey) {
        if (projectIdOrKey == null || projectIdOrKey.isBlank()) {
            return "";
        }
        if (projectIdOrKey.matches("[A-Za-z][A-Za-z0-9_]*")) {
            return projectIdOrKey.toUpperCase();
        }

        String url = jiraProperties.getBaseUrl().replaceAll("/$", "")
                + jiraProperties.getApiPath()
                + "/project/"
                + projectIdOrKey;

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );

        Map<String, Object> body = response.getBody();
        if (body != null && body.get("key") != null) {
            return String.valueOf(body.get("key"));
        }
        return projectIdOrKey;
    }

    private void addJiraProjects(List<?> list, Map<String, String> projects) {
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }

            Object id = map.get("id");
            Object key = map.get("key");
            Object name = map.get("name");

            if (id == null) {
                continue;
            }

            String label = (name != null ? name.toString() : "")
                    + (key != null ? " (" + key + ")" : "");
            if (label.isBlank()) {
                label = id.toString();
            }

            projects.put(label.trim(), id.toString());
        }
    }

    private String resolveOwnerKey(Map<String, Object> user) {
        Object accountId = user.get("accountId");
        if (accountId != null && !accountId.toString().isBlank()) {
            return accountId.toString();
        }
        Object key = user.get("key");
        if (key != null && !key.toString().isBlank()) {
            return key.toString();
        }
        Object name = user.get("name");
        return name != null ? name.toString() : "";
    }

    private String stringOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    @SuppressWarnings("unchecked")
    private String nestedName(Map<String, Object> fields, String key) {
        Object nested = fields.get(key);
        return nested instanceof Map<?, ?> map
                ? (String) map.get("name")
                : null;
    }

    private boolean matchesUser(String username, Object name, Object email, Object display) {
        return equalsIgnore(username, name)
                || equalsIgnore(username, email)
                || equalsIgnore(username, display);
    }

    private boolean equalsIgnore(String expected, Object actual) {
        return actual != null && expected.equalsIgnoreCase(actual.toString());
    }

    private String jiraText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder text = new StringBuilder();
            appendAdf(map, text);
            String result = text.toString().trim();
            return result.isEmpty() ? null : result;
        }
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    private void appendAdf(Object node, StringBuilder out) {
        if (node instanceof Map<?, ?> map) {
            Object type = map.get("type");
            if ("text".equals(type) && map.get("text") != null) {
                out.append(map.get("text"));
            }
            Object content = map.get("content");
            if (content instanceof List<?> list) {
                for (Object child : list) {
                    appendAdf(child, out);
                }
                if ("paragraph".equals(type) || "heading".equals(type)) {
                    out.append('\n');
                }
            }
        } else if (node instanceof List<?> list) {
            for (Object child : list) {
                appendAdf(child, out);
            }
        }
    }

    public Map<String, Object> searchUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        String url = jiraProperties.getBaseUrl().replaceAll("/$", "") + jiraProperties.getApiPath()
                + "/user/search";

        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    url + "?query=" + java.net.URLEncoder.encode(username.trim(), java.nio.charset.StandardCharsets.UTF_8)
                            + "&username=" + java.net.URLEncoder.encode(username.trim(), java.nio.charset.StandardCharsets.UTF_8),
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    List.class
            );

            List<Map<String, Object>> users = response.getBody();

            if (users == null || users.isEmpty()) {
                return null;
            }

            for (Object userObj : users) {
                if (userObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> user =
                            (Map<String, Object>) userObj;

                    Object name = user.get("name");
                    Object email = user.get("emailAddress");
                    Object display = user.get("displayName");

                    if (matchesUser(username, name, email, display)) {
                        return user;
                    }
                }
            }

            if (users.get(0) instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> first = (Map<String, Object>) users.get(0);
                return first;
            }

            return null;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error searching for Jira user: " + e.getMessage(),
                    e
            );
        }
    }

    public boolean isValidJiraUser(String username) {
        try {
            return searchUser(username) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserKey(String username) {
        return getZephyrOwnerId(username);
    }

    /**
     * Resolves a user identifier for Zephyr Scale Cloud.
     * Jira Cloud uses accountId (e.g. 712020:928dc7ca-...) rather than legacy JIRAUSER keys.
     */
    public String getZephyrOwnerId(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        String trimmed = username.trim();
        if (trimmed.contains(":") && trimmed.matches("\\d+:[0-9a-f\\-]+")) {
            return trimmed;
        }

        Map<String, Object> user = searchUser(trimmed);
        if (user == null && trimmed.contains("@")) {
            user = searchUserByEmail(trimmed);
        }
        if (user == null) {
            return null;
        }

        Object accountId = user.get("accountId");
        if (accountId != null && !accountId.toString().isBlank()) {
            return accountId.toString();
        }

        Object key = user.get("key");
        return key != null ? key.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> searchUserByEmail(String email) {
        String url = jiraProperties.getBaseUrl().replaceAll("/$", "") + jiraProperties.getApiPath()
                + "/user/search?query=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);

        ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                List.class
        );

        List<?> users = response.getBody();
        if (users == null) {
            return null;
        }

        for (Object userObj : users) {
            if (userObj instanceof Map<?, ?> map) {
                Object foundEmail = map.get("emailAddress");
                if (foundEmail != null && email.equalsIgnoreCase(foundEmail.toString())) {
                    return (Map<String, Object>) map;
                }
            }
        }
        return null;
    }

    public void postComment(String issueKey, String commentContent) {
        String url = jiraProperties.getBaseUrl().replaceAll("/$", "") + jiraProperties.getApiPath()
                + "/issue/"
                + issueKey
                + "/comment";

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (jiraProperties.getApiPath() != null
                    && jiraProperties.getApiPath().contains("/api/3")) {
                payload.put("body", adfDocument(commentContent));
            } else {
                payload.put("body", commentContent);
            }

            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, authHeaders()),
                    Map.class
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to post comment: " + e.getMessage(),
                    e
            );
        }
    }

    private Map<String, Object> adfDocument(String text) {
        Map<String, Object> textNode = new LinkedHashMap<>();
        textNode.put("type", "text");
        textNode.put("text", text == null ? "" : text);

        Map<String, Object> paragraph = new LinkedHashMap<>();
        paragraph.put("type", "paragraph");
        paragraph.put("content", List.of(textNode));

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("type", "doc");
        doc.put("version", 1);
        doc.put("content", List.of(paragraph));
        return doc;
    }

    public String getIssueId(String issueKey) {
        if (issueKey == null || issueKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Issue key cannot be null or blank"
            );
        }

        String numericIssueId =
                resolveNumericIssueId(issueKey.trim().toUpperCase());

        if (numericIssueId == null || numericIssueId.isBlank()) {
            throw new RuntimeException(
                    "Could not resolve numeric Jira issue ID for " + issueKey
            );
        }

        return numericIssueId;
    }

    @SuppressWarnings("unchecked")
    public List<LinkedStory> fetchChildWorkItems(String issueKey) {
        List<LinkedStory> children = new ArrayList<>();
        if (issueKey == null || issueKey.isBlank()) {
            return children;
        }
        String key = issueKey.trim().toUpperCase();
        String url = jiraProperties.getBaseUrl().replaceAll("/$", "") + jiraProperties.getApiPath()
                + "/issue/" + key + "?fields=subtasks";
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(authHeaders()), Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null || !(body.get("fields") instanceof Map<?, ?> fields)) {
                return children;
            }
            Object subtasks = ((Map<String, Object>) fields).get("subtasks");
            if (!(subtasks instanceof List<?> list)) {
                return children;
            }
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                LinkedStory child = new LinkedStory();
                child.setKey(map.get("key") != null ? String.valueOf(map.get("key")) : "");
                child.setId(map.get("id") != null ? String.valueOf(map.get("id")) : child.getKey());
                Object nestedFields = map.get("fields");
                if (nestedFields instanceof Map<?, ?> childFields) {
                    Map<String, Object> cf = (Map<String, Object>) childFields;
                    child.setSummary(cf.get("summary") != null ? String.valueOf(cf.get("summary")) : "");
                    child.setStatus(nestedName(cf, "status"));
                    child.setIssueType(nestedName(cf, "issuetype"));
                    child.setPriority(nestedName(cf, "priority"));
                }
                if (child.getKey() != null && !child.getKey().isBlank()) {
                    children.add(child);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch sub-tasks for " + key + ": " + e.getMessage());
        }
        return children;
    }

    @SuppressWarnings("unchecked")
    private List<JiraStory.Comment> extractComments(
            Map<String, Object> commentData
    ) {
        List<JiraStory.Comment> comments = new ArrayList<>();
        Object commentsObj = commentData.get("comments");

        if (commentsObj instanceof List<?> list) {
            for (Object commentObj : list) {
                if (commentObj instanceof Map<?, ?> commentMap) {
                    JiraStory.Comment comment = new JiraStory.Comment();
                    comment.setBody(jiraText(commentMap.get("body")));
                    comment.setCreated((String) commentMap.get("created"));

                    Map<String, Object> author =
                            (Map<String, Object>) commentMap.get("author");

                    if (author != null) {
                        comment.setAuthor((String) author.get("name"));
                        comment.setAuthorDisplayName(
                                (String) author.get("displayName")
                        );
                    }

                    comments.add(comment);
                }
            }
        }

        return comments;
    }

    @SuppressWarnings("unchecked")
    public ReleaseProcess fetchIssueWithLinkedStories(String issueKey) {
        if (issueKey == null || issueKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Issue key cannot be null or blank"
            );
        }

        String normalizedIssueKey = issueKey.trim().toUpperCase();
        String url = jiraProperties.getBaseUrl().replaceAll("/$", "") + jiraProperties.getApiPath()
                + "/issue/"
                + normalizedIssueKey
                + "?fields=id,key,summary,description,status,issuelinks,"
                + (jiraProperties.getCustomFields().getLinkedStories().isBlank() ? "" : jiraProperties.getCustomFields().getLinkedStories() + ",") + "project,fixVersions";

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                        "Jira API returned HTTP status "
                                + response.getStatusCode()
                                + " for issue "
                                + normalizedIssueKey
                );
            }

            Map<String, Object> body = response.getBody();

            if (body == null) {
                throw new RuntimeException(
                        "Empty response from Jira API for issue "
                                + normalizedIssueKey
                );
            }

            Object idObject = body.get("id");

            if (idObject == null) {
                throw new RuntimeException(
                        "Numeric Jira issue ID was not returned for "
                                + normalizedIssueKey
                );
            }

            String crNumericId = idObject.toString();
            Object fieldsObject = body.get("fields");

            if (!(fieldsObject instanceof Map<?, ?>)) {
                throw new RuntimeException(
                        "Missing fields object in Jira response for "
                                + normalizedIssueKey
                );
            }

            Map<String, Object> fields =
                    (Map<String, Object>) fieldsObject;

            String projectId =
                    extractProjectIdFromFields(fields, normalizedIssueKey);
            String productVersionId =
                    extractProductVersionIdFromFields(
                            fields,
                            normalizedIssueKey
                    );

            String crSummary = fields.get("summary") != null
                    ? fields.get("summary").toString()
                    : null;
            String crDescription = jiraText(fields.get("description"));
            String crStatus = nestedName(fields, "status");

            List<LinkedStory> linkedStories = new ArrayList<>();
            Object customField = fields.get(jiraProperties.getCustomFields().getLinkedStories());

            if (customField instanceof List<?> list && !list.isEmpty()) {
                linkedStories.addAll(
                        extractLinkedStoriesFromCustomField(list)
                );
                System.out.println(
                        "DEBUG: Extracted "
                                + linkedStories.size()
                                + " linked issues from linked-stories custom field"
                );
            } else {
                Object issueLinks = fields.get("issuelinks");

                if (issueLinks instanceof List<?> list) {
                    linkedStories.addAll(
                            extractLinkedStoriesFromIssueLinksEnhanced(list)
                    );
                }

                System.out.println(
                        "DEBUG: Extracted "
                                + linkedStories.size()
                                + " linked issues from issuelinks"
                );
            }

            linkedStories = deduplicateLinkedStories(linkedStories);

            ReleaseProcess releaseProcess = new ReleaseProcess();
            releaseProcess.setCrId(crNumericId);
            releaseProcess.setCrKey(normalizedIssueKey);
            releaseProcess.setCrSummary(crSummary);
            releaseProcess.setCrDescription(crDescription);
            releaseProcess.setStatus(crStatus);
            releaseProcess.setLinkedStories(linkedStories);
            releaseProcess.setProjectId(projectId);
            releaseProcess.setProductVersionId(productVersionId);
            releaseProcess.setFolderId(0);
            releaseProcess.setCreatedBy(appProperties.resolveOwner(jiraProperties));
            releaseProcess.setCreatedDate(
                    java.time.LocalDateTime.now().toString()
            );

            System.out.println(
                    "DEBUG: Release issue resolved -> crKey="
                            + normalizedIssueKey
                            + " crId="
                            + crNumericId
                            + " projectId="
                            + projectId
            );

            return releaseProcess;
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException(
                    "Issue not found: " + normalizedIssueKey,
                    e
            );
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new RuntimeException(
                    "Unauthorized while fetching Jira issue "
                            + normalizedIssueKey,
                    e
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error fetching issue with linked stories for "
                            + normalizedIssueKey
                            + ": "
                            + e.getMessage(),
                    e
            );
        }
    }

    @SuppressWarnings("unchecked")
    private String extractProjectIdFromFields(
            Map<String, Object> fields,
            String issueKey
    ) {
        Object projectObject = fields.get("project");

        if (!(projectObject instanceof Map<?, ?>)) {
            System.out.println(
                    "WARNING: No project object found for " + issueKey
            );
            return null;
        }

        Map<String, Object> project =
                (Map<String, Object>) projectObject;

        Object projectIdObject = project.get("id");

        if (projectIdObject == null) {
            System.out.println(
                    "WARNING: No projectId found for " + issueKey
            );
            return null;
        }

        return projectIdObject.toString();
    }

    @SuppressWarnings("unchecked")
    private String extractProductVersionIdFromFields(
            Map<String, Object> fields,
            String issueKey
    ) {
        Object fixVersionsObject = fields.get("fixVersions");

        if (!(fixVersionsObject instanceof List<?>)) {
            System.out.println(
                    "WARNING: No fixVersions field found for " + issueKey
            );
            return null;
        }

        List<?> fixVersions = (List<?>) fixVersionsObject;

        if (fixVersions.isEmpty()) {
            System.out.println(
                    "WARNING: Empty fixVersions for " + issueKey
            );
            return null;
        }

        Object firstVersion = fixVersions.get(0);

        if (!(firstVersion instanceof Map<?, ?>)) {
            return null;
        }

        Map<String, Object> version =
                (Map<String, Object>) firstVersion;

        Object versionId = version.get("id");
        return versionId != null ? versionId.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private List<LinkedStory> extractLinkedStoriesFromIssueLinksEnhanced(
            List<?> issueLinks
    ) {
        List<LinkedStory> stories = new ArrayList<>();

        for (Object linkObj : issueLinks) {
            if (linkObj instanceof Map<?, ?>) {
                Map<String, Object> link =
                        (Map<String, Object>) linkObj;

                Map<String, Object> inwardIssue =
                        (Map<String, Object>) link.get("inwardIssue");
                Map<String, Object> outwardIssue =
                        (Map<String, Object>) link.get("outwardIssue");

                Map<String, Object> linkedIssue =
                        inwardIssue != null ? inwardIssue : outwardIssue;

                if (linkedIssue != null) {
                    String issueKey = (String) linkedIssue.get("key");

                    try {
                        JiraStory fullStory = fetchIssue(issueKey);

                        LinkedStory story = new LinkedStory();
                        story.setId((String) linkedIssue.get("id"));
                        story.setKey(issueKey);
                        story.setSummary(fullStory.getSummary());
                        story.setDescription(fullStory.getDescription());
                        story.setIssueType(fullStory.getIssueType());
                        story.setStatus(fullStory.getStatus());
                        story.setPriority(fullStory.getPriority());
                        stories.add(story);
                    } catch (Exception e) {
                        System.err.println(
                                "Failed to fetch full details for linked story "
                                        + issueKey
                                        + ": "
                                        + e.getMessage()
                        );

                        LinkedStory story = new LinkedStory();
                        story.setId((String) linkedIssue.get("id"));
                        story.setKey(issueKey);

                        Map<String, Object> linkedFields =
                                (Map<String, Object>) linkedIssue.get("fields");

                        if (linkedFields != null) {
                            story.setSummary(
                                    (String) linkedFields.get("summary")
                            );
                            story.setDescription(
                                    (String) linkedFields.get("description")
                            );
                            story.setIssueType(
                                    nestedName(linkedFields, "issuetype")
                            );
                            story.setStatus(
                                    nestedName(linkedFields, "status")
                            );
                            story.setPriority(
                                    nestedName(linkedFields, "priority")
                            );
                        }

                        stories.add(story);
                    }
                }
            }
        }

        return stories;
    }

    @SuppressWarnings("unchecked")
    private List<LinkedStory> extractLinkedStoriesFromCustomField(
            List<?> customField
    ) {
        List<LinkedStory> stories = new ArrayList<>();

        if (customField == null || customField.isEmpty()) {
            return stories;
        }

        for (Object storyObject : customField) {
            String issueId = null;
            String issueKey = null;
            Map<String, Object> providedFields = null;

            if (storyObject instanceof String) {
                issueKey = storyObject.toString().trim();
            } else if (storyObject instanceof Map<?, ?>) {
                Map<String, Object> storyMap =
                        (Map<String, Object>) storyObject;

                Object idObject = storyMap.get("id");
                Object keyObject = storyMap.get("key");

                if (idObject != null) {
                    issueId = idObject.toString();
                }

                if (keyObject != null) {
                    issueKey = keyObject.toString();
                }

                Object fieldsObject = storyMap.get("fields");
                providedFields = fieldsObject instanceof Map<?, ?>
                        ? (Map<String, Object>) fieldsObject
                        : storyMap;
            }

            if (issueKey == null || issueKey.isBlank()) {
                System.err.println(
                        "Skipping linked-stories custom field entry because it does "
                                + "not contain an issue key"
                );
                continue;
            }

            LinkedStory story = new LinkedStory();
            story.setId(issueId);
            story.setKey(issueKey);
            populateLinkedStoryFromFields(story, providedFields);

            try {
                JiraStory fullStory = fetchIssue(issueKey);
                story.setSummary(fullStory.getSummary());
                story.setDescription(fullStory.getDescription());
                story.setIssueType(fullStory.getIssueType());
                story.setStatus(fullStory.getStatus());
                story.setPriority(fullStory.getPriority());

                if (story.getId() == null || story.getId().isBlank()) {
                    story.setId(resolveNumericIssueId(issueKey));
                }

                System.out.println(
                        "DEBUG: Enriched linked issue "
                                + issueKey
                                + " with summary='"
                                + story.getSummary()
                                + "', status='"
                                + story.getStatus()
                                + "'"
                );
            } catch (Exception e) {
                System.err.println(
                        "Failed to enrich linked issue "
                                + issueKey
                                + ": "
                                + e.getMessage()
                                + ". Using details available in custom field."
                );

                if (story.getId() == null || story.getId().isBlank()) {
                    try {
                        story.setId(resolveNumericIssueId(issueKey));
                    } catch (Exception idException) {
                        System.err.println(
                                "Failed to resolve numeric issue ID for "
                                        + issueKey
                                        + ": "
                                        + idException.getMessage()
                        );
                    }
                }
            }

            stories.add(story);
        }

        return deduplicateLinkedStories(stories);
    }

    public List<LinkedStory> getLinkedStories(String crKey) {
        return fetchIssueWithLinkedStories(crKey).getLinkedStories();
    }

    public List<TestCycle> getTestRunsLinkedToIssue(String issueKey) {
        if (issueKey == null || issueKey.isBlank()) {
            return new ArrayList<>();
        }

        try {
            String url = jiraProperties.getBaseUrl().replaceAll("/$", "") + jiraProperties.getApiPath()
                    + "/issue/"
                    + issueKey;

            ResponseEntity<Map> response = restTemplate.exchange(
                    url + "?fields=id",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null) {
                return new ArrayList<>();
            }

            Object issueIdObj = response.getBody().get("id");

            if (issueIdObj == null) {
                return new ArrayList<>();
            }

            String issueId = String.valueOf(issueIdObj);

            System.out.println(
                    "DEBUG: Resolved issue key "
                            + issueKey
                            + " to ID: "
                            + issueId
            );

            return getTestRunsFromZephyrTracelinks(issueId, issueKey);
        } catch (Exception e) {
            System.err.println(
                    "Error fetching test runs for issue "
                            + issueKey
                            + ": "
                            + e.getMessage()
            );
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<TestCycle> getTestRunsFromZephyrTracelinks(
            String issueId,
            String issueKey
    ) {
        List<TestCycle> testCycles = new ArrayList<>();

        String url = zephyrProperties.resolveApiBase(jiraProperties)
                + "/issue/"
                + issueId
                + "/tracelinks";

        System.out.println("DEBUG: Fetching tracelinks from Zephyr: " + url);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url + "?maxResults=50&startAt=0",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                System.out.println(
                        "DEBUG: Tracelinks response received for issue "
                                + issueKey
                );

                Map<String, Object> testRunData =
                        (Map<String, Object>) body.get("testRun");
                Map<String, Object> testCaseData =
                        (Map<String, Object>) body.get("testCase");

                List<TestCase> allTestCases = new ArrayList<>();

                if (testCaseData != null) {
                    Map<String, Object> coverageData =
                            (Map<String, Object>) testCaseData.get("coverage");

                    if (coverageData != null) {
                        List<?> coverageLinks =
                                (List<?>) coverageData.get("traceLinks");

                        if (coverageLinks != null) {
                            for (Object coverageObj : coverageLinks) {
                                if (coverageObj instanceof Map) {
                                    Map<String, Object> coverageLink =
                                            (Map<String, Object>) coverageObj;

                                    Map<String, Object> testCaseInfo =
                                            (Map<String, Object>) coverageLink
                                                    .get("testCase");

                                    if (testCaseInfo != null) {
                                        TestCase testCase = new TestCase();
                                        testCase.setTestName(
                                                (String) testCaseInfo.get("name")
                                        );
                                        testCase.setKey(
                                                (String) testCaseInfo.get("key")
                                        );

                                        Object statusObj =
                                                testCaseInfo.get("status");

                                        if (statusObj instanceof Map) {
                                            testCase.setStatus(
                                                    (String) ((Map<String, Object>)
                                                            statusObj).get("name")
                                            );
                                        }

                                        allTestCases.add(testCase);
                                    }
                                }
                            }
                        }
                    }
                }

                if (testRunData != null) {
                    Map<String, Object> relatedData =
                            (Map<String, Object>) testRunData.get("related");

                    if (relatedData != null) {
                        List<?> traceLinks =
                                (List<?>) relatedData.get("traceLinks");

                        if (traceLinks != null && !traceLinks.isEmpty()) {
                            System.out.println(
                                    "DEBUG: Found "
                                            + traceLinks.size()
                                            + " test cycles"
                            );

                            for (Object linkObj : traceLinks) {
                                if (linkObj instanceof Map) {
                                    Map<String, Object> linkData =
                                            (Map<String, Object>) linkObj;

                                    Map<String, Object> testCycleData =
                                            (Map<String, Object>) linkData
                                                    .get("testRun");

                                    if (testCycleData != null) {
                                        TestCycle cycle = new TestCycle();
                                        Object cycleId = linkData.get("id");

                                        cycle.setId(
                                                cycleId != null
                                                        ? String.valueOf(cycleId)
                                                        : ""
                                        );
                                        cycle.setKey(
                                                (String) testCycleData.get("key")
                                        );
                                        cycle.setName(
                                                (String) testCycleData.get("name")
                                        );

                                        Object statusObj =
                                                testCycleData.get("status");

                                        if (statusObj instanceof Map) {
                                            cycle.setStatus(
                                                    (String) ((Map<String, Object>)
                                                            statusObj).get("name")
                                            );
                                        }

                                        Object caseCount =
                                                testCycleData.get("testCaseCount");

                                        if (caseCount instanceof Number) {
                                            cycle.setTestCaseCount(
                                                    ((Number) caseCount).intValue()
                                            );
                                        }

                                        cycle.setTestCases(allTestCases);
                                        testCycles.add(cycle);

                                        System.out.println(
                                                "DEBUG: Added test cycle: "
                                                        + cycle.getKey()
                                                        + " "
                                                        + cycle.getName()
                                        );
                                    }
                                }
                            }
                        } else {
                            System.out.println(
                                    "DEBUG: No testRun.related.traceLinks "
                                            + "found for issue "
                                            + issueKey
                            );
                        }
                    } else {
                        System.out.println(
                                "DEBUG: No related tracelinks data found"
                        );
                    }
                } else {
                    System.out.println(
                            "DEBUG: No testRun data in response"
                    );
                }
            } else {
                System.err.println(
                        "Failed to fetch tracelinks, status: "
                                + response.getStatusCode()
                );
            }
        } catch (Exception e) {
            System.err.println(
                    "Error fetching tracelinks from Zephyr: "
                            + e.getMessage()
            );
            e.printStackTrace();
        }

        System.out.println(
                "DEBUG: Returning "
                        + testCycles.size()
                        + " test cycles for issue "
                        + issueKey
        );

        return testCycles;
    }

    private void populateLinkedStoryFromFields(
            LinkedStory story,
            Map<String, Object> fields
    ) {
        if (story == null || fields == null) {
            return;
        }

        Object summaryObject = fields.get("summary");

        if (summaryObject != null) {
            story.setSummary(summaryObject.toString());
        }

        Object descriptionObject = fields.get("description");

        if (descriptionObject != null) {
            story.setDescription(jiraText(descriptionObject));
        }

        story.setIssueType(nestedName(fields, "issuetype"));
        story.setStatus(nestedName(fields, "status"));
        story.setPriority(nestedName(fields, "priority"));
    }

    @SuppressWarnings("unchecked")
    private String resolveNumericIssueId(String issueKey) {
        if (issueKey == null || issueKey.isBlank()) {
            return null;
        }

        String url = jiraProperties.getBaseUrl().replaceAll("/$", "") + jiraProperties.getApiPath()
                + "/issue/"
                + issueKey
                + "?fields=id";

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

        Object idObject = response.getBody().get("id");
        return idObject == null ? null : idObject.toString();
    }

    private List<LinkedStory> deduplicateLinkedStories(
            List<LinkedStory> stories
    ) {
        Map<String, LinkedStory> unique = new LinkedHashMap<>();

        if (stories == null) {
            return new ArrayList<>();
        }

        for (LinkedStory story : stories) {
            if (story == null
                    || story.getKey() == null
                    || story.getKey().isBlank()) {
                continue;
            }

            unique.putIfAbsent(
                    story.getKey().trim().toUpperCase(),
                    story
            );
        }

        return new ArrayList<>(unique.values());
    }
}
