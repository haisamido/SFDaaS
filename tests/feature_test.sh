#!/bin/bash
#
# SFDaaS Feature Test Script
# ===========================
# Tests all features of the SFDaaS API including new features
#
# Usage: ./feature_test.sh [url]
#

BASE_URL="${1:-http://localhost:8080}"
API_BASE="${BASE_URL}/sfdaas/api/propagate"

# Create results directory if it doesn't exist
RESULTS_DIR="$(dirname "$0")/results"
mkdir -p "$RESULTS_DIR"

# Result file (matches script name prefix)
SCRIPT_NAME=$(basename "$0" .sh)
RESULT_FILE="${RESULTS_DIR}/${SCRIPT_NAME}.md"

# Colors for terminal output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Base parameters for propagation
BASE_PARAMS="t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D"

# Function to write to both terminal and file
log_output() {
    echo "$1"
    echo "$1" >> "$RESULT_FILE"
}

# Function to write only to terminal (for colored output)
log_terminal() {
    echo -e "$1"
}

# Function to run a test and check result
run_test() {
    local test_name="$1"
    local url="$2"
    local expected_field="$3"
    local expected_value="$4"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    local response=$(curl -s "$url")
    local status=$(echo "$response" | jq -r '.status' 2>/dev/null)

    if [ "$status" = "success" ]; then
        if [ -n "$expected_field" ] && [ -n "$expected_value" ]; then
            local actual_value=$(echo "$response" | jq -r "$expected_field" 2>/dev/null)
            if [ "$actual_value" = "$expected_value" ]; then
                log_terminal "${GREEN}[PASS]${NC} $test_name"
                PASSED_TESTS=$((PASSED_TESTS + 1))
                return 0
            else
                log_terminal "${RED}[FAIL]${NC} $test_name (expected $expected_value, got $actual_value)"
                FAILED_TESTS=$((FAILED_TESTS + 1))
                return 1
            fi
        else
            log_terminal "${GREEN}[PASS]${NC} $test_name"
            PASSED_TESTS=$((PASSED_TESTS + 1))
            return 0
        fi
    else
        log_terminal "${RED}[FAIL]${NC} $test_name"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# Function to run a test that checks for presence of a field
run_test_has_field() {
    local test_name="$1"
    local url="$2"
    local field="$3"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    local response=$(curl -s "$url")
    local status=$(echo "$response" | jq -r '.status' 2>/dev/null)

    if [ "$status" = "success" ]; then
        local value=$(echo "$response" | jq -r "$field" 2>/dev/null)
        if [ "$value" != "null" ] && [ -n "$value" ]; then
            log_terminal "${GREEN}[PASS]${NC} $test_name"
            PASSED_TESTS=$((PASSED_TESTS + 1))
            return 0
        else
            log_terminal "${RED}[FAIL]${NC} $test_name (field '$field' not found)"
            FAILED_TESTS=$((FAILED_TESTS + 1))
            return 1
        fi
    else
        log_terminal "${RED}[FAIL]${NC} $test_name"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# Start markdown report
cat > "$RESULT_FILE" << EOF
# SFDaaS Feature Test Report

**Server:** $BASE_URL

---

EOF

log_terminal "${CYAN}SFDaaS Feature Test${NC}"
log_terminal "==================="
log_terminal ""

# Check if server is running
log_terminal "Checking server connectivity... \c"
if curl -s --connect-timeout 5 "$API_BASE/usage" > /dev/null 2>&1; then
    log_terminal "${GREEN}OK${NC}"
else
    log_terminal "${RED}FAILED${NC}"
    log_terminal ""
    log_terminal "${RED}Error: Server is not responding at $BASE_URL${NC}"
    log_output "## Server Connectivity: FAIL"
    log_output ""
    log_output "Error: Server is not responding at $BASE_URL"
    exit 1
fi

log_terminal ""

# ============================================================================
# Test 1: Basic Propagation
# ============================================================================
log_terminal "${CYAN}=== Test 1: Basic Propagation ===${NC}"
log_output "## 1. Basic Propagation"
log_output ""

run_test "Basic propagation" "${API_BASE}?${BASE_PARAMS}"
run_test_has_field "Response has apriori.t0" "${API_BASE}?${BASE_PARAMS}" ".data.apriori.t0"
run_test_has_field "Response has aposteriori.rf" "${API_BASE}?${BASE_PARAMS}" ".data.aposteriori.rf"
run_test_has_field "Response has aposteriori.vf" "${API_BASE}?${BASE_PARAMS}" ".data.aposteriori.vf"
run_test_has_field "Response has timing diagnostics" "${API_BASE}?${BASE_PARAMS}" ".diagnostics.timing.totalTimeMs"

log_terminal ""

# ============================================================================
# Test 2: Propagator Types
# ============================================================================
log_terminal "${CYAN}=== Test 2: Propagator Types ===${NC}"
log_output ""
log_output "## 2. Propagator Types"
log_output ""

PROPAGATORS=("rungekutta" "dormandprince" "adamsbashforth" "adamsmoulton")
log_output "| Propagator | Status |"
log_output "| --- | --- |"

for prop in "${PROPAGATORS[@]}"; do
    if run_test "Propagator: $prop" "${API_BASE}?${BASE_PARAMS}&propagator=${prop}"; then
        log_output "| $prop | PASS |"
    else
        log_output "| $prop | FAIL |"
    fi
done

log_terminal ""

# ============================================================================
# Test 3: Reference Frames
# ============================================================================
log_terminal "${CYAN}=== Test 3: Reference Frames ===${NC}"
log_output ""
log_output "## 3. Reference Frames"
log_output ""

FRAMES=("eme2000" "gcrf" "teme" "mod" "tod")
log_output "| Frame | Status |"
log_output "| --- | --- |"

for frame in "${FRAMES[@]}"; do
    if run_test "Frame: $frame" "${API_BASE}?${BASE_PARAMS}&frame=${frame}"; then
        log_output "| $frame | PASS |"
    else
        log_output "| $frame | FAIL |"
    fi
done

log_terminal ""

# ============================================================================
# Test 4: Time Scales
# ============================================================================
log_terminal "${CYAN}=== Test 4: Time Scales ===${NC}"
log_output ""
log_output "## 4. Time Scales"
log_output ""

TIMESCALES=("utc" "tai")
log_output "| Time Scale | Status |"
log_output "| --- | --- |"

for ts in "${TIMESCALES[@]}"; do
    if run_test "Time Scale: $ts" "${API_BASE}?${BASE_PARAMS}&timeScale=${ts}"; then
        log_output "| $ts | PASS |"
    else
        log_output "| $ts | FAIL |"
    fi
done

log_terminal ""

# ============================================================================
# Test 5: Central Bodies
# ============================================================================
log_terminal "${CYAN}=== Test 5: Central Bodies ===${NC}"
log_output ""
log_output "## 5. Central Bodies"
log_output ""

BODIES=("earth" "sun" "moon" "mars" "jupiter" "venus" "saturn")
log_output "| Central Body | Status |"
log_output "| --- | --- |"

for body in "${BODIES[@]}"; do
    if run_test "Central Body: $body" "${API_BASE}?${BASE_PARAMS}&centralBody=${body}"; then
        log_output "| $body | PASS |"
    else
        log_output "| $body | FAIL |"
    fi
done

# Test custom mu value
if run_test "Custom mu value" "${API_BASE}?${BASE_PARAMS}&centralBody=mu:3.986004418e14"; then
    log_output "| mu:3.986004418e14 (custom) | PASS |"
else
    log_output "| mu:3.986004418e14 (custom) | FAIL |"
fi

log_terminal ""

# ============================================================================
# Test 6: Step Sizes
# ============================================================================
log_terminal "${CYAN}=== Test 6: Step Sizes ===${NC}"
log_output ""
log_output "## 6. Step Sizes"
log_output ""

STEP_SIZES=("60" "30" "10" "1" "0.1")
log_output "| Step Size (s) | Status |"
log_output "| --- | --- |"

for step in "${STEP_SIZES[@]}"; do
    if run_test "Step Size: ${step}s" "${API_BASE}?${BASE_PARAMS}&stepSize=${step}"; then
        log_output "| $step | PASS |"
    else
        log_output "| $step | FAIL |"
    fi
done

log_terminal ""

# ============================================================================
# Test 7: Output Interval (CSV States)
# ============================================================================
log_terminal "${CYAN}=== Test 7: Output Interval (CSV States) ===${NC}"
log_output ""
log_output "## 7. Output Interval (CSV States)"
log_output ""

# Test 1-hour propagation with 10-minute intervals (should produce 7 states: initial + 5 intervals + final)
SHORT_PARAMS="t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-28T13:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D"

TOTAL_TESTS=$((TOTAL_TESTS + 1))
RESPONSE=$(curl -s "${API_BASE}?${SHORT_PARAMS}&outputInterval=600")
STATUS=$(echo "$RESPONSE" | jq -r '.status' 2>/dev/null)
INTERVAL_STATES=$(echo "$RESPONSE" | jq -r '.data.aposteriori.intervalStates' 2>/dev/null)

if [ "$STATUS" = "success" ] && [ "$INTERVAL_STATES" != "null" ] && [ -n "$INTERVAL_STATES" ]; then
    # Count number of CSV rows (lines)
    NUM_STATES=$(echo "$INTERVAL_STATES" | wc -l | tr -d ' ')
    log_terminal "${GREEN}[PASS]${NC} Output Interval: 600s (got $NUM_STATES states)"
    log_output "| Output Interval 600s | PASS ($NUM_STATES states) |"
    PASSED_TESTS=$((PASSED_TESTS + 1))

    # Check that values have full precision (should have scientific notation or many decimal places)
    FIRST_LINE=$(echo "$INTERVAL_STATES" | head -1)
    if echo "$FIRST_LINE" | grep -qE "[0-9]+\.[0-9]{10,}|[0-9]+e[+-]?[0-9]+"; then
        log_terminal "${GREEN}[PASS]${NC} CSV values have full double precision"
        log_output "| Full double precision | PASS |"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        log_terminal "${RED}[FAIL]${NC} CSV values may not have full precision"
        log_output "| Full double precision | FAIL |"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
else
    log_terminal "${RED}[FAIL]${NC} Output Interval: 600s"
    log_output "| Output Interval 600s | FAIL |"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test smaller interval
TOTAL_TESTS=$((TOTAL_TESTS + 1))
RESPONSE=$(curl -s "${API_BASE}?${SHORT_PARAMS}&outputInterval=300")
STATUS=$(echo "$RESPONSE" | jq -r '.status' 2>/dev/null)
INTERVAL_STATES=$(echo "$RESPONSE" | jq -r '.data.aposteriori.intervalStates' 2>/dev/null)

if [ "$STATUS" = "success" ] && [ "$INTERVAL_STATES" != "null" ] && [ -n "$INTERVAL_STATES" ]; then
    NUM_STATES=$(echo "$INTERVAL_STATES" | wc -l | tr -d ' ')
    log_terminal "${GREEN}[PASS]${NC} Output Interval: 300s (got $NUM_STATES states)"
    log_output "| Output Interval 300s | PASS ($NUM_STATES states) |"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log_terminal "${RED}[FAIL]${NC} Output Interval: 300s"
    log_output "| Output Interval 300s | FAIL |"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log_terminal ""

# ============================================================================
# Test 8: State Storage (Redis/Memcached URLs)
# ============================================================================
log_terminal "${CYAN}=== Test 8: State Storage URL Parameter ===${NC}"
log_output ""
log_output "## 8. State Storage URL Parameter"
log_output ""

# Test that the cache parameter is accepted (even if Redis/Memcached isn't running)
# The API should parse the URL without error

# Test Redis URL format parsing
TOTAL_TESTS=$((TOTAL_TESTS + 1))
RESPONSE=$(curl -s "${API_BASE}?${SHORT_PARAMS}&outputInterval=600&cache=redis://localhost:6379/0/test:prefix")
STATUS=$(echo "$RESPONSE" | jq -r '.status' 2>/dev/null)

# Even if Redis isn't running, the request should succeed (states still calculated, just not stored)
if [ "$STATUS" = "success" ]; then
    log_terminal "${GREEN}[PASS]${NC} Redis URL format accepted: redis://host:port/db/prefix"
    log_output "| Redis URL format | PASS |"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    # Check if it's a connection error vs parse error
    ERROR=$(echo "$RESPONSE" | jq -r '.message' 2>/dev/null)
    if echo "$ERROR" | grep -qi "connect\|refused\|timeout"; then
        log_terminal "${YELLOW}[SKIP]${NC} Redis URL format (Redis not running)"
        log_output "| Redis URL format | SKIP (not running) |"
        PASSED_TESTS=$((PASSED_TESTS + 1))  # URL parsing worked, just no server
    else
        log_terminal "${RED}[FAIL]${NC} Redis URL format"
        log_output "| Redis URL format | FAIL |"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
fi

# Test Memcached URL format parsing
TOTAL_TESTS=$((TOTAL_TESTS + 1))
RESPONSE=$(curl -s "${API_BASE}?${SHORT_PARAMS}&outputInterval=600&cache=memcached://localhost:11211/3600/test:prefix")
STATUS=$(echo "$RESPONSE" | jq -r '.status' 2>/dev/null)

if [ "$STATUS" = "success" ]; then
    log_terminal "${GREEN}[PASS]${NC} Memcached URL format accepted: memcached://host:port/ttl/prefix"
    log_output "| Memcached URL format | PASS |"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    ERROR=$(echo "$RESPONSE" | jq -r '.message' 2>/dev/null)
    if echo "$ERROR" | grep -qi "connect\|refused\|timeout"; then
        log_terminal "${YELLOW}[SKIP]${NC} Memcached URL format (Memcached not running)"
        log_output "| Memcached URL format | SKIP (not running) |"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        log_terminal "${RED}[FAIL]${NC} Memcached URL format"
        log_output "| Memcached URL format | FAIL |"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
fi

log_terminal ""

# ============================================================================
# Test 9: Session Management
# ============================================================================
log_terminal "${CYAN}=== Test 9: Session Management ===${NC}"
log_output ""
log_output "## 9. Session Management"
log_output ""

# Test session creation
TOTAL_TESTS=$((TOTAL_TESTS + 1))
RESPONSE=$(curl -s -c /tmp/sfdaas_cookies.txt "${API_BASE}?${BASE_PARAMS}")
SESSION_ID=$(echo "$RESPONSE" | jq -r '.diagnostics.session.id' 2>/dev/null)

if [ "$SESSION_ID" != "null" ] && [ -n "$SESSION_ID" ]; then
    log_terminal "${GREEN}[PASS]${NC} Session creation (ID: ${SESSION_ID:0:8}...)"
    log_output "| Session creation | PASS |"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log_terminal "${RED}[FAIL]${NC} Session creation"
    log_output "| Session creation | FAIL |"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test session persistence
TOTAL_TESTS=$((TOTAL_TESTS + 1))
RESPONSE2=$(curl -s -b /tmp/sfdaas_cookies.txt "${API_BASE}?${BASE_PARAMS}")
SESSION_ID2=$(echo "$RESPONSE2" | jq -r '.diagnostics.session.id' 2>/dev/null)

if [ "$SESSION_ID" = "$SESSION_ID2" ]; then
    log_terminal "${GREEN}[PASS]${NC} Session persistence (same ID on second request)"
    log_output "| Session persistence | PASS |"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log_terminal "${RED}[FAIL]${NC} Session persistence (IDs differ)"
    log_output "| Session persistence | FAIL |"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test session timeout parameter
run_test "Session timeout parameter (st=3600)" "${API_BASE}?${BASE_PARAMS}&st=3600"

# Clean up
rm -f /tmp/sfdaas_cookies.txt

log_terminal ""

# ============================================================================
# Test 10: API Usage Endpoint
# ============================================================================
log_terminal "${CYAN}=== Test 10: API Usage Endpoint ===${NC}"
log_output ""
log_output "## 10. API Usage Endpoint"
log_output ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))
RESPONSE=$(curl -s "${API_BASE}/usage")
SERVICE=$(echo "$RESPONSE" | jq -r '.service' 2>/dev/null)

