package com.smartestate.web;

import com.smartestate.ai.IssueClassifier;
import com.smartestate.config.ChannelPolicy;
import com.smartestate.model.*;
import com.smartestate.repo.*;
import com.smartestate.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/resident")
public class ResidentController {

    private final CurrentUserService currentUser;
    private final NotificationRepository notificationRepo;
    private final DeliveryLogRepository deliveryLogRepo;
    private final NotificationPreferenceRepository prefRepo;
    private final IssueRepository issueRepo;
    private final MaintenanceScheduleRepository scheduleRepo;
    private final IssueService issueService;
    private final NotificationDispatchService dispatchService;
    private final FileStorageService fileStorage;
    private final IssueClassifier issueClassifier;
    private final ChannelPolicy channelPolicy;

    public ResidentController(CurrentUserService currentUser, NotificationRepository notificationRepo,
                              DeliveryLogRepository deliveryLogRepo, NotificationPreferenceRepository prefRepo,
                              IssueRepository issueRepo, MaintenanceScheduleRepository scheduleRepo,
                              IssueService issueService, NotificationDispatchService dispatchService,
                              FileStorageService fileStorage, IssueClassifier issueClassifier,
                              ChannelPolicy channelPolicy) {
        this.currentUser = currentUser;
        this.notificationRepo = notificationRepo;
        this.deliveryLogRepo = deliveryLogRepo;
        this.prefRepo = prefRepo;
        this.issueRepo = issueRepo;
        this.scheduleRepo = scheduleRepo;
        this.issueService = issueService;
        this.dispatchService = dispatchService;
        this.fileStorage = fileStorage;
        this.issueClassifier = issueClassifier;
        this.channelPolicy = channelPolicy;
    }

    @GetMapping
    public String dashboard(Model model) {
        User u = currentUser.get();
        List<InboxItem> inbox = inboxFor(u);
        model.addAttribute("inbox", inbox.stream().limit(5).toList());
        model.addAttribute("unreadCount", inbox.stream().filter(i -> !i.isRead()).count());
        model.addAttribute("issues", issueService.forReporter(u));
        model.addAttribute("openIssueCount",
                issueService.forReporter(u).stream()
                        .filter(i -> i.getStatus() != IssueStatus.RESOLVED && i.getStatus() != IssueStatus.WITHDRAWN)
                        .count());
        model.addAttribute("unit", u.getUnit());
        model.addAttribute("schedules", scheduleRepo.findAllByOrderByScheduledDateAsc());
        return "resident/dashboard";
    }

    @GetMapping("/inbox")
    public String inbox(Model model) {
        model.addAttribute("inbox", inboxFor(currentUser.get()));
        return "resident/inbox";
    }

    @GetMapping("/inbox/{id}")
    public String inboxItem(@PathVariable Long id, Model model) {
        User u = currentUser.get();
        Notification n = notificationRepo.findById(id).orElse(null);
        if (n == null) return "redirect:/resident/inbox";
        List<DeliveryLog> mine = deliveryLogRepo.findByNotification_IdAndRecipient_Id(id, u.getId());
        if (mine.isEmpty()) return "redirect:/resident/inbox"; // not addressed to this resident
        dispatchService.markRead(n, u);                         // FR-1.3 read receipt
        model.addAttribute("n", n);
        model.addAttribute("logs", deliveryLogRepo.findByNotification_IdAndRecipient_Id(id, u.getId()));
        return "resident/notification";
    }

    @GetMapping("/preferences")
    public String preferences(Model model) {
        User u = currentUser.get();
        ensurePreferences(u); // self-heal: a resident should always have a row per non-emergency category
        model.addAttribute("prefs", prefRepo.findByUserOrderByCategoryAsc(u));
        model.addAttribute("channels", Channel.values());
        model.addAttribute("languages", Language.values());
        model.addAttribute("enabledChannels", channelPolicy.enabledNames());
        model.addAttribute("defaultChannel", channelPolicy.defaultChannel().name());
        return "resident/preferences";
    }

