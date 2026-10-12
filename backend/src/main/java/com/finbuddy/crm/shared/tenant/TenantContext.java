package com.finbuddy.crm.shared.tenant;

import java.util.Optional;
import java.util.UUID;

/**
 * ThreadLocal-based context storing current Tenant ID and User ID for request lifecycle isolation.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new InheritableThreadLocal<>();
    private static final ThreadLocal<UUID> CURRENT_USER = new InheritableThreadLocal<>();

    private TenantContext() {
        // Utility class
    }

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static Optional<UUID> getOptionalTenantId() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    public static void setUserId(UUID userId) {
        CURRENT_USER.set(userId);
    }

    public static UUID getUserId() {
        return CURRENT_USER.get();
    }

    public static Optional<UUID> getOptionalUserId() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_USER.remove();
    }
}
