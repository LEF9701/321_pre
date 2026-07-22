package com.smartestate.web.api;

import com.smartestate.model.DeliveryLog;
import com.smartestate.model.Language;
import com.smartestate.model.Notification;
import com.smartestate.model.User;
import com.smartestate.repo.DeliveryLogRepository;
import com.smartestate.repo.NotificationRepository;
import com.smartestate.service.CurrentUserService;
import com.smartestate.service.NotificationDispatchService;
import com.smartestate.web.api.dto.NotificationDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The resident's mobile inbox: the notifications delivered to them (one entry per notification,
 * across whatever channels it went out on) and a read-receipt endpoint the app calls when the
 * resident opens one. Read receipts reuse {@link NotificationDispatchService#markRead}, which the
 * existing analytics already understand.
 */
@RestController
@RequestMapping("/api/mobile/notifications")
public class MobileNotificationController {

    private final DeliveryLogRepository deliveryLogRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDispatchService dispatchService;
    private final CurrentUserService currentUserService;

    public MobileNotificationController(DeliveryLogRepository deliveryLogRepository,
                                        NotificationRepository notificationRepository,
                                        NotificationDispatchService dispatchService,
                                        CurrentUserService currentUserService) {
        this.deliveryLogRepository = deliveryLogRepository;
        this.notificationRepository = notificationRepository;
        this.dispatchService = dispatchService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> list() {
        User user = currentUserService.get();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boolean zh = user.getLanguage() == Language.ZH;

        // Logs come back newest-first; collapse the per-channel rows to one entry per notification.
        List<DeliveryLog> logs = deliveryLogRepository.findByRecipientOrderByIdDesc(user);
        Map<Long, List<DeliveryLog>> byNotification = new LinkedHashMap<>();
        for (DeliveryLog l : logs) {
            if (l.getNotification() == null) continue;
            byNotification.computeIfAbsent(l.getNotification().getId(), k -> new ArrayList<>()).add(l);
        }

        List<NotificationDto> out = new ArrayList<>();
        for (List<DeliveryLog> group : byNotification.values()) {
            // Prefer a read log so "read" reflects any channel; else the most recent attempt.
            DeliveryLog rep = group.stream().filter(l -> l.getReadAt() != null).findFirst().orElse(group.get(0));
            out.add(NotificationDto.of(rep.getNotification(), rep, zh));
        }
        return ResponseEntity.ok(out);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        User user = currentUserService.get();
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Notification n = notificationRepository.findById(id).orElse(null);
        if (n == null) return ResponseEntity.notFound().build();
        dispatchService.markRead(n, user);
        return ResponseEntity.noContent().build();
    }
}
