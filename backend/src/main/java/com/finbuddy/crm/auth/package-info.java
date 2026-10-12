/**
 * Authentication module: OIDC token exchange, MFA TOTP issuance, and session revocation.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"shared", "users"}
)
package com.finbuddy.crm.auth;
