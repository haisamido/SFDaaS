package org.spaceflightdynamics.netty;

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

        JsonObject aprioriObj = new JsonObject();
        aprioriObj.addProperty("t0", apriori.get("t0"));
        aprioriObj.addProperty("r0", apriori.get("r0"));
        aprioriObj.addProperty("v0", apriori.get("v0"));
        data.add("apriori", aprioriObj);

        JsonObject aposterioriObj = new JsonObject();
        aposterioriObj.addProperty("tf", aposteriori.get("tf"));
        aposterioriObj.addProperty("rf", aposteriori.get("rf"));
        aposterioriObj.addProperty("vf", aposteriori.get("vf"));
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
        endpoints.addProperty("usage", "/SFDaaS/orekit/propagate/usage");
        endpoints.addProperty("propagate", "/SFDaaS/orekit/propagate");
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
        propagation.addProperty("t0", "Initial epoch (format: YYYY-MM-DDTHH:MM:SS.SSS, UTC)");
        propagation.addProperty("tf", "Final epoch (same format as t0)");
        propagation.addProperty("r0", "Initial position vector [x,y,z] in meters (J2000 frame)");
        propagation.addProperty("v0", "Initial velocity vector [vx,vy,vz] in m/s");
        parameters.add("propagation", propagation);

        response.add("parameters", parameters);

        // Example requests
        JsonArray examples = new JsonArray();

        JsonObject ex1 = new JsonObject();
        ex1.addProperty("description", "Basic propagation");
        ex1.addProperty("url", "http://localhost:8080/SFDaaS/orekit/propagate?" +
                "t0=2010-05-28T12:00:00.000&" +
                "tf=2010-05-29T12:00:00.000&" +
                "r0=[3198022.67,2901879.73,5142928.95]&" +
                "v0=[-6129.640631,4489.647187,1284.511245]");
        examples.add(ex1);

        JsonObject ex2 = new JsonObject();
        ex2.addProperty("description", "Propagation with caching");
        ex2.addProperty("url", "http://localhost:8080/SFDaaS/orekit/propagate?" +
                "cf=1&ca=127.0.0.1:11211&" +
                "t0=2010-05-28T12:00:00.000&" +
                "tf=2010-05-29T12:00:00.000&" +
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
