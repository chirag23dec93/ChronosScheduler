package com.chronos.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test/logs")
public class LogTestController {

    @GetMapping("/generate")
    public ResponseEntity<Map<String, String>> generateTestLogs() {
        log.trace("This is a TRACE level message");
        log.debug("This is a DEBUG level message");
        log.info("This is an INFO level message");
        log.warn("This is a WARN level message");
        log.error("This is an ERROR level message");

        try {
            throw new RuntimeException("Test exception");
        } catch (Exception e) {
            log.error("Caught test exception", e);
        }

        // Add some structured logging
        log.info("Job execution completed", Map.of(
            "jobId", "test-job-123",
            "duration", "1000ms",
            "status", "SUCCESS",
            "type", "TEST"
        ));

        return ResponseEntity.ok(Map.of("message", "Test logs generated"));
    }
}
