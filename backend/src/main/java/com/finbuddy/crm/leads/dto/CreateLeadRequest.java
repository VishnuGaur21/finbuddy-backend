package com.finbuddy.crm.leads.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record CreateLeadRequest(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone must be a valid 10-digit Indian mobile number")
        String phone,

        @Email(message = "Email must be valid")
        String email,

        @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Invalid PAN card format (e.g. ABCDE1234F)")
        String panCard,

        LocalDate dateOfBirth,
        String gender,
        String city,
        String state,
        String pincode,

        @NotBlank(message = "Employment type is required")
        String employmentType,

        @NotNull(message = "Monthly income is required")
        @DecimalMin(value = "5000.00", message = "Minimum monthly income is 5000.00")
        BigDecimal monthlyIncome,

        @NotNull(message = "Loan amount requested is required")
        @DecimalMin(value = "10000.00", message = "Minimum loan amount is 10000.00")
        BigDecimal loanAmountRequested,

        String source,
        UUID campaignId,
        UUID assignedAgentId,
        Integer creditScore,
        String bankAccountNumber,
        String bankIfsc,

        @NotNull(message = "Customer consent is mandatory under regulatory compliance")
        Boolean consentObtained
) {}
