package com.acc.testcraft_backend.controller;

import com.acc.testcraft_backend.model.JiraStory;
import com.acc.testcraft_backend.service.JiraService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/jira")
public class JiraController {

    private final JiraService jiraService;

    public JiraController(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    @GetMapping("/test")
    public ResponseEntity<String> testConnection() {
        return jiraService.testConnection()
                ? ResponseEntity.ok("Jira connection successful")
                : ResponseEntity.status(500)
                  .body("Jira connection failed");
    }

    @GetMapping("/issue/{key}")
    public ResponseEntity<JiraStory> getIssue(@PathVariable String key) {
        try {
            return ResponseEntity.ok(jiraService.fetchStory(key));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/comment")
    public ResponseEntity<?> postCommentToIssue(
            @RequestParam String issueKey,
            @RequestBody Map<String, String> body
    ) {
        try {
            if (issueKey == null || issueKey.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("issueKey is required");
            }

            String comment = body.get("comment");
            if (comment == null || comment.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("comment is required");
            }

            jiraService.postComment(issueKey, comment);

            return ResponseEntity.ok(
                    Map.of(
                            "issueKey", issueKey,
                            "postedBy", jiraService.getCommentAuthor(),
                            "initiatedBy", jiraService.getLastResolvedCommentAuthor()
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Failed to post comment: " + e.getMessage());
        }
    }
}