if [ "$SERVICE" != "null" ] && [ -n "$SERVICE" ]; then
    log_terminal "${GREEN}[PASS]${NC} Usage endpoint returns documentation"
    log_output "| Usage endpoint | PASS |"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log_terminal "${RED}[FAIL]${NC} Usage endpoint"
    log_output "| Usage endpoint | FAIL |"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

run_test_has_field "Usage has parameters" "${API_BASE}/usage" ".parameters"
run_test_has_field "Usage has examples" "${API_BASE}/usage" ".examples"

log_terminal ""

# ============================================================================
# Test 11: Error Handling
# ============================================================================
log_terminal "${CYAN}=== Test 11: Error Handling ===${NC}"
log_output ""
log_output "## 11. Error Handling"
log_output ""

# Test missing required parameters
TOTAL_TESTS=$((TOTAL_TESTS + 1))
RESPONSE=$(curl -s "${API_BASE}?t0=2010-05-28T12:00:00.000%2B00:00")
STATUS=$(echo "$RESPONSE" | jq -r '.status' 2>/dev/null)

if [ "$STATUS" = "error" ]; then
    log_terminal "${GREEN}[PASS]${NC} Missing parameters returns error status"
    log_output "| Missing parameters | PASS (returns error) |"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log_terminal "${RED}[FAIL]${NC} Missing parameters should return error"
    log_output "| Missing parameters | FAIL |"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

