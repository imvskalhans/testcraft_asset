package com.acc.testcraft_backend.controller;

import com.acc.testcraft_backend.model.AIReviewRequest;
import com.acc.testcraft_backend.model.AIReviewResponse;
import com.acc.testcraft_backend.service.AIReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-review")
public class AIReviewController {

    private final AIReviewService aiReviewService;

    public AIReviewController(AIReviewService aiReviewService) {
        this.aiReviewService = aiReviewService;
    }

    @PostMapping
    public ResponseEntity<?> getAIReview(@RequestBody AIReviewRequest request) {
        try {
            if (request.getIssueKey() == null || request.getIssueKey().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("issueKey is required");
            }

            AIReviewResponse response = aiReviewService.generateReview(request);

            return response.isSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(500).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Internal Server Error: " + e.getMessage());
        }
    }
}
