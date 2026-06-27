package com.example.circuitbreaker.controller;

import com.example.circuitbreaker.dto.UserDTO;
import com.example.circuitbreaker.service.UserDataService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CircuitBreakerController {

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @GetMapping("/users/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable String id) {
        UserDTO user = userDataService.fetchUser(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/circuit-breaker/state")
    public String getCircuitBreakerState() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
        return circuitBreaker.getState().toString();
    }

    @GetMapping("/circuit-breaker/metrics")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerMetrics() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        Map<String, Object> metricsMap = new HashMap<>();
        metricsMap.put("state", circuitBreaker.getState().toString());
        metricsMap.put("failureRate", metrics.getFailureRate());
        metricsMap.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
        metricsMap.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
        metricsMap.put("numberOfBufferedCalls", metrics.getNumberOfBufferedCalls());
        metricsMap.put("numberOfNotAllowedCalls", metrics.getNumberOfNotPermittedCalls());
        metricsMap.put("numberOfSlowCalls", metrics.getNumberOfSlowCalls());

        return ResponseEntity.ok(metricsMap);
    }
}
