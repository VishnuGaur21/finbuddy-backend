package com.finbuddy.crm.shared.security.pii;

import org.springframework.util.StringUtils;

/**
 * Utility functions for masking sensitive Personally Identifiable Information (PII).
 */
public final class MaskUtils {

    private MaskUtils() {}

    public static String mask(String value, PiiType type) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        return switch (type) {
            case PAN -> maskPan(value);
            case PHONE -> maskPhone(value);
            case BANK_ACCOUNT -> maskBankAccount(value);
            case AADHAAR -> maskAadhaar(value);
            case EMAIL -> maskEmail(value);
        };
    }

    public static String maskPan(String pan) {
        if (pan == null || pan.length() < 10) return "XXXXX****X";
        // Typical Indian PAN format: 5 letters, 4 digits, 1 letter. Mask the 4 middle digits: ABCDE****F
        return pan.substring(0, 5) + "****" + pan.substring(9);
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "XXXXXX";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() <= 4) return "******" + digits;
        int keep = 4;
        String lastFour = digits.substring(digits.length() - keep);
        return "XXXXXX" + lastFour;
    }

    public static String maskBankAccount(String acc) {
        if (acc == null || acc.length() <= 4) return "XXXX";
        int keep = 4;
        String lastFour = acc.substring(acc.length() - keep);
        return "X".repeat(acc.length() - keep) + lastFour;
    }

    public static String maskAadhaar(String aadhaar) {
        if (aadhaar == null || aadhaar.length() < 4) return "XXXX-XXXX-XXXX";
        String digits = aadhaar.replaceAll("[^0-9]", "");
        if (digits.length() < 4) return "XXXX-XXXX-XXXX";
        String lastFour = digits.substring(digits.length() - 4);
        return "XXXX-XXXX-" + lastFour;
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "x***@domain.com";
        int atIndex = email.indexOf('@');
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (username.length() <= 2) {
            return username.charAt(0) + "***" + domain;
        }
        return username.charAt(0) + "***" + username.charAt(username.length() - 1) + domain;
    }
}
