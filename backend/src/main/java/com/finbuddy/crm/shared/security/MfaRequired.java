package com.finbuddy.crm.shared.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that an endpoint or business method strictly requires Step-Up MFA authentication.
 * Used for financial disbursements, PII unmasking, batch operations, and system alterations.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MfaRequired {
    String message() default "Multi-Factor Authentication (MFA) step-up verification is required for this action";
}
