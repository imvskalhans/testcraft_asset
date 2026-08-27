package com.acc.testcraft_backend.controller;

import com.acc.testcraft_backend.model.EmailRequest;
import com.acc.testcraft_backend.model.FeedbackRequest;
import com.acc.testcraft_backend.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/feedback")
    public ResponseEntity<?> submitFeedback(
            @RequestBody FeedbackRequest request
    ) {
        try {
            return ResponseEntity.ok(
                    emailService.submitFeedback(request)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of(
                            "success", false,
                            "error", e.getMessage()
                    )
            );
        }
    }

    @PostMapping("/email")
    public ResponseEntity<?> createEmailDraft(
            @RequestBody EmailRequest request
    ) {
        try {
            return ResponseEntity.ok(
                    emailService.processEmail(request)
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of(
                            "success", false,
                            "error", e.getMessage()
                    )
            );
        }
    }
}