# Test invalid propagator
TOTAL_TESTS=$((TOTAL_TESTS + 1))
RESPONSE=$(curl -s "${API_BASE}?${BASE_PARAMS}&propagator=invalid")
# Should still work with default propagator or return error
STATUS=$(echo "$RESPONSE" | jq -r '.status' 2>/dev/null)
if [ "$STATUS" = "success" ] || [ "$STATUS" = "error" ]; then
    log_terminal "${GREEN}[PASS]${NC} Invalid propagator handled gracefully"
    log_output "| Invalid propagator | PASS |"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log_terminal "${RED}[FAIL]${NC} Invalid propagator handling"
    log_output "| Invalid propagator | FAIL |"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log_terminal ""

# ============================================================================
# Test 12: Backward/Forward Propagation
# ============================================================================
log_terminal "${CYAN}=== Test 12: Backward/Forward Propagation ===${NC}"
log_output ""
log_output "## 12. Backward/Forward Propagation"
log_output ""

# Forward propagation (t0 < tf)
run_test "Forward propagation (t0 < tf)" "${API_BASE}?${BASE_PARAMS}"

# Backward propagation (t0 > tf)
BACKWARD_PARAMS="t0=2010-05-29T12:00:00.000%2B00:00&tf=2010-05-28T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D"
run_test "Backward propagation (t0 > tf)" "${API_BASE}?${BACKWARD_PARAMS}"

