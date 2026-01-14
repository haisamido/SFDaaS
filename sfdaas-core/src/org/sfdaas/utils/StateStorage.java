package org.sfdaas.utils;

import java.net.URI;
import java.net.URISyntaxException;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Utility class for storing propagation states to external storage systems.
 * Currently supports Redis for interval state storage.
 *
 * Redis URL format: redis://host:port/db/prefix
 * Examples:
 *   redis://localhost:6379/0/sfdaas:states
 *   redis://localhost/0/myprefix
 *   redis://redis-server:6379/1/orbit:data
 */
public class StateStorage {

    private Jedis jedis;
    private boolean redisEnabled;
    private String redisPrefix;

    /**
     * Creates a new StateStorage instance from a cache URL.
     *
     * @param cacheUrl Cache URL in format: redis://host:port/db/prefix
     *                 Examples: redis://localhost:6379/0/sfdaas:states
     *                          redis://localhost/0/myprefix (port defaults to 6379)
     */
    public StateStorage(String cacheUrl) {
        this.jedis = null;
        this.redisEnabled = false;
        this.redisPrefix = "sfdaas:states";

        if (cacheUrl == null || cacheUrl.trim().isEmpty()) {
            return;
        }

        // Parse redis:// URL
        if (cacheUrl.startsWith("redis://")) {
            parseRedisUrl(cacheUrl);
        }
    }

    /**
     * Parses a Redis URL and establishes connection.
     * Format: redis://host:port/db/prefix
     *
     * @param redisUrl The Redis URL to parse
     */
    private void parseRedisUrl(String redisUrl) {
        try {
            URI uri = new URI(redisUrl);

            String host = uri.getHost();
            if (host == null || host.trim().isEmpty()) {
                System.err.println("Invalid Redis URL: missing host");
                return;
            }

            int port = uri.getPort();
            if (port == -1) {
                port = 6379; // Default Redis port
            }

            // Parse path: /db/prefix
            String path = uri.getPath();
            int db = 0;
            if (path != null && path.length() > 1) {
                // Remove leading slash
                path = path.substring(1);

                // Split by first slash: db/prefix
                int slashIndex = path.indexOf('/');
                if (slashIndex > 0) {
                    try {
                        db = Integer.parseInt(path.substring(0, slashIndex));
                    } catch (NumberFormatException e) {
                        // db stays 0
                    }
                    if (slashIndex < path.length() - 1) {
                        this.redisPrefix = path.substring(slashIndex + 1);
                    }
                } else {
                    // Only db number, no prefix
                    try {
                        db = Integer.parseInt(path);
                    } catch (NumberFormatException e) {
                        // Treat the whole path as prefix
                        this.redisPrefix = path;
                    }
                }
            }

            // Connect to Redis
            this.jedis = new Jedis(host, port);
            this.jedis.ping(); // Test connection

            // Select database if not default
            if (db != 0) {
                this.jedis.select(db);
            }

            this.redisEnabled = true;
            System.out.println("Redis connection established: " + host + ":" + port + " db=" + db + " prefix=" + this.redisPrefix);

        } catch (URISyntaxException e) {
            System.err.println("Invalid Redis URL syntax: " + e.getMessage());
        } catch (JedisException e) {
            System.err.println("Failed to connect to Redis: " + e.getMessage());
            this.jedis = null;
        }
    }

    /**
     * Checks if Redis storage is enabled and connected.
     *
     * @return true if Redis is available for storage
     */
    public boolean isRedisEnabled() {
        return redisEnabled && jedis != null;
    }

    /**
     * Stores a state value in Redis with a timestamp-based key.
     *
     * @param timestamp The timestamp string (colons will be replaced with hyphens)
     * @param value The state value to store (CSV format)
     */
    public void storeState(String timestamp, String value) {
        if (!isRedisEnabled()) {
            return;
        }

        try {
            String key = redisPrefix + ":" + timestamp.replace(":", "-");
            jedis.set(key, value);
        } catch (JedisException e) {
            System.err.println("Failed to store state in Redis: " + e.getMessage());
        }
    }

    /**
     * Retrieves a state value from Redis.
     *
     * @param timestamp The timestamp string
     * @return The stored value, or null if not found
     */
    public String getState(String timestamp) {
        if (!isRedisEnabled()) {
            return null;
        }

        try {
            String key = redisPrefix + ":" + timestamp.replace(":", "-");
            return jedis.get(key);
        } catch (JedisException e) {
            System.err.println("Failed to retrieve state from Redis: " + e.getMessage());
            return null;
        }
    }

    /**
     * Closes the Redis connection if open.
     */
    public void close() {
        if (jedis != null) {
            try {
                jedis.close();
            } catch (JedisException e) {
                System.err.println("Failed to close Redis connection: " + e.getMessage());
            }
        }
    }

    /**
     * Gets the configured Redis key prefix.
     *
     * @return The key prefix
     */
    public String getRedisPrefix() {
        return redisPrefix;
    }
}
