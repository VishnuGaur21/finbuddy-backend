package com.finbuddy.crm.leads.controller;

import com.finbuddy.crm.leads.domain.LeadStatus;
import com.finbuddy.crm.leads.dto.CreateLeadRequest;
import com.finbuddy.crm.leads.dto.LeadResponse;
import com.finbuddy.crm.leads.dto.UpdateLeadRequest;
import com.finbuddy.crm.leads.dto.UpdateLeadStatusRequest;
import com.finbuddy.crm.leads.service.LeadService;
import com.finbuddy.crm.shared.security.MfaRequired;
import com.finbuddy.crm.shared.security.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST Controller for Lead Management.
 * Enforces RBAC permissions across 11 roles, returns PII-masked JSON by default,
 * and requires Step-Up MFA for critical operations.
 */
@RestController
@RequestMapping("/leads")
@RequiredArgsConstructor
@Tag(name = "Leads Management", description = "Endpoints for managing leads, status pipelines, and PII data")
@SecurityRequirement(name = "BearerAuth")
public class LeadController {

    private final LeadService leadService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(UserRole.Roles.HAS_LEAD_WRITE)
    @Operation(summary = "Create a new lead", description = "Creates a lead within the caller's tenant boundary and logs an append-only audit event.")
    public ResponseEntity<LeadResponse> createLead(@Valid @RequestBody CreateLeadRequest request) {
        LeadResponse created = leadService.createLead(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize(UserRole.Roles.HAS_LEAD_READ)
    @Operation(summary = "Get lead by ID", description = "Returns lead data. Sensitive PII (PAN, phone, bank details) is masked unless the caller holds COMPLIANCE or SUPER_ADMIN roles.")
    public ResponseEntity<LeadResponse> getLeadById(@PathVariable UUID id) {
        LeadResponse response = leadService.getLeadById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize(UserRole.Roles.HAS_LEAD_READ)
    @Operation(summary = "List and search leads", description = "Retrieves paginated leads scoped to caller's tenant with optional status and agent filters.")
    public ResponseEntity<Page<LeadResponse>> getLeads(
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) UUID agentId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<LeadResponse> leads = leadService.getLeads(status, agentId, pageable);
        return ResponseEntity.ok(leads);
    }

    @PutMapping("/{id}")
    @PreAuthorize(UserRole.Roles.HAS_LEAD_WRITE)
    @Operation(summary = "Update lead details", description = "Updates lead details within tenant boundaries and tracks state diff in the audit trail.")
    public ResponseEntity<LeadResponse> updateLead(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLeadRequest request
    ) {
        LeadResponse updated = leadService.updateLead(id, request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(UserRole.Roles.HAS_LEAD_WRITE)
    @Operation(summary = "Update lead pipeline status", description = "Transitions lead through origination stages (e.g. QUALIFIED, UNDERWRITING, APPROVED).")
    public ResponseEntity<LeadResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLeadStatusRequest request
    ) {
        LeadResponse updated = leadService.updateLeadStatus(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(UserRole.Roles.HAS_ADMIN)
    @MfaRequired(message = "MFA verification is strictly required to delete lead records.")
    @Operation(summary = "Delete lead", description = "Privileged action requiring Step-Up MFA and admin roles.")
    public ResponseEntity<Void> deleteLead(@PathVariable UUID id) {
        leadService.deleteLead(id);
        return ResponseEntity.noContent().build();
    }
}
