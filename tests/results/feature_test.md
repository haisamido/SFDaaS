# SFDaaS Feature Test Report

**Server:** http://localhost:8080

---

## 1. Basic Propagation


## 2. Propagator Types

| Propagator | Status |
| --- | --- |
| rungekutta | PASS |
| dormandprince | PASS |
| adamsbashforth | PASS |
| adamsmoulton | PASS |

## 3. Reference Frames

| Frame | Status |
| --- | --- |
| eme2000 | PASS |
| gcrf | PASS |
| teme | PASS |
| mod | PASS |
| tod | PASS |

## 4. Time Scales

| Time Scale | Status |
| --- | --- |
| utc | PASS |
| tai | PASS |

## 5. Central Bodies

| Central Body | Status |
| --- | --- |
| earth | PASS |
| sun | PASS |
| moon | FAIL |
| mars | FAIL |
| jupiter | PASS |
| venus | PASS |
| saturn | PASS |
| mu:3.986004418e14 (custom) | PASS |

## 6. Step Sizes

| Step Size (s) | Status |
| --- | --- |
| 60 | PASS |
| 30 | PASS |
| 10 | PASS |
| 1 | PASS |
| 0.1 | PASS |

## 7. Output Interval (CSV States)

| Output Interval 600s | PASS (7 states) |
| Full double precision | PASS |
| Output Interval 300s | PASS (13 states) |

## 8. State Storage URL Parameter

| Redis URL format | PASS |
| Memcached URL format | PASS |

## 9. Session Management

| Session creation | FAIL |
| Session persistence | PASS |

## 10. API Usage Endpoint

| Usage endpoint | PASS |

## 11. Error Handling

| Missing parameters | PASS (returns error) |
| Invalid propagator | PASS |

## 12. Backward/Forward Propagation


---

## Summary

| Metric | Value |
| --- | --- |
| Total Tests | 44 |
| Passed | 41 |
| Failed | 3 |
| Success Rate | 93.18% |

---

**Feature test complete!**
