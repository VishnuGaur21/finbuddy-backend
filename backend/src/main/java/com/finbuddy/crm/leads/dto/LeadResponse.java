package com.finbuddy.crm.leads.dto;

import com.finbuddy.crm.leads.domain.Lead;
import com.finbuddy.crm.leads.domain.LeadStatus;
import com.finbuddy.crm.shared.security.pii.MaskPII;
import com.finbuddy.crm.shared.security.pii.PiiType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record LeadResponse(
        UUID id,
        UUID tenantId,
        String firstName,
        String lastName,

        @MaskPII(type = PiiType.PHONE)
        String phone,

        @MaskPII(type = PiiType.EMAIL)
        String email,

        @MaskPII(type = PiiType.PAN)
        String panCard,

        LocalDate dateOfBirth,
        String gender,
        String city,
        String state,
        String pincode,
        String employmentType,
        BigDecimal monthlyIncome,
        BigDecimal loanAmountRequested,
        LeadStatus leadStatus,
        String source,
        UUID campaignId,
        UUID assignedAgentId,
        Integer creditScore,

        @MaskPII(type = PiiType.BANK_ACCOUNT)
        String bankAccountNumber,

        String bankIfsc,
        Boolean consentObtained,
        OffsetDateTime consentTimestamp,
        Long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static LeadResponse fromEntity(Lead entity) {
        if (entity == null) return null;
        return LeadResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .panCard(entity.getPanCard())
                .dateOfBirth(entity.getDateOfBirth())
                .gender(entity.getGender())
                .city(entity.getCity())
                .state(entity.getState())
                .pincode(entity.getPincode())
                .employmentType(entity.getEmploymentType())
                .monthlyIncome(entity.getMonthlyIncome())
                .loanAmountRequested(entity.getLoanAmountRequested())
                .leadStatus(entity.getLeadStatus())
                .source(entity.getSource())
                .campaignId(entity.getCampaignId())
                .assignedAgentId(entity.getAssignedAgentId())
                .creditScore(entity.getCreditScore())
                .bankAccountNumber(entity.getBankAccountNumber())
                .bankIfsc(entity.getBankIfsc())
                .consentObtained(entity.getConsentObtained())
                .consentTimestamp(entity.getConsentTimestamp())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
