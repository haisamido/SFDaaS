# SFDaaS Memory Leak Test Report

**Server:** http://localhost:8080
**Iterations:** 1000
**Duration:** 69s
**PID:** 5274

---

## Test Results

| Metric | Value |
| --- | --- |
| Initial Memory | 531440 MB |
| Final Memory | 531504 MB |
| Memory Growth | 64 MB |
| Requests | 1000 |
| Duration | 69s |

## Analysis

**Status:** ⚠ WARNING - Moderate memory growth detected

⚠ Moderate memory growth detected (>50MB). Monitor over longer periods to confirm.

---

## Data Files

- CSV Data: [`memory_leak_test.csv`](memory_leak_test.csv)
- Report: [`memory_leak_test.md`](memory_leak_test.md)

## Visualization

To visualize the memory usage over time:

```bash
gnuplot -e "set terminal dumb; set datafile separator ','; plot '/Users/hido/development/github.com/haisamido/SFDaaS/tests/results/memory_leak_test.csv' using 1:3 with lines title 'Memory'"
```

**Memory leak test complete!**
