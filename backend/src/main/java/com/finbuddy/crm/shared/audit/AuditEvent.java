package com.finbuddy.crm.shared.audit;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain Event published when an auditable business action occurs.
 * Consumed by the Audit module to guarantee append-only, tamper-evident hash chaining.
 */
public record AuditEvent(
        UUID tenantId,
        UUID userId,
        AuditAction action,
        String entityName,
        String entityId,
        String oldStateJson,
        String newStateJson,
        String ipAddress,
        String userAgent,
        OffsetDateTime timestamp
) {
    public static AuditEvent of(
            UUID tenantId,
            UUID userId,
            AuditAction action,
            String entityName,
            String entityId,
            String oldStateJson,
            String newStateJson
    ) {
        return new AuditEvent(
                tenantId,
                userId,
                action,
                entityName,
                entityId,
                oldStateJson,
                newStateJson,
                null,
                null,
                OffsetDateTime.now()
        );
    }
}
