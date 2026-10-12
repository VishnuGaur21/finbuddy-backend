package com.finbuddy.crm.integrations.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Verifies HMAC-SHA256 signatures for inbound NBFC & Lending Partner webhook callbacks.
 * Enforces replay-attack prevention via timestamp tolerance checks and timing-safe comparisons.
 */
@Slf4j
@Component
public class WebhookSignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${finbuddy.webhooks.signing-secret:finbuddy_webhook_secret_key_prod}")
    private String defaultSigningSecret;

    @Value("${finbuddy.webhooks.signature-tolerance-seconds:300}")
    private long toleranceSeconds;

    public boolean verifySignature(String payload, String signature, String timestampHeader, String customSecret) {
        if (signature == null || timestampHeader == null) {
            log.warn("Missing webhook signature or timestamp header");
            return false;
        }

        // 1. Replay attack check: verify timestamp freshness
        try {
            long requestEpoch = Long.parseLong(timestampHeader);
            long currentEpoch = Instant.now().getEpochSecond();
            if (Math.abs(currentEpoch - requestEpoch) > toleranceSeconds) {
                log.warn("Webhook timestamp expired or out of tolerance window: request={}, current={}, delta={}",
                        requestEpoch, currentEpoch, Math.abs(currentEpoch - requestEpoch));
                return false;
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid webhook timestamp format: {}", timestampHeader);
            return false;
        }

        // 2. Compute expected HMAC: v1=HMAC_SHA256(timestamp + "." + payload, secret)
        String secretToUse = customSecret != null ? customSecret : defaultSigningSecret;
        String dataToSign = timestampHeader + "." + payload;
        String expectedSignature = computeHmacSha256(dataToSign, secretToUse);

        // 3. Timing-attack safe comparison
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.replace("v1=", "").getBytes(StandardCharsets.UTF_8)
        );
    }

    public String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to calculate HMAC signature", e);
        }
    }
}
