package com.smartestate.repo;

import com.smartestate.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByOrderByCreatedAtDesc();
    List<Notification> findByDispatchedTrueOrderByDispatchedAtDesc();
    List<Notification> findByDispatchedFalseAndScheduledAtNotNullAndScheduledAtBefore(LocalDateTime time);
}
