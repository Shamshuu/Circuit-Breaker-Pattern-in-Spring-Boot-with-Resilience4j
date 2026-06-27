# Circuit Breaker Pattern in Spring Boot with Resilience4j

This project demonstrates the implementation of the **Circuit Breaker** pattern in a Spring Boot application using **Resilience4j**. It simulates an unreliable external dependency and shows how a circuit breaker protects the application from cascading failures, provides fallback responses, logs state transitions, and exposes key metrics.

## Features

- **Spring Boot 3** & **Java 21 / 23**
- **Resilience4j Circuit Breaker**: Configured with a count-based sliding window.
- **Actuator Integration**: Exposes detailed metrics at `/actuator/circuitbreakers`.
- **Custom Endpoints**:
  - `GET /api/users/{id}`: Returns user data (or a fallback when the circuit is open).
  - `GET /api/circuit-breaker/state`: Programmatically checks the current state (`CLOSED`, `OPEN`, `HALF_OPEN`).
  - `GET /api/circuit-breaker/metrics`: Programmatically exposes key metrics.
- **Log Transition Listener**: Logs state changes of the circuit breaker to the console.
- **Mock Service Failures**: Supports failure injection/toggling at `/api/external/toggle` for deterministic testing.

---

## Build Instructions

To clean and compile the project, run:

```bash
mvn clean package
```

This will run the integration tests and package the application into a JAR file inside the `target` directory.

---

## Run Instructions

To run the Spring Boot application, you can use the Maven plugin:

```bash
mvn spring-boot:run
```

Alternatively, you can run the built JAR file directly:

```bash
java -jar target/circuit-breaker-demo-0.0.1-SNAPSHOT.jar
```

The application starts on port `8080`.

---

## Testing Instructions

We have provided a bash script `test-circuit-breaker.sh` in the root directory to verify the circuit breaker lifecycle.

### Running the Test Script

With the Spring Boot application running, run the following command in a Bash shell (such as Git Bash on Windows, WSL, or a Linux terminal):

```bash
./test-circuit-breaker.sh
```

### What to Expect

The test script will perform the following steps:
1. **Initial state check**: Queries `/api/circuit-breaker/state`. The expected output is `CLOSED`.
2. **Healthy request**: Sends a GET request to `/api/users/1`. The mock service returns a healthy response:
   ```json
   {
     "id": "1",
     "name": "External User 1",
     "email": "user1@external.com"
   }
   ```
3. **Fail toggle**: Toggles the mock service to fail.
4. **Triggers circuit open**: Makes 5 failing requests (matching `minimum-number-of-calls: 5`). Each returns the fallback user object:
   ```json
   {
     "id": "default-id",
     "name": "Default User",
     "email": "default@example.com"
   }
   ```
5. **State check**: Verifies the state has transitioned to `OPEN`.
6. **Wait period**: Waits 11 seconds for the open state duration (`wait-duration-in-open-state: 10s`) to expire.
7. **Half-Open state**: Toggles the mock service to succeed again and makes a request. The breaker enters the `HALF_OPEN` state.
8. **Recovers to Closed**: Makes a second request (matching `permitted-number-of-calls-in-half-open-state: 2`). The breaker transitions back to `CLOSED`.
9. **Log Verification**: Check the application console logs for state transition messages:
   ```text
   CircuitBreaker 'userService' changed state from CLOSED to OPEN
   CircuitBreaker 'userService' changed state from OPEN to HALF_OPEN
   CircuitBreaker 'userService' changed state from HALF_OPEN to CLOSED
   ```