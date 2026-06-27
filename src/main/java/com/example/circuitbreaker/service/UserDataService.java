package com.example.circuitbreaker.service;

import com.example.circuitbreaker.dto.UserDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserDataService {
    private static final Logger logger = LoggerFactory.getLogger(UserDataService.class);

    @Autowired
    private RestTemplate restTemplate;

    private final String externalServiceUrl = "http://localhost:8080/api/external/users/";

    @CircuitBreaker(name = "userService", fallbackMethod = "getFallbackUserData")
    public UserDTO fetchUser(String id) {
        logger.info("Calling external user service for id: {}", id);
        return restTemplate.getForObject(externalServiceUrl + id, UserDTO.class);
    }

    public UserDTO getFallbackUserData(String id, Throwable throwable) {
        logger.warn("Circuit breaker fallback triggered for user id {}. Reason: {}", id, throwable.toString());
        return new UserDTO("default-id", "Default User", "default@example.com");
    }
}
