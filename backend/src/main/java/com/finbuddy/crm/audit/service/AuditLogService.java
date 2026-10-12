package com.finbuddy.crm.audit.service;

import com.finbuddy.crm.audit.domain.AuditLog;
import com.finbuddy.crm.audit.repository.AuditLogRepository;
import com.finbuddy.crm.shared.audit.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing append-only, tamper-evident audit logs with cryptographic HMAC-SHA256 hash chaining.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final AuditLogRepository auditLogRepository;

    @Value("${finbuddy.audit.hmac-secret:finbuddy-crypto-tamper-evident-chain-secret-key-2026}")
    private String hmacSecret;

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public AuditLog appendAuditLog(AuditEvent event) {
        UUID tenantId = event.tenantId();

        // 1. Fetch previous hash in the tenant chain
        Optional<AuditLog> latestLog = auditLogRepository.findTopByTenantIdOrderByTimestampDesc(tenantId);
        String prevHash = latestLog.map(AuditLog::getEntryHash).orElse(GENESIS_HASH);

        OffsetDateTime timestamp = event.timestamp() != null ? event.timestamp() : OffsetDateTime.now();

        // 2. Compute current entry hash
        String entryHash = calculateEntryHash(
                prevHash,
                tenantId,
                event.userId(),
                event.action().name(),
                event.entityName(),
                event.entityId(),
                event.oldStateJson(),
                event.newStateJson(),
                timestamp
        );

        // 3. Persist immutable record
        AuditLog auditLog = AuditLog.builder()
                .tenantId(tenantId)
                .userId(event.userId())
                .action(event.action().name())
                .entityName(event.entityName())
                .entityId(event.entityId())
                .oldStateJson(event.oldStateJson())
                .newStateJson(event.newStateJson())
                .ipAddress(event.ipAddress())
                .userAgent(event.userAgent())
                .timestamp(timestamp)
                .prevHash(prevHash)
                .entryHash(entryHash)
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.debug("Appended tamper-evident audit record id={} for entity={}:{}", saved.getId(), saved.getEntityName(), saved.getEntityId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getTenantAuditLogs(UUID tenantId, Pageable pageable) {
        return auditLogRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * Verifies the entire cryptographic integrity of a tenant's audit trail.
     * Returns true if all hashes chain without tampering.
     */
    @Transactional(readOnly = true)
    public boolean verifyChainIntegrity(UUID tenantId) {
        List<AuditLog> logs = auditLogRepository.findByTenantIdOrderByTimestampAsc(tenantId);
        if (logs.isEmpty()) {
            return true;
        }

        String expectedPrevHash = GENESIS_HASH;

        for (AuditLog entry : logs) {
            if (!entry.getPrevHash().equals(expectedPrevHash)) {
                log.error("Tamper detected! Broken chain at log ID {}: expected prevHash {}, found {}",
                        entry.getId(), expectedPrevHash, entry.getPrevHash());
                return false;
            }

            String recomputedHash = calculateEntryHash(
                    entry.getPrevHash(),
                    entry.getTenantId(),
                    entry.getUserId(),
                    entry.getAction(),
                    entry.getEntityName(),
                    entry.getEntityId(),
                    entry.getOldStateJson(),
                    entry.getNewStateJson(),
                    entry.getTimestamp()
            );

            if (!entry.getEntryHash().equals(recomputedHash)) {
                log.error("Tamper detected! Hash mismatch at log ID {}: recorded {}, recomputed {}",
                        entry.getId(), entry.getEntryHash(), recomputedHash);
                return false;
            }

            expectedPrevHash = entry.getEntryHash();
        }

        log.info("Audit chain verified successfully for tenantId: {} (records count: {})", tenantId, logs.size());
        return true;
    }

    private String calculateEntryHash(
            String prevHash,
            UUID tenantId,
            UUID userId,
            String action,
            String entityName,
            String entityId,
            String oldStateJson,
            String newStateJson,
            OffsetDateTime timestamp
    ) {
        try {
            String payload = String.join("|",
                    prevHash != null ? prevHash : "",
                    tenantId != null ? tenantId.toString() : "",
                    userId != null ? userId.toString() : "",
                    action != null ? action : "",
                    entityName != null ? entityName : "",
                    entityId != null ? entityId : "",
                    oldStateJson != null ? oldStateJson : "",
                    newStateJson != null ? newStateJson : "",
                    timestamp != null ? timestamp.toString() : ""
            );

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);

            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to calculate cryptographic audit hash", e);
        }
    }
}
