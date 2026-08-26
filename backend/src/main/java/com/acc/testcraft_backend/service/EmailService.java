package com.acc.testcraft_backend.service;

import com.acc.testcraft_backend.model.EmailRequest;
import com.acc.testcraft_backend.model.FeedbackRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public Map<String, Object> submitFeedback(FeedbackRequest request) {
        log.info(
                "Feedback received from {}: rating={}, message={}",
                request.getUsername(),
                request.getRating(),
                request.getMessage()
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Feedback recorded");
        return response;
    }

    public Map<String, Object> processEmail(EmailRequest request) {
        log.info(
                "Email draft created: to={}, subject={}, issueKey={}",
                request.getTo(),
                request.getSubject(),
                request.getIssueKey()
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Email draft prepared (not sent — configure SMTP to enable)");
        response.put("to", request.getTo());
        response.put("subject", request.getSubject());
        return response;
    }
}
