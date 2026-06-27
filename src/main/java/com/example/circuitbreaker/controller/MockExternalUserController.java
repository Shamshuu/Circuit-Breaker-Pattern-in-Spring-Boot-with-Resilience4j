package com.example.circuitbreaker.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/external")
public class MockExternalUserController {
    private static final Logger logger = LoggerFactory.getLogger(MockExternalUserController.class);
    private final AtomicBoolean failRequests = new AtomicBoolean(false);

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable String id) {
        if (failRequests.get()) {
            logger.info("Mock external user service: simulating failure for user {}", id);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Mock external service failure");
        }
        logger.info("Mock external user service: returning user {}", id);
        return ResponseEntity.ok(Map.of(
                "id", id,
                "name", "External User " + id,
                "email", "user" + id + "@external.com"
        ));
    }

    @GetMapping("/toggle")
    public ResponseEntity<String> toggleFail(@RequestParam boolean fail) {
        failRequests.set(fail);
        logger.info("Mock external user service: failure toggled to {}", fail);
        return ResponseEntity.ok("Mock service failure state set to " + fail);
    }
}
