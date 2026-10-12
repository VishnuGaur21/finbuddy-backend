package com.finbuddy.crm.leads.domain;

import com.finbuddy.crm.shared.security.pii.MaskPII;
import com.finbuddy.crm.shared.security.pii.PiiType;
import com.finbuddy.crm.shared.tenant.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Enterprise Lead entity scoped strictly to a tenant, with field-level PII protection and optimistic locking.
 */
@Entity
@Table(name = "leads")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lead extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @MaskPII(type = PiiType.PHONE)
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @MaskPII(type = PiiType.EMAIL)
    @Column(name = "email", length = 255)
    private String email;

    @MaskPII(type = PiiType.PAN)
    @Column(name = "pan_card", length = 10)
    private String panCard;

    @Column(name = "aadhaar_hash", length = 64)
    private String aadhaarHash;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "employment_type", nullable = false, length = 50)
    private String employmentType;

    @Column(name = "monthly_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(name = "loan_amount_requested", nullable = false, precision = 15, scale = 2)
    private BigDecimal loanAmountRequested;

    @Enumerated(EnumType.STRING)
    @Column(name = "lead_status", nullable = false, length = 50)
    private LeadStatus leadStatus;

    @Column(name = "source", nullable = false, length = 100)
    private String source;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;

    @Column(name = "credit_score")
    private Integer creditScore;

    @MaskPII(type = PiiType.BANK_ACCOUNT)
    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc", length = 20)
    private String bankIfsc;

    @Column(name = "consent_obtained", nullable = false)
    private Boolean consentObtained;

    @Column(name = "consent_timestamp")
    private OffsetDateTime consentTimestamp;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
