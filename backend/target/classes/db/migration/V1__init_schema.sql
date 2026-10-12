-- ============================================================================
-- FINBUDDY CRM - Enterprise Multi-Tenant DDL Schema V1
-- PostgreSQL 15+ / 16+ compatible
-- Modular Monolith Domain Architecture:
-- 1. Identity & Access (Tenants, Users, RBAC, MFA)
-- 2. Marketing & Tracking (Campaigns, Ad Sets, Traffic Sources, Clicks)
-- 3. Lead Operations (Leads, Assignments, Activities, Notes)
-- 4. Omnichannel Conversations (Chats, Call Logs, WhatsApp)
-- 5. Lending Engine (Partners, Products, Applications, Dispatches, Offers)
-- 6. Commercials & Commissions (Payout Slabs, Ledgers, Invoices, Settlements)
-- 7. Governance, Integrations & Async (Audit Chain, Webhooks, Idempotency, Imports)
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- DOMAIN 1: IDENTITY, MULTI-TENANCY & RBAC
-- ============================================================================

CREATE TABLE IF NOT EXISTS organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    domain VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    settings JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_system_role BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed 11 Core Enterprise Roles
INSERT INTO roles (code, name, description) VALUES
    ('ROLE_SUPER_ADMIN', 'Super Admin', 'Platform-wide administrator with unrestricted root privilege'),
    ('ROLE_COMPANY_ADMIN', 'Company Admin', 'Tenant owner/admin with full control over tenant domain'),
    ('ROLE_MARKETING_MANAGER', 'Marketing Manager', 'Manages campaigns, budgets, and marketing operations'),
    ('ROLE_PERFORMANCE_MARKETER', 'Performance Marketer', 'Optimizes ads, clicks, attribution, and pixel tracking'),
    ('ROLE_DATA_VENDOR_MANAGER', 'Data/Vendor Manager', 'Controls bulk lead ingestion, vendor lists, and imports'),
    ('ROLE_SALES_MANAGER', 'Sales Manager', 'Oversees lead assignment, queues, and team conversion metrics'),
    ('ROLE_SALES_AGENT', 'Sales Agent', 'Interacts with leads, updates statuses, logs calls and notes'),
    ('ROLE_AFFILIATE', 'Affiliate Partner', 'External referral partner tracking referred leads & commissions'),
    ('ROLE_LENDING_PARTNER', 'Lending Partner (NBFC/Bank)', 'External lender reviewing dispatches and issuing offers'),
    ('ROLE_FINANCE', 'Finance Executive', 'Handles commission ledgers, invoices, payouts, and bank details'),
    ('ROLE_COMPLIANCE', 'Compliance & Risk Officer', 'Full PII unmasking access, audit log review, regulatory oversight')
ON CONFLICT (code) DO NOTHING;

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone_encrypted VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS mfa_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL DEFAULT 'TOTP',
    secret_encrypted VARCHAR(512) NOT NULL,
    backup_codes_hash TEXT,
    verified_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_mfa_user_type UNIQUE (user_id, type)
);

-- ============================================================================
-- DOMAIN 2: MARKETING & TRACKING
-- ============================================================================

CREATE TABLE IF NOT EXISTS campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) NOT NULL,
    channel VARCHAR(50) NOT NULL, -- GOOGLE_ADS, META, AFFILIATE, SMS, DIRECT
    budget NUMERIC(15, 2) DEFAULT 0.00,
    start_date DATE,
    end_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_campaign_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE IF NOT EXISTS ad_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    targeting_criteria JSONB DEFAULT '{}'::jsonb,
    budget NUMERIC(15, 2) DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS traffic_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    utm_source VARCHAR(100) NOT NULL,
    utm_medium VARCHAR(100),
    utm_campaign VARCHAR(100),
    webhook_api_key VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tracking_clicks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    campaign_id UUID REFERENCES campaigns(id) ON DELETE SET NULL,
    click_id VARCHAR(255) NOT NULL,
    ip_hash VARCHAR(64) NOT NULL,
    user_agent TEXT,
    referrer TEXT,
    clicked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- DOMAIN 3: LEAD OPERATIONS
-- ============================================================================

