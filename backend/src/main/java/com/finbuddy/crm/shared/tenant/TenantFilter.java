package com.finbuddy.crm.shared.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that establishes the multi-tenant context per incoming HTTP request.
 * Prioritizes the tenant_id claim in authenticated JWTs, falling back to the X-Tenant-ID header.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-ID";
    public static final String MDC_TENANT_KEY = "tenantId";
    public static final String MDC_USER_KEY = "userId";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            UUID tenantId = resolveTenantId(request);
            UUID userId = resolveUserId();

            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
                MDC.put(MDC_TENANT_KEY, tenantId.toString());
                response.setHeader(TENANT_HEADER, tenantId.toString());
            }

            if (userId != null) {
                TenantContext.setUserId(userId);
                MDC.put(MDC_USER_KEY, userId.toString());
            }

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
            MDC.remove(MDC_TENANT_KEY);
            MDC.remove(MDC_USER_KEY);
        }
    }

    private UUID resolveTenantId(HttpServletRequest request) {
        // 1. Try JWT claim if already authenticated or available
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String tenantClaim = jwt.getClaimAsString("tenant_id");
            if (StringUtils.hasText(tenantClaim)) {
                try {
                    return UUID.fromString(tenantClaim);
                } catch (IllegalArgumentException ex) {
                    log.warn("Invalid tenant_id UUID format in JWT claim: {}", tenantClaim);
                }
            }
        }

        // 2. Fallback to HTTP Header
        String headerTenant = request.getHeader(TENANT_HEADER);
        if (StringUtils.hasText(headerTenant)) {
            try {
                return UUID.fromString(headerTenant.trim());
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid X-Tenant-ID header UUID format: {}", headerTenant);
            }
        }

        return null;
    }

    private UUID resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            if (StringUtils.hasText(subject)) {
                try {
                    return UUID.fromString(subject);
                } catch (IllegalArgumentException ex) {
                    // Not a UUID, ignore
                }
            }
        }
        return null;
    }
}
