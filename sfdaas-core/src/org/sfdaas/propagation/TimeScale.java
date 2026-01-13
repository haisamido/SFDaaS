package org.sfdaas.propagation;

/**
 * Enumeration of supported time scales for epoch specification.
 *
 * Time scales define how time is measured and are critical for accurate
 * orbit propagation. Different time scales account for Earth's rotation
 * irregularities and relativistic effects.
 *
 * @author Haisam K. Ido <haisam.ido@gmail.com>
 * @license LGPL v3.0
 */
public enum TimeScale {
    /**
     * Coordinated Universal Time (UTC)
     * - Civil time standard based on atomic time with leap seconds
     * - Most commonly used time scale for general purposes
     * - Accounts for Earth's irregular rotation
     * - Default time scale for SFDaaS
     */
    UTC("utc", "Coordinated Universal Time (UTC)"),

    /**
     * International Atomic Time (TAI)
     * - Continuous atomic time scale without leap seconds
     * - More uniform than UTC
     * - TAI = UTC + (leap seconds)
     * - Used in precise scientific calculations
     */
    TAI("tai", "International Atomic Time (TAI)");

    private final String key;
    private final String displayName;

    /**
     * Constructor for TimeScale enum.
     *
     * @param key String identifier used in API requests (lowercase)
     * @param displayName Human-readable name for display
     */
    TimeScale(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    /**
     * Get the string key for this time scale.
     *
     * @return String key (e.g., "utc", "tai")
     */
    public String getKey() {
        return key;
    }

    /**
     * Get the display name for this time scale.
     *
     * @return Human-readable name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parse a time scale from a string key.
     * Case-insensitive lookup.
     *
     * @param key String key to parse (e.g., "utc", "UTC", "tai")
     * @return Corresponding TimeScale enum value
     * @throws IllegalArgumentException if key is not recognized
     */
    public static TimeScale fromKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return UTC; // Default to UTC
        }

        String lowerKey = key.trim().toLowerCase();
        for (TimeScale ts : TimeScale.values()) {
            if (ts.key.equals(lowerKey)) {
                return ts;
            }
        }

        // Default to UTC if unrecognized
        return UTC;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
