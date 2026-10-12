package com.finbuddy.crm.imports.domain;

import com.finbuddy.crm.shared.tenant.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * ImportJob entity tracking asynchronous, resumable bulk ingestion workloads.
 */
@Entity
@Table(name = "import_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJob extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "storage_path", nullable = false, length = 1024)
    private String storagePath;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Column(name = "total_rows", nullable = false)
    private Integer totalRows;

    @Column(name = "processed_rows", nullable = false)
    private Integer processedRows;

    @Column(name = "successful_rows", nullable = false)
    private Integer successfulRows;

    @Column(name = "failed_rows", nullable = false)
    private Integer failedRows;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ImportStatus status;

    @Column(name = "current_row_index", nullable = false)
    private Integer currentRowIndex;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
