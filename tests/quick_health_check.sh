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

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}SFDaaS Health Check${NC}"
echo "==================="
echo ""

# Check if server is running
echo -n "Checking server connectivity... "
if curl -s --connect-timeout 5 "$API_BASE/usage" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗${NC}"
    echo ""
    echo -e "${RED}Error: Server is not responding at $BASE_URL${NC}"
    echo ""
    echo "Please ensure SFDaaS is running:"
    echo "  task run"
    exit 1
fi

# Test basic propagation
echo -n "Testing basic propagation... "
RESPONSE=$(curl -s "${API_BASE}?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D")
STATUS=$(echo "$RESPONSE" | jq -r '.status' 2>/dev/null)

if [ "$STATUS" = "success" ]; then
    echo -e "${GREEN}✓${NC}"

    PROP_TIME=$(echo "$RESPONSE" | jq -r '.diagnostics.timing.propagationTimeMs')
    TOTAL_TIME=$(echo "$RESPONSE" | jq -r '.diagnostics.timing.totalTimeMs')

    echo "  Propagation Time: ${PROP_TIME}ms"
    echo "  Total Time: ${TOTAL_TIME}ms"
else
    echo -e "${RED}✗${NC}"
    echo ""
    echo "Propagation test failed"
    exit 1
fi

echo ""

# Test all propagators
echo "Testing all propagators:"
PROPAGATORS=("rungekutta" "dormandprince" "adamsbashforth" "adamsmoulton")

for prop in "${PROPAGATORS[@]}"; do
    echo -n "  $prop... "
    RESPONSE=$(curl -s "${API_BASE}?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D&propagator=${prop}")
    STATUS=$(echo "$RESPONSE" | jq -r '.status' 2>/dev/null)

    if [ "$STATUS" = "success" ]; then
        TIME=$(echo "$RESPONSE" | jq -r '.diagnostics.timing.propagationTimeMs')
        echo -e "${GREEN}✓${NC} (${TIME}ms)"
    else
        echo -e "${RED}✗${NC}"
    fi
done

echo ""

# Test reference frames
echo "Testing reference frames:"
FRAMES=("eme2000" "gcrf" "teme" "mod" "tod")

for frame in "${FRAMES[@]}"; do
    echo -n "  $frame... "
    RESPONSE=$(curl -s "${API_BASE}?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D&frame=${frame}")
    STATUS=$(echo "$RESPONSE" | jq -r '.status' 2>/dev/null)

    if [ "$STATUS" = "success" ]; then
        echo -e "${GREEN}✓${NC}"
    else
        echo -e "${RED}✗${NC}"
    fi
done

echo ""
echo "==================="
echo -e "${GREEN}Health check complete!${NC}"
