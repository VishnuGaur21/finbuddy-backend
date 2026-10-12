package com.finbuddy.crm.integrations.service;

import com.finbuddy.crm.shared.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Distributed Idempotency Manager backed by Redis.
 * Ensures duplicate webhook retries and delayed events are processed exactly once.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String IDEMPOTENCY_PREFIX = "finbuddy:idempotency:";
    private static final Duration LOCK_EXPIRY = Duration.ofMinutes(10);
    private static final Duration COMPLETED_CACHE_EXPIRY = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    public boolean tryAcquire(UUID tenantId, String idempotencyKey) {
        String key = buildRedisKey(tenantId, idempotencyKey);
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "PROCESSING", LOCK_EXPIRY);
        return Boolean.TRUE.equals(acquired);
    }

    public Optional<String> getExistingResponse(UUID tenantId, String idempotencyKey) {
        String key = buildRedisKey(tenantId, idempotencyKey);
        String val = redisTemplate.opsForValue().get(key);
        if (val != null && !val.equals("PROCESSING")) {
            return Optional.of(val);
        }
        return Optional.empty();
    }

    public void markCompleted(UUID tenantId, String idempotencyKey, String responsePayload) {
        String key = buildRedisKey(tenantId, idempotencyKey);
        redisTemplate.opsForValue().set(key, responsePayload, COMPLETED_CACHE_EXPIRY);
    }

    public void releaseLock(UUID tenantId, String idempotencyKey) {
        String key = buildRedisKey(tenantId, idempotencyKey);
        redisTemplate.delete(key);
    }

    private String buildRedisKey(UUID tenantId, String key) {
        return IDEMPOTENCY_PREFIX + (tenantId != null ? tenantId.toString() : "global") + ":" + key;
    }
}
