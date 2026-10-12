package com.finbuddy.crm.leads.dto;

import com.finbuddy.crm.leads.domain.LeadStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record UpdateLeadRequest(
        String firstName,
        String lastName,
        String email,
        String city,
        String state,
        String pincode,
        String employmentType,
        BigDecimal monthlyIncome,
        BigDecimal loanAmountRequested,
        UUID assignedAgentId,
        Integer creditScore,
        String bankAccountNumber,
        String bankIfsc,
        LeadStatus leadStatus
) {}
