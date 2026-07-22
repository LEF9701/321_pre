package com.smartestate.service;

import com.smartestate.model.Notification;
import com.smartestate.model.User;
import com.smartestate.repo.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Background jobs: scheduled-send poller (FR-1.4) and monthly fee reminders (FR-6.3). */
@Component
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    private final NotificationRepository notificationRepository;
    private final RecipientService recipientService;
    private final NotificationDispatchService dispatchService;
    private final FeeReminderService feeReminderService;
    private final AuditService auditService;

    public ScheduledJobs(NotificationRepository notificationRepository,
                         RecipientService recipientService,
                         NotificationDispatchService dispatchService,
                         FeeReminderService feeReminderService,
                         AuditService auditService) {
        this.notificationRepository = notificationRepository;
        this.recipientService = recipientService;
        this.dispatchService = dispatchService;
        this.feeReminderService = feeReminderService;
        this.auditService = auditService;
    }

    /** Every 20s, dispatch any scheduled notification whose time has arrived. */
    @Scheduled(fixedDelay = 20000)
    @Transactional
    public void dispatchDueScheduled() {
        List<Notification> due =
                notificationRepository.findByDispatchedFalseAndScheduledAtNotNullAndScheduledAtBefore(LocalDateTime.now());
        for (Notification n : due) {
            List<User> recipients = recipientService.resolve(n.getScopeType(), n.getScopeId());
            dispatchService.dispatch(n, recipients, false);
            auditService.log("system", "NOTIFY_SCHEDULED_SENT",
                    "\"" + n.getTitle() + "\" auto-dispatched to " + recipients.size() + " residents.", false);
            log.info("Scheduled notification '{}' dispatched to {} residents", n.getTitle(), recipients.size());
        }
    }

    /** First day of each month at 09:00: automated fee reminders with overdue escalation. */
    @Scheduled(cron = "0 0 9 1 * *")
    public void monthlyFeeReminders() {
        feeReminderService.run("scheduled-monthly");
    }
}
