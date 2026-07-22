package com.smartestate.service;

import com.smartestate.model.*;
import com.smartestate.repo.NotificationRepository;
import com.smartestate.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Automated fee reminders with overdue escalation (FR-6.3). */
@Service
public class FeeReminderService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDispatchService dispatchService;
    private final AuditService auditService;

    public FeeReminderService(UserRepository userRepository,
                              NotificationRepository notificationRepository,
                              NotificationDispatchService dispatchService,
                              AuditService auditService) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.dispatchService = dispatchService;
        this.auditService = auditService;
    }

    public static class Result {
        public int dueCount;
        public int overdueCount;
        public int getDueCount() { return dueCount; }
        public int getOverdueCount() { return overdueCount; }
        public int getTotal() { return dueCount + overdueCount; }
    }

    /** Runs monthly via the scheduler, or on demand from the management fees page. */
    @Transactional
    public Result run(String trigger) {
        List<User> due = new ArrayList<>();
        List<User> overdue = new ArrayList<>();
        for (User u : userRepository.findByRole(Role.RESIDENT)) {
            if (u.getUnit() == null) continue;
            FeeStatus fs = u.getUnit().getFeeStatus();
            if (fs == FeeStatus.OVERDUE) overdue.add(u);
            else if (fs == FeeStatus.DUE) due.add(u);
        }

        Result r = new Result();
        if (!due.isEmpty()) {
            Notification n = build(
                    "Monthly Management Fee Reminder",
                    "每月管理費提醒",
                    "This is a reminder that your management fee for this month is now due. "
                            + "Please settle the outstanding balance before the due date to avoid a late charge.",
                    "謹此提醒，本月管理費現已到期，請於到期日前繳付，以免產生逾期費用。",
                    Severity.INFO);
            r.dueCount = dispatchService.dispatch(n, due, false);
        }
        if (!overdue.isEmpty()) {
            Notification n = build(
                    "OVERDUE Management Fee — Final Notice",
                    "管理費逾期 — 最後通知",
                    "Our records show your management fee is OVERDUE. This is an escalated final notice. "
                            + "Please settle the outstanding balance immediately. Continued non-payment may be "
                            + "referred to the Owners' Corporation.",
                    "本紀錄顯示你的管理費已逾期。此為升級的最後通知。請立即繳付欠款，逾期未付或會轉介業主立案法團跟進。",
                    Severity.WARNING);
            r.overdueCount = dispatchService.dispatch(n, overdue, false);
        }

        auditService.log("FEE_REMINDER_RUN",
                trigger + ": due=" + r.dueCount + ", overdue(escalated)=" + r.overdueCount);
        return r;
    }

    /** Send a single reminder to the resident of one unit (from the per-unit fee page). */
    @Transactional
    public Result runForUnit(Long unitId, boolean escalate) {
        Result r = new Result();
        User resident = userRepository.findByRole(Role.RESIDENT).stream()
                .filter(u -> u.getUnit() != null && u.getUnit().getId().equals(unitId))
                .findFirst().orElse(null);
        if (resident == null) return r;

        Notification n = escalate
                ? build("OVERDUE Management Fee — Final Notice",
                    "管理費逾期 — 最後通知",
                    "Our records show your management fee is OVERDUE. This is an escalated final notice. "
                            + "Please settle the outstanding balance immediately.",
                    "本紀錄顯示你的管理費已逾期。此為升級的最後通知，請立即繳付欠款。",
                    Severity.WARNING)
                : build("Management Fee Reminder",
                    "管理費提醒",
                    "This is a reminder that your management fee is outstanding. "
                            + "Please settle the balance before the due date to avoid a late charge.",
                    "謹此提醒，你的管理費尚未繳付，請於到期日前繳付，以免產生逾期費用。",
                    Severity.INFO);
        int c = dispatchService.dispatch(n, List.of(resident), false);
        if (escalate) r.overdueCount = c; else r.dueCount = c;
        auditService.log("FEE_REMINDER_UNIT",
                "unit=" + unitId + " escalate=" + escalate + " sent=" + c);
        return r;
    }

    private Notification build(String title, String titleZh, String en, String zh, Severity severity) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setTitleZh(titleZh);
        n.setContentEn(en);
        n.setContentZh(zh);
        n.setCategory(NoticeCategory.FEE_REMINDER);
        n.setSeverity(severity);
        n.setCreatedByName("System (automated)");
        n.setCreatedAt(LocalDateTime.now());
        n.setRecipientScope("Residents with outstanding fees");
        return notificationRepository.save(n);
    }
}
