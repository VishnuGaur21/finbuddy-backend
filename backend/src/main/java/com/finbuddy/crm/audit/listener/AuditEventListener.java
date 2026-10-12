package com.finbuddy.crm.audit.listener;

import com.finbuddy.crm.audit.service.AuditLogService;
import com.finbuddy.crm.shared.audit.AuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Spring Modulith listener that reliably receives AuditEvents and persists them to the tamper-evident log.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogService auditLogService;

    @ApplicationModuleListener
    public void onAuditEvent(AuditEvent event) {
        log.info("Processing audit event: action={}, entityName={}, entityId={}",
                event.action(), event.entityName(), event.entityId());
        try {
            auditLogService.appendAuditLog(event);
        } catch (Exception ex) {
            log.error("Failed to append audit log for event: {}", event, ex);
        }
    }
}
