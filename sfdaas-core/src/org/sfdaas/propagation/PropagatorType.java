package org.sfdaas.propagation;

/**
 * Enum representing available numerical propagator types.
 * Each type corresponds to a specific integrator implementation.
 *
 * @author SFDaaS
 */
public enum PropagatorType {

    /**
     * Classical 4th order Runge-Kutta integrator (fixed step)
     */
    RUNGE_KUTTA("rungekutta", "Classical Runge-Kutta (RK4)"),

    /**
     * Dormand-Prince 8(5,3) integrator (adaptive step)
     */
    DORMAND_PRINCE("dormandprince", "Dormand-Prince 8(5,3)"),

    /**
     * Adams-Bashforth multi-step integrator (variable step)
     */
    ADAMS_BASHFORTH("adamsbashforth", "Adams-Bashforth"),

    /**
     * Adams-Moulton multi-step integrator (variable step)
     */
    ADAMS_MOULTON("adamsmoulton", "Adams-Moulton");

    private final String key;
    private final String displayName;

    PropagatorType(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get PropagatorType from string key
     * @param key The string key (e.g., "rungekutta", "dormandprince")
     * @return The corresponding PropagatorType
     * @throws IllegalArgumentException if key is not recognized
     */
    public static PropagatorType fromKey(String key) {
        if (key == null) {
            return RUNGE_KUTTA; // Default
        }

        String normalizedKey = key.toLowerCase().trim();
        for (PropagatorType type : values()) {
            if (type.key.equals(normalizedKey)) {
                return type;
            }
        }

        // Default to Runge-Kutta if not found
        return RUNGE_KUTTA;
    }
}
