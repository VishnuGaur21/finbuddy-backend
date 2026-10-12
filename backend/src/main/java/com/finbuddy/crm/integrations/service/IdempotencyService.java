package com.finbuddy.crm.integrations.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Distributed Idempotency Manager backed by Redis with local memory fallback.
 * Ensures duplicate webhook retries and delayed events are processed exactly once.
 */
@Slf4j
@Service
public class IdempotencyService {

    private static final String IDEMPOTENCY_PREFIX = "finbuddy:idempotency:";
    private static final Duration LOCK_EXPIRY = Duration.ofMinutes(10);
    private static final Duration COMPLETED_CACHE_EXPIRY = Duration.ofDays(7);

    private final Optional<StringRedisTemplate> redisTemplate;
    private final ConcurrentMap<String, String> localFallbackCache = new ConcurrentHashMap<>();

    @Autowired
    public IdempotencyService(Optional<StringRedisTemplate> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryAcquire(UUID tenantId, String idempotencyKey) {
        String key = buildRedisKey(tenantId, idempotencyKey);
        try {
            if (redisTemplate.isPresent()) {
                Boolean acquired = redisTemplate.get().opsForValue().setIfAbsent(key, "PROCESSING", LOCK_EXPIRY);
                return Boolean.TRUE.equals(acquired);
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable, falling back to in-memory lock: {}", ex.getMessage());
        }
        return localFallbackCache.putIfAbsent(key, "PROCESSING") == null;
    }

    public Optional<String> getExistingResponse(UUID tenantId, String idempotencyKey) {
        String key = buildRedisKey(tenantId, idempotencyKey);
        try {
            if (redisTemplate.isPresent()) {
                String val = redisTemplate.get().opsForValue().get(key);
                if (val != null && !val.equals("PROCESSING")) {
                    return Optional.of(val);
                }
                return Optional.empty();
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable, checking in-memory cache: {}", ex.getMessage());
        }
        String localVal = localFallbackCache.get(key);
        if (localVal != null && !localVal.equals("PROCESSING")) {
            return Optional.of(localVal);
        }
        return Optional.empty();
    }

    public void markCompleted(UUID tenantId, String idempotencyKey, String responsePayload) {
        String key = buildRedisKey(tenantId, idempotencyKey);
        try {
            if (redisTemplate.isPresent()) {
                redisTemplate.get().opsForValue().set(key, responsePayload, COMPLETED_CACHE_EXPIRY);
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable, storing response in memory: {}", ex.getMessage());
        }
        localFallbackCache.put(key, responsePayload);
    }

    public void releaseLock(UUID tenantId, String idempotencyKey) {
        String key = buildRedisKey(tenantId, idempotencyKey);
        try {
            if (redisTemplate.isPresent()) {
                redisTemplate.get().delete(key);
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable, clearing in-memory lock: {}", ex.getMessage());
        }
        localFallbackCache.remove(key);
    }

    private String buildRedisKey(UUID tenantId, String key) {
        return IDEMPOTENCY_PREFIX + (tenantId != null ? tenantId.toString() : "global") + ":" + key;
    }
}
