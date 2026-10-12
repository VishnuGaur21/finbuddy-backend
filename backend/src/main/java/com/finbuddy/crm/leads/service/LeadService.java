package com.finbuddy.crm.leads.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finbuddy.crm.leads.domain.Lead;
import com.finbuddy.crm.leads.domain.LeadStatus;
import com.finbuddy.crm.leads.dto.CreateLeadRequest;
import com.finbuddy.crm.leads.dto.LeadResponse;
import com.finbuddy.crm.leads.dto.UpdateLeadRequest;
import com.finbuddy.crm.leads.dto.UpdateLeadStatusRequest;
import com.finbuddy.crm.leads.repository.LeadRepository;
import com.finbuddy.crm.shared.audit.AuditAction;
import com.finbuddy.crm.shared.audit.AuditEvent;
import com.finbuddy.crm.shared.error.BusinessException;
import com.finbuddy.crm.shared.error.ResourceNotFoundException;
import com.finbuddy.crm.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service orchestrating the Lead lifecycle with strict cross-tenant filtering
 * and automatic append-only audit event propagation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public LeadResponse createLead(CreateLeadRequest request) {
        UUID tenantId = getRequiredTenantId();
        UUID userId = TenantContext.getUserId();

        // 1. Deduplication validation within tenant boundary
        if (leadRepository.existsByTenantIdAndPhone(tenantId, request.phone())) {
            throw new BusinessException("A lead with phone number " + request.phone() + " already exists in this tenant organization.");
        }

        if (request.panCard() != null && leadRepository.existsByTenantIdAndPanCard(tenantId, request.panCard())) {
            throw new BusinessException("A lead with PAN " + request.panCard() + " already exists in this tenant organization.");
        }

        // 2. Build and persist Lead entity
        Lead lead = Lead.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .phone(request.phone().trim())
                .email(request.email() != null ? request.email().trim().toLowerCase() : null)
                .panCard(request.panCard() != null ? request.panCard().trim().toUpperCase() : null)
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .city(request.city())
                .state(request.state())
                .pincode(request.pincode())
                .employmentType(request.employmentType())
                .monthlyIncome(request.monthlyIncome())
                .loanAmountRequested(request.loanAmountRequested())
                .leadStatus(LeadStatus.NEW)
                .source(request.source() != null ? request.source() : "DIRECT_API")
                .campaignId(request.campaignId())
                .assignedAgentId(request.assignedAgentId())
                .creditScore(request.creditScore())
                .bankAccountNumber(request.bankAccountNumber())
                .bankIfsc(request.bankIfsc())
                .consentObtained(request.consentObtained())
                .consentTimestamp(Boolean.TRUE.equals(request.consentObtained()) ? OffsetDateTime.now() : null)
                .build();
        lead.setTenantId(tenantId);

        Lead savedLead = leadRepository.save(lead);
        log.info("Successfully created Lead id={} for tenantId={}", savedLead.getId(), tenantId);

        // 3. Publish Append-Only Audit Event
        String newStateJson = toJson(savedLead);
        eventPublisher.publishEvent(AuditEvent.of(
                tenantId,
                userId,
                AuditAction.CREATE,
                "Lead",
                savedLead.getId().toString(),
                null,
                newStateJson
        ));

        return LeadResponse.fromEntity(savedLead);
    }

    @Transactional(readOnly = true)
    public LeadResponse getLeadById(UUID id) {
        UUID tenantId = getRequiredTenantId();
        Lead lead = leadRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));

        return LeadResponse.fromEntity(lead);
    }

    @Transactional(readOnly = true)
    public Page<LeadResponse> getLeads(LeadStatus status, UUID agentId, Pageable pageable) {
        UUID tenantId = getRequiredTenantId();

        Page<Lead> page;
        if (status != null) {
            page = leadRepository.findByTenantIdAndLeadStatus(tenantId, status, pageable);
        } else if (agentId != null) {
            page = leadRepository.findByTenantIdAndAssignedAgentId(tenantId, agentId, pageable);
        } else {
            page = leadRepository.findAllByTenantId(tenantId, pageable);
        }

        return page.map(LeadResponse::fromEntity);
    }

    @Transactional
    public LeadResponse updateLead(UUID id, UpdateLeadRequest request) {
        UUID tenantId = getRequiredTenantId();
        UUID userId = TenantContext.getUserId();

        Lead existingLead = leadRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));

        String oldStateJson = toJson(existingLead);

        if (request.firstName() != null) existingLead.setFirstName(request.firstName().trim());
        if (request.lastName() != null) existingLead.setLastName(request.lastName().trim());
        if (request.email() != null) existingLead.setEmail(request.email().trim().toLowerCase());
        if (request.city() != null) existingLead.setCity(request.city());
        if (request.state() != null) existingLead.setState(request.state());
        if (request.pincode() != null) existingLead.setPincode(request.pincode());
        if (request.employmentType() != null) existingLead.setEmploymentType(request.employmentType());
        if (request.monthlyIncome() != null) existingLead.setMonthlyIncome(request.monthlyIncome());
        if (request.loanAmountRequested() != null) existingLead.setLoanAmountRequested(request.loanAmountRequested());
        if (request.assignedAgentId() != null) existingLead.setAssignedAgentId(request.assignedAgentId());
        if (request.creditScore() != null) existingLead.setCreditScore(request.creditScore());
        if (request.bankAccountNumber() != null) existingLead.setBankAccountNumber(request.bankAccountNumber());
        if (request.bankIfsc() != null) existingLead.setBankIfsc(request.bankIfsc());
        if (request.leadStatus() != null) existingLead.setLeadStatus(request.leadStatus());

        Lead updatedLead = leadRepository.save(existingLead);
        String newStateJson = toJson(updatedLead);

        // Publish Audit Event for Record Change
        eventPublisher.publishEvent(AuditEvent.of(
                tenantId,
                userId,
                AuditAction.UPDATE,
                "Lead",
                updatedLead.getId().toString(),
                oldStateJson,
                newStateJson
        ));

        return LeadResponse.fromEntity(updatedLead);
    }

    @Transactional
    public LeadResponse updateLeadStatus(UUID id, UpdateLeadStatusRequest request) {
        UUID tenantId = getRequiredTenantId();
        UUID userId = TenantContext.getUserId();

        Lead existingLead = leadRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));

        LeadStatus oldStatus = existingLead.getLeadStatus();
        existingLead.setLeadStatus(request.status());

        Lead saved = leadRepository.save(existingLead);

        // Publish Audit Event specifically tracking Stage Transition
        eventPublisher.publishEvent(AuditEvent.of(
                tenantId,
                userId,
                AuditAction.STATUS_CHANGE,
                "Lead",
                saved.getId().toString(),
                "{\"status\":\"" + oldStatus + "\"}",
                "{\"status\":\"" + request.status() + "\",\"reason\":\"" + request.reasonNote() + "\"}"
        ));

        return LeadResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteLead(UUID id) {
        UUID tenantId = getRequiredTenantId();
        UUID userId = TenantContext.getUserId();

        Lead existingLead = leadRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));

        String oldStateJson = toJson(existingLead);
        leadRepository.delete(existingLead);

        // Publish Tamper-Evident Delete Audit Event
        eventPublisher.publishEvent(AuditEvent.of(
                tenantId,
                userId,
                AuditAction.DELETE,
                "Lead",
                id.toString(),
                oldStateJson,
                null
        ));
    }

    private UUID getRequiredTenantId() {
        return TenantContext.getOptionalTenantId()
                .orElseThrow(() -> new IllegalStateException("Cross-tenant violation: No active Tenant ID established for current request"));
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize entity to JSON for audit log", e);
            return "{}";
        }
    }
}
