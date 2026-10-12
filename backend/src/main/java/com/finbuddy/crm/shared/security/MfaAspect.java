package com.finbuddy.crm.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AOP Aspect intercepting methods annotated with @MfaRequired.
 * Verifies that the JWT contains 'mfa_verified: true' or an 'amr' claim containing 'mfa'/'totp'.
 */
@Slf4j
@Aspect
@Component
public class MfaAspect {

    @Before("@annotation(mfaRequired) || @within(mfaRequired)")
    public void enforceMfaVerification(MfaRequired mfaRequired) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new MfaRequiredException("Unauthenticated request: MFA state could not be determined");
        }

        // Check 1: Explicit boolean claim 'mfa_verified'
        Boolean mfaVerified = jwt.getClaimAsBoolean("mfa_verified");
        if (Boolean.TRUE.equals(mfaVerified)) {
            return;
        }

        // Check 2: Standard RFC 8176 Authentication Method Reference (amr) array claim
        List<String> amr = jwt.getClaimAsStringList("amr");
        if (amr != null && (amr.contains("mfa") || amr.contains("totp") || amr.contains("otp"))) {
            return;
        }

        log.warn("MFA verification check failed for user: {}", jwt.getSubject());
        throw new MfaRequiredException(mfaRequired.message());
    }
}
