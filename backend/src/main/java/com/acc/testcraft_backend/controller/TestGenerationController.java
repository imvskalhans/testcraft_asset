package com.acc.testcraft_backend.controller;

import com.acc.testcraft_backend.model.TestGenerationRequest;
import com.acc.testcraft_backend.model.TestGenerationResponse;
import com.acc.testcraft_backend.service.TestGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/generate")
public class TestGenerationController {

    private final TestGenerationService testGenerationService;

    public TestGenerationController(TestGenerationService testGenerationService) {
        this.testGenerationService = testGenerationService;
    }

    @PostMapping
    public ResponseEntity<?> generateTests(@RequestBody TestGenerationRequest request) {
        try {
            if (request.getIssueKey() == null || request.getIssueKey().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("issueKey is required");
            }

            if (request.getTestType() == null || request.getTestType().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("testType is required");
            }

            if (request.getTestCount() <= 0) {
                return ResponseEntity.badRequest().body("testCount must be greater than 0");
            }

            TestGenerationResponse response = testGenerationService.generate(request);

            return response.isParseSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.status(202).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Internal Server Error: " + e.getMessage());
        }
    }
}
