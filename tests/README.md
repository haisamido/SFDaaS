# SFDaaS Testing Suite

This directory contains comprehensive testing tools, scripts, and documentation for stress testing and validating the SFDaaS application.

## Directory Structure

```
tests/
├── README.md                        # This file - complete testing documentation
├── STRESS_TESTING.md                # Comprehensive stress testing guide
├── comprehensive_stress_test.sh*    # Full 10-test stress testing suite
├── memory_leak_test.sh*             # Memory leak detection script
├── quick_health_check.sh*           # Fast health check (~5-10s)
└── results/                         # Test results output directory (git-ignored)
    └── .gitignore
```

**Note:** All test results are automatically saved to the `results/` directory to keep test outputs organized and separate from source files.

## Scripts Overview

### 1. Comprehensive Stress Test
**File:** `comprehensive_stress_test.sh`

Complete stress testing suite that runs 10 different test scenarios and generates detailed reports.

**Usage:**
```bash
# Run full test suite
./comprehensive_stress_test.sh

# Quick test mode (reduced iterations)
./comprehensive_stress_test.sh --quick

# Test remote server
./comprehensive_stress_test.sh --url http://example.com

# Verbose output
./comprehensive_stress_test.sh --verbose

# Custom output directory
./comprehensive_stress_test.sh --output my_results

# Combined options
./comprehensive_stress_test.sh -q -v -u http://localhost:8080
```

**Tests Included:**
1. Light Load (500 requests, 25 concurrent)
2. Medium Load (1000 requests, 50 concurrent)
3. Heavy Load (2000 requests, 100 concurrent)
4. Sustained Load (60s duration)
5. Propagator Performance Comparison
6. Reference Frame Comparison
7. Step Size Performance
8. Long Duration Propagation
9. Memory Stability Check
10. Mixed Workload

**Output:**
- Detailed test results in timestamped directory
- `SUMMARY.md` - Comprehensive test summary
- Individual `.txt` files for each test
- `.tsv` files for time-series data analysis

---

### 2. Quick Health Check
**File:** `quick_health_check.sh`

Fast health check to verify SFDaaS is running and all components are functional.

**Usage:**
```bash
# Check localhost
./quick_health_check.sh

# Check remote server
./quick_health_check.sh http://example.com
```

**Checks:**
- Server connectivity
- Basic propagation test
- All propagators (rungekutta, dormandprince, adamsbashforth, adamsmoulton)
- All reference frames (eme2000, gcrf, teme, mod, tod)

**Run Time:** ~5-10 seconds

---

### 3. Memory Leak Test
**File:** `memory_leak_test.sh`

Runs repeated requests while monitoring memory usage to detect potential memory leaks.

**Usage:**
```bash
# Run 1000 iterations (default)
./memory_leak_test.sh

# Custom iteration count
./memory_leak_test.sh 5000

# Test remote server
./memory_leak_test.sh 1000 http://example.com
```

**Features:**
- Monitors RSS memory usage
- Samples memory every 50 requests
- Generates CSV output for analysis
- Calculates memory growth
- Provides recommendations

**Output:**
- `memory_test_YYYYMMDD_HHMMSS.csv` - Time-series memory data

---

## Prerequisites

All scripts require the following tools:

```bash
# macOS
brew install curl jq apache-bench bc

# Ubuntu/Debian
sudo apt-get install curl jq apache2-utils bc

# Verify installations
curl --version
jq --version
ab -V
bc --version
```

---

## Quick Start

1. **Ensure SFDaaS is running:**
   ```bash
   # From project root
   task run

   # Or manually
   java -jar target/SFDaaS-with-dependencies.jar
   ```

2. **Run health check:**
   ```bash
   cd tests
   ./quick_health_check.sh
   ```

3. **Run quick stress test:**
   ```bash
   ./comprehensive_stress_test.sh --quick
   ```

4. **Check for memory leaks:**
   ```bash
   ./memory_leak_test.sh 500
   ```

---

## Understanding Results

### Success Criteria

✅ **Good Performance:**
- Request success rate > 99%
- Mean response time < 500ms for 24hr propagation
- 95th percentile < 1000ms
- Stable memory usage
- No connection errors

🚨 **Performance Issues:**
- Request failure rate > 1%
- 95th percentile > 2000ms
- Growing memory usage
- CPU constantly at 100%
- Connection timeouts

### Apache Bench Metrics

Key metrics to watch in test results:

- **Requests per second:** Throughput capacity (higher is better)
- **Time per request:** Average latency (lower is better)
- **50% (median):** Typical response time
- **95% percentile:** SLA target threshold
- **99% percentile:** Outlier detection
- **Failed requests:** Should be 0

### Memory Analysis

Normal behavior:
- Initial growth as JVM warms up
- Stabilization after ~100 requests
- Periodic drops from garbage collection
- Growth < 50MB over 1000 requests

Warning signs:
- Continuous linear growth
- Growth > 100MB over 1000 requests
- No stabilization after warm-up
- No GC activity

---

