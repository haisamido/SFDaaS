# SFDaaS Stress Testing Guide

This document provides comprehensive methods for stress testing the Space Flight Dynamics as a Service (SFDaaS) application.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start Tests](#quick-start-tests)
3. [Concurrent Request Testing](#concurrent-request-testing)
4. [Load Testing Tools](#load-testing-tools)
5. [Propagation Stress Tests](#propagation-stress-tests)
6. [Memory and Resource Testing](#memory-and-resource-testing)
7. [Monitoring During Tests](#monitoring-during-tests)
8. [Test Scripts](#test-scripts)
9. [Performance Benchmarks](#performance-benchmarks)
10. [Interpreting Results](#interpreting-results)

---

## Prerequisites

Install required tools:

```bash
# macOS
brew install apache-bench wrk jq

# Ubuntu/Debian
sudo apt-get install apache2-utils wrk jq

# Check installations
ab -V
wrk --version
jq --version
```

---

## Quick Start Tests

### Single Request Timing

```bash
# Basic propagation timing
time curl -s "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D" | jq '.diagnostics.timing'
```

### Quick Load Test

```bash
# 100 requests, 10 concurrent
ab -n 100 -c 10 "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D"
```

---

## Concurrent Request Testing

### Apache Bench (ab)

#### Light Load Test
```bash
# 500 requests with 25 concurrent connections
ab -n 500 -c 25 -g results_light.tsv \
  "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D"
```

#### Medium Load Test
```bash
# 1000 requests with 50 concurrent connections
ab -n 1000 -c 50 -g results_medium.tsv \
  "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D"
```

#### Heavy Load Test
```bash
# 5000 requests with 100 concurrent connections
ab -n 5000 -c 100 -g results_heavy.tsv \
  "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D"
```

#### Sustained Load Test
```bash
# 60 second duration with 50 connections
ab -t 60 -c 50 \
  "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D"
```

### wrk (Modern HTTP Benchmarking)

#### Basic wrk Test
```bash
# 4 threads, 50 connections, 30 seconds
wrk -t4 -c50 -d30s \
  "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D"
```

#### High Concurrency Test
```bash
# 10 threads, 200 connections, 60 seconds
wrk -t10 -c200 -d60s --latency \
  "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D"
```

#### Ramp-Up Test with wrk Script

Create `ramp_up.lua`:
```lua
-- ramp_up.lua
counter = 0
request = function()
   counter = counter + 1
   wrk.format(nil, wrk.path .. "&req=" .. counter)
end
```

Run:
```bash
wrk -t4 -c50 -d60s -s ramp_up.lua \
  "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D"
```

---

## Load Testing Tools

### curl-based Parallel Testing

```bash
#!/bin/bash
# parallel_test.sh

NUM_REQUESTS=100
CONCURRENT=10

echo "Sending $NUM_REQUESTS requests with $CONCURRENT concurrent..."

for i in $(seq 1 $NUM_REQUESTS); do
    (
        START=$(date +%s%N)
        RESPONSE=$(curl -s -w "\n%{http_code}" \
          "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D")
        END=$(date +%s%N)

        HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
        DURATION=$(echo "scale=3; ($END - $START) / 1000000000" | bc)

        echo "Request $i: HTTP $HTTP_CODE, Duration: ${DURATION}s"
    ) &

    # Limit concurrent requests
    if [ $((i % CONCURRENT)) -eq 0 ]; then
        wait
    fi
done

wait
echo "All requests completed"
```

### GNU Parallel

```bash
# Install GNU parallel
brew install parallel  # macOS
# or
sudo apt-get install parallel  # Linux

# Run 50 parallel requests
seq 1 50 | parallel -j 10 \
  'curl -s "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D" | jq -r ".status"'
```

---

## Propagation Stress Tests

### Long Duration Propagation

Test computational intensity with extended time ranges:

```bash
# 7 days
curl -s "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-06-04T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D" | jq '.diagnostics.timing'

# 30 days
curl -s "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-06-27T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D" | jq '.diagnostics.timing'

# 90 days
curl -s "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-08-26T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D" | jq '.diagnostics.timing'

# 365 days (1 year)
curl -s "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2011-05-28T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D" | jq '.diagnostics.timing'
```

### Small Step Size Testing

Stress the integrator with fine-grained steps:

```bash
# 10 second steps
time curl -s "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D&stepSize=10"

# 1 second steps
time curl -s "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D&stepSize=1"

# 0.1 second steps (use shorter duration)
time curl -s "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-28T13:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D&stepSize=0.1"

# 0.01 second steps (use very short duration)
time curl -s "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-28T12:10:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D&stepSize=0.01"
```

### Propagator Comparison

Test all integrators under identical conditions:

```bash
#!/bin/bash
# test_all_propagators.sh

PROPAGATORS=("rungekutta" "dormandprince" "adamsbashforth" "adamsmoulton")

echo "Propagator Performance Comparison"
echo "=================================="
echo ""

for prop in "${PROPAGATORS[@]}"; do
    echo "Testing $prop..."

    # Run 3 times and average
    TOTAL=0
    for i in {1..3}; do
        RESULT=$(curl -s "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D&propagator=$prop" | jq -r '.diagnostics.timing.propagationTimeMs')
        TOTAL=$(echo "$TOTAL + $RESULT" | bc)
    done

    AVG=$(echo "scale=2; $TOTAL / 3" | bc)
    echo "  Average: ${AVG}ms"
    echo ""
done
```

### Reference Frame Comparison

```bash
#!/bin/bash
# test_all_frames.sh

FRAMES=("eme2000" "gcrf" "teme" "mod" "tod")

echo "Reference Frame Performance Comparison"
echo "======================================"
echo ""

for frame in "${FRAMES[@]}"; do
    echo "Testing $frame..."

    RESULT=$(curl -s "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D&frame=$frame" | jq -r '.diagnostics.timing.propagationTimeMs')

    echo "  Propagation Time: ${RESULT}ms"
    echo ""
done
```

---

## Memory and Resource Testing

### Memory Leak Detection

```bash
#!/bin/bash
# memory_leak_test.sh

echo "Memory Leak Test - Running 1000 requests"
echo "Monitor memory with: watch -n 1 'ps aux | grep java'"
echo ""

PID=$(ps aux | grep 'SFDaaS' | grep -v grep | awk '{print $2}')
echo "SFDaaS PID: $PID"
echo ""

for i in {1..1000}; do
    curl -s "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D" > /dev/null

    if [ $((i % 100)) -eq 0 ]; then
        MEM=$(ps -p $PID -o rss= | awk '{printf "%.2f MB", $1/1024}')
        echo "Request $i: Memory usage = $MEM"
    fi

    sleep 0.1
done

echo ""
echo "Test completed. Check memory usage."
```

### JVM Monitoring

```bash
# Get Java process ID
PID=$(ps aux | grep 'SFDaaS' | grep -v grep | awk '{print $2}')

# Monitor garbage collection
jstat -gc $PID 1000

# Monitor heap usage
jstat -gccapacity $PID

# Full JVM stats
jcmd $PID VM.info
```

### Connection Pool Testing

```bash
#!/bin/bash
# connection_pool_test.sh

echo "Connection Pool Stress Test"
echo "==========================="

# Monitor active connections
watch -n 1 'netstat -an | grep 8080 | grep ESTABLISHED | wc -l' &
WATCH_PID=$!

# Run heavy concurrent load
ab -n 10000 -c 200 \
  "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D"

# Stop monitoring
kill $WATCH_PID
```

---

## Monitoring During Tests

### CPU and Memory Monitoring

```bash
# Real-time system monitoring
htop

# Java process specific
watch -n 1 'ps aux | grep java | grep -v grep'

# Memory details
watch -n 1 'free -h'

# CPU per core
mpstat 1
```

### Network Monitoring

```bash
# Active connections to port 8080
watch -n 1 'netstat -an | grep 8080'

# Connection state summary
watch -n 1 'netstat -an | grep 8080 | awk "{print \$6}" | sort | uniq -c'

# Network I/O
iftop -i lo  # loopback interface for localhost testing
```

### Application Logs

```bash
# Tail server logs (if logging to file)
tail -f logs/sfdaas.log

# With Docker
docker logs -f <container_id>

# Filter for errors
tail -f logs/sfdaas.log | grep -i error
```

### System Load

```bash
# 1-minute, 5-minute, 15-minute load averages
uptime

# Detailed load info
w

# Load over time
watch -n 1 uptime
```

---

## Test Scripts

### Comprehensive Stress Test Script

```bash
#!/bin/bash
# comprehensive_stress_test.sh

set -e

BASE_URL="http://localhost:8080/sfdaas/propagate"
BASE_PARAMS="t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D"

RESULTS_DIR="stress_test_results_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo "SFDaaS Comprehensive Stress Test"
echo "================================="
echo "Results will be saved to: $RESULTS_DIR"
echo ""

# Test 1: Light Load
echo "Test 1: Light Load (500 requests, 25 concurrent)"
ab -n 500 -c 25 -g "$RESULTS_DIR/light_load.tsv" \
  "${BASE_URL}?${BASE_PARAMS}" > "$RESULTS_DIR/light_load.txt" 2>&1
echo "  ✓ Completed"

# Test 2: Medium Load
echo "Test 2: Medium Load (1000 requests, 50 concurrent)"
ab -n 1000 -c 50 -g "$RESULTS_DIR/medium_load.tsv" \
  "${BASE_URL}?${BASE_PARAMS}" > "$RESULTS_DIR/medium_load.txt" 2>&1
echo "  ✓ Completed"

# Test 3: Heavy Load
echo "Test 3: Heavy Load (2000 requests, 100 concurrent)"
ab -n 2000 -c 100 -g "$RESULTS_DIR/heavy_load.tsv" \
  "${BASE_URL}?${BASE_PARAMS}" > "$RESULTS_DIR/heavy_load.txt" 2>&1
echo "  ✓ Completed"

# Test 4: Sustained Load
echo "Test 4: Sustained Load (60s duration, 50 concurrent)"
ab -t 60 -c 50 \
  "${BASE_URL}?${BASE_PARAMS}" > "$RESULTS_DIR/sustained_load.txt" 2>&1
echo "  ✓ Completed"

# Test 5: Propagator Comparison
echo "Test 5: Propagator Comparison"
for prop in rungekutta dormandprince adamsbashforth adamsmoulton; do
    echo "  Testing $prop..."
    ab -n 100 -c 10 \
      "${BASE_URL}?${BASE_PARAMS}&propagator=${prop}" > "$RESULTS_DIR/prop_${prop}.txt" 2>&1
done
echo "  ✓ Completed"

# Test 6: Different Step Sizes
echo "Test 6: Step Size Comparison"
for step in 60 30 10 1; do
    echo "  Testing stepSize=${step}s..."
    ab -n 100 -c 10 \
      "${BASE_URL}?${BASE_PARAMS}&stepSize=${step}" > "$RESULTS_DIR/step_${step}.txt" 2>&1
done
echo "  ✓ Completed"

# Generate summary report
echo ""
echo "Generating summary report..."
cat > "$RESULTS_DIR/SUMMARY.txt" << EOF
SFDaaS Stress Test Summary
==========================
Test Date: $(date)
Results Directory: $RESULTS_DIR

Test Results:
-------------

Light Load (500 requests, 25 concurrent):
$(grep "Requests per second" "$RESULTS_DIR/light_load.txt")
$(grep "Time per request" "$RESULTS_DIR/light_load.txt" | head -1)

Medium Load (1000 requests, 50 concurrent):
$(grep "Requests per second" "$RESULTS_DIR/medium_load.txt")
$(grep "Time per request" "$RESULTS_DIR/medium_load.txt" | head -1)

Heavy Load (2000 requests, 100 concurrent):
$(grep "Requests per second" "$RESULTS_DIR/heavy_load.txt")
$(grep "Time per request" "$RESULTS_DIR/heavy_load.txt" | head -1)

Sustained Load (60s, 50 concurrent):
$(grep "Requests per second" "$RESULTS_DIR/sustained_load.txt")
$(grep "Time per request" "$RESULTS_DIR/sustained_load.txt" | head -1)

EOF

echo ""
echo "================================="
echo "All tests completed successfully!"
echo "Results saved to: $RESULTS_DIR"
echo "View summary: cat $RESULTS_DIR/SUMMARY.txt"
```

### Quick Health Check Script

```bash
#!/bin/bash
# health_check.sh

BASE_URL="http://localhost:8080/sfdaas/propagate"

echo "SFDaaS Health Check"
echo "==================="
echo ""

# Check if server is running
if ! curl -s --connect-timeout 5 "$BASE_URL/usage" > /dev/null; then
    echo "❌ Server is not responding"
    exit 1
fi

echo "✓ Server is running"
echo ""

# Test basic propagation
echo "Testing basic propagation..."
START=$(date +%s%N)
RESPONSE=$(curl -s -w "\n%{http_code}" \
  "${BASE_URL}?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D")
END=$(date +%s%N)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)
DURATION=$(echo "scale=3; ($END - $START) / 1000000000" | bc)

if [ "$HTTP_CODE" != "200" ]; then
    echo "❌ HTTP Error: $HTTP_CODE"
    exit 1
fi

STATUS=$(echo "$BODY" | jq -r '.status')
if [ "$STATUS" != "success" ]; then
    echo "❌ Propagation failed"
    echo "$BODY" | jq .
    exit 1
fi

echo "✓ Basic propagation successful"
echo "  Duration: ${DURATION}s"
echo "  Status: $STATUS"
echo ""

# Test all propagators
echo "Testing all propagators..."
for prop in rungekutta dormandprince adamsbashforth adamsmoulton; do
    RESPONSE=$(curl -s "${BASE_URL}?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D&propagator=${prop}")
    STATUS=$(echo "$RESPONSE" | jq -r '.status')

    if [ "$STATUS" = "success" ]; then
        echo "  ✓ $prop"
    else
        echo "  ❌ $prop failed"
    fi
done

echo ""
echo "==================="
echo "Health check complete"
```

---

## Performance Benchmarks

### Expected Performance Baselines

Based on typical hardware (4-core CPU, 8GB RAM):

| Test Type | Requests/sec | Avg Response Time | Notes |
|-----------|--------------|-------------------|-------|
| Light Load (25 concurrent) | 40-60 | 400-600ms | Baseline performance |
| Medium Load (50 concurrent) | 50-80 | 600-1000ms | Optimal throughput |
| Heavy Load (100 concurrent) | 40-70 | 1000-2000ms | System saturation point |
| Single Request | N/A | 50-200ms | No contention |

### Propagator Performance

| Propagator | Avg Time (24hr propagation) | Memory Usage | Accuracy |
|------------|----------------------------|--------------|----------|
| Runge-Kutta | ~50ms | Low | Good |
| Dormand-Prince | ~80ms | Medium | Excellent |
| Adams-Bashforth | ~20ms | Low | Good |
| Adams-Moulton | ~15ms | Low | Very Good |

---

## Interpreting Results

### Apache Bench Output

Key metrics to watch:

```
Requests per second:    [#.## [#/sec] (mean)]
  → Higher is better, indicates throughput capacity

Time per request:       [##.### [ms] (mean)]
  → Lower is better, average latency per request

Time per request:       [#.### [ms] (mean, across all concurrent requests)]
  → Server processing time without concurrency effects

Percentage of requests served within a certain time:
  50%    ##ms      → Median response time
  95%    ##ms      → 95th percentile (SLA target)
  99%    ##ms      → 99th percentile (outliers)
  100%   ##ms      → Slowest request
```

### Warning Signs

🚨 **Performance Issues Indicators:**

- Request failure rate > 1%
- 95th percentile > 2000ms
- Memory usage growing continuously (leak)
- CPU constantly at 100%
- Increasing response times over sustained load
- Connection timeouts or refused connections

### Success Criteria

✅ **Good Performance Indicators:**

- Request success rate > 99%
- 95th percentile < 1000ms for 24hr propagation
- Stable memory usage under sustained load
- CPU scales linearly with load
- Response times remain consistent
- No connection errors

---

## Advanced Testing

### Docker Resource Limits

Test behavior under resource constraints:

```bash
# Limited memory (512MB)
docker run -d --memory="512m" --name sfdaas-mem-test sfdaas
ab -n 1000 -c 50 "http://localhost:8080/sfdaas/propagate?..."

# Limited CPU (1 core)
docker run -d --cpus="1.0" --name sfdaas-cpu-test sfdaas
ab -n 1000 -c 50 "http://localhost:8080/sfdaas/propagate?..."

# Both limits
docker run -d --memory="512m" --cpus="1.0" --name sfdaas-limited sfdaas
ab -n 1000 -c 50 "http://localhost:8080/sfdaas/propagate?..."
```

### Chaos Testing

```bash
# Kill and restart during load test
ab -n 10000 -c 50 "http://localhost:8080/sfdaas/propagate?..." &
sleep 10
task stop
sleep 5
task run
# Observe recovery behavior
```

### Long-Running Stability Test

```bash
#!/bin/bash
# stability_test.sh - Run for 24 hours

DURATION=86400  # 24 hours in seconds
RATE=1          # 1 request per second

echo "Starting 24-hour stability test..."
END_TIME=$(($(date +%s) + DURATION))

while [ $(date +%s) -lt $END_TIME ]; do
    curl -s "http://localhost:8080/sfdaas/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D" > /dev/null
    sleep $RATE
done

echo "24-hour test completed"
```

---

## Troubleshooting

### Common Issues

**Issue: Connection refused**
```bash
# Check if server is running
ps aux | grep java | grep SFDaaS

# Check port binding
netstat -an | grep 8080

# Restart server
task stop && task run
```

**Issue: Out of memory errors**
```bash
# Increase JVM heap size
export JAVA_OPTS="-Xmx2g -Xms512m"
task run
```

**Issue: Slow response times**
```bash
# Check system load
uptime

# Check disk I/O
iostat -x 1

# Check if OreKit data files are cached
ls -la orekit-data/
```

---

## Best Practices

1. **Start Small**: Begin with light load, gradually increase
2. **Monitor Resources**: Watch CPU, memory, disk I/O during tests
3. **Isolate Variables**: Test one parameter change at a time
4. **Baseline First**: Establish baseline performance before optimization
5. **Repeat Tests**: Run multiple times to account for variance
6. **Document Results**: Save all test outputs for comparison
7. **Test Realistic Scenarios**: Use actual expected workload patterns
8. **Cool Down**: Allow system to stabilize between heavy tests

---

## References

- [Apache Bench Documentation](https://httpd.apache.org/docs/2.4/programs/ab.html)
- [wrk GitHub](https://github.com/wg/wrk)
- [OreKit Documentation](https://www.orekit.org/)
- SFDaaS API Documentation: `http://localhost:8080/sfdaas/propagate/usage`

---

## Support

For issues or questions about stress testing SFDaaS:
- GitHub Issues: [https://github.com/your-org/sfdaas/issues](https://github.com/your-org/sfdaas/issues)
- Check server logs for detailed error information
- Verify system requirements are met before extensive testing
