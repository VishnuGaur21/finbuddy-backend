package com.finbuddy.crm.leads.dto;

import com.finbuddy.crm.leads.domain.LeadStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateLeadStatusRequest(
        @NotNull(message = "New lead status is required")
        LeadStatus status,

        String reasonNote
) {}
