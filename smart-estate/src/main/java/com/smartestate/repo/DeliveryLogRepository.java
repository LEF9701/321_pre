package com.smartestate.repo;

import com.smartestate.model.DeliveryLog;
import com.smartestate.model.Notification;
import com.smartestate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, Long> {
    List<DeliveryLog> findByNotificationOrderByIdAsc(Notification notification);
    List<DeliveryLog> findByRecipientOrderByIdDesc(User recipient);
    List<DeliveryLog> findByNotification_IdAndRecipient_Id(Long notificationId, Long recipientId);
}
