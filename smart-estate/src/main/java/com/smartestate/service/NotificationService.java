package com.smartestate.service;

import com.smartestate.model.Notification;
import com.smartestate.model.User;
import com.smartestate.repo.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Management-facing compose/dispatch/schedule (FR-2.3, FR-1.1, FR-1.4). */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RecipientService recipientService;
    private final NotificationDispatchService dispatchService;
    private final AuditService auditService;

    public NotificationService(NotificationRepository notificationRepository,
                               RecipientService recipientService,
                               NotificationDispatchService dispatchService,
                               AuditService auditService) {
        this.notificationRepository = notificationRepository;
        this.recipientService = recipientService;
        this.dispatchService = dispatchService;
        this.auditService = auditService;
    }

    /** Send immediately to the resolved recipient scope. Returns number of residents reached. */
    @Transactional
    public int sendNow(Notification n) {
        if (n.getCreatedAt() == null) n.setCreatedAt(LocalDateTime.now());
        n = notificationRepository.save(n);
        List<User> recipients = recipientService.resolve(n.getScopeType(), n.getScopeId());
        boolean forced = n.getCategory() != null && n.getCategory().isForcedAllChannels();
        int count = dispatchService.dispatch(n, recipients, forced);
        DeliveryStats stats = dispatchService.statsFor(n);
        auditService.log("NOTIFY_SEND",
                "\"" + n.getTitle() + "\" [" + n.getCategory() + "] to " + count + " residents; "
                        + stats.delivered + "/" + stats.attempts + " delivered.");
        return count;
    }

    /** Queue for later automatic dispatch by the scheduler (FR-1.4). */
    @Transactional
    public void schedule(Notification n) {
        if (n.getCreatedAt() == null) n.setCreatedAt(LocalDateTime.now());
        n.setDispatched(false);
        notificationRepository.save(n);
        auditService.log("NOTIFY_SCHEDULE",
                "\"" + n.getTitle() + "\" scheduled for " + n.getScheduledAt());
    }

    public List<Notification> history() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }
}
