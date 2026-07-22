package com.smartestate.web;

import com.smartestate.model.*;
import com.smartestate.repo.MessageTemplateRepository;
import com.smartestate.repo.NotificationRepository;
import com.smartestate.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/emergency")
public class EmergencyController {

    private final EmergencyService emergencyService;
    private final RecipientService recipientService;
    private final MessageTemplateRepository templateRepo;
    private final NotificationRepository notificationRepo;
    private final NotificationDispatchService dispatchService;

    public EmergencyController(EmergencyService emergencyService, RecipientService recipientService,
                              MessageTemplateRepository templateRepo, NotificationRepository notificationRepo,
                              NotificationDispatchService dispatchService) {
        this.emergencyService = emergencyService;
        this.recipientService = recipientService;
        this.templateRepo = templateRepo;
        this.notificationRepo = notificationRepo;
        this.dispatchService = dispatchService;
    }

    @GetMapping
    public String page(@RequestParam(required = false) String template, Model model) {
        EmergencyEvent active = emergencyService.active().orElse(null);
        model.addAttribute("active", active);
        model.addAttribute("types", EmergencyType.values());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("emergencyTemplates", templateRepo.findByEmergencyOrderByNameAsc(true));
        model.addAttribute("residentCount", recipientService.all().size());

        if (template != null) {
            templateRepo.findByCode(template).ifPresent(t -> {
                model.addAttribute("preTitle", t.getTitleEn());
                model.addAttribute("preTitleZh", t.getTitleZh());
                model.addAttribute("preContentEn", t.getContentEn());
                model.addAttribute("preContentZh", t.getContentZh());
                model.addAttribute("preSeverity", t.getSeverity());
            });
        }

        // emergency broadcasts + their delivery stats
        List<BroadcastRow> rows = new ArrayList<>();
        for (Notification n : notificationRepo.findAllByOrderByCreatedAtDesc()) {
            if (n.getCategory() == NoticeCategory.EMERGENCY && n.isDispatched()) {
                rows.add(new BroadcastRow(n, dispatchService.statsFor(n)));
            }
        }
        model.addAttribute("broadcasts", rows);
        return "admin/emergency";
    }

    @PostMapping("/declare")
    public String declare(@RequestParam EmergencyType type, @RequestParam(required = false) String note,
                          RedirectAttributes ra) {
        emergencyService.declare(type, note);
        Flash.msg(ra, "emergency.declared", "Emergency declared: " + type.getLabel()
                + ". The system is now in elevated state — broadcast to all residents below.", type.name());
        return "redirect:/admin/emergency";
    }

    @PostMapping("/broadcast")
    public String broadcast(@RequestParam String title, @RequestParam(required = false) String titleZh,
                            @RequestParam String contentEn,
                            @RequestParam(required = false) String contentZh, @RequestParam Severity severity,
                            RedirectAttributes ra) {
        EmergencyEvent active = emergencyService.active().orElse(null);
        if (active == null) {
            Flash.error(ra, "emergency.noneActive",
                    "No active emergency. Declare an emergency before broadcasting.");
            return "redirect:/admin/emergency";
        }
        List<User> residents = recipientService.all();
        Notification n = emergencyService.broadcast(active, title, titleZh, contentEn, contentZh, severity, residents);
        DeliveryStats stats = dispatchService.statsFor(n);
        Flash.msg(ra, "emergency.broadcastSent",
                "Forced broadcast sent to " + residents.size() + " residents across SMS, WhatsApp and Email ("
                        + stats.delivered + "/" + stats.attempts + " delivered). Run voice escalation for anyone not yet reached.",
                residents.size(), stats.delivered, stats.attempts);
        return "redirect:/admin/emergency";
    }

    @PostMapping("/escalate")
    public String escalate(@RequestParam Long notificationId, RedirectAttributes ra) {
        Notification n = notificationRepo.findById(notificationId).orElse(null);
        if (n == null) {
            Flash.error(ra, "emergency.broadcastNotFound", "Broadcast not found.");
            return "redirect:/admin/emergency";
        }
        int placed = emergencyService.escalate(n);
        if (placed == 0) {
            Flash.msg(ra, "emergency.escalationNoneNeeded",
                    "Voice escalation complete: every resident had already been reached on another channel.");
        } else {
            Flash.msg(ra, "emergency.escalationPlaced",
                    "Voice escalation placed " + placed + " automated call(s) to residents not reached on any other channel.",
                    placed);
        }
        return "redirect:/admin/emergency";
    }

    @PostMapping("/close")
    public String close(RedirectAttributes ra) {
        emergencyService.active().ifPresent(emergencyService::close);
        Flash.msg(ra, "emergency.closed",
                "Emergency closed. Routing has returned to normal (per-resident preferences).");
        return "redirect:/admin/emergency";
    }

    public static class BroadcastRow {
        private final Notification notification;
        private final DeliveryStats stats;
        public BroadcastRow(Notification n, DeliveryStats s) { this.notification = n; this.stats = s; }
        public Notification getNotification() { return notification; }
        public DeliveryStats getStats() { return stats; }
    }
}
