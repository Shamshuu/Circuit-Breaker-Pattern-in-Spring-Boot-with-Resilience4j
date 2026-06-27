package com.example.circuitbreaker.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakerLogListener {
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerLogListener.class);

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @PostConstruct
    public void registerListeners() {
        logger.info("Registering state transition event listeners for circuit breakers...");
        
        // Register listener for the default instance
        circuitBreakerRegistry.circuitBreaker("userService")
                .getEventPublisher()
                .onStateTransition(event -> {
                    logger.info("CircuitBreaker '{}' changed state from {} to {}",
                            event.getCircuitBreakerName(),
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState());
                });

        // Registry-wide listener for dynamically created circuit breakers
        circuitBreakerRegistry.getEventPublisher()
                .onEntryAdded(entryAddedEvent -> {
                    String name = entryAddedEvent.getAddedEntry().getName();
                    logger.info("Circuit breaker added to registry: {}", name);
                    entryAddedEvent.getAddedEntry().getEventPublisher()
                            .onStateTransition(event -> {
                                logger.info("CircuitBreaker '{}' changed state from {} to {}",
                                        event.getCircuitBreakerName(),
                                        event.getStateTransition().getFromState(),
                                        event.getStateTransition().getToState());
                            });
                });
    }
}
