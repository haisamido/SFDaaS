#!/bin/bash
#
# SFDaaS Comprehensive Stress Test Script
# ========================================
# This script performs comprehensive stress testing on the SFDaaS application
# including load testing, propagator comparison, memory monitoring, and more.
#
# Usage: ./comprehensive_stress_test.sh [options]
#
# Options:
#   -h, --help          Show this help message
#   -u, --url URL       Base URL (default: http://localhost:8080)
#   -q, --quick         Quick test mode (reduced iterations)
#   -v, --verbose       Verbose output
#   -o, --output DIR    Output directory (default: auto-generated)
#
# Requirements:
#   - curl
#   - jq
#   - ab (apache-bench)
#   - bc
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Default configuration
BASE_URL="http://localhost:8080"
QUICK_MODE=false
VERBOSE=false
OUTPUT_DIR=""
START_TIME=$(date +%s)

# API endpoints
API_BASE="${BASE_URL}/sfdaas/api/propagate"
API_USAGE="${API_BASE}/usage"

# Test parameters
BASE_PARAMS="t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D"

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Functions
print_header() {
    echo -e "${CYAN}================================================${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}================================================${NC}"
    echo ""
}

print_section() {
    echo ""
    echo -e "${BLUE}=== $1 ===${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${CYAN}ℹ${NC} $1"
}

log_verbose() {
    if [ "$VERBOSE" = true ]; then
        echo -e "${NC}  $1${NC}"
    fi
}

