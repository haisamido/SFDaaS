package org.sfdaas.wasm;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSExport;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSString;

/**
 * WebAssembly entry point for SFDaaS.
 * Exposes orbit propagation functionality to JavaScript via TeaVM.
 *
 * Usage from JavaScript:
 *   const result = SFDaaS.propagate({
 *     t0: "2010-05-28T12:00:00.000+00:00",
 *     tf: "2010-05-29T12:00:00.000+00:00",
 *     r0: [3198022.67, 2901879.73, 5142928.95],
 *     v0: [-6129.640631, 4489.647187, 1284.511245],
 *     propagator: "rungekutta",
 *     stepSize: 60
 *   });
 */
public class SFDaaSWasm {

    private static boolean initialized = false;

    /**
     * Main entry point - called when WASM module loads
     */
    public static void main(String[] args) {
        log("SFDaaS WASM module loading...");
        initialize();
        log("SFDaaS WASM module ready!");

        // Notify JavaScript that module is ready
        onModuleReady();
    }

    /**
     * Initialize the propagation engine.
     * NOTE: In WASM context, Orekit data must be pre-loaded or fetched via JS interop.
     */
    @JSExport
    public static void initialize() {
        if (initialized) {
            return;
        }

        try {
            // TODO: Initialize Orekit with embedded or fetched data
            // In browser context, we cannot use file system directly
            // Options:
            // 1. Embed minimal ephemeris data in WASM binary
            // 2. Fetch data via JavaScript and pass to WASM
            // 3. Use simplified two-body dynamics without ephemeris

            log("Initializing SFDaaS propagation engine...");

            // For now, use simplified two-body dynamics
            initialized = true;

            log("SFDaaS engine initialized (two-body dynamics mode)");
        } catch (Exception e) {
            logError("Failed to initialize SFDaaS: " + e.getMessage());
        }
    }

    /**
     * Propagate orbit from initial state to final epoch.
     *
     * @param params JSON object with propagation parameters
     * @return JSON string with propagation results
     */
    @JSExport
    public static String propagate(JSObject params) {
        if (!initialized) {
            return errorJson("SFDaaS not initialized. Call initialize() first.");
        }

        try {
            // Extract parameters from JavaScript object
            String t0 = getStringParam(params, "t0");
            String tf = getStringParam(params, "tf");
            double[] r0 = getDoubleArrayParam(params, "r0");
            double[] v0 = getDoubleArrayParam(params, "v0");
            String propagator = getStringParamOrDefault(params, "propagator", "rungekutta");
            double stepSize = getDoubleParamOrDefault(params, "stepSize", 60.0);
            String frame = getStringParamOrDefault(params, "frame", "eme2000");
            String centralBody = getStringParamOrDefault(params, "centralBody", "earth");
            double outputInterval = getDoubleParamOrDefault(params, "outputInterval", 0.0);

            log("Propagating from " + t0 + " to " + tf);

            // Perform propagation using simplified two-body dynamics
            // TODO: Integrate with actual Orekit propagator when data loading is resolved
            PropagationResult result = propagateTwoBody(t0, tf, r0, v0, centralBody, stepSize, outputInterval);

            return result.toJson();

        } catch (Exception e) {
            logError("Propagation failed: " + e.getMessage());
            return errorJson("Propagation failed: " + e.getMessage());
        }
    }

    /**
     * Get the current version of SFDaaS WASM module.
     */
    @JSExport
    public static String getVersion() {
        return "1.0.0-wasm";
    }

    /**
     * Check if the module is initialized and ready.
     */
    @JSExport
    public static boolean isReady() {
        return initialized;
    }

    // =========================================================================
    // Two-Body Propagation (Simplified Keplerian dynamics)
    // =========================================================================

