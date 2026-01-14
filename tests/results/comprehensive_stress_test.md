# SFDaaS Stress Test Summary

**Duration:** 79s (1.31 minutes)
**Server URL:** http://localhost:8080
**Quick Mode:** false

---

## Overall Results

- **Total Tests:** 21
- **Passed:** 21 ✓
- **Failed:** 0 ✗
- **Success Rate:** 100.00%

---

## Test Results Summary

### Load Testing

#### Light Load (500 requests, 25 concurrent)
```
Requests per second:    430.91 [#/sec] (mean)
Time per request:       58.017 [ms] (mean)
```

#### Medium Load (1000 requests, 50 concurrent)
```
Requests per second:    347.70 [#/sec] (mean)
Time per request:       143.803 [ms] (mean)
Document Path:          /sfdaas/api/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D
  95%    240
```

#### Heavy Load (2000 requests, 100 concurrent)
```
Requests per second:    300.14 [#/sec] (mean)
Time per request:       333.176 [ms] (mean)
Document Path:          /sfdaas/api/propagate?t0=2010-05-28T12:00:00.000%2B00:00&tf=2010-05-29T12:00:00.000%2B00:00&r0=%5B3198022.67,2901879.73,5142928.95%5D&v0=%5B-6129.640631,4489.647187,1284.511245%5D
  95%    527
  99%    611
Failed requests:        1951
```

#### Sustained Load (60s, 50 concurrent)
```
Complete requests:      17361
Requests per second:    289.34 [#/sec] (mean)
```

---

### Propagator Performance Comparison

```
Propagator | Avg Time (ms) | Min (ms) | Max (ms) | Status
-----------|---------------|----------|----------|-------
rungekutta | 4.33 | 2 | 9 | ✓
dormandprince | .33 | 0 | 1 | ✓
adamsbashforth | 1.00 | 1 | 1 | ✓
adamsmoulton | .33 | 0 | 1 | ✓
```

---

### Reference Frame Comparison

```
Frame      | Time (ms) | Status
-----------|-----------|-------
eme2000 | 2 | ✓
gcrf | 2 | ✓
teme | 5 | ✓
mod | 3 | ✓
tod | 4 | ✓
```

---

### Step Size Performance

```
Step Size  | Time (ms) | Status
-----------|-----------|-------
60s | 2 | ✓
30s | 3 | ✓
10s | 10 | ✓
1s | 102 | ✓
```

---

## Files Generated

- `light_load.txt` - Light load test results
- `light_load.tsv` - Light load test data (tab-separated)
- `medium_load.txt` - Medium load test results
- `medium_load.tsv` - Medium load test data
- `heavy_load.txt` - Heavy load test results
- `heavy_load.tsv` - Heavy load test data
- `sustained_load.txt` - Sustained load test results
- `propagator_results.txt` - Propagator comparison data
- `frame_results.txt` - Reference frame comparison data
- `stepsize_results.txt` - Step size comparison data

---

## Recommendations

✓ All tests passed successfully!
✓ The system is performing well under stress.
⚠ Failed requests detected under heavy load. Consider:
  - Increasing JVM heap size
  - Tuning Netty thread pool
  - Adding connection limits

---

## How to Analyze Results

1. **Review Apache Bench outputs** for detailed statistics
2. **Check TSV files** for time-series analysis
3. **Compare propagator performance** to choose optimal integrator
4. **Verify memory stability** over extended runs
5. **Identify bottlenecks** from 95th/99th percentile times

For more details, see individual test result files in: `./tests/results/comprehensive_stress_test_results/`

