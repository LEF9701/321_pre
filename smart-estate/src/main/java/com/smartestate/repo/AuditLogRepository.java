package com.smartestate.repo;

import com.smartestate.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop200ByOrderByTimestampDesc();
    List<AuditLog> findByEmergencyRelatedTrueOrderByTimestampDesc();
}