    @PostMapping("/preferences")
    public String savePreferences(@RequestParam Map<String, String> params, RedirectAttributes ra) {
        User u = currentUser.get();
        for (NotificationPreference p : prefRepo.findByUserOrderByCategoryAsc(u)) {
            String ch = params.get("channel_" + p.getId());
            String lang = params.get("language_" + p.getId());
            if (ch != null) {
                Channel c = Channel.valueOf(ch);
                p.setChannel(channelPolicy.isEnabled(c) ? c : channelPolicy.defaultChannel()); // reject disabled channels
            }
            if (lang != null) p.setLanguage(Language.valueOf(lang));
            prefRepo.save(p);
        }
        Flash.msg(ra, "prefs.saved", "Your communication preferences have been saved.");
        return "redirect:/resident/preferences";
    }

    @GetMapping("/issues")
    public String issues(Model model) {
        model.addAttribute("issues", issueService.forReporter(currentUser.get()));
        return "resident/issues";
    }

    @GetMapping("/issues/new")
    public String newIssue(Model model) {
        model.addAttribute("categories", IssueCategory.values());
        return "resident/issue_new";
    }

    /** FR-8.3: classify the typed description and pre-select a category (resident can override). */
    @PostMapping("/issues/suggest")
    public String suggest(@RequestParam(required = false) String description, Model model) {
        model.addAttribute("categories", IssueCategory.values());
        model.addAttribute("description", description);
        if (description != null && !description.isBlank()) {
            model.addAttribute("suggestion", issueClassifier.classify(description));
        }
        return "resident/issue_new";
    }

    @PostMapping("/issues")
    public String createIssue(@RequestParam IssueCategory category,
                              @RequestParam String description,
                              @RequestParam(required = false) MultipartFile photo,
                              RedirectAttributes ra) {
        String filename = fileStorage.store(photo);
        Issue issue = issueService.create(currentUser.get(), category, description, filename);
        Flash.msg(ra, "issue.submitted", "Issue submitted. Your tracking number is " + issue.getRefNumber() + ".",
                issue.getRefNumber());
        return "redirect:/resident/issues/" + issue.getId();
    }

    @GetMapping("/issues/{id}")
    public String issueDetail(@PathVariable Long id, Model model) {
        Issue issue = issueRepo.findById(id).orElse(null);
        User u = currentUser.get();
        if (issue == null || issue.getReporter() == null || !issue.getReporter().getId().equals(u.getId())) {
            return "redirect:/resident/issues";
        }
        model.addAttribute("issue", issue);
        model.addAttribute("comments", issueService.comments(issue));
        return "resident/issue_detail";
    }

    /** Resident posts a comment / update on their own issue (two-way thread with management). */
    @PostMapping("/issues/{id}/comment")
    public String commentIssue(@PathVariable Long id, @RequestParam String body, RedirectAttributes ra) {
        User u = currentUser.get();
        Issue issue = ownedIssue(id, u);
        if (issue == null) return "redirect:/resident/issues";
        if (body != null && !body.isBlank()) {
            issueService.addComment(issue, u, body.trim());
            Flash.msg(ra, "issue.commentAdded", "Your comment was added.");
        }
        return "redirect:/resident/issues/" + id;
    }

    /** Resident withdraws their own issue while it is still just submitted. */
    @PostMapping("/issues/{id}/withdraw")
    public String withdrawIssue(@PathVariable Long id, RedirectAttributes ra) {
        User u = currentUser.get();
        Issue issue = ownedIssue(id, u);
        if (issue == null) return "redirect:/resident/issues";
        if (issue.getStatus() == IssueStatus.SUBMITTED) {
            issueService.withdraw(issue);
            Flash.msg(ra, "issue.withdrawn", "Issue " + issue.getRefNumber() + " was withdrawn.",
                    issue.getRefNumber());
        } else {
            Flash.error(ra, "issue.withdrawTooLate",
                    "This issue is already being handled and can no longer be withdrawn.");
        }
        return "redirect:/resident/issues/" + id;
    }

