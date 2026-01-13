package org.sfdaas.propagation;

/**
 * Enum representing available orbit representation types.
 * Each type corresponds to a specific OreKit Orbit implementation.
 *
 * @see <a href="https://www.orekit.org/site-orekit-11.3/apidocs/org/orekit/orbits/package-summary.html">OreKit Orbit Types</a>
 * @author SFDaaS
 */
public enum OrbitType {

    /**
     * Cartesian Orbit - Position and Velocity vectors
     * Most straightforward representation using Cartesian coordinates
     */
    CARTESIAN("cartesian", "Cartesian Orbit", "Position (x,y,z) and Velocity (vx,vy,vz) vectors"),

    /**
     * Keplerian Orbit - Classical orbital elements
     * Uses: a (semi-major axis), e (eccentricity), i (inclination),
     * Ω (RAAN), ω (argument of perigee), ν (true anomaly)
     */
    KEPLERIAN("keplerian", "Keplerian Orbit", "Classical orbital elements (a, e, i, Ω, ω, ν)"),

    /**
     * Circular Orbit - Modified elements for near-circular orbits
     * Uses: a, ex, ey, i, Ω, αₘ (latitude argument)
     * Better numerical stability for low eccentricity orbits
     */
    CIRCULAR("circular", "Circular Orbit", "Modified elements for near-circular orbits (a, ex, ey, i, Ω, αₘ)"),

    /**
     * Equinoctial Orbit - Non-singular elements
     * Uses: a, ex, ey, hx, hy, λ
     * Avoids singularities at zero eccentricity and inclination
     */
    EQUINOCTIAL("equinoctial", "Equinoctial Orbit", "Non-singular elements (a, ex, ey, hx, hy, λ)");

    private final String key;
    private final String displayName;
    private final String description;

    OrbitType(String key, String displayName, String description) {
        this.key = key;
        this.displayName = displayName;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get OrbitType from string key
     * @param key The string key (e.g., "cartesian", "keplerian", "circular", "equinoctial")
     * @return The corresponding OrbitType
     * @throws IllegalArgumentException if key is not recognized
     */
    public static OrbitType fromKey(String key) {
        if (key == null) {
            return CARTESIAN; // Default
        }

        String normalizedKey = key.toLowerCase().trim();
        for (OrbitType type : values()) {
            if (type.key.equals(normalizedKey)) {
                return type;
            }
        }

        // Default to Cartesian if not found
        return CARTESIAN;
    }
}
