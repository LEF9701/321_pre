package com.smartestate.repo;

import com.smartestate.model.NoticeCategory;
import com.smartestate.model.NotificationPreference;
import com.smartestate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    List<NotificationPreference> findByUserOrderByCategoryAsc(User user);
    Optional<NotificationPreference> findByUserAndCategory(User user, NoticeCategory category);
}
