package com.finbuddy.crm.shared.security.pii;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

/**
 * Jackson Serializer that inspects caller authorities and applies masking if the caller
 * does not possess least-privilege bypass permissions (e.g., COMPLIANCE or SUPER_ADMIN).
 */
public class PiiMaskingSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private final PiiType piiType;

    public PiiMaskingSerializer() {
        this.piiType = PiiType.PHONE;
    }

    public PiiMaskingSerializer(PiiType piiType) {
        this.piiType = piiType;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        if (canViewUnmaskedPii()) {
            gen.writeString(value);
        } else {
            gen.writeString(MaskUtils.mask(value, this.piiType));
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        if (property != null) {
            MaskPII annotation = property.getAnnotation(MaskPII.class);
            if (annotation != null) {
                return new PiiMaskingSerializer(annotation.type());
            }
        }
        return this;
    }

    private boolean canViewUnmaskedPii() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        // Compliance Officers and Super Admins are permitted to view raw PII
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_SUPER_ADMIN".equals(role)
                    || "ROLE_COMPLIANCE".equals(role)
                    || "PII_UNMASK".equals(role)) {
                return true;
            }
        }
        return false;
    }
}
