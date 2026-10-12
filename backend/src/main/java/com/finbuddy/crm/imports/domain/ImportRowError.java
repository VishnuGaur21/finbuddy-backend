package com.finbuddy.crm.imports.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Granular row-level failure log captured during bulk file processing.
 */
@Entity
@Table(name = "import_row_errors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRowError {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(name = "column_name", length = 100)
    private String columnName;

    @Column(name = "raw_value")
    private String rawValue;

    @Column(name = "error_message", nullable = false)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
