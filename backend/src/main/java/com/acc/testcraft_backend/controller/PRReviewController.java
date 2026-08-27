package com.acc.testcraft_backend.controller;

import com.acc.testcraft_backend.model.PRReviewRequest;
import com.acc.testcraft_backend.service.PRReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pr-review")
public class PRReviewController {
    private final PRReviewService service;

    public PRReviewController(PRReviewService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<?> review(@RequestBody PRReviewRequest request) {
        if (request == null || request.getUrl() == null || request.getUrl().isBlank()) {
            return ResponseEntity.badRequest().body("A GitHub or Bitbucket pull request URL is required");
        }
        try {
            var response = service.review(request);
            return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