    private Issue ownedIssue(Long id, User u) {
        Issue issue = issueRepo.findById(id).orElse(null);
        if (issue == null || issue.getReporter() == null || !issue.getReporter().getId().equals(u.getId())) {
            return null;
        }
        return issue;
    }

    @GetMapping("/fees")
    public String fees(Model model) {
        User u = currentUser.get();
        model.addAttribute("unit", u.getUnit());
        // fee-related notices act as the resident's reminder history
        List<InboxItem> feeHistory = inboxFor(u).stream()
                .filter(i -> i.getNotification().getCategory() == NoticeCategory.FEE_REMINDER)
                .collect(Collectors.toList());
        model.addAttribute("feeHistory", feeHistory);
        return "resident/fees";
    }

    @GetMapping("/maintenance")
    public String maintenance(Model model) {
        model.addAttribute("schedules", scheduleRepo.findAllByOrderByScheduledDateAsc());
        return "resident/maintenance";
    }

    /**
     * Guarantee the resident has a notification preference for every non-emergency category, so the
     * preferences table is never empty (e.g. for residents created outside the seeder). Only fills
     * in missing categories; existing choices are left untouched. Emergency is excluded because it
     * always forces every channel regardless of preference.
     */
    private void ensurePreferences(User u) {
        if (u == null) return;
        Set<NoticeCategory> existing = prefRepo.findByUserOrderByCategoryAsc(u).stream()
                .map(NotificationPreference::getCategory)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(NoticeCategory.class)));
        Channel def = channelPolicy.defaultChannel();
        Language lang = u.getLanguage() == null ? Language.EN : u.getLanguage();
        for (NoticeCategory cat : NoticeCategory.values()) {
            if (cat.isForcedAllChannels() || existing.contains(cat)) continue;
            NotificationPreference p = new NotificationPreference();
            p.setUser(u);
            p.setCategory(cat);
            p.setChannel(def);
            p.setLanguage(lang);
            prefRepo.save(p);
        }
    }

    // ---- inbox assembly ---------------------------------------------------

    private List<InboxItem> inboxFor(User u) {
        List<DeliveryLog> logs = deliveryLogRepo.findByRecipientOrderByIdDesc(u);
        Map<Long, InboxItem> byNotification = new LinkedHashMap<>();
        for (DeliveryLog l : logs) {
            Notification n = l.getNotification();
            InboxItem item = byNotification.computeIfAbsent(n.getId(), k -> new InboxItem(n));
            item.addLog(l);
        }
        List<InboxItem> items = new ArrayList<>(byNotification.values());
        items.sort(Comparator.comparing((InboxItem i) ->
                i.getNotification().getDispatchedAt() == null
                        ? i.getNotification().getCreatedAt()
                        : i.getNotification().getDispatchedAt()).reversed());
        return items;
    }

    /** One inbox row: a notification plus this resident's delivery status across channels. */
    public static class InboxItem {
        private final Notification notification;
        private final List<DeliveryLog> logs = new ArrayList<>();

        public InboxItem(Notification n) { this.notification = n; }
        public void addLog(DeliveryLog l) { logs.add(l); }

        public Notification getNotification() { return notification; }
        public List<DeliveryLog> getLogs() { return logs; }

        public boolean isRead() {
            return logs.stream().anyMatch(l -> l.getStatus() == DeliveryStatus.READ);
        }
        public DeliveryStatus getBestStatus() {
            DeliveryStatus[] order = {DeliveryStatus.READ, DeliveryStatus.DELIVERED,
                    DeliveryStatus.SENT, DeliveryStatus.PENDING, DeliveryStatus.FAILED};
            for (DeliveryStatus s : order) {
                for (DeliveryLog l : logs) if (l.getStatus() == s) return s;
            }
            return DeliveryStatus.PENDING;
        }
        public String getChannelsSummary() {
            return logs.stream().map(l -> l.getChannel().getLabel()).distinct().collect(Collectors.joining(", "));
        }
    }
}
