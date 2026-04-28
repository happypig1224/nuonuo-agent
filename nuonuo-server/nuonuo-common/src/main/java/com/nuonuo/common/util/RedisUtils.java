package com.nuonuo.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Enterprise-grade Redis utility class (zero external third-party dependencies).
 * Provides unified static operations for StringRedisTemplate,
 * completely eliminating verbose constructor injection across business modules.
 *
 * Supported features:
 * 1. High-performance non-blocking key scan via SCAN cursor;
 * 2. Distributed locking & idempotency via SETNX;
 * 3. Unit test mocking friendly via setTestStringRedisTemplate hook.
 */
@Component
public class RedisUtils implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(RedisUtils.class);

    private static volatile StringRedisTemplate staticStringRedisTemplate;
    private static volatile ObjectMapper staticObjectMapper;

    public RedisUtils() {}

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        try {
            staticStringRedisTemplate = context.getBean(StringRedisTemplate.class);
        } catch (Exception e) {
            log.warn("[REDIS-UTILS] StringRedisTemplate bean not found during context hydration: {}", e.getMessage());
        }
        try {
            staticObjectMapper = context.getBean(ObjectMapper.class);
        } catch (Exception e) {
            log.debug("[REDIS-UTILS] ObjectMapper bean not found: {}", e.getMessage());
        }
    }

    // =========================================================================
    // Unit Test Hooks (Allows pure Mockito isolation without Spring Context)
    // =========================================================================

    public static void setTestStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        staticStringRedisTemplate = stringRedisTemplate;
    }

    public static void setTestObjectMapper(ObjectMapper objectMapper) {
        staticObjectMapper = objectMapper;
    }

    public static void resetTestTemplates() {
        staticStringRedisTemplate = null;
        staticObjectMapper = null;
    }

    private static ObjectMapper getObjectMapper() {
        if (staticObjectMapper != null) {
            return staticObjectMapper;
        }
        synchronized (RedisUtils.class) {
            if (staticObjectMapper == null) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.findAndRegisterModules();
                mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                staticObjectMapper = mapper;
            }
            return staticObjectMapper;
        }
    }

    public static StringRedisTemplate getStringRedisTemplate() {
        return staticStringRedisTemplate;
    }

    // =========================================================================
    // Core String Operations
    // =========================================================================

    /**
     * Gets string value by key.
     */
    public static String get(String key) {
        if (key == null || staticStringRedisTemplate == null) {
            return null;
        }
        return staticStringRedisTemplate.opsForValue().get(key);
    }

    public static String getString(String key) {
        return get(key);
    }

    /**
     * Gets deserialized object by key using Jackson ObjectMapper.
     */
    public static <T> T get(String key, Class<T> clazz) {
        String json = get(key);
        if (json == null || json.isBlank() || clazz == null) {
            return null;
        }
        if (clazz == String.class) {
            return clazz.cast(json);
        }
        try {
            return getObjectMapper().readValue(json, clazz);
        } catch (Exception e) {
            log.warn("[REDIS-UTILS] Failed to deserialize JSON for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Sets serialized object with Duration TTL.
     */
    public static void set(String key, Object value, Duration timeout) {
        if (key == null || value == null) {
            return;
        }
        if (value instanceof String strVal) {
            set(key, strVal, timeout);
            return;
        }
        try {
            String json = getObjectMapper().writeValueAsString(value);
            set(key, json, timeout);
        } catch (Exception e) {
            log.warn("[REDIS-UTILS] Failed to serialize JSON for key {}: {}", key, e.getMessage());
        }
    }

    /**
     * Sets serialized object with base TTL plus random jitter.
     */
    public static void setWithJitter(String key, Object value, Duration baseTtl, Duration maxJitter) {
        if (key == null || value == null) {
            return;
        }
        Duration ttl = baseTtl != null ? baseTtl : Duration.ofMinutes(30);
        if (maxJitter != null && !maxJitter.isZero() && !maxJitter.isNegative()) {
            long jitterSeconds = java.util.concurrent.ThreadLocalRandom.current().nextLong(maxJitter.toSeconds() + 1);
            ttl = ttl.plusSeconds(jitterSeconds);
        }
        set(key, value, ttl);
    }

    /**
     * Sets string key-value without TTL.
     */
    public static void set(String key, String value) {
        if (key == null || staticStringRedisTemplate == null) {
            return;
        }
        staticStringRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * Sets string key-value with Duration TTL.
     */
    public static void set(String key, String value, Duration timeout) {
        if (key == null || staticStringRedisTemplate == null) {
            return;
        }
        if (timeout == null) {
            staticStringRedisTemplate.opsForValue().set(key, value);
        } else {
            staticStringRedisTemplate.opsForValue().set(key, value, timeout);
        }
    }

    /**
     * Sets string key-value with timeout and TimeUnit.
     */
    public static void set(String key, String value, long timeout, TimeUnit unit) {
        if (key == null || staticStringRedisTemplate == null) {
            return;
        }
        staticStringRedisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * Distributed lock & idempotency setIfAbsent (SETNX) with Duration TTL.
     */
    public static Boolean setIfAbsent(String key, String value, Duration timeout) {
        if (key == null) {
            return Boolean.FALSE;
        }
        if (staticStringRedisTemplate == null) {
            return Boolean.TRUE;
        }
        return staticStringRedisTemplate.opsForValue().setIfAbsent(key, value, timeout);
    }

    /**
     * Distributed lock & idempotency setIfAbsent (SETNX) with TimeUnit.
     */
    public static Boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        if (key == null) {
            return Boolean.FALSE;
        }
        if (staticStringRedisTemplate == null) {
            return Boolean.TRUE;
        }
        return staticStringRedisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    // =========================================================================
    // Key Lifecycle & Management Operations
    // =========================================================================

    /**
     * Checks if key exists.
     */
    public static Boolean hasKey(String key) {
        if (key == null || staticStringRedisTemplate == null) {
            return Boolean.FALSE;
        }
        return staticStringRedisTemplate.hasKey(key);
    }

    /**
     * Deletes single key.
     */
    public static Boolean delete(String key) {
        if (key == null || staticStringRedisTemplate == null) {
            return Boolean.FALSE;
        }
        return staticStringRedisTemplate.delete(key);
    }

    /**
     * Deletes collection of keys.
     */
    public static Long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty() || staticStringRedisTemplate == null) {
            return 0L;
        }
        return staticStringRedisTemplate.delete(keys);
    }

    /**
     * Sets expiration on key.
     */
    public static Boolean expire(String key, Duration timeout) {
        if (key == null || staticStringRedisTemplate == null || timeout == null) {
            return Boolean.FALSE;
        }
        return staticStringRedisTemplate.expire(key, timeout);
    }

    public static Boolean expire(String key, long timeout, TimeUnit unit) {
        if (key == null || staticStringRedisTemplate == null) {
            return Boolean.FALSE;
        }
        return staticStringRedisTemplate.expire(key, timeout, unit);
    }

    /**
     * Gets remaining TTL in seconds.
     */
    public static Long getExpire(String key) {
        if (key == null || staticStringRedisTemplate == null) {
            return -2L;
        }
        return staticStringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    public static Long getExpire(String key, TimeUnit unit) {
        if (key == null || staticStringRedisTemplate == null) {
            return -2L;
        }
        return staticStringRedisTemplate.getExpire(key, unit);
    }

    // =========================================================================
    // Atomic Counters & Delta Operations
    // =========================================================================

    /**
     * Atomic increment by 1.
     */
    public static Long increment(String key) {
        if (key == null || staticStringRedisTemplate == null) {
            return null;
        }
        return staticStringRedisTemplate.opsForValue().increment(key);
    }

    /**
     * Atomic increment by delta.
     */
    public static Long increment(String key, long delta) {
        if (key == null || staticStringRedisTemplate == null) {
            return null;
        }
        return staticStringRedisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * Atomic decrement by 1.
     */
    public static Long decrement(String key) {
        if (key == null || staticStringRedisTemplate == null) {
            return null;
        }
        return staticStringRedisTemplate.opsForValue().decrement(key);
    }

    /**
     * Atomic decrement by delta.
     */
    public static Long decrement(String key, long delta) {
        if (key == null || staticStringRedisTemplate == null) {
            return null;
        }
        return staticStringRedisTemplate.opsForValue().decrement(key, delta);
    }

    // =========================================================================
    // Lua Script Execution
    // =========================================================================

    /**
     * Executes Redis Lua Script atomically.
     */
    public static <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
        if (staticStringRedisTemplate == null || script == null) {
            return null;
        }
        return staticStringRedisTemplate.execute(script, keys, args);
    }

    // =========================================================================
    // Non-blocking SCAN Operations
    // =========================================================================

    /**
     * Non-blocking key scan replacing KEYS command.
     */
    public static Set<String> scan(String pattern) {
        Set<String> keys = new HashSet<>();
        if (staticStringRedisTemplate == null || pattern == null) {
            return keys;
        }
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        try {
            Set<String> scanned = staticStringRedisTemplate.execute((RedisCallback<Set<String>>) connection -> {
                Set<String> set = new HashSet<>();
                try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                    while (cursor != null && cursor.hasNext()) {
                        set.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                }
                return set;
            });
            if (scanned != null) {
                keys.addAll(scanned);
            }
        } catch (Exception e) {
            log.error("[REDIS-UTILS] SCAN execution failed for pattern: {}", pattern, e);
        }
        return keys;
    }

    /**
     * Deletes matching keys via non-blocking SCAN.
     */
    public static long deleteByScan(String pattern) {
        Set<String> keys = scan(pattern);
        if (keys.isEmpty()) {
            return 0L;
        }
        Long deleted = delete(keys);
        return deleted != null ? deleted : 0L;
    }

    // =========================================================================
    // Health Check / Connection Operations
    // =========================================================================

    /**
     * Executes PING against Redis connection.
     * @return ping response (e.g. "PONG"), or null if Redis connection factory is not configured.
     */
    public static String ping() {
        if (staticStringRedisTemplate == null || staticStringRedisTemplate.getConnectionFactory() == null) {
            return null;
        }
        try (RedisConnection connection = staticStringRedisTemplate.getConnectionFactory().getConnection()) {
            return connection.ping();
        }
    }
}
