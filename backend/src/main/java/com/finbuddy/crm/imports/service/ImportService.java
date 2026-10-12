package com.finbuddy.crm.imports.service;

import com.finbuddy.crm.imports.domain.ImportJob;
import com.finbuddy.crm.imports.domain.ImportRowError;
import com.finbuddy.crm.imports.domain.ImportStatus;
import com.finbuddy.crm.imports.dto.ImportJobResponse;
import com.finbuddy.crm.imports.dto.ImportRowErrorResponse;
import com.finbuddy.crm.imports.repository.ImportJobRepository;
import com.finbuddy.crm.imports.repository.ImportRowErrorRepository;
import com.finbuddy.crm.leads.domain.Lead;
import com.finbuddy.crm.leads.domain.LeadStatus;
import com.finbuddy.crm.leads.repository.LeadRepository;
import com.finbuddy.crm.shared.audit.AuditAction;
import com.finbuddy.crm.shared.audit.AuditEvent;
import com.finbuddy.crm.shared.error.BusinessException;
import com.finbuddy.crm.shared.error.ResourceNotFoundException;
import com.finbuddy.crm.shared.storage.StorageService;
import com.finbuddy.crm.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Enterprise Bulk Ingestion Engine.
 * Handles asynchronous streaming CSV imports, resumable checkpoints,
 * line-by-line validation, and multi-tenant persistence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private static final int BATCH_SIZE = 100;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");

    private final ImportJobRepository importJobRepository;
    private final ImportRowErrorRepository rowErrorRepository;
    private final LeadRepository leadRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ImportJobResponse initiateImport(MultipartFile file) {
        UUID tenantId = getRequiredTenantId();
        UUID userId = TenantContext.getUserId();

        if (file.isEmpty()) {
            throw new BusinessException("Uploaded file cannot be empty");
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "import.csv";
        String fileType = originalName.toLowerCase().endsWith(".csv") ? "CSV" : "XLSX";

        if (!"CSV".equals(fileType)) {
            throw new BusinessException("Currently only CSV format is supported for high-throughput streaming import.");
        }

        // 1. Upload raw file to S3
        String storageKey = "tenants/" + tenantId + "/imports/" + UUID.randomUUID() + "-" + originalName;
        try (InputStream is = file.getInputStream()) {
            storageService.uploadFile(storageKey, is, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new BusinessException("Failed to persist import file to object storage", e);
        }

        // 2. Count estimated rows for progress monitoring
        int estimatedRows = countCsvRows(storageKey);

        // 3. Create ImportJob record
        ImportJob job = ImportJob.builder()
                .fileName(originalName)
                .originalFileName(originalName)
                .storagePath(storageKey)
                .fileType(fileType)
                .totalRows(estimatedRows)
                .processedRows(0)
                .successfulRows(0)
                .failedRows(0)
                .status(ImportStatus.PENDING)
                .currentRowIndex(0)
                .createdBy(userId)
                .build();
        job.setTenantId(tenantId);

        ImportJob savedJob = importJobRepository.save(job);

        // 4. Publish Audit Trail Event
        eventPublisher.publishEvent(AuditEvent.of(
                tenantId,
                userId,
                AuditAction.BATCH_IMPORT,
                "ImportJob",
                savedJob.getId().toString(),
                null,
                "{\"fileName\":\"" + originalName + "\",\"totalRows\":" + estimatedRows + "}"
        ));

        log.info("Initialized bulk import job id={} for tenantId={}", savedJob.getId(), tenantId);

        // 5. Trigger Async Worker
        processImportJobAsync(savedJob.getId(), tenantId, userId);

        return ImportJobResponse.fromEntity(savedJob);
    }

    @Async
    public void processImportJobAsync(UUID jobId, UUID tenantId, UUID userId) {
        // Bind TenantContext into the background worker thread
        TenantContext.setTenantId(tenantId);
        TenantContext.setUserId(userId);

        try {
            log.info("Starting asynchronous processing for ImportJob id={}", jobId);
            executeImportWorkflow(jobId, tenantId, userId);
        } catch (Exception ex) {
            log.error("Fatal error processing ImportJob id={}: ", jobId, ex);
            markJobFailed(jobId, ex.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    private void executeImportWorkflow(UUID jobId, UUID tenantId, UUID userId) {
        ImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("ImportJob not found: " + jobId));

        job.setStatus(ImportStatus.IN_PROGRESS);
        importJobRepository.save(job);

        int resumeFromRow = job.getCurrentRowIndex();
        int currentRow = 0;
        int processedCount = job.getProcessedRows();
        int successCount = job.getSuccessfulRows();
        int failCount = job.getFailedRows();

        List<ImportRowError> errorBatch = new ArrayList<>();
        List<Lead> leadBatch = new ArrayList<>();

        try (InputStream s3Stream = storageService.downloadFile(job.getStoragePath());
             BufferedReader reader = new BufferedReader(new InputStreamReader(s3Stream, StandardCharsets.UTF_8));
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            for (CSVRecord record : parser) {
                currentRow++;

                // Resumable capability: Skip rows that were already processed in earlier attempts
                if (currentRow <= resumeFromRow) {
                    continue;
                }

                // Line-by-line validation
                RowValidationResult validation = validateAndParseRow(record, currentRow, tenantId, jobId);

                if (!validation.isValid()) {
                    failCount++;
                    errorBatch.addAll(validation.getErrors());
                } else {
                    successCount++;
                    leadBatch.add(validation.getLead());
                }
                processedCount++;

                // Flush in batches of BATCH_SIZE
                if (processedCount % BATCH_SIZE == 0) {
                    flushBatch(leadBatch, errorBatch);
                    checkpointProgress(jobId, currentRow, processedCount, successCount, failCount);
                }
            }

            // Flush remaining trailing records
            flushBatch(leadBatch, errorBatch);

            // Mark job completion
            completeJob(jobId, currentRow, processedCount, successCount, failCount, tenantId, userId);

        } catch (Exception e) {
            log.error("Error reading CSV for job {}: ", jobId, e);
            checkpointProgress(jobId, currentRow, processedCount, successCount, failCount);
            markJobFailed(jobId, e.getMessage());
        }
    }

    private RowValidationResult validateAndParseRow(CSVRecord record, int rowNumber, UUID tenantId, UUID jobId) {
        List<ImportRowError> errors = new ArrayList<>();

        String firstName = getRecordValue(record, "first_name", "firstName");
        String lastName = getRecordValue(record, "last_name", "lastName");
        String phone = getRecordValue(record, "phone", "mobile");
        String email = getRecordValue(record, "email");
        String pan = getRecordValue(record, "pan", "pan_card");
        String monthlyIncomeStr = getRecordValue(record, "monthly_income", "monthlyIncome");
        String loanAmountStr = getRecordValue(record, "loan_amount", "loanAmountRequested");
        String employmentType = getRecordValue(record, "employment_type", "employmentType");

        if (firstName == null || firstName.isBlank()) {
            errors.add(buildError(jobId, rowNumber, "firstName", firstName, "First name is mandatory"));
        }
        if (lastName == null || lastName.isBlank()) {
            errors.add(buildError(jobId, rowNumber, "lastName", lastName, "Last name is mandatory"));
        }

        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            errors.add(buildError(jobId, rowNumber, "phone", phone, "Must be a valid 10-digit Indian phone number"));
        } else if (leadRepository.existsByTenantIdAndPhone(tenantId, phone)) {
            errors.add(buildError(jobId, rowNumber, "phone", phone, "Duplicate phone: Lead already exists in tenant"));
        }

        if (pan != null && !pan.isBlank()) {
            if (!PAN_PATTERN.matcher(pan.toUpperCase()).matches()) {
                errors.add(buildError(jobId, rowNumber, "panCard", pan, "Invalid PAN format"));
            } else if (leadRepository.existsByTenantIdAndPanCard(tenantId, pan.toUpperCase())) {
                errors.add(buildError(jobId, rowNumber, "panCard", pan, "Duplicate PAN: Lead already exists in tenant"));
            }
        }

        BigDecimal monthlyIncome = BigDecimal.ZERO;
        try {
            if (monthlyIncomeStr != null && !monthlyIncomeStr.isBlank()) {
                monthlyIncome = new BigDecimal(monthlyIncomeStr);
                if (monthlyIncome.compareTo(BigDecimal.valueOf(5000)) < 0) {
                    errors.add(buildError(jobId, rowNumber, "monthlyIncome", monthlyIncomeStr, "Monthly income must be at least 5000"));
                }
            } else {
                errors.add(buildError(jobId, rowNumber, "monthlyIncome", null, "Monthly income is mandatory"));
            }
        } catch (NumberFormatException e) {
            errors.add(buildError(jobId, rowNumber, "monthlyIncome", monthlyIncomeStr, "Invalid numeric format for monthly income"));
        }

        BigDecimal loanAmount = BigDecimal.ZERO;
        try {
            if (loanAmountStr != null && !loanAmountStr.isBlank()) {
                loanAmount = new BigDecimal(loanAmountStr);
                if (loanAmount.compareTo(BigDecimal.valueOf(10000)) < 0) {
                    errors.add(buildError(jobId, rowNumber, "loanAmountRequested", loanAmountStr, "Loan amount must be at least 10000"));
                }
            } else {
                errors.add(buildError(jobId, rowNumber, "loanAmountRequested", null, "Loan amount requested is mandatory"));
            }
        } catch (NumberFormatException e) {
            errors.add(buildError(jobId, rowNumber, "loanAmountRequested", loanAmountStr, "Invalid numeric format for loan amount"));
        }

        if (!errors.isEmpty()) {
            return new RowValidationResult(false, errors, null);
        }

        Lead lead = Lead.builder()
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .phone(phone.trim())
                .email(email != null ? email.trim().toLowerCase() : null)
                .panCard(pan != null ? pan.trim().toUpperCase() : null)
                .employmentType(employmentType != null && !employmentType.isBlank() ? employmentType : "SALARIED")
                .monthlyIncome(monthlyIncome)
                .loanAmountRequested(loanAmount)
                .leadStatus(LeadStatus.NEW)
                .source("BULK_IMPORT")
                .consentObtained(true)
                .consentTimestamp(OffsetDateTime.now())
                .build();
        lead.setTenantId(tenantId);

        return new RowValidationResult(true, errors, lead);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void flushBatch(List<Lead> leads, List<ImportRowError> errors) {
        if (!leads.isEmpty()) {
            leadRepository.saveAll(leads);
            leads.clear();
        }
        if (!errors.isEmpty()) {
            rowErrorRepository.saveAll(errors);
            errors.clear();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkpointProgress(UUID jobId, int currentRow, int processed, int success, int failed) {
        importJobRepository.findById(jobId).ifPresent(job -> {
            job.setCurrentRowIndex(currentRow);
            job.setProcessedRows(processed);
            job.setSuccessfulRows(success);
            job.setFailedRows(failed);
            importJobRepository.save(job);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeJob(UUID jobId, int currentRow, int processed, int success, int failed, UUID tenantId, UUID userId) {
        importJobRepository.findById(jobId).ifPresent(job -> {
            job.setCurrentRowIndex(currentRow);
            job.setProcessedRows(processed);
            job.setSuccessfulRows(success);
            job.setFailedRows(failed);
            job.setStatus(failed > 0 ? ImportStatus.PARTIALLY_COMPLETED : ImportStatus.COMPLETED);
            job.setCompletedAt(OffsetDateTime.now());
            importJobRepository.save(job);

            eventPublisher.publishEvent(AuditEvent.of(
                    tenantId,
                    userId,
                    AuditAction.BATCH_IMPORT,
                    "ImportJob",
                    jobId.toString(),
                    "{\"status\":\"IN_PROGRESS\"}",
                    "{\"status\":\"" + job.getStatus() + "\",\"successful\":" + success + ",\"failed\":" + failed + "}"
            ));
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markJobFailed(UUID jobId, String errorMessage) {
        importJobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(ImportStatus.FAILED);
            importJobRepository.save(job);
        });
    }

    @Transactional
    public ImportJobResponse resumeImportJob(UUID jobId) {
        UUID tenantId = getRequiredTenantId();
        UUID userId = TenantContext.getUserId();

        ImportJob job = importJobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ImportJob not found: " + jobId));

        if (job.getStatus() != ImportStatus.FAILED && job.getStatus() != ImportStatus.CANCELLED) {
            throw new BusinessException("Only FAILED or CANCELLED jobs can be resumed. Current status: " + job.getStatus());
        }

        job.setStatus(ImportStatus.PENDING);
        importJobRepository.save(job);

        // Resume async execution from checkpoint
        processImportJobAsync(jobId, tenantId, userId);

        return ImportJobResponse.fromEntity(job);
    }

    @Transactional(readOnly = true)
    public ImportJobResponse getJobStatus(UUID jobId) {
        UUID tenantId = getRequiredTenantId();
        ImportJob job = importJobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ImportJob not found: " + jobId));

        return ImportJobResponse.fromEntity(job);
    }

    @Transactional(readOnly = true)
    public Page<ImportRowErrorResponse> getJobErrors(UUID jobId, Pageable pageable) {
        UUID tenantId = getRequiredTenantId();
        // Verify job belongs to caller's tenant
        importJobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ImportJob not found: " + jobId));

        return rowErrorRepository.findByJobId(jobId, pageable)
                .map(ImportRowErrorResponse::fromEntity);
    }

    private int countCsvRows(String storageKey) {
        int count = 0;
        try (InputStream is = storageService.downloadFile(storageKey);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            while (reader.readLine() != null) {
                count++;
            }
            return Math.max(0, count - 1); // Exclude header
        } catch (Exception e) {
            log.warn("Could not pre-count CSV rows: {}", e.getMessage());
            return 0;
        }
    }

    private String getRecordValue(CSVRecord record, String... headerAliases) {
        for (String alias : headerAliases) {
            if (record.isMapped(alias)) {
                return record.get(alias);
            }
        }
        return null;
    }

    private ImportRowError buildError(UUID jobId, int rowNumber, String column, String value, String errorMsg) {
        return ImportRowError.builder()
                .jobId(jobId)
                .rowNumber(rowNumber)
                .columnName(column)
                .rawValue(value)
                .errorMessage(errorMsg)
                .build();
    }

    private UUID getRequiredTenantId() {
        return TenantContext.getOptionalTenantId()
                .orElseThrow(() -> new IllegalStateException("Multi-tenant violation: No tenant context found"));
    }

    private static class RowValidationResult {
        private final boolean valid;
        private final List<ImportRowError> errors;
        private final Lead lead;

        public RowValidationResult(boolean valid, List<ImportRowError> errors, Lead lead) {
            this.valid = valid;
            this.errors = errors;
            this.lead = lead;
        }

        public boolean isValid() { return valid; }
        public List<ImportRowError> getErrors() { return errors; }
        public Lead getLead() { return lead; }
    }
}