    /**
     * Simplified two-body propagation using Keplerian dynamics.
     * This is a fallback when full Orekit data is not available.
     */
    private static PropagationResult propagateTwoBody(
            String t0, String tf, double[] r0, double[] v0,
            String centralBody, double stepSize, double outputInterval) {

        // Get gravitational parameter for central body
        double mu = getGravitationalParameter(centralBody);

        // Parse epochs (simplified - assumes UTC)
        long t0Millis = parseIso8601ToMillis(t0);
        long tfMillis = parseIso8601ToMillis(tf);
        double dt = (tfMillis - t0Millis) / 1000.0;

        // Convert Cartesian to orbital elements
        OrbitalElements elements = cartesianToOrbital(r0, v0, mu);
        OrbitalElements originalElements = new OrbitalElements();
        originalElements.a = elements.a;
        originalElements.e = elements.e;
        originalElements.i = elements.i;
        originalElements.omega = elements.omega;
        originalElements.OMEGA = elements.OMEGA;
        originalElements.M = elements.M;

        // Mean motion
        double n = Math.sqrt(mu / Math.pow(elements.a, 3));

        // Generate interval states if outputInterval is specified
        StringBuilder intervalStates = null;
        if (outputInterval > 0 && dt > outputInterval) {
            intervalStates = new StringBuilder();
            int numSteps = (int) Math.floor(dt / outputInterval);

            for (int step = 0; step <= numSteps; step++) {
                double tStep = step * outputInterval;
                if (tStep > dt) tStep = dt;

                // Calculate epoch for this step
                long stepMillis = t0Millis + (long)(tStep * 1000);
                String stepEpoch = millisToIso8601(stepMillis);

                // Propagate to this time
                double M_step = originalElements.M + n * tStep;
                M_step = M_step % (2 * Math.PI);
                if (M_step < 0) M_step += 2 * Math.PI;

                elements.M = M_step;
                double[] state = orbitalToCartesian(elements, mu);

                // Append to CSV: t,rx,ry,rz,vx,vy,vz
                if (step > 0) intervalStates.append("\n");
                intervalStates.append(String.format("%s,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f",
                    stepEpoch, state[0], state[1], state[2], state[3], state[4], state[5]));
            }
        }

        // Propagate mean anomaly to final epoch
        double Mf = originalElements.M + n * dt;

        // Normalize mean anomaly to [0, 2*PI]
        Mf = Mf % (2 * Math.PI);
        if (Mf < 0) Mf += 2 * Math.PI;

        // Convert back to Cartesian
        elements.M = Mf;
        double[] rfvf = orbitalToCartesian(elements, mu);

        double[] rf = new double[] { rfvf[0], rfvf[1], rfvf[2] };
        double[] vf = new double[] { rfvf[3], rfvf[4], rfvf[5] };

        PropagationResult result = new PropagationResult(t0, tf, r0, v0, rf, vf);
        if (intervalStates != null) {
            result.intervalStates = intervalStates.toString();
        }
        return result;
    }

    /**
     * Convert milliseconds since epoch to ISO 8601 string.
     */
    private static String millisToIso8601(long millis) {
        // Simplified conversion
        long totalSeconds = millis / 1000;
        int ms = (int)(millis % 1000);

        long days = totalSeconds / 86400;
        int timeOfDay = (int)(totalSeconds % 86400);
        int hours = timeOfDay / 3600;
        int minutes = (timeOfDay % 3600) / 60;
        int seconds = timeOfDay % 60;

        // Calculate year, month, day from days since 1970
        int year = 1970;
        int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        while (true) {
            int daysInYear = isLeapYear(year) ? 366 : 365;
            if (days < daysInYear) break;
            days -= daysInYear;
            year++;
        }

        if (isLeapYear(year)) daysInMonth[1] = 29;

        int month = 0;
        while (days >= daysInMonth[month]) {
            days -= daysInMonth[month];
            month++;
        }
        month++; // 1-indexed
        int day = (int)days + 1;

        return String.format("%04d-%02d-%02dT%02d:%02d:%02d.%03d+00:00",
            year, month, day, hours, minutes, seconds, ms);
    }

    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    /**
     * Get gravitational parameter (GM) for a celestial body.
     */
    private static double getGravitationalParameter(String body) {
        switch (body.toLowerCase()) {
            case "earth": return 3.986004418e14;
            case "sun": return 1.32712440018e20;
            case "moon": return 4.9028e12;
            case "mars": return 4.282837e13;
            case "jupiter": return 1.26686534e17;
            case "venus": return 3.24859e14;
            case "saturn": return 3.7931187e16;
            default: return 3.986004418e14; // Default to Earth
        }
    }

    /**
     * Parse duration between two ISO 8601 timestamps in seconds.
     * Simplified parser - assumes both times are in UTC.
     */
    private static double parseDurationSeconds(String t0, String tf) {
        // Simplified parsing - extract components
        // Format: YYYY-MM-DDTHH:MM:SS.SSS+00:00
        try {
            long ms0 = parseIso8601ToMillis(t0);
            long msf = parseIso8601ToMillis(tf);
            return (msf - ms0) / 1000.0;
        } catch (Exception e) {
            // Default to 1 day if parsing fails
            return 86400.0;
        }
    }

