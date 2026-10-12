package com.finbuddy.crm.leads.repository;

import com.finbuddy.crm.leads.domain.Lead;
import com.finbuddy.crm.leads.domain.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Leads with strict tenant isolation guarantees.
 */
@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    Optional<Lead> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Lead> findAllByTenantId(UUID tenantId, Pageable pageable);

    Page<Lead> findByTenantIdAndLeadStatus(UUID tenantId, LeadStatus leadStatus, Pageable pageable);

    Page<Lead> findByTenantIdAndAssignedAgentId(UUID tenantId, UUID assignedAgentId, Pageable pageable);

    boolean existsByTenantIdAndPhone(UUID tenantId, String phone);

    boolean existsByTenantIdAndPanCard(UUID tenantId, String panCard);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.tenantId = :tenantId AND l.leadStatus = :status")
    long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") LeadStatus status);
}
