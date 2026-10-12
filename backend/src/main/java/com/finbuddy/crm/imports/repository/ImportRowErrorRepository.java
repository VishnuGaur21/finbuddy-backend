package com.finbuddy.crm.imports.repository;

import com.finbuddy.crm.imports.domain.ImportRowError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImportRowErrorRepository extends JpaRepository<ImportRowError, UUID> {

    Page<ImportRowError> findByJobId(UUID jobId, Pageable pageable);

    List<ImportRowError> findByJobIdOrderByRowNumberAsc(UUID jobId);

    long countByJobId(UUID jobId);
}
