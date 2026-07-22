package com.smartestate;

import com.smartestate.model.*;
import com.smartestate.repo.DeliveryLogRepository;
import com.smartestate.repo.DeviceTokenRepository;
import com.smartestate.repo.NotificationRepository;
import com.smartestate.repo.UserRepository;
import com.smartestate.service.NotificationDispatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves the PUSH channel fans an emergency broadcast out to each active device token, one
 * DeliveryLog per device, using the offline mock engine (APNs disabled by default in tests).
 */
@SpringBootTest
class PushDispatchIntegrationTest {

    @Autowired NotificationDispatchService dispatchService;
    @Autowired UserRepository userRepository;
    @Autowired DeviceTokenRepository deviceTokenRepository;
    @Autowired DeliveryLogRepository deliveryLogRepository;
    @Autowired NotificationRepository notificationRepository;

    @Test
    void emergencyBroadcastCreatesOnePushLogPerActiveDevice() {
        User admin = userRepository.findByEmail("admin@smartestate.hk").orElseThrow();

        // Two active devices + one inactive: expect a push log for the two active only.
        saveToken(admin, "aaaa-" + admin.getId(), true);
        saveToken(admin, "bbbb-" + admin.getId(), true);
        saveToken(admin, "cccc-" + admin.getId(), false);

        Notification n = new Notification();
        n.setTitle("FIRE ALARM — EVACUATE NOW");
        n.setContentEn("A fire has been reported. Evacuate immediately.");
        n.setCategory(NoticeCategory.EMERGENCY);
        n.setSeverity(Severity.CRITICAL);
        notificationRepository.save(n);

        dispatchService.dispatch(n, List.of(admin), true); // forceAllChannels -> includes PUSH

        List<DeliveryLog> pushLogs = deliveryLogRepository.findByNotificationOrderByIdAsc(n).stream()
                .filter(l -> l.getChannel() == Channel.PUSH)
                .toList();

        assertEquals(2, pushLogs.size(), "one push log per ACTIVE device");
        assertTrue(pushLogs.stream().allMatch(l -> l.getStatus() != null), "each push log has a status");
        assertTrue(pushLogs.stream().map(DeliveryLog::getRecipientAddress).anyMatch(a -> a.startsWith("aaaa-")));
        assertTrue(pushLogs.stream().map(DeliveryLog::getRecipientAddress).anyMatch(a -> a.startsWith("bbbb-")));
        assertTrue(pushLogs.stream().map(DeliveryLog::getRecipientAddress).noneMatch(a -> a.startsWith("cccc-")),
                "inactive device is not pushed to");
    }

    private void saveToken(User user, String token, boolean active) {
        DeviceToken dt = new DeviceToken();
        dt.setUser(user);
        dt.setToken(token);
        dt.setPlatform(Platform.IOS);
        dt.setEnvironment(PushEnvironment.SANDBOX);
        dt.setActive(active);
        dt.setCreatedAt(LocalDateTime.now());
        dt.setLastSeenAt(LocalDateTime.now());
        deviceTokenRepository.save(dt);
    }
}