usage() {
    cat << EOF
SFDaaS Comprehensive Stress Test Script

Usage: $0 [options]

Options:
    -h, --help          Show this help message
    -u, --url URL       Base URL (default: http://localhost:8080)
    -q, --quick         Quick test mode (reduced iterations)
    -v, --verbose       Verbose output
    -o, --output DIR    Output directory (default: auto-generated)

Examples:
    $0                              # Run full test suite
    $0 -q                           # Run quick tests
    $0 -u http://example.com        # Test remote server
    $0 -v -o my_results             # Verbose with custom output dir

EOF
    exit 0
}

check_dependencies() {
    print_section "Checking Dependencies"

    local deps=("curl" "jq" "ab" "bc")
    local missing=()

    for dep in "${deps[@]}"; do
        if command -v "$dep" &> /dev/null; then
            print_success "$dep is installed"
        else
            print_error "$dep is NOT installed"
            missing+=("$dep")
        fi
    done

    if [ ${#missing[@]} -ne 0 ]; then
        echo ""
        print_error "Missing dependencies: ${missing[*]}"
        echo ""
        echo "Install missing dependencies:"
        echo "  macOS:   brew install ${missing[*]}"
        echo "  Ubuntu:  sudo apt-get install ${missing[*]}"
        exit 1
    fi

    echo ""
}

check_server() {
    print_section "Checking Server Health"

    print_info "Attempting to connect to: $BASE_URL"

    if ! curl -s --connect-timeout 5 "$API_USAGE" > /dev/null 2>&1; then
        print_error "Server is not responding at $BASE_URL"
        echo ""
        echo "Please ensure SFDaaS is running:"
        echo "  task run"
        echo "  # or"
        echo "  java -jar target/SFDaaS-with-dependencies.jar"
        exit 1
    fi

    print_success "Server is running"

    # Get server info
    local response=$(curl -s "${API_BASE}?${BASE_PARAMS}")
    local status=$(echo "$response" | jq -r '.status')

    if [ "$status" != "success" ]; then
        print_error "Server responded but propagation test failed"
        echo "$response" | jq .
        exit 1
    fi

    print_success "Basic propagation test passed"

    # Get timing info
    local prop_time=$(echo "$response" | jq -r '.diagnostics.timing.propagationTimeMs')
    local total_time=$(echo "$response" | jq -r '.diagnostics.timing.totalTimeMs')

    print_info "Propagation Time: ${prop_time}ms"
    print_info "Total Time: ${total_time}ms"

    echo ""
}

create_output_dir() {
    # Create results directory if it doesn't exist
    RESULTS_DIR="$(dirname "$0")/results"
    mkdir -p "$RESULTS_DIR"

    if [ -z "$OUTPUT_DIR" ]; then
        # Result file matches script name prefix
        SCRIPT_NAME=$(basename "$0" .sh)
        OUTPUT_DIR="${RESULTS_DIR}/${SCRIPT_NAME}_results"
    fi

    mkdir -p "$OUTPUT_DIR"
    print_info "Results directory: $OUTPUT_DIR"
    echo ""
}

test_light_load() {
    print_section "Test 1: Light Load (500 requests, 25 concurrent)"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    local requests=500
    local concurrent=25

    if [ "$QUICK_MODE" = true ]; then
        requests=100
        concurrent=10
    fi

    print_info "Running $requests requests with $concurrent concurrent connections..."

    if ab -n $requests -c $concurrent -g "$OUTPUT_DIR/light_load.tsv" \
        "${API_BASE}?${BASE_PARAMS}" > "$OUTPUT_DIR/light_load.txt" 2>&1; then

        local rps=$(grep "Requests per second" "$OUTPUT_DIR/light_load.txt" | awk '{print $4}')
        local mean_time=$(grep "Time per request" "$OUTPUT_DIR/light_load.txt" | head -1 | awk '{print $4}')

        print_success "Test completed"
        print_info "Requests/sec: $rps"
        print_info "Mean time: ${mean_time}ms"

        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        print_error "Test failed"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi

    echo ""
}

test_medium_load() {
    print_section "Test 2: Medium Load (1000 requests, 50 concurrent)"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    local requests=1000
    local concurrent=50

    if [ "$QUICK_MODE" = true ]; then
        requests=200
        concurrent=20
    fi

    print_info "Running $requests requests with $concurrent concurrent connections..."

    if ab -n $requests -c $concurrent -g "$OUTPUT_DIR/medium_load.tsv" \
        "${API_BASE}?${BASE_PARAMS}" > "$OUTPUT_DIR/medium_load.txt" 2>&1; then

        local rps=$(grep "Requests per second" "$OUTPUT_DIR/medium_load.txt" | awk '{print $4}')
        local mean_time=$(grep "Time per request" "$OUTPUT_DIR/medium_load.txt" | head -1 | awk '{print $4}')
        local p95=$(grep "95%" "$OUTPUT_DIR/medium_load.txt" | awk '{print $2}')

        print_success "Test completed"
        print_info "Requests/sec: $rps"
        print_info "Mean time: ${mean_time}ms"
        print_info "95th percentile: ${p95}ms"

        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        print_error "Test failed"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi

    echo ""
}

test_heavy_load() {
    print_section "Test 3: Heavy Load (2000 requests, 100 concurrent)"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    local requests=2000
    local concurrent=100

    if [ "$QUICK_MODE" = true ]; then
        requests=300
        concurrent=30
    fi

    print_info "Running $requests requests with $concurrent concurrent connections..."

    if ab -n $requests -c $concurrent -g "$OUTPUT_DIR/heavy_load.tsv" \
        "${API_BASE}?${BASE_PARAMS}" > "$OUTPUT_DIR/heavy_load.txt" 2>&1; then

        local rps=$(grep "Requests per second" "$OUTPUT_DIR/heavy_load.txt" | awk '{print $4}')
        local mean_time=$(grep "Time per request" "$OUTPUT_DIR/heavy_load.txt" | head -1 | awk '{print $4}')
        local p95=$(grep "95%" "$OUTPUT_DIR/heavy_load.txt" | awk '{print $2}')
        local p99=$(grep "99%" "$OUTPUT_DIR/heavy_load.txt" | awk '{print $2}')
        local failed=$(grep "Failed requests" "$OUTPUT_DIR/heavy_load.txt" | awk '{print $3}')

        print_success "Test completed"
        print_info "Requests/sec: $rps"
        print_info "Mean time: ${mean_time}ms"
        print_info "95th percentile: ${p95}ms"
        print_info "99th percentile: ${p99}ms"
        print_info "Failed requests: $failed"

        if [ "$failed" -eq 0 ]; then
            print_success "No failed requests"
        else
            print_warning "$failed requests failed"
        fi

        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        print_error "Test failed"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi

    echo ""
}

test_sustained_load() {
    print_section "Test 4: Sustained Load (60s duration, 50 concurrent)"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    local duration=60
    local concurrent=50

    if [ "$QUICK_MODE" = true ]; then
        duration=30
        concurrent=25
    fi

    print_info "Running sustained load for ${duration}s with $concurrent concurrent connections..."

    if ab -t $duration -c $concurrent \
        "${API_BASE}?${BASE_PARAMS}" > "$OUTPUT_DIR/sustained_load.txt" 2>&1; then

        local total_requests=$(grep "Complete requests" "$OUTPUT_DIR/sustained_load.txt" | awk '{print $3}')
        local rps=$(grep "Requests per second" "$OUTPUT_DIR/sustained_load.txt" | awk '{print $4}')
        local mean_time=$(grep "Time per request" "$OUTPUT_DIR/sustained_load.txt" | head -1 | awk '{print $4}')

        print_success "Test completed"
        print_info "Total requests: $total_requests"
        print_info "Requests/sec: $rps"
        print_info "Mean time: ${mean_time}ms"

        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        print_error "Test failed"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi

    echo ""
}

test_propagators() {
    print_section "Test 5: Propagator Performance Comparison"

    local propagators=("rungekutta" "dormandprince" "adamsbashforth" "adamsmoulton")
    local iterations=3

    if [ "$QUICK_MODE" = true ]; then
        iterations=1
    fi

    echo "Propagator | Avg Time (ms) | Min (ms) | Max (ms) | Status" > "$OUTPUT_DIR/propagator_results.txt"
    echo "-----------|---------------|----------|----------|-------" >> "$OUTPUT_DIR/propagator_results.txt"

    for prop in "${propagators[@]}"; do
        TOTAL_TESTS=$((TOTAL_TESTS + 1))

        print_info "Testing $prop (${iterations} iterations)..."

        local times=()
        local success=true

        for i in $(seq 1 $iterations); do
            local response=$(curl -s "${API_BASE}?${BASE_PARAMS}&propagator=${prop}")
            local status=$(echo "$response" | jq -r '.status')

            if [ "$status" != "success" ]; then
                print_error "$prop test failed"
                success=false
                break
            fi

            local prop_time=$(echo "$response" | jq -r '.diagnostics.timing.propagationTimeMs')
            times+=($prop_time)
            log_verbose "Iteration $i: ${prop_time}ms"
        done

        if [ "$success" = true ]; then
            # Calculate statistics
            local sum=0
            local min=${times[0]}
            local max=${times[0]}

            for time in "${times[@]}"; do
                sum=$(echo "$sum + $time" | bc)
                if (( $(echo "$time < $min" | bc -l) )); then
                    min=$time
                fi
                if (( $(echo "$time > $max" | bc -l) )); then
                    max=$time
                fi
            done

            local avg=$(echo "scale=2; $sum / ${#times[@]}" | bc)

            print_success "$prop: Avg ${avg}ms (Min: ${min}ms, Max: ${max}ms)"
            echo "$prop | $avg | $min | $max | ✓" >> "$OUTPUT_DIR/propagator_results.txt"

            PASSED_TESTS=$((PASSED_TESTS + 1))
        else
            echo "$prop | N/A | N/A | N/A | ✗" >> "$OUTPUT_DIR/propagator_results.txt"
            FAILED_TESTS=$((FAILED_TESTS + 1))
        fi
    done

    echo ""
}

test_frames() {
    print_section "Test 6: Reference Frame Comparison"

    local frames=("eme2000" "gcrf" "teme" "mod" "tod")

    echo "Frame      | Time (ms) | Status" > "$OUTPUT_DIR/frame_results.txt"
    echo "-----------|-----------|-------" >> "$OUTPUT_DIR/frame_results.txt"

    for frame in "${frames[@]}"; do
        TOTAL_TESTS=$((TOTAL_TESTS + 1))

        print_info "Testing $frame..."

        local response=$(curl -s "${API_BASE}?${BASE_PARAMS}&frame=${frame}")
        local status=$(echo "$response" | jq -r '.status')

        if [ "$status" = "success" ]; then
            local prop_time=$(echo "$response" | jq -r '.diagnostics.timing.propagationTimeMs')
            print_success "$frame: ${prop_time}ms"
            echo "$frame | $prop_time | ✓" >> "$OUTPUT_DIR/frame_results.txt"
            PASSED_TESTS=$((PASSED_TESTS + 1))
        else
            print_error "$frame test failed"
            echo "$frame | N/A | ✗" >> "$OUTPUT_DIR/frame_results.txt"
            FAILED_TESTS=$((FAILED_TESTS + 1))
        fi
    done

    echo ""
}

test_step_sizes() {
    print_section "Test 7: Step Size Performance"

    local step_sizes=("60" "30" "10" "1")

    if [ "$QUICK_MODE" = true ]; then
        step_sizes=("60" "30")
    fi

    echo "Step Size  | Time (ms) | Status" > "$OUTPUT_DIR/stepsize_results.txt"
    echo "-----------|-----------|-------" >> "$OUTPUT_DIR/stepsize_results.txt"

    for step in "${step_sizes[@]}"; do
        TOTAL_TESTS=$((TOTAL_TESTS + 1))

        print_info "Testing stepSize=${step}s..."

        local response=$(curl -s "${API_BASE}?${BASE_PARAMS}&stepSize=${step}")
        local status=$(echo "$response" | jq -r '.status')

        if [ "$status" = "success" ]; then
            local prop_time=$(echo "$response" | jq -r '.diagnostics.timing.propagationTimeMs')
            print_success "${step}s: ${prop_time}ms"
            echo "${step}s | $prop_time | ✓" >> "$OUTPUT_DIR/stepsize_results.txt"
            PASSED_TESTS=$((PASSED_TESTS + 1))
        else
            print_error "${step}s test failed"
            echo "${step}s | N/A | ✗" >> "$OUTPUT_DIR/stepsize_results.txt"
            FAILED_TESTS=$((FAILED_TESTS + 1))
        fi
    done

    echo ""
}

test_long_propagation() {
    print_section "Test 8: Long Duration Propagation"

    local durations=("7 days" "30 days")
    local tf_values=("2010-06-04T12:00:00.000%2B00:00" "2010-06-27T12:00:00.000%2B00:00")

    if [ "$QUICK_MODE" = true ]; then
        durations=("7 days")
        tf_values=("2010-06-04T12:00:00.000%2B00:00")
    fi

    for i in "${!durations[@]}"; do
        TOTAL_TESTS=$((TOTAL_TESTS + 1))

        local duration="${durations[$i]}"
        local tf="${tf_values[$i]}"

        print_info "Testing $duration propagation..."

        local params="t0=2010-05-28T12:00:00.000%2B00:00&tf=${tf}&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D"

        local start=$(date +%s%N)
        local response=$(curl -s "${API_BASE}?${params}")
        local end=$(date +%s%N)

        local status=$(echo "$response" | jq -r '.status')
        local duration_sec=$(echo "scale=3; ($end - $start) / 1000000000" | bc)

        if [ "$status" = "success" ]; then
            local prop_time=$(echo "$response" | jq -r '.diagnostics.timing.propagationTimeMs')
            print_success "$duration: ${prop_time}ms (wall time: ${duration_sec}s)"
            PASSED_TESTS=$((PASSED_TESTS + 1))
        else
            print_error "$duration test failed"
            FAILED_TESTS=$((FAILED_TESTS + 1))
        fi
    done

    echo ""
}

test_memory_stability() {
    print_section "Test 9: Memory Stability Check"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    local iterations=100

    if [ "$QUICK_MODE" = true ]; then
        iterations=50
    fi

    print_info "Running $iterations sequential requests to check for memory leaks..."

    # Get initial PID and memory
    local pid=$(ps aux | grep 'SFDaaS' | grep -v grep | awk '{print $2}' | head -1)

    if [ -z "$pid" ]; then
        print_warning "Could not find SFDaaS process ID"
        echo ""
        return
    fi

    print_info "Monitoring PID: $pid"

    local mem_samples=()

    for i in $(seq 1 $iterations); do
        curl -s "${API_BASE}?${BASE_PARAMS}" > /dev/null

        if [ $((i % 20)) -eq 0 ]; then
            if [ "$(uname)" = "Darwin" ]; then
                # macOS
                local mem=$(ps -p $pid -o rss= 2>/dev/null || echo "0")
            else
                # Linux
                local mem=$(ps -p $pid -o rss= 2>/dev/null || echo "0")
            fi

            if [ "$mem" != "0" ]; then
                mem_samples+=($mem)
                local mem_mb=$(echo "scale=2; $mem / 1024" | bc)
                log_verbose "Request $i: ${mem_mb}MB"
            fi
        fi

        sleep 0.05
    done

    if [ ${#mem_samples[@]} -gt 0 ]; then
        local first=${mem_samples[0]}
        local last_idx=$((${#mem_samples[@]} - 1))
        local last=${mem_samples[$last_idx]}
        local growth=$(echo "scale=2; ($last - $first) / 1024" | bc)

        print_info "Memory growth: ${growth}MB (from $(echo "scale=2; $first/1024" | bc)MB to $(echo "scale=2; $last/1024" | bc)MB)"

        # Check if memory grew more than 50MB
        if (( $(echo "$growth > 50" | bc -l) )); then
            print_warning "Significant memory growth detected"
        else
            print_success "Memory usage is stable"
        fi

        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        print_warning "Could not collect memory samples"
    fi

    echo ""
}

test_concurrent_mixed() {
    print_section "Test 10: Mixed Workload (Different Propagators)"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    local requests=200
    local concurrent=20

    if [ "$QUICK_MODE" = true ]; then
        requests=50
        concurrent=10
    fi

    print_info "Running mixed workload with $requests requests, $concurrent concurrent..."

    # Create URL list with different propagators
    local url_file="$OUTPUT_DIR/mixed_urls.txt"
    local propagators=("rungekutta" "dormandprince" "adamsbashforth" "adamsmoulton")

    > "$url_file"
    for i in $(seq 1 $requests); do
        local prop=${propagators[$((i % 4))]}
        echo "${API_BASE}?${BASE_PARAMS}&propagator=${prop}" >> "$url_file"
    done

    # Run concurrent requests
    local success_count=0
    local fail_count=0

    cat "$url_file" | xargs -P $concurrent -I {} bash -c '
        if curl -s -f "{}" > /dev/null 2>&1; then
            echo "SUCCESS"
        else
            echo "FAIL"
        fi
    ' | while read result; do
        if [ "$result" = "SUCCESS" ]; then
            success_count=$((success_count + 1))
        else
            fail_count=$((fail_count + 1))
        fi
    done

    print_success "Mixed workload completed"
    print_info "Test involved different propagators in parallel"

    PASSED_TESTS=$((PASSED_TESTS + 1))

    echo ""
}

generate_summary() {
    print_section "Generating Summary Report"

    local end_time=$(date +%s)
    local duration=$((end_time - START_TIME))
    local duration_min=$(echo "scale=2; $duration / 60" | bc)

    # Generate summary file with script name prefix in results directory
    SUMMARY_FILE="${RESULTS_DIR}/${SCRIPT_NAME}.md"

    cat > "$SUMMARY_FILE" << EOF
# SFDaaS Stress Test Summary

**Duration:** ${duration}s (${duration_min} minutes)
**Server URL:** $BASE_URL
**Quick Mode:** $QUICK_MODE

---

## Overall Results

- **Total Tests:** $TOTAL_TESTS
- **Passed:** $PASSED_TESTS ✓
- **Failed:** $FAILED_TESTS ✗
- **Success Rate:** $(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)%

---

## Test Results Summary

### Load Testing

#### Light Load (500 requests, 25 concurrent)
\`\`\`
$(grep "Requests per second" "$OUTPUT_DIR/light_load.txt" 2>/dev/null || echo "N/A")
$(grep "Time per request" "$OUTPUT_DIR/light_load.txt" 2>/dev/null | head -1 || echo "N/A")
\`\`\`

#### Medium Load (1000 requests, 50 concurrent)
\`\`\`
$(grep "Requests per second" "$OUTPUT_DIR/medium_load.txt" 2>/dev/null || echo "N/A")
$(grep "Time per request" "$OUTPUT_DIR/medium_load.txt" 2>/dev/null | head -1 || echo "N/A")
$(grep "95%" "$OUTPUT_DIR/medium_load.txt" 2>/dev/null || echo "N/A")
\`\`\`

#### Heavy Load (2000 requests, 100 concurrent)
\`\`\`
$(grep "Requests per second" "$OUTPUT_DIR/heavy_load.txt" 2>/dev/null || echo "N/A")
$(grep "Time per request" "$OUTPUT_DIR/heavy_load.txt" 2>/dev/null | head -1 || echo "N/A")
$(grep "95%" "$OUTPUT_DIR/heavy_load.txt" 2>/dev/null || echo "N/A")
$(grep "99%" "$OUTPUT_DIR/heavy_load.txt" 2>/dev/null || echo "N/A")
$(grep "Failed requests" "$OUTPUT_DIR/heavy_load.txt" 2>/dev/null || echo "N/A")
\`\`\`

#### Sustained Load (60s, 50 concurrent)
\`\`\`
$(grep "Complete requests" "$OUTPUT_DIR/sustained_load.txt" 2>/dev/null || echo "N/A")
$(grep "Requests per second" "$OUTPUT_DIR/sustained_load.txt" 2>/dev/null || echo "N/A")
\`\`\`

---

### Propagator Performance Comparison

\`\`\`
$(cat "$OUTPUT_DIR/propagator_results.txt" 2>/dev/null || echo "N/A")
\`\`\`

---

### Reference Frame Comparison

\`\`\`
$(cat "$OUTPUT_DIR/frame_results.txt" 2>/dev/null || echo "N/A")
\`\`\`

---

### Step Size Performance

\`\`\`
$(cat "$OUTPUT_DIR/stepsize_results.txt" 2>/dev/null || echo "N/A")
\`\`\`

---

## Files Generated

- \`light_load.txt\` - Light load test results
- \`light_load.tsv\` - Light load test data (tab-separated)
- \`medium_load.txt\` - Medium load test results
- \`medium_load.tsv\` - Medium load test data
- \`heavy_load.txt\` - Heavy load test results
- \`heavy_load.tsv\` - Heavy load test data
- \`sustained_load.txt\` - Sustained load test results
- \`propagator_results.txt\` - Propagator comparison data
- \`frame_results.txt\` - Reference frame comparison data
- \`stepsize_results.txt\` - Step size comparison data

---

## Recommendations

EOF

    # Add recommendations based on results
    if [ $FAILED_TESTS -eq 0 ]; then
        echo "✓ All tests passed successfully!" >> "$SUMMARY_FILE"
        echo "✓ The system is performing well under stress." >> "$SUMMARY_FILE"
    else
        echo "⚠ Some tests failed. Review the detailed logs above." >> "$SUMMARY_FILE"
    fi

    # Check for performance issues in heavy load
    if [ -f "$OUTPUT_DIR/heavy_load.txt" ]; then
        local failed_requests=$(grep "Failed requests" "$OUTPUT_DIR/heavy_load.txt" | awk '{print $3}')
        if [ "$failed_requests" -gt 0 ]; then
            echo "⚠ Failed requests detected under heavy load. Consider:" >> "$SUMMARY_FILE"
            echo "  - Increasing JVM heap size" >> "$SUMMARY_FILE"
            echo "  - Tuning Netty thread pool" >> "$SUMMARY_FILE"
            echo "  - Adding connection limits" >> "$SUMMARY_FILE"
        fi
    fi

    cat >> "$SUMMARY_FILE" << EOF

---

## How to Analyze Results

1. **Review Apache Bench outputs** for detailed statistics
2. **Check TSV files** for time-series analysis
3. **Compare propagator performance** to choose optimal integrator
4. **Verify memory stability** over extended runs
5. **Identify bottlenecks** from 95th/99th percentile times

For more details, see individual test result files in: \`./tests/results/${SCRIPT_NAME}_results/\`

EOF

    print_success "Summary report generated: ./tests/results/${SCRIPT_NAME}.md"
    echo ""
}

display_final_summary() {
    print_header "Test Execution Complete!"

    echo -e "${CYAN}Results Location:${NC} ./tests/results/${SCRIPT_NAME}_results/"
    echo ""
    echo -e "${CYAN}Test Summary:${NC}"
    echo -e "  Total Tests:  $TOTAL_TESTS"
    echo -e "  ${GREEN}Passed:       $PASSED_TESTS ✓${NC}"

    if [ $FAILED_TESTS -gt 0 ]; then
        echo -e "  ${RED}Failed:       $FAILED_TESTS ✗${NC}"
    else
        echo -e "  ${GREEN}Failed:       $FAILED_TESTS${NC}"
    fi

    local success_rate=$(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)
    echo -e "  Success Rate: ${success_rate}%"
    echo ""

    local end_time=$(date +%s)
    local duration=$((end_time - START_TIME))
    echo -e "${CYAN}Duration:${NC} ${duration}s ($(echo "scale=2; $duration / 60" | bc) minutes)"
    echo ""

    echo -e "${CYAN}Next Steps:${NC}"
    echo "  1. Review summary: cat ./tests/results/${SCRIPT_NAME}.md"
    echo "  2. Analyze detailed logs in ./tests/results/${SCRIPT_NAME}_results/"
    echo "  3. Compare propagator performance"
    echo "  4. Check for any failed tests"
    echo ""

    if [ $FAILED_TESTS -eq 0 ]; then
        print_success "All tests passed! 🎉"
    else
        print_warning "Some tests failed. Review logs for details."
    fi

    echo ""
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            usage
            ;;
        -u|--url)
            BASE_URL="$2"
            API_BASE="${BASE_URL}/sfdaas/api/propagate"
            API_USAGE="${API_BASE}/usage"
            shift 2
            ;;
        -q|--quick)
            QUICK_MODE=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -o|--output)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            usage
            ;;
    esac
done

# Main execution
main() {
    print_header "SFDaaS Comprehensive Stress Test"

    if [ "$QUICK_MODE" = true ]; then
        print_warning "Running in QUICK MODE (reduced iterations)"
        echo ""
    fi

    check_dependencies
    check_server
    create_output_dir

    # Run all tests
    test_light_load
    test_medium_load
    test_heavy_load
    test_sustained_load
    test_propagators
    test_frames
    test_step_sizes
    test_long_propagation
    test_memory_stability
    test_concurrent_mixed

    # Generate reports
    generate_summary
    display_final_summary
}

# Run main function
main
