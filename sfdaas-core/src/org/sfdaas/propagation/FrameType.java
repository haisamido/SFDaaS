package org.sfdaas.propagation;

/**
 * Enum representing available reference frames for orbit propagation.
 * Each frame type corresponds to a specific inertial or Earth-fixed reference frame.
 *
 * @author SFDaaS
 */
public enum FrameType {

    /**
     * EME2000 - Earth Mean Equator 2000 (J2000) - Inertial
     * The standard inertial frame for orbit propagation
     */
    EME2000("eme2000", "EME2000 (J2000)", "Inertial frame based on Earth's mean equator and equinox at J2000.0"),

    /**
     * GCRF - Geocentric Celestial Reference Frame - Inertial
     * The IAU 2000 recommended inertial frame
     */
    GCRF("gcrf", "GCRF", "Geocentric Celestial Reference Frame (IAU 2000)"),

    /**
     * ITRF - International Terrestrial Reference Frame - Earth-fixed
     * Rotating frame fixed to the Earth
     */
    ITRF("itrf", "ITRF", "International Terrestrial Reference Frame (Earth-fixed, rotating)"),

    /**
     * TEME - True Equator Mean Equinox - Inertial
     * Used by SGP4/SDP4 propagators and TLE elements
     */
    TEME("teme", "TEME", "True Equator Mean Equinox (used by TLE/SGP4)"),

    /**
     * MOD - Mean of Date - Inertial
     * Mean equator and equinox of date
     */
    MOD("mod", "MOD", "Mean Equator and Equinox of Date"),

    /**
     * TOD - True of Date - Inertial
     * True equator and equinox of date
     */
    TOD("tod", "TOD", "True Equator and Equinox of Date");

    private final String key;
    private final String displayName;
    private final String description;

    FrameType(String key, String displayName, String description) {
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
     * Get FrameType from string key
     * @param key The string key (e.g., "eme2000", "gcrf", "itrf")
     * @return The corresponding FrameType
     * @throws IllegalArgumentException if key is not recognized
     */
    public static FrameType fromKey(String key) {
        if (key == null) {
            return EME2000; // Default
        }

        String normalizedKey = key.toLowerCase().trim();
        for (FrameType type : values()) {
            if (type.key.equals(normalizedKey)) {
                return type;
            }
        }

        // Default to EME2000 if not found
        return EME2000;
    }
}
