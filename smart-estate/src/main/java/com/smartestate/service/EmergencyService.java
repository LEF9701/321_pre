package com.smartestate.service;

import com.smartestate.model.*;
import com.smartestate.repo.EmergencyEventRepository;
import com.smartestate.repo.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Emergency alert lifecycle (FR-7). */
@Service
public class EmergencyService {

    private final EmergencyEventRepository emergencyRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDispatchService dispatchService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public EmergencyService(EmergencyEventRepository emergencyRepository,
                            NotificationRepository notificationRepository,
                            NotificationDispatchService dispatchService,
                            AuditService auditService,
                            CurrentUserService currentUserService) {
        this.emergencyRepository = emergencyRepository;
        this.notificationRepository = notificationRepository;
        this.dispatchService = dispatchService;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    public Optional<EmergencyEvent> active() {
        return emergencyRepository.findFirstByActiveTrueOrderByDeclaredAtDesc();
    }

    public List<EmergencyEvent> history() {
        return emergencyRepository.findAllByOrderByDeclaredAtDesc();
    }

    /** FR-7.1: admin declares an emergency; system enters elevated state. */
    @Transactional
    public EmergencyEvent declare(EmergencyType type, String note) {
        EmergencyEvent e = new EmergencyEvent();
        e.setType(type);
        e.setNote(note);
        e.setActive(true);
        e.setDeclaredAt(LocalDateTime.now());
        e.setDeclaredBy(currentUserService.displayName());
        e = emergencyRepository.save(e);
        auditService.logEmergency("EMERGENCY_DECLARE",
                type.getLabel() + " declared by " + e.getDeclaredBy() + ". " + (note == null ? "" : note));
        return e;
    }

    /**
     * FR-7.2 / FR-7.4: forced multi-channel broadcast. Only callable while an emergency is active.
     * Overrides every resident's channel preference and pushes SMS + WhatsApp + Email to all.
     */
    @Transactional
    public Notification broadcast(EmergencyEvent e, String title, String titleZh,
                                  String contentEn, String contentZh,
                                  Severity severity, List<User> residents) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setTitleZh(titleZh);
        n.setContentEn(contentEn);
        n.setContentZh(contentZh);
        n.setCategory(NoticeCategory.EMERGENCY);
        n.setSeverity(severity);
        n.setCreatedByName(currentUserService.displayName());
        n.setCreatedAt(LocalDateTime.now());
        n.setRecipientScope("ALL residents (preferences overridden)");
        n.setEmergencyEvent(e);
        n = notificationRepository.save(n);

        int count = dispatchService.dispatch(n, residents, true);
        DeliveryStats stats = dispatchService.statsFor(n);
        auditService.logEmergency("EMERGENCY_BROADCAST",
                "\"" + title + "\" forced to " + count + " residents; "
                        + stats.delivered + "/" + stats.attempts + " delivered across "
                        + stats.perChannel.size() + " channels.");
        return n;
    }

    /** FR-7.3: voice-call escalation for residents not yet reached. */
    @Transactional
    public int escalate(Notification n) {
        int placed = dispatchService.runVoiceEscalation(n);
        auditService.logEmergency("EMERGENCY_VOICE_ESCALATION",
                placed + " voice call(s) placed for residents undelivered on all other channels (\"" + n.getTitle() + "\").");
        return placed;
    }

    /** FR-7.6: admin closes emergency mode; routing returns to normal. */
    @Transactional
    public void close(EmergencyEvent e) {
        e.setActive(false);
        e.setClosedAt(LocalDateTime.now());
        emergencyRepository.save(e);
        auditService.logEmergency("EMERGENCY_CLOSE",
                e.getType().getLabel() + " closed by " + currentUserService.displayName() + ".");
    }
}
