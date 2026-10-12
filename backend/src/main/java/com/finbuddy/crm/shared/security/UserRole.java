package com.finbuddy.crm.shared.security;

import lombok.Getter;

/**
 * 11 Distinct Enterprise RBAC Roles for Finbuddy CRM with principle of least privilege.
 */
@Getter
public enum UserRole {
    SUPER_ADMIN("ROLE_SUPER_ADMIN", "Super Administrator"),
    COMPANY_ADMIN("ROLE_COMPANY_ADMIN", "Company / Tenant Administrator"),
    MARKETING_MANAGER("ROLE_MARKETING_MANAGER", "Marketing Manager"),
    PERFORMANCE_MARKETER("ROLE_PERFORMANCE_MARKETER", "Performance Marketer"),
    DATA_VENDOR_MANAGER("ROLE_DATA_VENDOR_MANAGER", "Data & Vendor Manager"),
    SALES_MANAGER("ROLE_SALES_MANAGER", "Sales Team Manager"),
    SALES_AGENT("ROLE_SALES_AGENT", "Direct Sales Agent"),
    AFFILIATE("ROLE_AFFILIATE", "External Affiliate Partner"),
    LENDING_PARTNER("ROLE_LENDING_PARTNER", "External Lending Partner / NBFC"),
    FINANCE("ROLE_FINANCE", "Finance & Settlement Executive"),
    COMPLIANCE("ROLE_COMPLIANCE", "Compliance, Risk & Audit Officer");

    private final String authority;
    private final String displayName;

    UserRole(String authority, String displayName) {
        this.authority = authority;
        this.displayName = displayName;
    }

    public static final class Roles {
        public static final String SUPER_ADMIN = "SUPER_ADMIN";
        public static final String COMPANY_ADMIN = "COMPANY_ADMIN";
        public static final String MARKETING_MANAGER = "MARKETING_MANAGER";
        public static final String PERFORMANCE_MARKETER = "PERFORMANCE_MARKETER";
        public static final String DATA_VENDOR_MANAGER = "DATA_VENDOR_MANAGER";
        public static final String SALES_MANAGER = "SALES_MANAGER";
        public static final String SALES_AGENT = "SALES_AGENT";
        public static final String AFFILIATE = "AFFILIATE";
        public static final String LENDING_PARTNER = "LENDING_PARTNER";
        public static final String FINANCE = "FINANCE";
        public static final String COMPLIANCE = "COMPLIANCE";

        public static final String HAS_ADMIN = "hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN')";
        public static final String HAS_LEAD_WRITE = "hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN', 'SALES_MANAGER', 'SALES_AGENT', 'DATA_VENDOR_MANAGER')";
        public static final String HAS_LEAD_READ = "hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN', 'SALES_MANAGER', 'SALES_AGENT', 'MARKETING_MANAGER', 'PERFORMANCE_MARKETER', 'FINANCE', 'COMPLIANCE')";
        public static final String HAS_IMPORT_WRITE = "hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN', 'DATA_VENDOR_MANAGER')";
        public static final String HAS_FINANCE_PRIVILEGE = "hasAnyRole('SUPER_ADMIN', 'COMPANY_ADMIN', 'FINANCE')";
        public static final String HAS_COMPLIANCE_PRIVILEGE = "hasAnyRole('SUPER_ADMIN', 'COMPLIANCE')";

        private Roles() {}
    }
}
