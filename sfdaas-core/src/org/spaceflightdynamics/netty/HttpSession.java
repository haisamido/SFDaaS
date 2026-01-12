package org.spaceflightdynamics.netty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an HTTP session with attributes and metadata.
 * Mimics javax.servlet.http.HttpSession for compatibility with existing code.
 */
public class HttpSession {
    private final String sessionId;
    private final long creationTime;
    private long lastAccessedTime;
    private int maxInactiveInterval; // in seconds
    private final Map<String, Object> attributes;

    public HttpSession(String sessionId) {
        this.sessionId = sessionId;
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = this.creationTime;
        this.maxInactiveInterval = 1800; // Default: 30 minutes
        this.attributes = new ConcurrentHashMap<>();
    }

    /**
     * Returns the unique session identifier.
     */
    public String getId() {
        return sessionId;
    }

    /**
     * Returns the time when this session was created (milliseconds since epoch).
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Returns the last time the client sent a request associated with this session
     * (milliseconds since epoch).
     */
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    /**
     * Updates the last accessed time to now.
     */
    public void updateLastAccessedTime() {
        this.lastAccessedTime = System.currentTimeMillis();
    }

    /**
     * Sets the maximum time interval (in seconds) that the session should remain
     * active without a request.
     */
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    /**
     * Returns the maximum time interval (in seconds) that the session can be inactive.
     */
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    /**
     * Returns the attribute bound to this name in this session, or null if no
     * attribute is bound under this name.
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Binds an object to this session with the specified name.
     */
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    /**
     * Removes the attribute bound to this name in this session.
     */
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    /**
     * Checks if this session has expired based on the last accessed time
     * and the max inactive interval.
     */
    public boolean isExpired() {
        if (maxInactiveInterval <= 0) {
            return false; // Never expires
        }
        long now = System.currentTimeMillis();
        long inactiveTime = now - lastAccessedTime;
        return inactiveTime > (maxInactiveInterval * 1000L);
    }

    @Override
    public String toString() {
        return String.format("HttpSession[id=%s, created=%d, lastAccessed=%d, maxInactive=%d]",
                sessionId, creationTime, lastAccessedTime, maxInactiveInterval);
    }
}
