package com.finbuddy.crm.imports.repository;

import com.finbuddy.crm.imports.domain.ImportJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {

    Optional<ImportJob> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<ImportJob> findByTenantId(UUID tenantId, Pageable pageable);
}
