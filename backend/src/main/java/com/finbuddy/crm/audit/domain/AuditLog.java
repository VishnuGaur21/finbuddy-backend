package com.finbuddy.crm.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Append-only, tamper-evident audit log entity with cryptographic hash chaining.
 */
@Entity
@Table(name = "tamper_evident_audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "action", nullable = false, updatable = false, length = 100)
    private String action;

    @Column(name = "entity_name", nullable = false, updatable = false, length = 100)
    private String entityName;

    @Column(name = "entity_id", nullable = false, updatable = false, length = 100)
    private String entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_state_json", updatable = false, columnDefinition = "jsonb")
    private String oldStateJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_state_json", updatable = false, columnDefinition = "jsonb")
    private String newStateJson;

    @Column(name = "ip_address", updatable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", updatable = false)
    private String userAgent;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private OffsetDateTime timestamp;

    @Column(name = "prev_hash", nullable = false, updatable = false, length = 64)
    private String prevHash;

    @Column(name = "entry_hash", nullable = false, updatable = false, length = 64)
    private String entryHash;
}
