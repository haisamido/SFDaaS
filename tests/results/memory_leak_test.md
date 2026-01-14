# SFDaaS Memory Leak Test Report

**Server:** http://localhost:8080
**Iterations:** 1000
**Duration:** 68s

---

## Test Results

| Metric | Value |
| --- | --- |
| Initial Memory | 1420544 MB |
| Final Memory | 1420896 MB |
| Memory Growth | 352 MB |
| Requests | 1000 |
| Duration | 68s |

## Analysis

**Status:** ✗ FAIL - Significant memory growth detected

⚠ Significant memory growth detected (>100MB). This may indicate a memory leak.

**Recommendations:**
1. Profile with JProfiler or YourKit
2. Analyze heap dumps
3. Review object retention

---

## Data Files

- CSV Data: [`memory_leak_test.csv`](memory_leak_test.csv)
- Report: [`memory_leak_test.md`](memory_leak_test.md)

## Visualization

To visualize the memory usage over time:

```bash
gnuplot -e "set terminal dumb; set datafile separator ','; plot './tests/results/memory_leak_test.csv' using 1:4 with lines title 'Memory (MB)'"
```

**Memory leak test complete!**