    private static long parseIso8601ToMillis(String iso) {
        // Very simplified ISO 8601 parser
        // Format: YYYY-MM-DDTHH:MM:SS.SSS+00:00
        String[] parts = iso.split("T");
        String[] dateParts = parts[0].split("-");
        String timePart = parts[1].split("\\+")[0];
        String[] timeParts = timePart.split(":");

        int year = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]);
        int day = Integer.parseInt(dateParts[2]);
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        double second = Double.parseDouble(timeParts[2]);

        // Simplified calculation (doesn't account for leap years properly)
        long days = (year - 1970) * 365L + (year - 1969) / 4;
        int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        for (int i = 0; i < month - 1; i++) {
            days += daysInMonth[i];
        }
        days += day - 1;

        long millis = days * 86400000L;
        millis += hour * 3600000L;
        millis += minute * 60000L;
        millis += (long)(second * 1000);

        return millis;
    }

    // =========================================================================
    // Orbital Mechanics Utilities
    // =========================================================================

    private static class OrbitalElements {
        double a;     // Semi-major axis
        double e;     // Eccentricity
        double i;     // Inclination
        double omega; // Argument of perigee
        double OMEGA; // Right ascension of ascending node
        double M;     // Mean anomaly
    }

    /**
     * Convert Cartesian state to orbital elements.
     */
    private static OrbitalElements cartesianToOrbital(double[] r, double[] v, double mu) {
        OrbitalElements el = new OrbitalElements();

        double rMag = Math.sqrt(r[0]*r[0] + r[1]*r[1] + r[2]*r[2]);
        double vMag = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);

        // Specific angular momentum
        double[] h = new double[] {
            r[1]*v[2] - r[2]*v[1],
            r[2]*v[0] - r[0]*v[2],
            r[0]*v[1] - r[1]*v[0]
        };
        double hMag = Math.sqrt(h[0]*h[0] + h[1]*h[1] + h[2]*h[2]);

        // Node vector
        double[] n = new double[] { -h[1], h[0], 0 };
        double nMag = Math.sqrt(n[0]*n[0] + n[1]*n[1]);

        // Eccentricity vector
        double rdotv = r[0]*v[0] + r[1]*v[1] + r[2]*v[2];
        double[] eVec = new double[] {
            (vMag*vMag - mu/rMag) * r[0] / mu - rdotv * v[0] / mu,
            (vMag*vMag - mu/rMag) * r[1] / mu - rdotv * v[1] / mu,
            (vMag*vMag - mu/rMag) * r[2] / mu - rdotv * v[2] / mu
        };
        el.e = Math.sqrt(eVec[0]*eVec[0] + eVec[1]*eVec[1] + eVec[2]*eVec[2]);

        // Semi-major axis
        double energy = vMag*vMag/2 - mu/rMag;
        el.a = -mu / (2 * energy);

        // Inclination
        el.i = Math.acos(h[2] / hMag);

        // Right ascension of ascending node
        if (nMag > 1e-10) {
            el.OMEGA = Math.acos(n[0] / nMag);
            if (n[1] < 0) el.OMEGA = 2*Math.PI - el.OMEGA;
        } else {
            el.OMEGA = 0;
        }

        // Argument of perigee
        if (nMag > 1e-10 && el.e > 1e-10) {
            double ndote = n[0]*eVec[0] + n[1]*eVec[1] + n[2]*eVec[2];
            el.omega = Math.acos(ndote / (nMag * el.e));
            if (eVec[2] < 0) el.omega = 2*Math.PI - el.omega;
        } else {
            el.omega = 0;
        }

        // True anomaly
        double trueAnomaly;
        if (el.e > 1e-10) {
            double edotr = eVec[0]*r[0] + eVec[1]*r[1] + eVec[2]*r[2];
            trueAnomaly = Math.acos(edotr / (el.e * rMag));
            if (rdotv < 0) trueAnomaly = 2*Math.PI - trueAnomaly;
        } else {
            trueAnomaly = 0;
        }

        // Mean anomaly from true anomaly
        double E = 2 * Math.atan(Math.sqrt((1-el.e)/(1+el.e)) * Math.tan(trueAnomaly/2));
        el.M = E - el.e * Math.sin(E);
        if (el.M < 0) el.M += 2*Math.PI;

        return el;
    }

    /**
     * Convert orbital elements to Cartesian state.
     */
    private static double[] orbitalToCartesian(OrbitalElements el, double mu) {
        // Solve Kepler's equation for eccentric anomaly
        double E = solveKepler(el.M, el.e);

        // True anomaly
        double nu = 2 * Math.atan(Math.sqrt((1+el.e)/(1-el.e)) * Math.tan(E/2));

        // Distance
        double r = el.a * (1 - el.e * Math.cos(E));

        // Position in orbital plane
        double xOrbital = r * Math.cos(nu);
        double yOrbital = r * Math.sin(nu);

        // Velocity in orbital plane
        double p = el.a * (1 - el.e*el.e);
        double h = Math.sqrt(mu * p);
        double vxOrbital = -mu/h * Math.sin(nu);
        double vyOrbital = mu/h * (el.e + Math.cos(nu));

        // Rotation matrices
        double cosO = Math.cos(el.OMEGA);
        double sinO = Math.sin(el.OMEGA);
        double cosw = Math.cos(el.omega);
        double sinw = Math.sin(el.omega);
        double cosi = Math.cos(el.i);
        double sini = Math.sin(el.i);

        // Transform to inertial frame
        double[] result = new double[6];

        // Position
        result[0] = (cosO*cosw - sinO*sinw*cosi) * xOrbital + (-cosO*sinw - sinO*cosw*cosi) * yOrbital;
        result[1] = (sinO*cosw + cosO*sinw*cosi) * xOrbital + (-sinO*sinw + cosO*cosw*cosi) * yOrbital;
        result[2] = (sinw*sini) * xOrbital + (cosw*sini) * yOrbital;

        // Velocity
        result[3] = (cosO*cosw - sinO*sinw*cosi) * vxOrbital + (-cosO*sinw - sinO*cosw*cosi) * vyOrbital;
        result[4] = (sinO*cosw + cosO*sinw*cosi) * vxOrbital + (-sinO*sinw + cosO*cosw*cosi) * vyOrbital;
        result[5] = (sinw*sini) * vxOrbital + (cosw*sini) * vyOrbital;

        return result;
    }

    /**
     * Solve Kepler's equation M = E - e*sin(E) using Newton-Raphson.
     */
    private static double solveKepler(double M, double e) {
        double E = M; // Initial guess
        for (int i = 0; i < 50; i++) {
            double dE = (E - e * Math.sin(E) - M) / (1 - e * Math.cos(E));
            E -= dE;
            if (Math.abs(dE) < 1e-12) break;
        }
        return E;
    }

    // =========================================================================
    // Result class
    // =========================================================================

    private static class PropagationResult {
        String t0, tf;
        double[] r0, v0, rf, vf;
        String intervalStates;  // CSV format: t,rx,ry,rz,vx,vy,vz

        PropagationResult(String t0, String tf, double[] r0, double[] v0, double[] rf, double[] vf) {
            this.t0 = t0;
            this.tf = tf;
            this.r0 = r0;
            this.v0 = v0;
            this.rf = rf;
            this.vf = vf;
            this.intervalStates = null;
        }

        String toJson() {
            StringBuilder json = new StringBuilder();
            json.append("{\"status\":\"success\",\"data\":{");
            json.append(String.format(
                "\"apriori\":{\"t0\":\"%s\",\"r0\":[%.6f,%.6f,%.6f],\"v0\":[%.6f,%.6f,%.6f]},",
                t0, r0[0], r0[1], r0[2], v0[0], v0[1], v0[2]));
            json.append(String.format(
                "\"aposteriori\":{\"tf\":\"%s\",\"rf\":[%.6f,%.6f,%.6f],\"vf\":[%.6f,%.6f,%.6f]",
                tf, rf[0], rf[1], rf[2], vf[0], vf[1], vf[2]));

            if (intervalStates != null && !intervalStates.isEmpty()) {
                // Escape the interval states for JSON (replace newlines with \n)
                String escaped = intervalStates.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
                json.append(",\"intervalStates\":\"").append(escaped).append("\"");
            }

            json.append("}},\"engine\":\"wasm-twoBody\",\"version\":\"");
            json.append(getVersion());
            json.append("\"}");

            return json.toString();
        }
    }

    // =========================================================================
    // JavaScript Interop
    // =========================================================================

    @JSBody(params = {"message"}, script = "console.log('[SFDaaS WASM] ' + message);")
    private static native void log(String message);

    @JSBody(params = {"message"}, script = "console.error('[SFDaaS WASM] ' + message);")
    private static native void logError(String message);

    @JSBody(script = "if (typeof window.onSFDaaSReady === 'function') { window.onSFDaaSReady(); }")
    private static native void onModuleReady();

    @JSBody(params = {"obj", "key"}, script = "return obj[key] || '';")
    private static native String getStringParam(JSObject obj, String key);

    @JSBody(params = {"obj", "key", "defaultVal"}, script = "return obj[key] || defaultVal;")
    private static native String getStringParamOrDefault(JSObject obj, String key, String defaultVal);

    @JSBody(params = {"obj", "key", "defaultVal"}, script = "return obj[key] !== undefined ? obj[key] : defaultVal;")
    private static native double getDoubleParamOrDefault(JSObject obj, String key, double defaultVal);

    @JSBody(params = {"obj", "key"}, script = "return obj[key] || [];")
    private static native double[] getDoubleArrayParam(JSObject obj, String key);

    private static String errorJson(String message) {
        return "{\"status\":\"error\",\"message\":\"" + message.replace("\"", "\\\"") + "\"}";
    }
}
