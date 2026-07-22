package com.smartestate.web;

import com.smartestate.model.*;
import com.smartestate.repo.*;
import com.smartestate.service.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/mgmt")
public class ManagementController {

    private final UserRepository userRepo;
    private final UnitRepository unitRepo;
    private final BuildingRepository buildingRepo;
    private final BlockRepository blockRepo;
    private final IssueRepository issueRepo;
    private final NotificationRepository notificationRepo;
    private final DeliveryLogRepository deliveryLogRepo;
    private final MessageTemplateRepository templateRepo;
    private final NotificationPreferenceRepository prefRepo;
    private final IssueService issueService;
    private final NotificationService notificationService;
    private final NotificationDispatchService dispatchService;
    private final FeeReminderService feeReminderService;
    private final AuditService auditService;
    private final PasswordEncoder encoder;

    public ManagementController(UserRepository userRepo, UnitRepository unitRepo, BuildingRepository buildingRepo,
                                BlockRepository blockRepo, IssueRepository issueRepo, NotificationRepository notificationRepo,
                                DeliveryLogRepository deliveryLogRepo, MessageTemplateRepository templateRepo,
                                NotificationPreferenceRepository prefRepo, IssueService issueService,
                                NotificationService notificationService, NotificationDispatchService dispatchService,
                                FeeReminderService feeReminderService, AuditService auditService, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.unitRepo = unitRepo;
        this.buildingRepo = buildingRepo;
        this.blockRepo = blockRepo;
        this.issueRepo = issueRepo;
        this.notificationRepo = notificationRepo;
        this.deliveryLogRepo = deliveryLogRepo;
        this.templateRepo = templateRepo;
        this.prefRepo = prefRepo;
        this.issueService = issueService;
        this.notificationService = notificationService;
        this.dispatchService = dispatchService;
        this.feeReminderService = feeReminderService;
        this.auditService = auditService;
        this.encoder = encoder;
    }

    @GetMapping
    public String dashboard(Model model) {
        List<Issue> issues = issueRepo.findAllByOrderByCreatedAtDesc();
        long openIssues = issues.stream().filter(i -> i.getStatus() != IssueStatus.RESOLVED).count();
        long residents = userRepo.countByRole(Role.RESIDENT);
        long overdueUnits = unitRepo.findAll().stream().filter(u -> u.getFeeStatus() == FeeStatus.OVERDUE).count();
        model.addAttribute("residentCount", residents);
        model.addAttribute("openIssues", openIssues);
        model.addAttribute("overdueUnits", overdueUnits);
        model.addAttribute("recentIssues", issues.stream().limit(5).toList());
        model.addAttribute("recentNotifications", notificationService.history().stream().limit(5).toList());
        return "mgmt/dashboard";
    }

    // ---- Resident database (FR-2.1) --------------------------------------

    @GetMapping("/residents")
    public String residents(@RequestParam(required = false) String q,
                            @RequestParam(required = false) Long block, Model model) {
        List<User> all = userRepo.findByRoleOrderByNameAsc(Role.RESIDENT);
        String query = q == null ? "" : q.trim().toLowerCase();
        List<User> filtered = all.stream().filter(u -> {
            boolean matchesText = query.isEmpty()
                    || (u.getName() != null && u.getName().toLowerCase().contains(query))
                    || (u.getEmail() != null && u.getEmail().toLowerCase().contains(query))
                    || (u.getPhone() != null && u.getPhone().toLowerCase().contains(query));
            boolean matchesBlock = block == null || (u.getUnit() != null && u.getUnit().getFloor() != null
                    && u.getUnit().getFloor().getBlock() != null
                    && block.equals(u.getUnit().getFloor().getBlock().getId()));
            return matchesText && matchesBlock;
        }).toList();
        model.addAttribute("residents", filtered);
        model.addAttribute("q", q);
        model.addAttribute("blockFilter", block);
        model.addAttribute("blocks", blockRepo.findAll());
        return "mgmt/residents";
    }

