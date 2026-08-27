package com.acc.testcraft_backend.controller;

import com.acc.testcraft_backend.model.JenkinsLogRequest;
import com.acc.testcraft_backend.service.JenkinsLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jenkins-log")
public class JenkinsLogController {
    private final JenkinsLogService service;

    public JenkinsLogController(JenkinsLogService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<?> analyze(@RequestBody JenkinsLogRequest request) {
        if (request == null || request.getUrl() == null || request.getUrl().isBlank()) {
            return ResponseEntity.badRequest().body("A Jenkins job or build URL is required");
        }
        try {
            var response = service.analyze(request);
            return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.internalServerError().body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