CREATE TABLE IF NOT EXISTS leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    pan_card VARCHAR(10),
    aadhaar_hash VARCHAR(64),
    date_of_birth DATE,
    gender VARCHAR(20),
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(10),
    employment_type VARCHAR(50) NOT NULL, -- SALARIED, SELF_EMPLOYED, BUSINESS
    monthly_income NUMERIC(15, 2) NOT NULL,
    loan_amount_requested NUMERIC(15, 2) NOT NULL,
    lead_status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    source VARCHAR(100) NOT NULL DEFAULT 'ORGANIC',
    campaign_id UUID REFERENCES campaigns(id) ON DELETE SET NULL,
    assigned_agent_id UUID REFERENCES users(id) ON DELETE SET NULL,
    credit_score INT,
    bank_account_number VARCHAR(50),
    bank_ifsc VARCHAR(20),
    consent_obtained BOOLEAN NOT NULL DEFAULT FALSE,
    consent_timestamp TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lead_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    lead_id UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    agent_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_by UUID REFERENCES users(id),
    reason VARCHAR(255),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lead_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    lead_id UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    activity_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb,
    actor_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lead_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    lead_id UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    note TEXT NOT NULL,
    author_id UUID NOT NULL REFERENCES users(id),
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- DOMAIN 4: OMNICHANNEL CONVERSATIONS
-- ============================================================================

CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    lead_id UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    channel VARCHAR(50) NOT NULL, -- WHATSAPP, TELEPHONY, SMS, EMAIL, IN_APP
    external_chat_id VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    initiated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_message_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS call_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    lead_id UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    agent_id UUID REFERENCES users(id) ON DELETE SET NULL,
    call_sid VARCHAR(255) NOT NULL,
    direction VARCHAR(20) NOT NULL, -- INBOUND, OUTBOUND
    duration_seconds INT DEFAULT 0,
    recording_s3_url TEXT,
    call_status VARCHAR(50) NOT NULL,
    disposition VARCHAR(100),
    called_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS whatsapp_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    lead_id UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    message_sid VARCHAR(255) NOT NULL,
    direction VARCHAR(20) NOT NULL, -- INBOUND, OUTBOUND
    content TEXT,
    template_name VARCHAR(100),
    status VARCHAR(50) NOT NULL, -- QUEUED, SENT, DELIVERED, READ, FAILED
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- DOMAIN 5: LENDING ENGINE (PARTNERS, PRODUCTS & APPLICATIONS)
-- ============================================================================

CREATE TABLE IF NOT EXISTS lending_partners (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    partner_code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    api_base_url VARCHAR(500),
    webhook_secret VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    integration_type VARCHAR(50) NOT NULL DEFAULT 'REST_API',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_lender_tenant_code UNIQUE (tenant_id, partner_code)
);

CREATE TABLE IF NOT EXISTS partner_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    partner_id UUID NOT NULL REFERENCES lending_partners(id) ON DELETE CASCADE,
    product_name VARCHAR(255) NOT NULL,
    product_code VARCHAR(50) NOT NULL,
    product_type VARCHAR(50) NOT NULL, -- PERSONAL_LOAN, BUSINESS_LOAN, LAP, CREDIT_CARD
    min_income NUMERIC(15, 2) NOT NULL,
    min_cibil INT NOT NULL DEFAULT 650,
    interest_rate_min NUMERIC(5, 2),
    interest_rate_max NUMERIC(5, 2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS loan_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    lead_id UUID NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    partner_id UUID NOT NULL REFERENCES lending_partners(id) ON DELETE CASCADE,
    partner_product_id UUID REFERENCES partner_products(id),
    application_reference VARCHAR(100) NOT NULL,
    requested_amount NUMERIC(15, 2) NOT NULL,
    approved_amount NUMERIC(15, 2),
    tenure_months INT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, SUBMITTED, UNDERWRITING, APPROVED, REJECTED, DISBURSED
    submitted_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_app_tenant_ref UNIQUE (tenant_id, application_reference)
);

CREATE TABLE IF NOT EXISTS lender_dispatches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    application_id UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    payload_hash VARCHAR(64) NOT NULL,
    request_payload JSONB NOT NULL,
    response_payload JSONB,
    http_status INT,
    dispatched_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lender_offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    application_id UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    offer_reference VARCHAR(100) NOT NULL,
    sanctioned_amount NUMERIC(15, 2) NOT NULL,
    roi NUMERIC(5, 2) NOT NULL,
    emi NUMERIC(15, 2) NOT NULL,
    processing_fee NUMERIC(15, 2) DEFAULT 0.00,
    valid_until TIMESTAMP WITH TIME ZONE,
    offer_status VARCHAR(50) NOT NULL DEFAULT 'SANCTIONED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- DOMAIN 6: COMMERCIALS & COMMISSIONS
-- ============================================================================

