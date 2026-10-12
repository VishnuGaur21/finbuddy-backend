package com.finbuddy.crm.integrations.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Builder
public record LenderWebhookPayload(
        @NotBlank(message = "Event type is required")
        @JsonProperty("event_type")
        String eventType,

        @NotBlank(message = "Application reference is required")
        @JsonProperty("application_reference")
        String applicationReference,

        @JsonProperty("partner_loan_id")
        String partnerLoanId,

        @JsonProperty("status")
        String status, // SANCTIONED, DISBURSED, REJECTED, DOCS_PENDING

        @JsonProperty("sanctioned_amount")
        BigDecimal sanctionedAmount,

        @JsonProperty("disbursed_amount")
        BigDecimal disbursedAmount,

        @JsonProperty("interest_rate")
        BigDecimal interestRate,

        @JsonProperty("tenure_months")
        Integer tenureMonths,

        @JsonProperty("event_timestamp")
        OffsetDateTime eventTimestamp,

        @JsonProperty("metadata")
        Map<String, Object> metadata
) {}
