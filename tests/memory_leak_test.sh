#!/bin/bash
#
# Memory Leak Detection Test for SFDaaS
# ======================================
# Runs repeated requests and monitors memory usage over time
#
# Usage: ./memory_leak_test.sh [iterations] [url]
#

ITERATIONS="${1:-1000}"
BASE_URL="${2:-http://localhost:8080}"
API_BASE="${BASE_URL}/sfdaas/api/propagate"
BASE_PARAMS="t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}SFDaaS Memory Leak Test${NC}"
echo "======================="
echo ""
echo "Iterations: $ITERATIONS"
echo "URL: $BASE_URL"
echo ""

# Find SFDaaS process
PID=$(ps aux | grep 'SFDaaS' | grep -v grep | awk '{print $2}' | head -1)

if [ -z "$PID" ]; then
    echo -e "${RED}Error: Could not find SFDaaS process${NC}"
    echo ""
    echo "Please ensure SFDaaS is running:"
    echo "  task run"
    exit 1
fi

echo -e "${GREEN}Found SFDaaS process: PID $PID${NC}"
echo ""

# Create results directory if it doesn't exist
RESULTS_DIR="$(dirname "$0")/results"
mkdir -p "$RESULTS_DIR"

# Result file (matches script name prefix)
SCRIPT_NAME=$(basename "$0" .sh)
OUTPUT_FILE="${RESULTS_DIR}/${SCRIPT_NAME}.csv"
REPORT_FILE="${RESULTS_DIR}/${SCRIPT_NAME}.md"

echo "Step,Iteration,RSS_KB,RSS_MB" > "$OUTPUT_FILE"

echo "Running $ITERATIONS requests..."
echo "Memory samples will be taken every 50 requests"
echo ""
echo "Monitor memory in real-time with:"
echo "  watch -n 1 'ps -p $PID -o pid,rss,vsz,%mem,command'"
echo ""

START_TIME=$(date +%s)
STEP=0

for i in $(seq 1 $ITERATIONS); do
    # Make request
    curl -s "${API_BASE}?${BASE_PARAMS}" > /dev/null

    # Sample memory every 50 requests
    if [ $((i % 50)) -eq 0 ]; then
        STEP=$((STEP + 1))
        if [ "$(uname)" = "Darwin" ]; then
            # macOS
            MEM_RSS=$(ps -p $PID -o rss= 2>/dev/null | tr -d ' ')
        else
            # Linux
            MEM_RSS=$(ps -p $PID -o rss= 2>/dev/null | tr -d ' ')
        fi

        if [ -n "$MEM_RSS" ] && [ "$MEM_RSS" != "0" ]; then
            MEM_MB=$(echo "scale=2; $MEM_RSS / 1024" | bc)

            echo "$STEP,$i,$MEM_RSS,$MEM_MB" >> "$OUTPUT_FILE"

            echo -e "Step $STEP (Request $i): ${CYAN}${MEM_MB} MB${NC}"
        else
            echo -e "${YELLOW}Step $STEP (Request $i): Could not read memory${NC}"
        fi
    fi

    # Small delay to avoid overwhelming the server
    sleep 0.05
done

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "======================="
echo -e "${GREEN}Test completed!${NC}"
echo ""
echo "Duration: ${DURATION}s"
echo "Results saved to: ./tests/results/${SCRIPT_NAME}.csv"
echo ""

# Analyze results
FIRST_MEM=$(head -2 "$OUTPUT_FILE" | tail -1 | cut -d',' -f3)
LAST_MEM=$(tail -1 "$OUTPUT_FILE" | cut -d',' -f3)

if [ -n "$FIRST_MEM" ] && [ -n "$LAST_MEM" ]; then
    GROWTH=$(echo "scale=2; $LAST_MEM - $FIRST_MEM" | bc)

    echo "Memory Analysis:"
    echo "  Initial: ${FIRST_MEM} MB"
    echo "  Final:   ${LAST_MEM} MB"
    echo "  Growth:  ${GROWTH} MB"
    echo ""

    # Check for significant growth
    if (( $(echo "$GROWTH > 100" | bc -l) )); then
        echo -e "${RED}⚠ Significant memory growth detected (>100MB)${NC}"
        echo "This may indicate a memory leak. Consider:"
        echo "  1. Profiling with JProfiler or YourKit"
        echo "  2. Analyzing heap dumps"
        echo "  3. Reviewing object retention"
    elif (( $(echo "$GROWTH > 50" | bc -l) )); then
        echo -e "${YELLOW}⚠ Moderate memory growth detected (>50MB)${NC}"
        echo "Monitor over longer periods to confirm"
    else
        echo -e "${GREEN}✓ Memory usage appears stable${NC}"
    fi
fi

echo ""
echo "Visualize results:"
echo "  gnuplot -e \"set terminal dumb; set datafile separator ','; plot './tests/results/${SCRIPT_NAME}.csv' using 1:4 with lines title 'Memory (MB)'\""

# Generate markdown report
cat > "$REPORT_FILE" << EOF
# SFDaaS Memory Leak Test Report

**Server:** $BASE_URL
**Iterations:** $ITERATIONS
**Duration:** ${DURATION}s

---

## Test Results

EOF

if [ -n "$FIRST_MEM" ] && [ -n "$LAST_MEM" ]; then
    GROWTH=$(echo "scale=2; $LAST_MEM - $FIRST_MEM" | bc)

    cat >> "$REPORT_FILE" << EOF
| Metric | Value |
| --- | --- |
| Initial Memory | ${FIRST_MEM} MB |
| Final Memory | ${LAST_MEM} MB |
| Memory Growth | ${GROWTH} MB |
| Requests | $ITERATIONS |
| Duration | ${DURATION}s |

## Analysis

EOF

    # Check for significant growth
    if (( $(echo "$GROWTH > 100" | bc -l) )); then
        cat >> "$REPORT_FILE" << EOF
**Status:** ✗ FAIL - Significant memory growth detected

⚠ Significant memory growth detected (>100MB). This may indicate a memory leak.

**Recommendations:**
1. Profile with JProfiler or YourKit
2. Analyze heap dumps
3. Review object retention

EOF
    elif (( $(echo "$GROWTH > 50" | bc -l) )); then
        cat >> "$REPORT_FILE" << EOF
**Status:** ⚠ WARNING - Moderate memory growth detected

⚠ Moderate memory growth detected (>50MB). Monitor over longer periods to confirm.

EOF
    else
        cat >> "$REPORT_FILE" << EOF
**Status:** ✓ PASS - Memory usage appears stable

✓ Memory usage appears stable. No significant memory growth detected.

EOF
    fi
fi

cat >> "$REPORT_FILE" << EOF
---

## Data Files

- CSV Data: [\`${SCRIPT_NAME}.csv\`](${SCRIPT_NAME}.csv)
- Report: [\`${SCRIPT_NAME}.md\`](${SCRIPT_NAME}.md)

## Visualization

To visualize the memory usage over time:

\`\`\`bash
gnuplot -e "set terminal dumb; set datafile separator ','; plot './tests/results/${SCRIPT_NAME}.csv' using 1:4 with lines title 'Memory (MB)'"
\`\`\`

**Memory leak test complete!**
EOF

echo ""
echo "Report saved to: ./tests/results/${SCRIPT_NAME}.md"
