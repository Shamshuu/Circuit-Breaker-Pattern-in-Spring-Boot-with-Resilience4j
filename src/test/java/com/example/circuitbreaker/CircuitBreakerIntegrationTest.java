package com.example.circuitbreaker;

import com.example.circuitbreaker.dto.UserDTO;
import com.example.circuitbreaker.service.UserDataService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CircuitBreakerIntegrationTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    public void setUp() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
        circuitBreaker.reset();
    }

    @Test
    public void testSuccessfulExternalCall() {
        UserDTO mockUser = new UserDTO("1", "John Doe", "john@example.com");
        Mockito.when(restTemplate.getForObject(anyString(), eq(UserDTO.class)))
                .thenReturn(mockUser);

        UserDTO result = userDataService.fetchUser("1");

        assertThat(result.getId()).isEqualTo("1");
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    public void testFailingCallTriggersFallbackAndTripsCircuit() {
        // Configure mock RestTemplate to throw an exception
        Mockito.when(restTemplate.getForObject(anyString(), eq(UserDTO.class)))
                .thenThrow(new RestClientException("Connection timed out"));

        // Make calls to trigger fallback and trip circuit.
        // slidingWindowSize: 10, minimumNumberOfCalls: 5, failureRateThreshold: 50%
        // Let's call 5 times (minimumNumberOfCalls). All will fail, so failure rate is 100% (> 50%).
        for (int i = 0; i < 5; i++) {
            UserDTO fallbackUser = userDataService.fetchUser("1");
            assertThat(fallbackUser.getId()).isEqualTo("default-id");
            assertThat(fallbackUser.getName()).isEqualTo("Default User");
            assertThat(fallbackUser.getEmail()).isEqualTo("default@example.com");
        }

        // The circuit breaker should now be OPEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // A call now shouldn't even reach the REST template because circuit is open
        Mockito.reset(restTemplate);
        UserDTO fallbackUser = userDataService.fetchUser("1");
        assertThat(fallbackUser.getId()).isEqualTo("default-id");
        Mockito.verifyNoInteractions(restTemplate);
    }
}
