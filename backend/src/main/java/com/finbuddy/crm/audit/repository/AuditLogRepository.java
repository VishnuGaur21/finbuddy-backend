package com.finbuddy.crm.audit.repository;

import com.finbuddy.crm.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Optional<AuditLog> findTopByTenantIdOrderByTimestampDesc(UUID tenantId);

    Page<AuditLog> findByTenantId(UUID tenantId, Pageable pageable);

    List<AuditLog> findByTenantIdOrderByTimestampAsc(UUID tenantId);

    Page<AuditLog> findByTenantIdAndEntityNameAndEntityId(UUID tenantId, String entityName, String entityId, Pageable pageable);
}