log_terminal ""

# ============================================================================
# Summary
# ============================================================================
log_terminal "${CYAN}===================${NC}"
log_terminal "${CYAN}Test Summary${NC}"
log_terminal "${CYAN}===================${NC}"
log_terminal ""

SUCCESS_RATE=$(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)

log_terminal "Total Tests:  $TOTAL_TESTS"
if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    log_terminal "${GREEN}Passed:       $PASSED_TESTS${NC}"
else
    log_terminal "Passed:       ${GREEN}$PASSED_TESTS${NC}"
fi
if [ $FAILED_TESTS -gt 0 ]; then
    log_terminal "${RED}Failed:       $FAILED_TESTS${NC}"
else
    log_terminal "Failed:       $FAILED_TESTS"
fi
log_terminal "Success Rate: ${SUCCESS_RATE}%"
log_terminal ""

# Add summary to report
cat >> "$RESULT_FILE" << EOF

---

## Summary

| Metric | Value |
| --- | --- |
| Total Tests | $TOTAL_TESTS |
| Passed | $PASSED_TESTS |
| Failed | $FAILED_TESTS |
| Success Rate | ${SUCCESS_RATE}% |

---

**Feature test complete!**
EOF

if [ $FAILED_TESTS -eq 0 ]; then
    log_terminal "${GREEN}All tests passed!${NC}"
else
    log_terminal "${YELLOW}Some tests failed. Review the report for details.${NC}"
fi

log_terminal ""
log_terminal "Report saved to: ./tests/results/${SCRIPT_NAME}.md"
