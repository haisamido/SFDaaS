package org.sfdaas.propagation;

/**
 * Enumeration of supported space flight dynamics engines.
 * Each engine provides orbit propagation capabilities with different
 * features, algorithms, and supported models.
 *
 * @author SFDaaS
 */
public enum EngineType {
    /**
     * OreKit - ORbit Extrapolation KIT
     * Java-based space dynamics library with extensive force models
     * and numerical integrators. Currently the primary engine.
     */
    OREKIT("orekit", "OreKit", "ORbit Extrapolation KIT - Java space dynamics library", true),

    /**
     * GMAT - General Mission Analysis Tool
     * NASA's open-source space mission analysis tool.
     * Currently a placeholder for future implementation.
     */
    GMAT("gmat", "GMAT", "General Mission Analysis Tool - NASA mission analysis", false);

    private final String key;
    private final String displayName;
    private final String description;
    private final boolean implemented;

    /**
     * Constructor for EngineType enum.
     *
     * @param key The URL/API key for this engine
     * @param displayName Human-readable display name
     * @param description Brief description of the engine
     * @param implemented Whether this engine is currently implemented
     */
    EngineType(String key, String displayName, String description, boolean implemented) {
        this.key = key;
        this.displayName = displayName;
        this.description = description;
        this.implemented = implemented;
    }

    /**
     * Get the URL/API key for this engine type.
     *
     * @return The engine key string
     */
    public String getKey() {
        return key;
    }

    /**
     * Get the human-readable display name.
     *
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the description of this engine.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if this engine is currently implemented.
     *
     * @return true if implemented, false if placeholder
     */
    public boolean isImplemented() {
        return implemented;
    }

    /**
     * Get an EngineType from its key string.
     *
     * @param key The engine key (e.g., "orekit", "gmat")
     * @return The corresponding EngineType, or OREKIT if not found (default)
     */
    public static EngineType fromKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return OREKIT; // Default engine
        }

        String normalizedKey = key.trim().toLowerCase();
        for (EngineType engine : values()) {
            if (engine.key.equals(normalizedKey)) {
                return engine;
            }
        }

        return OREKIT; // Default fallback
    }

    /**
     * Get all implemented engines.
     *
     * @return Array of implemented EngineType values
     */
    public static EngineType[] getImplemented() {
        return java.util.Arrays.stream(values())
                .filter(EngineType::isImplemented)
                .toArray(EngineType[]::new);
    }

    @Override
    public String toString() {
        return displayName + (implemented ? "" : " (Coming Soon)");
    }
}
