# SFDaaS Health Check Report

**Server:** http://localhost:8080

---

## Server Connectivity: ✓ PASS

## Basic Propagation: ✓ PASS

- Propagation Time: 3ms
- Total Time: 3ms

## Propagators

| Propagator | rungekutta | dormandprince | adamsbashforth | adamsmoulton |
| --- | --- | --- | --- | --- |
| Status | ✓ | ✓ | ✓ | ✓ |
| Time (ms) | 3 | 1 | 1 | 1 |
| API | [link](http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2010-05-29T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]&propagator=rungekutta) | [link](http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2010-05-29T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]&propagator=dormandprince) | [link](http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2010-05-29T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]&propagator=adamsbashforth) | [link](http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2010-05-29T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]&propagator=adamsmoulton) |

## Reference Frames

| Frame | eme2000 | gcrf | teme | mod | tod |
| --- | --- | --- | --- | --- | --- |
| Status | ✓ | ✓ | ✓ | ✓ | ✓ |
| API | [link](http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2010-05-29T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]&frame=eme2000) | [link](http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2010-05-29T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]&frame=gcrf) | [link](http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2010-05-29T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]&frame=teme) | [link](http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2010-05-29T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]&frame=mod) | [link](http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2010-05-29T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]&frame=tod) |

## Time Scales

| Time Scale | utc | tai |
| --- | --- | --- |
| Status | ✓ | ✓ |
| API | [link](http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2010-05-29T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]&timeScale=utc) | [link](http://localhost:8080/SFDaaS/orekit/propagate?t0=2010-05-28T12:00:00.000&tf=2010-05-29T12:00:00.000&r0=[3198022.67,2901879.73,5142928.95]&v0=[-6129.640631,4489.647187,1284.511245]&timeScale=tai) |

---

**Health check complete!**
