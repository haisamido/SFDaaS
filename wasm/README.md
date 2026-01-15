# SFDaaS WASM - Browser-Based Orbit Propagation

This directory contains a WebAssembly (WASM) version of SFDaaS that runs entirely in the browser without requiring a server.

## What Was Achieved

### Browser-Based Orbit Propagation
- **Standalone two-body (Keplerian) dynamics** implementation in Java, transpiled to JavaScript via TeaVM
- **No server required** - all computations run client-side in the browser
- **Cross-platform** - works on any modern browser (Chrome, Firefox, Safari, Edge)
- **Instant results** - no network latency for propagation calculations

### Features
- Full Keplerian orbit propagation using classical orbital mechanics
- Support for multiple central bodies (Earth, Sun, Moon, Mars, Jupiter, Venus, Saturn)
- Input via Cartesian (position/velocity) or Keplerian orbital elements
- ISO 8601 timestamp parsing for epoch specification
- JSON output format compatible with the server-based API
- Responsive web interface matching the main SFDaaS design

## Technical Implementation

### Build System
- **TeaVM 0.10.2** - Java to JavaScript transpiler
- **Maven** - Build management
- **Docker support** - For environments with JDK > 21

### Core Algorithms (in `SFDaaSWasm.java`)

1. **Cartesian to Orbital Elements Conversion**
   - Specific angular momentum vector: **h** = **r** × **v**
   - Eccentricity vector derivation
   - Classical orbital elements: a, e, i, Ω, ω, M

2. **Kepler's Equation Solver**
   - Newton-Raphson iteration: M = E - e·sin(E)
   - Convergence to 10⁻¹² tolerance
   - Maximum 50 iterations

3. **Orbital Elements to Cartesian Conversion**
   - Eccentric to true anomaly transformation
   - Perifocal to inertial frame rotation (3-1-3 Euler angles)

4. **Time Propagation**
   - Mean motion: n = √(μ/a³)
   - Mean anomaly advancement: M_f = M_0 + n·Δt

## Strengths

### Performance
- Sub-millisecond propagation times for typical orbits
- No network overhead - instant response
- Efficient Newton-Raphson convergence (typically 3-5 iterations)

### Accessibility
- Works offline after initial page load
- No account or authentication required
- No server infrastructure needed for deployment
- Can be hosted on static file servers (GitHub Pages, S3, etc.)

### Accuracy
- Double-precision floating-point arithmetic
- Standard astrodynamics formulas from Vallado/Bate-Mueller-White
- Suitable for educational purposes and quick estimates

### Portability
- Single HTML + JS file deployment
- No dependencies beyond a modern browser
- Works on desktop and mobile devices

## Limitations

### Physics Model
- **Two-body dynamics only** - no perturbation forces
- No atmospheric drag modeling
- No solar radiation pressure
- No gravitational harmonics (J2, J3, etc.)
- No third-body gravitational effects (Sun/Moon perturbations)
- No relativistic corrections

### Time Handling
- Simplified ISO 8601 parser (UTC only)
- No leap second handling
- No TAI/TT/TDB time scale conversions
- Approximate date-to-seconds conversion (simplified leap year handling)

### Reference Frames
- EME2000/J2000 inertial frame only
- No Earth-fixed (ITRF) frame support
- No frame transformations
- No precession/nutation modeling

### Orbit Types
- Elliptical orbits only (0 ≤ e < 1)
- No hyperbolic trajectory support
- No parabolic orbit handling
- Edge cases near e=0 or e≈1 may have reduced accuracy

### Data & Ephemeris
- No planetary ephemeris data
- Fixed gravitational parameters (no time-varying values)
- No Earth orientation parameters

## When to Use WASM vs Server Version

| Use Case | WASM | Server (OreKit) |
|----------|------|-----------------|
| Quick orbital estimates | ✓ | ✓ |
| Educational demonstrations | ✓ | ✓ |
| Offline access needed | ✓ | ✗ |
| High-fidelity propagation | ✗ | ✓ |
| Perturbation analysis | ✗ | ✓ |
| Long-duration propagation | ✗ | ✓ |
| Mission planning | ✗ | ✓ |
| Conjunction assessment | ✗ | ✓ |

## Building

### With JDK 21 or Earlier
```bash
task wasm:build
```

### With Docker (Any JDK Version)
```bash
task wasm:build-docker
```

### Serving Locally
```bash
task wasm:serve
# Open http://localhost:8000
```

## File Structure

```
wasm/
├── README.md                          # This file
├── pom.xml                            # Maven build configuration
├── src/
│   └── org/sfdaas/wasm/
│       └── SFDaaSWasm.java           # Main WASM entry point & dynamics
├── webapp/
│   └── index.html                     # Web interface (source)
└── target/
    └── wasm/
        ├── index.html                 # Web interface (built)
        ├── sfdaas.js                  # Transpiled JavaScript
        └── sfdaas.js.map             # Source map for debugging
```

## References

The orbital mechanics implementation is based on standard astrodynamics textbooks:

1. Vallado, D.A. "Fundamentals of Astrodynamics and Applications" (4th Ed.)
2. Bate, R.R., Mueller, D.D., White, J.E. "Fundamentals of Astrodynamics"
3. Curtis, H.D. "Orbital Mechanics for Engineering Students"

## Future Enhancements

Potential improvements that could be added:

- [ ] Hyperbolic orbit support
- [ ] Basic J2 perturbation (secular effects)
- [ ] Improved time parsing with proper calendar handling
- [ ] Orbit visualization (2D/3D)
- [ ] State vector interpolation for trajectory output
- [ ] Export to TLE format
- [ ] Import from TLE format (SGP4 would require significant work)
