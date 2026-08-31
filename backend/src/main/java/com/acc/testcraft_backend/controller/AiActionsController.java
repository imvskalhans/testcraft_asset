package com.acc.testcraft_backend.controller;

import com.acc.testcraft_backend.model.AiActionRunRequest;
import com.acc.testcraft_backend.model.AiActionRunResponse;
import com.acc.testcraft_backend.service.AiActionsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiActionsController {

    private final AiActionsService aiActionsService;

    public AiActionsController(AiActionsService aiActionsService) {
        this.aiActionsService = aiActionsService;
    }

    @GetMapping("/actions")
    public ResponseEntity<?> listActions() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "actions", aiActionsService.listActions()
        ));
    }

    @PostMapping("/run")
    public ResponseEntity<?> runAction(@RequestBody AiActionRunRequest request) {
        try {
            if (request.getActionId() == null || request.getActionId().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "actionId is required"
                ));
            }

            AiActionRunResponse response = aiActionsService.run(request);
            return response.isSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Failed to run AI action: " + e.getMessage()
            ));
        }
    }
}
