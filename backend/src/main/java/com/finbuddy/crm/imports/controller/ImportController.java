package com.finbuddy.crm.imports.controller;

import com.finbuddy.crm.imports.dto.ImportJobResponse;
import com.finbuddy.crm.imports.dto.ImportRowErrorResponse;
import com.finbuddy.crm.imports.service.ImportService;
import com.finbuddy.crm.shared.security.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST Controller for Bulk Ingestion Engine.
 * Supports asynchronous uploading of CSV files, real-time status polling,
 * row-level diagnostic retrieval, and resumable execution.
 */
@RestController
@RequestMapping("/imports")
@RequiredArgsConstructor
@Tag(name = "Bulk Ingestion & Imports", description = "Asynchronous bulk lead ingestion, resumable jobs, and row-level error reporting")
@SecurityRequirement(name = "BearerAuth")
public class ImportController {

    private final ImportService importService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize(UserRole.Roles.HAS_IMPORT_WRITE)
    @Operation(summary = "Upload bulk CSV file for async ingestion",
            description = "Persists file to S3 object storage and queues background streaming worker with row-level validation.")
    public ResponseEntity<ImportJobResponse> uploadImportFile(@RequestParam("file") MultipartFile file) {
        ImportJobResponse job = importService.initiateImport(file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
    }

    @GetMapping("/{id}")
    @PreAuthorize(UserRole.Roles.HAS_LEAD_READ)
    @Operation(summary = "Get import job status and progress",
            description = "Polls real-time progress, processed counters, and current stage of the ingestion workload.")
    public ResponseEntity<ImportJobResponse> getJobStatus(@PathVariable UUID id) {
        ImportJobResponse job = importService.getJobStatus(id);
        return ResponseEntity.ok(job);
    }

    @GetMapping("/{id}/errors")
    @PreAuthorize(UserRole.Roles.HAS_LEAD_READ)
    @Operation(summary = "Get row-level validation errors",
            description = "Returns paginated list of failed rows, invalid columns, and specific error diagnostics.")
    public ResponseEntity<Page<ImportRowErrorResponse>> getJobErrors(
            @PathVariable UUID id,
            @PageableDefault(size = 50, sort = "rowNumber", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<ImportRowErrorResponse> errors = importService.getJobErrors(id, pageable);
        return ResponseEntity.ok(errors);
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize(UserRole.Roles.HAS_IMPORT_WRITE)
    @Operation(summary = "Resume failed or interrupted import job",
            description = "Resumes stream processing from the last checkpointed row without duplicating previously ingested records.")
    public ResponseEntity<ImportJobResponse> resumeImportJob(@PathVariable UUID id) {
        ImportJobResponse resumedJob = importService.resumeImportJob(id);
        return ResponseEntity.ok(resumedJob);
    }
}