CREATE TABLE IF NOT EXISTS payout_slabs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    partner_id UUID NOT NULL REFERENCES lending_partners(id) ON DELETE CASCADE,
    product_type VARCHAR(50) NOT NULL,
    min_disbursal NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    max_disbursal NUMERIC(15, 2),
    payout_percentage NUMERIC(5, 2),
    fixed_payout NUMERIC(15, 2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS commission_ledgers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    partner_id UUID NOT NULL REFERENCES lending_partners(id) ON DELETE CASCADE,
    application_id UUID NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    disbursal_amount NUMERIC(15, 2) NOT NULL,
    commission_amount NUMERIC(15, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACCRUED', -- ACCRUED, INVOICED, SETTLED, DISPUTED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    partner_id UUID NOT NULL REFERENCES lending_partners(id) ON DELETE CASCADE,
    invoice_number VARCHAR(100) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL,
    gst_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_APPROVAL',
    due_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_invoice_tenant_num UNIQUE (tenant_id, invoice_number)
);

CREATE TABLE IF NOT EXISTS partner_settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    settlement_reference VARCHAR(100) NOT NULL,
    amount_paid NUMERIC(15, 2) NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS'
);

-- ============================================================================
-- DOMAIN 7: GOVERNANCE, INTEGRATIONS & ASYNC PROCESSING
-- ============================================================================

-- Tamper-Evident Append-Only Audit Trail (Cryptographic Hash Chained)
CREATE TABLE IF NOT EXISTS tamper_evident_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    old_state_json JSONB,
    new_state_json JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    prev_hash VARCHAR(64) NOT NULL,
    entry_hash VARCHAR(64) NOT NULL
);

-- Webhook Verification & Inbound Callbacks
CREATE TABLE IF NOT EXISTS webhook_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    partner_code VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    headers JSONB NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'RECEIVED', -- RECEIVED, PROCESSED, DUPLICATE, FAILED
    signature VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    error_details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_webhook_tenant_key UNIQUE (tenant_id, idempotency_key)
);

-- Distributed Idempotency Locks
CREATE TABLE IF NOT EXISTS idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    key VARCHAR(255) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    response_payload JSONB,
    status_code INT,
    locked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_idempotency_tenant_key UNIQUE (tenant_id, key)
);

-- Bulk Asynchronous File Imports & Row-Level Error Tracking
CREATE TABLE IF NOT EXISTS import_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    file_type VARCHAR(50) NOT NULL, -- CSV, XLSX
    total_rows INT NOT NULL DEFAULT 0,
    processed_rows INT NOT NULL DEFAULT 0,
    successful_rows INT NOT NULL DEFAULT 0,
    failed_rows INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, COMPLETED, FAILED, PARTIALLY_COMPLETED
    current_row_index INT NOT NULL DEFAULT 0,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS import_row_errors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES import_jobs(id) ON DELETE CASCADE,
    row_number INT NOT NULL,
    column_name VARCHAR(100),
    raw_value TEXT,
    error_message TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- INDEXES FOR TENANT ISOLATION & QUERY OPTIMIZATION
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_users_tenant ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_leads_tenant ON leads(tenant_id);
CREATE INDEX IF NOT EXISTS idx_leads_tenant_status ON leads(tenant_id, lead_status);
CREATE INDEX IF NOT EXISTS idx_leads_tenant_phone ON leads(tenant_id, phone);
CREATE INDEX IF NOT EXISTS idx_leads_tenant_pan ON leads(tenant_id, pan_card);
CREATE INDEX IF NOT EXISTS idx_leads_assigned_agent ON leads(tenant_id, assigned_agent_id);
CREATE INDEX IF NOT EXISTS idx_lead_activities_tenant_lead ON lead_activities(tenant_id, lead_id);
CREATE INDEX IF NOT EXISTS idx_campaigns_tenant ON campaigns(tenant_id);
CREATE INDEX IF NOT EXISTS idx_loan_apps_tenant_lead ON loan_applications(tenant_id, lead_id);
CREATE INDEX IF NOT EXISTS idx_loan_apps_tenant_partner ON loan_applications(tenant_id, partner_id);
CREATE INDEX IF NOT EXISTS idx_audit_tenant_time ON tamper_evident_audit_logs(tenant_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON tamper_evident_audit_logs(tenant_id, entity_name, entity_id);
CREATE INDEX IF NOT EXISTS idx_webhook_tenant_key ON webhook_logs(tenant_id, idempotency_key);
CREATE INDEX IF NOT EXISTS idx_import_jobs_tenant ON import_jobs(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_import_row_errors_job ON import_row_errors(job_id);
