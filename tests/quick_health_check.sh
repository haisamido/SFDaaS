#!/bin/bash
#
# Quick Health Check for SFDaaS
# ==============================
# Performs a quick health check to verify SFDaaS is running correctly
#
# Usage: ./quick_health_check.sh [url]
#

BASE_URL="${1:-http://localhost:8080}"
API_BASE="${BASE_URL}/SFDaaS/orekit/propagate"

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

# Function to write to both terminal and file
log_output() {
    echo "$1"
    echo "$1" >> "$RESULT_FILE"
}

# Function to write only to terminal (for colored output)
log_terminal() {
    echo -e "$1"
}

# Start markdown report
cat > "$RESULT_FILE" << EOF
# SFDaaS Health Check Report

**Server:** $BASE_URL

---

EOF

log_terminal "${CYAN}SFDaaS Health Check${NC}"
log_terminal "==================="
log_terminal ""

# Check if server is running
log_terminal "Checking server connectivity... \c"
if curl -s --connect-timeout 5 "$API_BASE/usage" > /dev/null 2>&1; then
    log_terminal "${GREEN}✓${NC}"
    log_output "## Server Connectivity: ✓ PASS"
else
    log_terminal "${RED}✗${NC}"
    log_output "## Server Connectivity: ✗ FAIL"
    log_terminal ""
    log_terminal "${RED}Error: Server is not responding at $BASE_URL${NC}"
    log_output ""
    log_output "Error: Server is not responding at $BASE_URL"
    log_output ""
    log_output "Please ensure SFDaaS is running: \`task run\`"
    exit 1
fi
log_output ""

# Test basic propagation
log_terminal "Testing basic propagation... \c"
RESPONSE=$(curl -s "${API_BASE}?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D")
STATUS=$(echo "$RESPONSE" | jq -r '.status' 2>/dev/null)

if [ "$STATUS" = "success" ]; then
    log_terminal "${GREEN}✓${NC}"

    PROP_TIME=$(echo "$RESPONSE" | jq -r '.diagnostics.timing.propagationTimeMs')
    TOTAL_TIME=$(echo "$RESPONSE" | jq -r '.diagnostics.timing.totalTimeMs')

    log_terminal "  Propagation Time: ${PROP_TIME}ms"
    log_terminal "  Total Time: ${TOTAL_TIME}ms"

    log_output "## Basic Propagation: ✓ PASS"
    log_output ""
    log_output "- Propagation Time: ${PROP_TIME}ms"
    log_output "- Total Time: ${TOTAL_TIME}ms"
else
    log_terminal "${RED}✗${NC}"
    log_output "## Basic Propagation: ✗ FAIL"
    log_terminal ""
    log_terminal "Propagation test failed"
    exit 1
fi

log_output ""
log_terminal ""
log_terminal "==================="
log_terminal ""

# Test all propagators and build markdown table
log_output "## Propagators"
log_output ""
PROPAGATORS=("rungekutta" "dormandprince" "adamsbashforth" "adamsmoulton")

# Build table header
TABLE_HEADER="| Propagator "
for prop in "${PROPAGATORS[@]}"; do
    TABLE_HEADER+="| $prop "
done
TABLE_HEADER+="|"
log_output "$TABLE_HEADER"

# Build separator
TABLE_SEP="| --- "
for prop in "${PROPAGATORS[@]}"; do
    TABLE_SEP+="| --- "
done
TABLE_SEP+="|"
log_output "$TABLE_SEP"

# Build data row with status
STATUS_ROW="| Status "
for prop in "${PROPAGATORS[@]}"; do
    RESPONSE=$(curl -s "${API_BASE}?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D&propagator=${prop}")
    STATUS=$(echo "$RESPONSE" | jq -r '.status' 2>/dev/null)

    if [ "$STATUS" = "success" ]; then
        STATUS_ROW+="| ✓ "
    else
        STATUS_ROW+="| ✗ "
    fi
done
STATUS_ROW+="|"
log_output "$STATUS_ROW"

# Build data row with timing
TIME_ROW="| Time (ms) "
for prop in "${PROPAGATORS[@]}"; do
    RESPONSE=$(curl -s "${API_BASE}?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D&propagator=${prop}")
    STATUS=$(echo "$RESPONSE" | jq -r '.status' 2>/dev/null)

    if [ "$STATUS" = "success" ]; then
        TIME=$(echo "$RESPONSE" | jq -r '.diagnostics.timing.propagationTimeMs')
        TIME_ROW+="| $TIME "
    else
        TIME_ROW+="| - "
    fi
done
TIME_ROW+="|"
log_output "$TIME_ROW"

log_output ""

# Test reference frames and build markdown table
log_output "## Reference Frames"
log_output ""
FRAMES=("eme2000" "gcrf" "teme" "mod" "tod")

# Build table header
FRAME_HEADER="| Frame "
for frame in "${FRAMES[@]}"; do
    FRAME_HEADER+="| $frame "
done
FRAME_HEADER+="|"
log_output "$FRAME_HEADER"

# Build separator
FRAME_SEP="| --- "
for frame in "${FRAMES[@]}"; do
    FRAME_SEP+="| --- "
done
FRAME_SEP+="|"
log_output "$FRAME_SEP"

# Build data row with status
FRAME_ROW="| Status "
for frame in "${FRAMES[@]}"; do
    RESPONSE=$(curl -s "${API_BASE}?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D&frame=${frame}")
    STATUS=$(echo "$RESPONSE" | jq -r '.status' 2>/dev/null)

    if [ "$STATUS" = "success" ]; then
        FRAME_ROW+="| ✓ "
    else
        FRAME_ROW+="| ✗ "
    fi
done
FRAME_ROW+="|"
log_output "$FRAME_ROW"

log_output ""

# Test time scales
log_output "## Time Scales"
log_output ""
TIMESCALES=("utc" "tai")

# Build table header
TS_HEADER="| Time Scale "
for ts in "${TIMESCALES[@]}"; do
    TS_HEADER+="| $ts "
done
TS_HEADER+="|"
log_output "$TS_HEADER"

# Build separator
TS_SEP="| --- "
for ts in "${TIMESCALES[@]}"; do
    TS_SEP+="| --- "
done
TS_SEP+="|"
log_output "$TS_SEP"

# Build data row with status
TS_ROW="| Status "
for ts in "${TIMESCALES[@]}"; do
    RESPONSE=$(curl -s "${API_BASE}?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D&timeScale=${ts}")
    STATUS=$(echo "$RESPONSE" | jq -r '.status' 2>/dev/null)

    if [ "$STATUS" = "success" ]; then
        TS_ROW+="| ✓ "
    else
        TS_ROW+="| ✗ "
    fi
done
TS_ROW+="|"
log_output "$TS_ROW"

log_output ""
log_output "---"
log_output ""
log_output "**Health check complete!**"

log_terminal ""
log_terminal "==================="
log_terminal "${GREEN}Health check complete!${NC}"
log_terminal ""
log_terminal "Report saved to: ${RESULT_FILE}"
