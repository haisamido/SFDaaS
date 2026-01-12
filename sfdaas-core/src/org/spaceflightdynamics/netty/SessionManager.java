package org.spaceflightdynamics.netty;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages HTTP sessions in memory.
 * Thread-safe session storage with automatic cleanup of expired sessions.
 */
public class SessionManager {
    private final Map<String, HttpSession> sessions;
    private final ScheduledExecutorService cleanupExecutor;

    public SessionManager() {
        this.sessions = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        startCleanupTask();
    }

    /**
     * Creates a new HTTP session with a unique ID.
     */
    public HttpSession createSession() {
        String sessionId = generateSessionId();
        HttpSession session = new HttpSession(sessionId);
        sessions.put(sessionId, session);
        System.out.println("Created new session: " + sessionId);
        return session;
    }

    /**
     * Retrieves an existing session by ID, or null if not found or expired.
     */
    public HttpSession getSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        HttpSession session = sessions.get(sessionId);
        if (session != null && session.isExpired()) {
            sessions.remove(sessionId);
            System.out.println("Removed expired session: " + sessionId);
            return null;
        }
        return session;
    }

    /**
     * Retrieves an existing session or creates a new one if not found.
     * If create is false and session doesn't exist, returns null.
     */
    public HttpSession getSession(String sessionId, boolean create) {
        HttpSession session = getSession(sessionId);
        if (session == null && create) {
            session = createSession();
        }
        return session;
    }

    /**
     * Removes a session from the manager.
     */
    public void removeSession(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
            System.out.println("Removed session: " + sessionId);
        }
    }

    /**
     * Generates a unique session ID using UUID.
     */
    private String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Starts a background task to clean up expired sessions every 60 seconds.
     */
    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            cleanupExpiredSessions();
        }, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * Removes all expired sessions from the session store.
     */
    private void cleanupExpiredSessions() {
        int removedCount = 0;
        for (Map.Entry<String, HttpSession> entry : sessions.entrySet()) {
            if (entry.getValue().isExpired()) {
                sessions.remove(entry.getKey());
                removedCount++;
            }
        }
        if (removedCount > 0) {
            System.out.println("Cleaned up " + removedCount + " expired session(s)");
        }
    }

    /**
     * Returns the current number of active sessions.
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Shuts down the cleanup task executor.
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