    @GetMapping("/residents/new")
    public String newResident(Model model) {
        model.addAttribute("resident", new User());
        model.addAttribute("units", unitRepo.findAll());
        model.addAttribute("languages", Language.values());
        model.addAttribute("isNew", true);
        return "mgmt/resident_form";
    }

    @PostMapping("/residents")
    public String createResident(@RequestParam String name, @RequestParam String email,
                                 @RequestParam(required = false) String phone, @RequestParam Language language,
                                 @RequestParam(required = false) Long unitId, RedirectAttributes ra) {
        if (userRepo.findByEmail(email).isPresent()) {
            Flash.error(ra, "resident.emailExists", "A user with email " + email + " already exists.", email);
            return "redirect:/mgmt/residents/new";
        }
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPhone(phone);
        u.setLanguage(language);
        u.setRole(Role.RESIDENT);
        u.setEnabled(true);
        u.setPasswordHash(encoder.encode("resident123"));
        assignUnit(u, unitId);
        u = userRepo.save(u);
        createDefaultPreferences(u);
        Flash.msg(ra, "resident.created", "Resident " + name + " created (default password: resident123).", name);
        return "redirect:/mgmt/residents";
    }

    @GetMapping("/residents/{id}/edit")
    public String editResident(@PathVariable Long id, Model model) {
        User u = userRepo.findById(id).orElseThrow();
        model.addAttribute("resident", u);
        model.addAttribute("units", unitRepo.findAll());
        model.addAttribute("languages", Language.values());
        model.addAttribute("isNew", false);
        return "mgmt/resident_form";
    }

    @PostMapping("/residents/{id}")
    public String updateResident(@PathVariable Long id, @RequestParam String name, @RequestParam String email,
                                 @RequestParam(required = false) String phone, @RequestParam Language language,
                                 @RequestParam(required = false) Long unitId, RedirectAttributes ra) {
        User u = userRepo.findById(id).orElseThrow();
        u.setName(name);
        u.setEmail(email);
        u.setPhone(phone);
        u.setLanguage(language);
        assignUnit(u, unitId);
        userRepo.save(u);
        Flash.msg(ra, "resident.updated", "Resident " + name + " updated.", name);
        return "redirect:/mgmt/residents";
    }

    @PostMapping("/residents/{id}/delete")
    public String deleteResident(@PathVariable Long id, RedirectAttributes ra) {
        User u = userRepo.findById(id).orElse(null);
        if (u != null && u.getRole() == Role.RESIDENT) {
            prefRepo.findByUserOrderByCategoryAsc(u).forEach(prefRepo::delete);
            userRepo.delete(u);
            Flash.msg(ra, "resident.removed", "Resident removed.");
        }
        return "redirect:/mgmt/residents";
    }

    // ---- Notifications: compose / dispatch / analytics (FR-1, FR-2.2, FR-2.3) ----

    @GetMapping("/notifications")
    public String notifications(Model model) {
        List<NotificationStatsRow> rows = new ArrayList<>();
        for (Notification n : notificationService.history()) {
            rows.add(new NotificationStatsRow(n, dispatchService.statsFor(n)));
        }
        model.addAttribute("rows", rows);
        return "mgmt/notifications";
    }

    @GetMapping("/notifications/new")
    public String composeForm(@RequestParam(required = false) String template, Model model) {
        model.addAttribute("templates", templateRepo.findByEmergencyOrderByNameAsc(false));
        model.addAttribute("categories", nonEmergencyCategories());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("buildings", buildingRepo.findAllByOrderByNameAsc());
        model.addAttribute("blocks", blockRepo.findAll());
        // prefill from a chosen template
        if (template != null) {
            templateRepo.findByCode(template).ifPresent(t -> {
                model.addAttribute("preTitle", t.getTitleEn());
                model.addAttribute("preTitleZh", t.getTitleZh());
                model.addAttribute("preContentEn", t.getContentEn());
                model.addAttribute("preContentZh", t.getContentZh());
                model.addAttribute("preCategory", t.getCategory());
                model.addAttribute("preSeverity", t.getSeverity());
                model.addAttribute("preTemplate", t.getCode());
            });
        }
        return "mgmt/notification_new";
    }

