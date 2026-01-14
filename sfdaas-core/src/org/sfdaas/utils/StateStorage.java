package org.sfdaas.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Utility class for storing propagation states to external storage systems.
 * Supports Redis and Memcached for interval state storage.
 *
 * URL formats:
 *   redis://host:port/db/prefix
 *   memcached://host:port/ttl/prefix
 *
 * Examples:
 *   redis://localhost:6379/0/sfdaas:states
 *   redis://localhost/0/myprefix
 *   memcached://localhost:11211/3600/sfdaas:states
 *   memcached://localhost/3600/myprefix
 */
public class StateStorage {

    private Jedis jedis;
    private MemcachedClient memcached;
    private boolean redisEnabled;
    private boolean memcachedEnabled;
    private String keyPrefix;
    private int memcachedTtl;

    /**
     * Creates a new StateStorage instance from a cache URL.
     *
     * @param cacheUrl Cache URL in format:
     *                 redis://host:port/db/prefix
     *                 memcached://host:port/ttl/prefix
     */
    public StateStorage(String cacheUrl) {
        this.jedis = null;
        this.memcached = null;
        this.redisEnabled = false;
        this.memcachedEnabled = false;
        this.keyPrefix = "sfdaas:states";
        this.memcachedTtl = 3600; // Default 1 hour

        if (cacheUrl == null || cacheUrl.trim().isEmpty()) {
            return;
        }

        if (cacheUrl.startsWith("redis://")) {
            parseRedisUrl(cacheUrl);
        } else if (cacheUrl.startsWith("memcached://")) {
            parseMemcachedUrl(cacheUrl);
        }
    }

    /**
     * Parses a Redis URL and establishes connection.
     * Format: redis://host:port/db/prefix
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
                path = path.substring(1); // Remove leading slash

                int slashIndex = path.indexOf('/');
                if (slashIndex > 0) {
                    try {
                        db = Integer.parseInt(path.substring(0, slashIndex));
                    } catch (NumberFormatException e) {
                        // db stays 0
                    }
                    if (slashIndex < path.length() - 1) {
                        this.keyPrefix = path.substring(slashIndex + 1);
                    }
                } else {
                    try {
                        db = Integer.parseInt(path);
                    } catch (NumberFormatException e) {
                        this.keyPrefix = path;
                    }
                }
            }

            // Connect to Redis
            this.jedis = new Jedis(host, port);
            this.jedis.ping();

            if (db != 0) {
                this.jedis.select(db);
            }

            this.redisEnabled = true;
            System.out.println("Redis connection established: " + host + ":" + port + " db=" + db + " prefix=" + this.keyPrefix);

        } catch (URISyntaxException e) {
            System.err.println("Invalid Redis URL syntax: " + e.getMessage());
        } catch (JedisException e) {
            System.err.println("Failed to connect to Redis: " + e.getMessage());
            this.jedis = null;
        }
    }

    /**
     * Parses a Memcached URL and establishes connection.
     * Format: memcached://host:port/ttl/prefix
     */
    private void parseMemcachedUrl(String memcachedUrl) {
        try {
            URI uri = new URI(memcachedUrl);

            String host = uri.getHost();
            if (host == null || host.trim().isEmpty()) {
                System.err.println("Invalid Memcached URL: missing host");
                return;
            }

            int port = uri.getPort();
            if (port == -1) {
                port = 11211; // Default Memcached port
            }

            // Parse path: /ttl/prefix
            String path = uri.getPath();
            if (path != null && path.length() > 1) {
                path = path.substring(1); // Remove leading slash

                int slashIndex = path.indexOf('/');
                if (slashIndex > 0) {
                    try {
                        this.memcachedTtl = Integer.parseInt(path.substring(0, slashIndex));
                    } catch (NumberFormatException e) {
                        // ttl stays default
                    }
                    if (slashIndex < path.length() - 1) {
                        this.keyPrefix = path.substring(slashIndex + 1);
                    }
                } else {
                    try {
                        this.memcachedTtl = Integer.parseInt(path);
                    } catch (NumberFormatException e) {
                        this.keyPrefix = path;
                    }
                }
            }

            // Connect to Memcached
            String address = host + ":" + port;
            this.memcached = new MemcachedClient(
                new BinaryConnectionFactory(),
                AddrUtil.getAddresses(address)
            );

            this.memcachedEnabled = true;
            System.out.println("Memcached connection established: " + address + " ttl=" + this.memcachedTtl + " prefix=" + this.keyPrefix);

        } catch (URISyntaxException e) {
            System.err.println("Invalid Memcached URL syntax: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Failed to connect to Memcached: " + e.getMessage());
            this.memcached = null;
        }
    }

    /**
     * Checks if any storage backend is enabled and connected.
     */
    public boolean isEnabled() {
        return isRedisEnabled() || isMemcachedEnabled();
    }

    /**
     * Checks if Redis storage is enabled and connected.
     */
    public boolean isRedisEnabled() {
        return redisEnabled && jedis != null;
    }

    /**
     * Checks if Memcached storage is enabled and connected.
     */
    public boolean isMemcachedEnabled() {
        return memcachedEnabled && memcached != null;
    }

    /**
     * Stores a state value with a timestamp-based key.
     *
     * @param timestamp The timestamp string (colons will be replaced with hyphens)
     * @param value The state value to store (CSV format)
     */
    public void storeState(String timestamp, String value) {
        String key = keyPrefix + ":" + timestamp.replace(":", "-");

        if (isRedisEnabled()) {
            try {
                jedis.set(key, value);
            } catch (JedisException e) {
                System.err.println("Failed to store state in Redis: " + e.getMessage());
            }
        } else if (isMemcachedEnabled()) {
            try {
                memcached.set(key, memcachedTtl, value);
            } catch (Exception e) {
                System.err.println("Failed to store state in Memcached: " + e.getMessage());
            }
        }
    }

    /**
     * Retrieves a state value.
     *
     * @param timestamp The timestamp string
     * @return The stored value, or null if not found
     */
    public String getState(String timestamp) {
        String key = keyPrefix + ":" + timestamp.replace(":", "-");

        if (isRedisEnabled()) {
            try {
                return jedis.get(key);
            } catch (JedisException e) {
                System.err.println("Failed to retrieve state from Redis: " + e.getMessage());
                return null;
            }
        } else if (isMemcachedEnabled()) {
            try {
                Object value = memcached.get(key);
                return value != null ? value.toString() : null;
            } catch (Exception e) {
                System.err.println("Failed to retrieve state from Memcached: " + e.getMessage());
                return null;
            }
        }

        return null;
    }

    /**
     * Closes the storage connection if open.
     */
    public void close() {
        if (jedis != null) {
            try {
                jedis.close();
            } catch (JedisException e) {
                System.err.println("Failed to close Redis connection: " + e.getMessage());
            }
        }
        if (memcached != null) {
            try {
                memcached.shutdown();
            } catch (Exception e) {
                System.err.println("Failed to close Memcached connection: " + e.getMessage());
            }
        }
    }

    /**
     * Gets the configured key prefix.
     */
    public String getKeyPrefix() {
        return keyPrefix;
    }
}
