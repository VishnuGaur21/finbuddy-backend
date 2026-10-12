package com.finbuddy.crm.imports.dto;

import com.finbuddy.crm.imports.domain.ImportJob;
import com.finbuddy.crm.imports.domain.ImportStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record ImportJobResponse(
        UUID id,
        UUID tenantId,
        String originalFileName,
        String fileType,
        Integer totalRows,
        Integer processedRows,
        Integer successfulRows,
        Integer failedRows,
        ImportStatus status,
        Integer currentRowIndex,
        UUID createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime completedAt
) {
    public static ImportJobResponse fromEntity(ImportJob job) {
        if (job == null) return null;
        return ImportJobResponse.builder()
                .id(job.getId())
                .tenantId(job.getTenantId())
                .originalFileName(job.getOriginalFileName())
                .fileType(job.getFileType())
                .totalRows(job.getTotalRows())
                .processedRows(job.getProcessedRows())
                .successfulRows(job.getSuccessfulRows())
                .failedRows(job.getFailedRows())
                .status(job.getStatus())
                .currentRowIndex(job.getCurrentRowIndex())
                .createdBy(job.getCreatedBy())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }
}