    @PostMapping("/notifications")
    public String send(@RequestParam String title, @RequestParam(required = false) String titleZh,
                       @RequestParam String contentEn,
                       @RequestParam(required = false) String contentZh, @RequestParam NoticeCategory category,
                       @RequestParam Severity severity, @RequestParam String scopeType,
                       @RequestParam(required = false) Long scopeId, @RequestParam String when,
                       @RequestParam(required = false) String scheduledAt, RedirectAttributes ra) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setTitleZh(titleZh);
        n.setContentEn(contentEn);
        n.setContentZh(contentZh);
        n.setCategory(category);
        n.setSeverity(severity);
        n.setCreatedByName(loggedInName());
        n.setCreatedAt(LocalDateTime.now());
        n.setScopeType(scopeType);
        n.setScopeId("ALL".equals(scopeType) ? null : scopeId);
        n.setRecipientScope(scopeDescription(scopeType, scopeId));

        if ("later".equals(when) && scheduledAt != null && !scheduledAt.isBlank()) {
            n.setScheduledAt(LocalDateTime.parse(scheduledAt));
            notificationService.schedule(n);
            Flash.msg(ra, "notice.scheduled", "Notification scheduled for " + n.getScheduledAt() + ".",
                    n.getScheduledAt());
            return "redirect:/mgmt/notifications";
        }
        int count = notificationService.sendNow(n);
        Flash.msg(ra, "notice.dispatched", "Notification dispatched to " + count + " resident(s).", count);
        return "redirect:/mgmt/notifications/" + n.getId();
    }

    @GetMapping("/notifications/{id}")
    public String notificationDetail(@PathVariable Long id, Model model) {
        Notification n = notificationRepo.findById(id).orElseThrow();
        model.addAttribute("n", n);
        model.addAttribute("logs", dispatchService.logsFor(n));
        model.addAttribute("stats", dispatchService.statsFor(n));
        return "mgmt/notification_detail";
    }

    /** FR-2.3: edit a notice after it was created/published. */
    @GetMapping("/notifications/{id}/edit")
    public String editNotificationForm(@PathVariable Long id, Model model) {
        model.addAttribute("n", notificationRepo.findById(id).orElseThrow());
        model.addAttribute("categories", NoticeCategory.values());
        model.addAttribute("severities", Severity.values());
        return "mgmt/notification_edit";
    }

    @PostMapping("/notifications/{id}")
    public String updateNotification(@PathVariable Long id, @RequestParam String title,
                                     @RequestParam(required = false) String titleZh,
                                     @RequestParam String contentEn, @RequestParam(required = false) String contentZh,
                                     @RequestParam NoticeCategory category, @RequestParam Severity severity,
                                     RedirectAttributes ra) {
        Notification n = notificationRepo.findById(id).orElseThrow();
        n.setTitle(title);
        n.setTitleZh(titleZh);
        n.setContentEn(contentEn);
        n.setContentZh(contentZh);
        n.setCategory(category);
        n.setSeverity(severity);
        notificationRepo.save(n);
        auditService.log("NOTIFY_EDIT", "\"" + title + "\" edited");
        if (n.isDispatched()) {
            Flash.msg(ra, "notice.updatedDispatched",
                    "Notice updated. (It was already dispatched, so recipients keep the version they received.)");
        } else {
            Flash.msg(ra, "notice.updatedScheduled",
                    "Notice updated. The scheduled send will use the new content.");
        }
        return "redirect:/mgmt/notifications/" + id;
    }

    @GetMapping("/analytics")
    public String analytics(Model model) {
        DeliveryStats overall = DeliveryStats.of(deliveryLogRepo.findAll());
        List<NotificationStatsRow> rows = new ArrayList<>();
        for (Notification n : notificationService.history()) {
            if (n.isDispatched()) rows.add(new NotificationStatsRow(n, dispatchService.statsFor(n)));
        }
        model.addAttribute("overall", overall);
        model.addAttribute("rows", rows);
        return "mgmt/analytics";
    }

    // ---- Issue / maintenance tracking (FR-2.4, FR-5.x) -------------------

    @GetMapping("/issues")
    public String issues(@RequestParam(required = false) IssueStatus status, Model model) {
        List<Issue> issues = status == null
                ? issueRepo.findAllByOrderByCreatedAtDesc()
                : issueRepo.findByStatusOrderByCreatedAtDesc(status);
        model.addAttribute("issues", issues);
        model.addAttribute("statuses", IssueStatus.values());
        model.addAttribute("statusFilter", status);
        return "mgmt/issues";
    }

    @GetMapping("/issues/{id}")
    public String issueDetail(@PathVariable Long id, Model model) {
        Issue issue = issueRepo.findById(id).orElseThrow();
        model.addAttribute("issue", issue);
        model.addAttribute("comments", issueService.comments(issue));
        model.addAttribute("statuses", IssueStatus.values());
        model.addAttribute("priorities", IssuePriority.values());
        return "mgmt/issue_detail";
    }

    /** Management replies on the issue thread; the resident is auto-notified. */
    @PostMapping("/issues/{id}/comment")
    public String commentIssue(@PathVariable Long id, @RequestParam String body, RedirectAttributes ra) {
        Issue issue = issueRepo.findById(id).orElseThrow();
        if (body != null && !body.isBlank()) {
            issueService.addComment(issue, loggedInUser(), body.trim());
            Flash.msg(ra, "issue.replyPosted", "Reply posted. The resident has been notified.");
        }
        return "redirect:/mgmt/issues/" + id;
    }

    @PostMapping("/issues/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam IssueStatus status,
                               @RequestParam(required = false) String note, RedirectAttributes ra) {
        issueService.updateStatus(issueRepo.findById(id).orElseThrow(), status, note);
        Flash.msg(ra, "issue.statusUpdated",
                "Status updated to " + status.getLabel() + ". Resident has been notified.", status.name());
        return "redirect:/mgmt/issues/" + id;
    }

    @PostMapping("/issues/{id}/priority")
    public String updatePriority(@PathVariable Long id, @RequestParam IssuePriority priority, RedirectAttributes ra) {
        issueService.updatePriority(issueRepo.findById(id).orElseThrow(), priority);
        Flash.msg(ra, "issue.prioritySet", "Priority set to " + priority.getLabel() + ".", priority.name());
        return "redirect:/mgmt/issues/" + id;
    }

    @PostMapping("/issues/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam String assignee, RedirectAttributes ra) {
        issueService.assign(issueRepo.findById(id).orElseThrow(), assignee);
        Flash.msg(ra, "issue.assigned", "Issue assigned to " + assignee + ".", assignee);
        return "redirect:/mgmt/issues/" + id;
    }

    // ---- Financial reporting (FR-2.5) ------------------------------------

    @GetMapping("/fees")
    public String fees(Model model) {
        List<Unit> units = unitRepo.findAll();
        Map<Long, String> occupant = new HashMap<>();
        for (User u : userRepo.findByRole(Role.RESIDENT)) {
            if (u.getUnit() != null) occupant.put(u.getUnit().getId(), u.getName());
        }
        List<Unit> occupied = units.stream().filter(u -> u.getStatus() == UnitStatus.OCCUPIED).toList();
        long paid = occupied.stream().filter(u -> u.getFeeStatus() == FeeStatus.PAID).count();
        long due = occupied.stream().filter(u -> u.getFeeStatus() == FeeStatus.DUE).count();
        long overdue = occupied.stream().filter(u -> u.getFeeStatus() == FeeStatus.OVERDUE).count();
        model.addAttribute("rows", occupied.stream()
                .filter(u -> u.getFeeStatus() != FeeStatus.PAID)
                .sorted(Comparator.comparing(Unit::getFeeStatus).reversed())
                .toList());
        model.addAttribute("occupant", occupant);
        model.addAttribute("paid", paid);
        model.addAttribute("due", due);
        model.addAttribute("overdue", overdue);
        model.addAttribute("totalOccupied", occupied.size());
        model.addAttribute("collectionRate", occupied.isEmpty() ? 0 : Math.round(paid * 100f / occupied.size()));
        return "mgmt/fees";
    }

    @PostMapping("/fees/run-reminders")
    public String runReminders(RedirectAttributes ra) {
        FeeReminderService.Result r = feeReminderService.run("manual-management");
        Flash.msg(ra, "fees.remindersDispatched", "Fee reminders dispatched: " + r.getDueCount()
                + " due, " + r.getOverdueCount() + " overdue (escalated).", r.getDueCount(), r.getOverdueCount());
        return "redirect:/mgmt/fees";
    }

    /** Per-unit fee detail: define/edit the fee and send that single unit a reminder. */
    @GetMapping("/fees/units/{id}")
    public String feeDetail(@PathVariable Long id, Model model) {
        Unit unit = unitRepo.findById(id).orElseThrow();
        User resident = residentOfUnit(id);
        model.addAttribute("unit", unit);
        model.addAttribute("occupant", resident);
        model.addAttribute("feeStatuses", FeeStatus.values());
        model.addAttribute("reminders", feeReminderHistory(resident));
        return "mgmt/fee_detail";
    }

    @PostMapping("/fees/units/{id}")
    public String defineFee(@PathVariable Long id, @RequestParam FeeStatus feeStatus,
                            @RequestParam(required = false) BigDecimal balance,
                            @RequestParam(required = false) String dueDate, RedirectAttributes ra) {
        Unit unit = unitRepo.findById(id).orElseThrow();
        unit.setFeeStatus(feeStatus);
        unit.setBalance(balance == null ? BigDecimal.ZERO : balance);
        unit.setDueDate(dueDate == null || dueDate.isBlank() ? null : LocalDate.parse(dueDate));
        unitRepo.save(unit);
        auditService.log("FEE_DEFINE", "Set fee for " + unit.getFullCode()
                + " -> " + feeStatus.getLabel() + " (HK$ " + unit.getBalance() + ")");
        Flash.msg(ra, "fees.updatedForUnit", "Fee updated for " + unit.getFullCode() + ".", unit.getFullCode());
        return "redirect:/mgmt/fees/units/" + id;
    }

    @PostMapping("/fees/units/{id}/remind")
    public String remindUnit(@PathVariable Long id,
                             @RequestParam(required = false, defaultValue = "false") boolean escalate,
                             RedirectAttributes ra) {
        Unit unit = unitRepo.findById(id).orElseThrow();
        User resident = residentOfUnit(id);
        if (resident == null) {
            Flash.error(ra, "fees.noResidentForUnit",
                    "No resident is assigned to this unit, so no reminder could be sent.");
            return "redirect:/mgmt/fees/units/" + id;
        }
        feeReminderService.runForUnit(id, escalate);
        if (escalate) {
            Flash.msg(ra, "fees.reminderSentFinal",
                    "Final (overdue) notice sent to " + resident.getName() + " for " + unit.getFullCode() + ".",
                    resident.getName(), unit.getFullCode());
        } else {
            Flash.msg(ra, "fees.reminderSent",
                    "Reminder sent to " + resident.getName() + " for " + unit.getFullCode() + ".",
                    resident.getName(), unit.getFullCode());
        }
        return "redirect:/mgmt/fees/units/" + id;
    }

    // ---- helpers ----------------------------------------------------------

    private void assignUnit(User u, Long unitId) {
        if (unitId == null) { u.setUnit(null); return; }
        Unit unit = unitRepo.findById(unitId).orElse(null);
        u.setUnit(unit);
        if (unit != null && unit.getStatus() == UnitStatus.VACANT) {
            unit.setStatus(UnitStatus.OCCUPIED);
            unitRepo.save(unit);
        }
    }

    private void createDefaultPreferences(User u) {
        for (NoticeCategory cat : NoticeCategory.values()) {
            if (cat.isForcedAllChannels()) continue;
            NotificationPreference p = new NotificationPreference();
            p.setUser(u);
            p.setCategory(cat);
            p.setChannel(Channel.EMAIL);
            p.setLanguage(u.getLanguage());
            prefRepo.save(p);
        }
    }

    private List<NoticeCategory> nonEmergencyCategories() {
        List<NoticeCategory> list = new ArrayList<>();
        for (NoticeCategory c : NoticeCategory.values()) if (!c.isForcedAllChannels()) list.add(c);
        return list;
    }

    private String scopeDescription(String scopeType, Long scopeId) {
        return switch (scopeType) {
            case "BUILDING" -> scopeId == null ? "All residents" :
                    "Building: " + buildingRepo.findById(scopeId).map(Building::getName).orElse("?");
            case "BLOCK" -> scopeId == null ? "All residents" :
                    "Block: " + blockRepo.findById(scopeId).map(Block::getBlockName).orElse("?");
            default -> "All residents";
        };
    }

    private String loggedInName() {
        User u = loggedInUser();
        return u == null ? "Management" : u.getName();
    }

    private User loggedInUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : userRepo.findByEmail(auth.getName()).orElse(null);
    }

    private User residentOfUnit(Long unitId) {
        return userRepo.findByRole(Role.RESIDENT).stream()
                .filter(u -> u.getUnit() != null && u.getUnit().getId().equals(unitId))
                .findFirst().orElse(null);
    }

    /** FEE_REMINDER notices delivered to this resident, newest first. */
    private List<FeeReminderRow> feeReminderHistory(User resident) {
        if (resident == null) return new ArrayList<>();
        Map<Long, FeeReminderRow> byNotif = new LinkedHashMap<>();
        for (DeliveryLog l : deliveryLogRepo.findByRecipientOrderByIdDesc(resident)) {
            Notification n = l.getNotification();
            if (n.getCategory() != NoticeCategory.FEE_REMINDER) continue;
            byNotif.computeIfAbsent(n.getId(), k -> new FeeReminderRow(n)).addLog(l);
        }
        return new ArrayList<>(byNotif.values());
    }

    /** One row of fee-reminder history for a single unit's resident. */
    public static class FeeReminderRow {
        private final Notification notification;
        private final List<DeliveryLog> logs = new ArrayList<>();
        public FeeReminderRow(Notification n) { this.notification = n; }
        public void addLog(DeliveryLog l) { logs.add(l); }
        public Notification getNotification() { return notification; }
        public String getChannels() {
            return logs.stream().map(l -> l.getChannel().getLabel()).distinct().collect(Collectors.joining(", "));
        }
        public DeliveryStatus getStatus() {
            DeliveryStatus[] order = {DeliveryStatus.READ, DeliveryStatus.DELIVERED,
                    DeliveryStatus.SENT, DeliveryStatus.PENDING, DeliveryStatus.FAILED};
            for (DeliveryStatus s : order) for (DeliveryLog l : logs) if (l.getStatus() == s) return s;
            return DeliveryStatus.PENDING;
        }
    }

    /** Row for the notification history / analytics tables. */
    public static class NotificationStatsRow {
        private final Notification notification;
        private final DeliveryStats stats;
        public NotificationStatsRow(Notification n, DeliveryStats s) { this.notification = n; this.stats = s; }
        public Notification getNotification() { return notification; }
        public DeliveryStats getStats() { return stats; }
    }
}
