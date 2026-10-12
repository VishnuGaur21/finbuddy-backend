/**
 * Integrations module: External NBFC webhooks, HMAC signature verification, and distributed idempotency.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"shared"}
)
package com.finbuddy.crm.integrations;