## Advanced Usage

### Monitoring During Tests

**Terminal 1 - Run test:**
```bash
./comprehensive_stress_test.sh
```

**Terminal 2 - Monitor memory:**
```bash
watch -n 1 'ps aux | grep SFDaaS | grep -v grep'
```

**Terminal 3 - Monitor connections:**
```bash
watch -n 1 'netstat -an | grep 8080 | wc -l'
```

### Custom Test Scenarios

You can modify the scripts or create custom tests:

```bash
# Heavy load with specific propagator
ab -n 5000 -c 200 \
  "http://localhost:8080/SFDaaS/orekit/propagate?t0=...&propagator=dormandprince"

# Long duration with small steps
ab -n 100 -c 10 \
  "http://localhost:8080/SFDaaS/orekit/propagate?t0=...&stepSize=1"

# Parallel different configurations
seq 1 100 | parallel -j 20 \
  'curl -s "http://localhost:8080/SFDaaS/orekit/propagate?t0=...&propagator=$(shuf -n1 -e rungekutta dormandprince adamsbashforth adamsmoulton)"'
```

### Analyzing Results

**View summary:**
```bash
cat stress_test_results_*/SUMMARY.md
```

**Extract key metrics:**
```bash
grep "Requests per second" stress_test_results_*/light_load.txt
grep "95%" stress_test_results_*/heavy_load.txt
```

**Plot memory usage (requires gnuplot):**
```bash
gnuplot -e "set terminal png; set output 'memory.png'; set datafile separator ','; plot 'memory_test_*.csv' using 1:3 with lines title 'Memory (MB)'"
```

**Compare propagators:**
```bash
cat stress_test_results_*/propagator_results.txt
```

---

## Troubleshooting

### Script fails with "command not found"

Install missing dependencies:
```bash
# macOS
brew install <missing-tool>

# Ubuntu/Debian
sudo apt-get install <missing-tool>
```

### "Server is not responding"

1. Check if SFDaaS is running:
   ```bash
   ps aux | grep SFDaaS
   ```

2. Start the server:
   ```bash
   task run
   ```

3. Verify port 8080 is available:
   ```bash
   netstat -an | grep 8080
   ```

### High failure rate in tests

Possible causes:
- Insufficient system resources
- JVM heap too small
- Netty thread pool exhausted
- Network issues

Solutions:
- Increase JVM heap: `export JAVA_OPTS="-Xmx4g"`
- Reduce concurrent connections in test
- Monitor system resources during test
- Check server logs for errors

### Memory keeps growing

1. Run memory leak test with more iterations:
   ```bash
   ./memory_leak_test.sh 5000
   ```

2. Analyze heap dump:
   ```bash
   jmap -dump:format=b,file=heap.bin <PID>
   ```

3. Profile with Java tools:
   ```bash
   jvisualvm
   ```

---

## Best Practices

1. **Start Small:** Begin with quick tests, gradually increase load
2. **Baseline First:** Establish baseline performance before optimization
3. **Isolate Changes:** Test one parameter at a time
4. **Document Results:** Save all test outputs for comparison
5. **Cool Down:** Allow system to stabilize between heavy tests
6. **Monitor Resources:** Watch CPU, memory, disk I/O during tests
7. **Repeat Tests:** Run multiple times to account for variance
8. **Test Realistic Scenarios:** Use expected workload patterns

---

## Performance Baselines

Expected performance on typical hardware (4-core CPU, 8GB RAM):

| Scenario | Requests/sec | Avg Response | Notes |
|----------|--------------|--------------|-------|
| Light Load (25 concurrent) | 40-60 | 400-600ms | Baseline |
| Medium Load (50 concurrent) | 50-80 | 600-1000ms | Optimal |
| Heavy Load (100 concurrent) | 40-70 | 1000-2000ms | Saturation |
| Single Request | N/A | 50-200ms | No contention |

Propagator performance (24hr propagation):

| Propagator | Time | Accuracy |
|------------|------|----------|
| Runge-Kutta | ~50ms | Good |
| Dormand-Prince | ~80ms | Excellent |
| Adams-Bashforth | ~20ms | Good |
| Adams-Moulton | ~15ms | Very Good |

---

## Contributing

To add new test scripts:

1. Create executable script in this directory
2. Follow naming convention: `descriptive_name_test.sh`
3. Include usage documentation in script header
4. Add entry to this README
5. Test on both macOS and Linux if possible

---

## Additional Resources

- [Apache Bench Documentation](https://httpd.apache.org/docs/2.4/programs/ab.html)
- [wrk GitHub](https://github.com/wg/wrk)
- [JMeter](https://jmeter.apache.org/)
- [GNU Parallel](https://www.gnu.org/software/parallel/)
- [OreKit Documentation](https://www.orekit.org/)
- SFDaaS Stress Testing Guide: `STRESS_TESTING.md`
- SFDaaS API Documentation: `http://localhost:8080/SFDaaS/orekit/propagate/usage`

---

## License

These scripts are part of the SFDaaS project.
