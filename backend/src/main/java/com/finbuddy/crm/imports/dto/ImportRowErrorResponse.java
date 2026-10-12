package com.finbuddy.crm.imports.dto;

import com.finbuddy.crm.imports.domain.ImportRowError;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record ImportRowErrorResponse(
        UUID id,
        UUID jobId,
        Integer rowNumber,
        String columnName,
        String rawValue,
        String errorMessage,
        OffsetDateTime createdAt
) {
    public static ImportRowErrorResponse fromEntity(ImportRowError error) {
        if (error == null) return null;
        return ImportRowErrorResponse.builder()
                .id(error.getId())
                .jobId(error.getJobId())
                .rowNumber(error.getRowNumber())
                .columnName(error.getColumnName())
                .rawValue(error.getRawValue())
                .errorMessage(error.getErrorMessage())
                .createdAt(error.getCreatedAt())
                .build();
    }
}
