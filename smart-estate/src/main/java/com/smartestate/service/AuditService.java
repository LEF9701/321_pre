package com.smartestate.service;

import com.smartestate.model.AuditLog;
import com.smartestate.repo.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** Lightweight audit trail for sensitive reads/writes (NFR Security) and emergency compliance (FR-7.5). */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    public AuditService(AuditLogRepository auditLogRepository, CurrentUserService currentUserService) {
        this.auditLogRepository = auditLogRepository;
        this.currentUserService = currentUserService;
    }

    public void log(String action, String detail) {
        record(action, detail, false);
    }

    public void logEmergency(String action, String detail) {
        record(action, detail, true);
    }

    public void log(String actor, String action, String detail, boolean emergencyRelated) {
        AuditLog entry = new AuditLog();
        entry.setTimestamp(LocalDateTime.now());
        entry.setActor(actor);
        entry.setAction(action);
        entry.setDetail(detail);
        entry.setEmergencyRelated(emergencyRelated);
        auditLogRepository.save(entry);
    }

    private void record(String action, String detail, boolean emergencyRelated) {
        String actor = currentUserService.displayName();
        if (actor == null || actor.isBlank()) actor = "system";
        log(actor, action, detail, emergencyRelated);
    }
}
