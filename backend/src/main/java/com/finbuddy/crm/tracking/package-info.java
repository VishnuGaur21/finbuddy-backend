/**
 * Tracking module: Click tracking, UTM attribution, pixel ingestion, and fraud detection.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"shared", "campaigns"}
)
package com.finbuddy.crm.tracking;
