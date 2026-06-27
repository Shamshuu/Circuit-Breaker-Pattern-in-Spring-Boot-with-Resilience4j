#!/bin/bash

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Starting Circuit Breaker Demonstration ===${NC}"
echo -e "${YELLOW}Initial State Verification:${NC}"

# Check current state (should be CLOSED)
STATE=$(curl -s http://localhost:8080/api/circuit-breaker/state)
echo -e "Current Circuit Breaker State: ${GREEN}$STATE${NC}"

# Ensure mock external service is in SUCCESS state (fail=false)
echo -e "\n${YELLOW}Setting mock external service to SUCCESS state...${NC}"
curl -s "http://localhost:8080/api/external/toggle?fail=false"
echo ""

# Call user data endpoint
echo -e "${YELLOW}Making a request to /api/users/1 (Expect success):${NC}"
curl -i -s http://localhost:8080/api/users/1
echo ""

# Toggle mock external service to FAIL state
echo -e "\n${RED}Toggling mock external service to FAIL state...${NC}"
curl -s "http://localhost:8080/api/external/toggle?fail=true"
echo ""

# Make 5 failing requests to trip the circuit (minimum-number-of-calls is 5)
echo -e "\n${YELLOW}Making 5 requests to trip the circuit breaker...${NC}"
for i in {1..5}
do
   echo -e "Request #$i:"
   curl -s http://localhost:8080/api/users/1
   echo ""
done

# Check current state (should be OPEN)
echo -e "\n${YELLOW}Verifying Circuit Breaker state (Expect OPEN):${NC}"
STATE=$(curl -s http://localhost:8080/api/circuit-breaker/state)
echo -e "Current Circuit Breaker State: ${RED}$STATE${NC}"

# Call user data endpoint while circuit is open (should return fallback)
echo -e "\n${YELLOW}Making a request while OPEN (Expect fallback response immediately):${NC}"
curl -i -s http://localhost:8080/api/users/1
echo ""

# Wait for the wait-duration-in-open-state (10s)
echo -e "\n${YELLOW}Waiting 11 seconds for open state duration to expire...${NC}"
sleep 11

# The circuit should transition to HALF_OPEN when a request is made.
# Let's verify state (should still be OPEN or transition to HALF_OPEN)
echo -e "\n${YELLOW}Toggling mock external service back to SUCCESS state...${NC}"
curl -s "http://localhost:8080/api/external/toggle?fail=false"
echo ""

# Making first request in HALF_OPEN (permitted-number-of-calls-in-half-open-state: 2)
echo -e "\n${YELLOW}Making first request after wait duration (transitions to HALF_OPEN):${NC}"
curl -s http://localhost:8080/api/users/1
echo ""

STATE=$(curl -s http://localhost:8080/api/circuit-breaker/state)
echo -e "Current Circuit Breaker State: ${YELLOW}$STATE${NC}"

# Making second request in HALF_OPEN (completing the 2 permitted calls)
echo -e "\n${YELLOW}Making second request in HALF_OPEN:${NC}"
curl -s http://localhost:8080/api/users/1
echo ""

# The circuit breaker should transition back to CLOSED
echo -e "\n${YELLOW}Verifying final Circuit Breaker state (Expect CLOSED):${NC}"
STATE=$(curl -s http://localhost:8080/api/circuit-breaker/state)
echo -e "Current Circuit Breaker State: ${GREEN}$STATE${NC}"

# Check metrics
echo -e "\n${YELLOW}Fetching final metrics:${NC}"
curl -s http://localhost:8080/api/circuit-breaker/metrics
echo -e "\n"

echo -e "${GREEN}=== Circuit Breaker Demonstration Completed Successfully ===${NC}"
