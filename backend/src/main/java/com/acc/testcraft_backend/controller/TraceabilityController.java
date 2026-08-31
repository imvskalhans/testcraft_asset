package com.acc.testcraft_backend.controller;

import com.acc.testcraft_backend.model.TraceabilityDashboardResponse;
import com.acc.testcraft_backend.service.TraceabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/traceability")
public class TraceabilityController {

    private final TraceabilityService traceabilityService;

    public TraceabilityController(TraceabilityService traceabilityService) {
        this.traceabilityService = traceabilityService;
    }

    @PostMapping("/dashboard")
    public ResponseEntity<?> dashboard(@RequestBody Map<String, String> request) {
        try {
            String issueKey = request.get("issueKey");
            if (issueKey == null || issueKey.isBlank()) {
                TraceabilityDashboardResponse response = new TraceabilityDashboardResponse();
                response.setSuccess(false);
                response.setError("Issue key is required");
                return ResponseEntity.badRequest().body(response);
            }

            TraceabilityDashboardResponse response =
                    traceabilityService.buildDashboard(issueKey);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            TraceabilityDashboardResponse response = new TraceabilityDashboardResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            TraceabilityDashboardResponse response = new TraceabilityDashboardResponse();
            response.setSuccess(false);
            response.setError("Failed to load traceability: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
