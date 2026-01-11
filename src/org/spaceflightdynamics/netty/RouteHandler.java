package org.spaceflightdynamics.netty;

import io.netty.handler.codec.http.FullHttpRequest;
import org.spaceflightdynamics.propagation.Propagator;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Handles routing and business logic for different HTTP endpoints.
 */
public class RouteHandler {

    /**
     * Handles the /orekit/propagate/usage endpoint.
     * Returns usage documentation in JSON format.
     */
    public static String handleUsage(FullHttpRequest request, HttpSession session, String remoteAddress) {
        return JsonResponseBuilder.buildUsageResponse(session);
    }

    /**
     * Handles the /orekit/propagate endpoint.
     * Performs orbit propagation with optional caching.
     */
    public static String handlePropagate(
            FullHttpRequest request,
            HttpSession session,
            Map<String, String> params,
            String remoteAddress) {

        long startTime = System.currentTimeMillis();

        // Date formatter for diagnostics
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS Z");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Extract parameters
        String ca = params.get("ca"); // Cache server addresses
        String cf = params.getOrDefault("cf", "0"); // Cache flag (0=disabled, 1=enabled)
        String ct = params.getOrDefault("ct", "60"); // Cache TTL in seconds
        String ck = params.get("ck"); // Custom cache key

        String sf = params.get("sf"); // Session flag
        String st = params.getOrDefault("st", "1800"); // Session timeout

        String t0 = params.get("t0"); // Initial epoch
        String r0 = params.get("r0"); // Initial position
        String v0 = params.get("v0"); // Initial velocity
        String tf = params.get("tf"); // Final epoch

        // Validate required parameters
        if (t0 == null || r0 == null || v0 == null || tf == null) {
            return JsonResponseBuilder.buildMissingParametersError(
                    new String[]{"t0", "r0", "v0", "tf"});
        }

        // Update session timeout if requested
        if (sf != null && st != null) {
            try {
                session.setMaxInactiveInterval(Integer.parseInt(st));
            } catch (NumberFormatException e) {
                // Ignore invalid session timeout
            }
        }

        // Build apriori state for response
        Map<String, String> apriori = new HashMap<>();
        apriori.put("t0", t0);
        apriori.put("r0", r0);
        apriori.put("v0", v0);

        // Initialize diagnostics
        Map<String, Object> diagnostics = new HashMap<>();
        Map<String, Object> cachingInfo = new HashMap<>();
        Map<String, Object> timingInfo = new HashMap<>();

        // Build cache key
        String username = System.getProperty("user.name");
        String sessionId = session.getId();
        String cacheKey = username + "]|[" + sessionId + "]|[" + t0 + "]|[" + r0 + "]|[" + v0 + "]|[" + tf;
        if (ck != null) {
            cacheKey = ck; // Use custom cache key if provided
        }

        // Propagation results
        Map<String, String> aposteriori = new HashMap<>();
        boolean cacheHit = false;
        long propagationStart = 0;
        long propagationEnd = 0;

        try {
            // Caching logic
            if ("1".equals(cf)) {
                if (ca == null || ca.trim().isEmpty()) {
                    return JsonResponseBuilder.buildErrorResponse(
                            "Caching enabled (cf=1) but no cache server address provided (ca parameter missing)",
                            400);
                }

                cachingInfo.put("enabled", true);
                cachingInfo.put("servers", ca.split("\\s+"));
                cachingInfo.put("ttl", Integer.parseInt(ct));
                cachingInfo.put("key", cacheKey);

                // Try to get from cache
                MemcachedClient cache = new MemcachedClient(
                        new BinaryConnectionFactory(),
                        AddrUtil.getAddresses(ca));

                Object cachedContent = cache.get(cacheKey);

                if (cachedContent != null) {
                    // Cache hit
                    cacheHit = true;
                    @SuppressWarnings("unchecked")
                    HashMap<String, String> finalState = (HashMap<String, String>) cachedContent;

                    aposteriori.put("tf", finalState.get("tf"));
                    aposteriori.put("rf", finalState.get("rf"));
                    aposteriori.put("vf", finalState.get("vf"));

                    cachingInfo.put("hit", true);
                    cachingInfo.put("retrievedAt", df.format(new Date()));
                    cache.shutdown();
                } else {
                    // Cache miss - need to propagate
                    cachingInfo.put("hit", false);

                    propagationStart = System.currentTimeMillis();
                    Propagator propagator = new Propagator(r0, v0, t0, tf);
                    HashMap<String, String> finalState = propagator.propagate();
                    propagationEnd = System.currentTimeMillis();

                    aposteriori.put("tf", finalState.get("tf"));
                    aposteriori.put("rf", finalState.get("rf"));
                    aposteriori.put("vf", finalState.get("vf"));

                    // Store in cache
                    cache.set(cacheKey, Integer.parseInt(ct), finalState);
                    cachingInfo.put("storedAt", df.format(new Date()));
                    cachingInfo.put("expiresAt", df.format(new Date(System.currentTimeMillis() + Integer.parseInt(ct) * 1000L)));
                    cache.shutdown();
                }
            } else {
                // No caching - just propagate
                cachingInfo.put("enabled", false);

                propagationStart = System.currentTimeMillis();
                Propagator propagator = new Propagator(r0, v0, t0, tf);
                HashMap<String, String> finalState = propagator.propagate();
                propagationEnd = System.currentTimeMillis();

                aposteriori.put("tf", finalState.get("tf"));
                aposteriori.put("rf", finalState.get("rf"));
                aposteriori.put("vf", finalState.get("vf"));
            }

            // Build assumptions section
            Map<String, String> assumptions = new HashMap<>();
            assumptions.put("1", "The epochs, t0 and tf, are assumed to be in UTC.");
            assumptions.put("2", "The radius and velocity vectors are in meters and meters/second, respectively.");
            assumptions.put("3", "The frame is assumed to be the J2000 Earth-centered one.");

            // Build timing info with propagation timestamps
            if (propagationStart > 0 && propagationEnd > 0) {
                timingInfo.put("propagationTimeMs", propagationEnd - propagationStart);
                timingInfo.put("propagationStart", df.format(new Date(propagationStart)));
                timingInfo.put("propagationEnd", df.format(new Date(propagationEnd)));
            }
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            timingInfo.put("totalTimeMs", totalTime);
            timingInfo.put("runStart", df.format(new Date(startTime)));
            timingInfo.put("runStop", df.format(new Date(endTime)));

            // Build session info with detailed properties
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("jsessionid", session.getId());
            sessionInfo.put("created", df.format(new Date(session.getCreationTime())));
            sessionInfo.put("creationTime", session.getCreationTime());
            sessionInfo.put("lastAccessedTime", session.getLastAccessedTime());
            sessionInfo.put("lastAccessed", df.format(new Date(session.getLastAccessedTime())));
            sessionInfo.put("maxInactiveInterval", session.getMaxInactiveInterval());
            sessionInfo.put("expiryIn", session.getMaxInactiveInterval() + " seconds");
            long expiryTime = session.getLastAccessedTime() + (session.getMaxInactiveInterval() * 1000L);
            sessionInfo.put("expiryDate", df.format(new Date(expiryTime)));

            // Build request info with full details
            Map<String, Object> requestInfo = new HashMap<>();
            requestInfo.put("method", request.method().name());
            requestInfo.put("uri", request.uri());
            requestInfo.put("protocol", request.protocolVersion().text());
            requestInfo.put("remoteAddress", remoteAddress);

            // Parse query string
            String uri = request.uri();
            String queryString = null;
            if (uri.contains("?")) {
                queryString = uri.substring(uri.indexOf("?") + 1);
            }
            requestInfo.put("queryString", queryString);
            requestInfo.put("queryDecoded", queryString);

            // Add authentication info (currently null for this implementation)
            requestInfo.put("authentication", null);
            requestInfo.put("remoteUser", null);
            requestInfo.put("pathInfo", null);

            // Full request URL
            String host = request.headers().get("host");
            String scheme = "http"; // Could detect https if needed
            String fullUrl = scheme + "://" + host + uri;
            requestInfo.put("requestURL", fullUrl);
            requestInfo.put("requestURI", uri);

            // Extract headers
            Map<String, String> headers = new HashMap<>();
            request.headers().forEach(entry -> {
                headers.put(entry.getKey(), entry.getValue());
            });
            requestInfo.put("headers", headers);

            // Build system properties
            Map<String, String> systemInfo = new HashMap<>();
            systemInfo.put("username", System.getProperty("user.name"));
            systemInfo.put("homeDirectory", System.getProperty("user.home"));
            systemInfo.put("userCWD", System.getProperty("user.dir"));

            // Build OreKit info
            Map<String, String> orekitInfo = new HashMap<>();
            orekitInfo.put("version", "13.1.2");
            String orekitDataPath = System.getProperty("orekit.data.path", "./data");
            orekitInfo.put("dataPath", orekitDataPath);
            orekitInfo.put("orekitDataPathProperty", "orekit.data.path");

            // Assemble diagnostics
            diagnostics.put("assumptions", assumptions);
            diagnostics.put("timing", timingInfo);
            diagnostics.put("propagation", timingInfo); // Alias for compatibility
            diagnostics.put("caching", cachingInfo);
            diagnostics.put("session", sessionInfo);
            diagnostics.put("request", requestInfo);
            diagnostics.put("system", systemInfo);
            diagnostics.put("orekit", orekitInfo);

            return JsonResponseBuilder.buildPropagationResponse(apriori, aposteriori, diagnostics);

        } catch (Exception e) {
            e.printStackTrace();
            return JsonResponseBuilder.buildErrorResponse(
                    "Error during propagation: " + e.getMessage(),
                    500);
        }
    }

    /**
     * Handles 404 Not Found errors.
     */
    public static String handle404(String uri) {
        return JsonResponseBuilder.buildErrorResponse(
                "Endpoint not found: " + uri,
                404);
    }
}
