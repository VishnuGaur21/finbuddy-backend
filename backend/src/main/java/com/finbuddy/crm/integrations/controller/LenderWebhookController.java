package com.finbuddy.crm.integrations.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finbuddy.crm.integrations.dto.LenderWebhookPayload;
import com.finbuddy.crm.integrations.service.IdempotencyService;
import com.finbuddy.crm.integrations.service.WebhookSignatureVerifier;
import com.finbuddy.crm.shared.audit.AuditAction;
import com.finbuddy.crm.shared.audit.AuditEvent;
import com.finbuddy.crm.shared.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Public REST Controller handling cryptographically signed callbacks from NBFCs and Banking Partners.
 * Enforces HMAC-SHA256 signature verification, replay protection, and distributed idempotency.
 */
@Slf4j
@RestController
@RequestMapping("/integrations/webhooks")
@RequiredArgsConstructor
@Tag(name = "Partner Webhooks", description = "Inbound signed callbacks from lending partners and NBFCs")
public class LenderWebhookController {

    private final WebhookSignatureVerifier signatureVerifier;
    private final IdempotencyService idempotencyService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/lenders/{partnerCode}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Receive signed loan status callback",
            description = "Validates partner HMAC signature, checks idempotency key to reject duplicates, and processes application updates.")
    public ResponseEntity<Map<String, Object>> handleLenderCallback(
            @PathVariable String partnerCode,
            @RequestHeader(value = "X-Signature") String signature,
            @RequestHeader(value = "X-Timestamp") String timestamp,
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader,
            @RequestBody String rawPayload
    ) {
        log.info("Received partner callback from partnerCode={}, idempotencyKey={}", partnerCode, idempotencyKey);

        UUID tenantId = tenantHeader != null ? UUID.fromString(tenantHeader) : TenantContext.getTenantId();

        // 1. Check for Duplicate Callbacks using Idempotency Service
        Optional<String> cachedResponse = idempotencyService.getExistingResponse(tenantId, idempotencyKey);
        if (cachedResponse.isPresent()) {
            log.info("Duplicate callback detected for idempotencyKey={}. Returning cached 200 response.", idempotencyKey);
            return ResponseEntity.ok(Map.of(
                    "status", "DUPLICATE_ACKNOWLEDGED",
                    "idempotencyKey", idempotencyKey,
                    "message", "Webhook callback was previously processed successfully."
            ));
        }

        // 2. Cryptographic Signature & Replay Verification
        boolean isValid = signatureVerifier.verifySignature(rawPayload, signature, timestamp, null);
        if (!isValid) {
            log.warn("Invalid webhook signature or stale timestamp for partnerCode={}", partnerCode);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "REJECTED",
                    "error", "Invalid HMAC signature or timestamp expired outside tolerance window"
            ));
        }

        // 3. Acquire Distributed Lock for Idempotency
        if (!idempotencyService.tryAcquire(tenantId, idempotencyKey)) {
            log.warn("Concurrent callback detected for idempotencyKey={}", idempotencyKey);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "CONFLICT",
                    "error", "A webhook with this idempotency key is already in progress."
            ));
        }

        try {
            LenderWebhookPayload payload = objectMapper.readValue(rawPayload, LenderWebhookPayload.class);

            // 4. Log and Publish Audit Event
            eventPublisher.publishEvent(AuditEvent.of(
                    tenantId,
                    null,
                    AuditAction.WEBHOOK_RECEIVED,
                    "LenderWebhook",
                    payload.applicationReference(),
                    null,
                    rawPayload
            ));

            log.info("Successfully processed callback event={} for ref={}", payload.eventType(), payload.applicationReference());

            Map<String, Object> response = Map.of(
                    "status", "PROCESSED",
                    "partnerCode", partnerCode,
                    "applicationReference", payload.applicationReference(),
                    "idempotencyKey", idempotencyKey
            );

            // 5. Store completed response in Redis idempotency cache
            idempotencyService.markCompleted(tenantId, idempotencyKey, objectMapper.writeValueAsString(response));

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.error("Failed to process webhook payload: ", ex);
            idempotencyService.releaseLock(tenantId, idempotencyKey);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "ERROR",
                    "error", "Internal error processing partner callback: " + ex.getMessage()
            ));
        }
    }
}
