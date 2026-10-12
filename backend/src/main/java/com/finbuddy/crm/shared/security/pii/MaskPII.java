package com.finbuddy.crm.shared.security.pii;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Field-level annotation that triggers context-aware masking during Jackson JSON serialization.
 * Users without COMPLIANCE, FINANCE, or explicit PII_UNMASK authority will receive masked values.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = PiiMaskingSerializer.class)
public @interface MaskPII {
    PiiType type();
}
