package org.sfdaas.api.netty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.Map;
import java.util.HashMap;

/**
 * Builds JSON responses for the HTTP API.
 * Handles formatting of propagation results, usage documentation, and error messages.
 */
public class JsonResponseBuilder {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    /**
     * Builds a JSON response for a successful propagation request.
     *
     * @param apriori Map containing initial state (t0, r0, v0)
     * @param aposteriori Map containing final state (tf, rf, vf)
     * @param diagnostics Map containing diagnostic information
     * @return JSON string
     */
    public static String buildPropagationResponse(
            Map<String, String> apriori,
            Map<String, String> aposteriori,
            Map<String, Object> diagnostics) {

        JsonObject response = new JsonObject();
        response.addProperty("status", "success");

        // Data section
        JsonObject data = new JsonObject();

        // Convert apriori map to JSON object (includes all fields)
        JsonObject aprioriObj = (JsonObject) gson.toJsonTree(apriori);
        data.add("apriori", aprioriObj);

        JsonObject aposterioriObj = new JsonObject();
        aposterioriObj.addProperty("tf", aposteriori.get("tf"));
        aposterioriObj.addProperty("rf", aposteriori.get("rf"));
        aposterioriObj.addProperty("vf", aposteriori.get("vf"));

        // Add interval states if present (CSV format with newline-separated rows)
        if (aposteriori.containsKey("intervalStates")) {
            String intervalStatesCsv = aposteriori.get("intervalStates");
            aposterioriObj.addProperty("intervalStates", intervalStatesCsv);
        }

        data.add("aposteriori", aposterioriObj);

        response.add("data", data);

        // Diagnostics section
        if (diagnostics != null && !diagnostics.isEmpty()) {
            response.add("diagnostics", gson.toJsonTree(diagnostics));
        }

        return gson.toJson(response);
    }

    /**
     * Builds a JSON response for the usage endpoint.
     *
     * @param session Current HTTP session (may be null)
     * @return JSON string
     */
    public static String buildUsageResponse(HttpSession session) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "success");
        response.addProperty("service", "Space Flight Dynamics as a Service (SFDaaS)");
        response.addProperty("version", "1.0.0");

        // Endpoints
        JsonObject endpoints = new JsonObject();
        endpoints.addProperty("usage", "/sfdaas/api/propagate/usage");
        endpoints.addProperty("propagate", "/sfdaas/api/propagate");
        response.add("endpoints", endpoints);

        // Parameters
        JsonObject parameters = new JsonObject();

        JsonObject caching = new JsonObject();
        caching.addProperty("cf", "Caching flag (0=disabled, 1=enabled). Default: 0");
        caching.addProperty("ca", "Caching server address(es), e.g., 127.0.0.1:11211");
        caching.addProperty("ct", "Cache TTL in seconds. Default: 60");
        caching.addProperty("ck", "Custom cache key (optional)");
        parameters.add("caching", caching);

        JsonObject session_params = new JsonObject();
        session_params.addProperty("sf", "Session flag (1=use session values)");
        session_params.addProperty("st", "Session timeout in seconds. Default: 1800");
        parameters.add("session", session_params);

        JsonObject propagation = new JsonObject();
        propagation.addProperty("t0", "Initial epoch (ISO 8601: YYYY-MM-DDTHH:MM:SS.SSS+00:00, UTC)");
        propagation.addProperty("tf", "Final epoch (ISO 8601: YYYY-MM-DDTHH:MM:SS.SSS+00:00, UTC)");
        propagation.addProperty("r0", "Initial position vector [x,y,z] in meters");
        propagation.addProperty("v0", "Initial velocity vector [vx,vy,vz] in m/s");
        propagation.addProperty("orbitType", "Orbit representation: cartesian (default), keplerian, circular, equinoctial. Note: Only cartesian is currently implemented");
        propagation.addProperty("propagator", "Propagator type: rungekutta (default), dormandprince, adamsbashforth, adamsmoulton");
        propagation.addProperty("stepSize", "Integrator step size in seconds. Default: 60");
        propagation.addProperty("frame", "Reference frame: eme2000 (default), gcrf, itrf, teme, mod, tod");
        propagation.addProperty("centralBody", "Central body for gravitational parameter (μ): earth (default), sun, moon, mars, jupiter, venus, saturn. Determines the central attraction coefficient used in propagation");
        propagation.addProperty("timeScale", "Time scale for epoch specification: utc (default - Coordinated Universal Time with leap seconds), tai (International Atomic Time - continuous atomic time)");
        propagation.addProperty("outputInterval", "Output interval in seconds (optional). If specified, intermediate states will be included in the response at the specified time intervals between t0 and tf");
        propagation.addProperty("forceModels", "Force models: Comma-separated list (gravity,thirdbody,drag,srp,relativity) or 'none' (default). Note: Not yet implemented - currently uses two-body dynamics only");
        parameters.add("propagation", propagation);

        response.add("parameters", parameters);

        // Example requests
        JsonArray examples = new JsonArray();

        JsonObject ex1 = new JsonObject();
        ex1.addProperty("description", "Basic propagation");
        ex1.addProperty("url", "http://localhost:8080/sfdaas/api/propagate?" +
                "t0=2010-05-28T12:00:00.000+00:00&" +
                "tf=2010-05-29T12:00:00.000+00:00&" +
                "r0=[3198022.67,2901879.73,5142928.95]&" +
                "v0=[-6129.640631,4489.647187,1284.511245]");
        examples.add(ex1);

        JsonObject ex2 = new JsonObject();
        ex2.addProperty("description", "Propagation with caching");
        ex2.addProperty("url", "http://localhost:8080/sfdaas/api/propagate?" +
                "cf=1&ca=127.0.0.1:11211&" +
                "t0=2010-05-28T12:00:00.000+00:00&" +
                "tf=2010-05-29T12:00:00.000+00:00&" +
                "r0=[3198022.67,2901879.73,5142928.95]&" +
                "v0=[-6129.640631,4489.647187,1284.511245]");
        examples.add(ex2);

        response.add("examples", examples);

        // Session info if available
        if (session != null) {
            JsonObject sessionInfo = new JsonObject();
            sessionInfo.addProperty("id", session.getId());
            sessionInfo.addProperty("creationTime", session.getCreationTime());
            sessionInfo.addProperty("lastAccessedTime", session.getLastAccessedTime());
            sessionInfo.addProperty("maxInactiveInterval", session.getMaxInactiveInterval());
            response.add("session", sessionInfo);
        }

        return gson.toJson(response);
    }

    /**
     * Builds a JSON error response.
     *
     * @param message Error message
     * @param statusCode HTTP status code
     * @return JSON string
     */
    public static String buildErrorResponse(String message, int statusCode) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "error");
        response.addProperty("message", message);
        response.addProperty("code", statusCode);
        return gson.toJson(response);
    }

    /**
     * Builds a JSON error response for missing required parameters.
     *
     * @param missingParams Array of missing parameter names
     * @return JSON string
     */
    public static String buildMissingParametersError(String[] missingParams) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "error");
        response.addProperty("message", "Missing required parameters");
        response.addProperty("code", 400);

        JsonArray params = new JsonArray();
        for (String param : missingParams) {
            params.add(param);
        }
        response.add("missingParameters", params);

        return gson.toJson(response);
    }
}
