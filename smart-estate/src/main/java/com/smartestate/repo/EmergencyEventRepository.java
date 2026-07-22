package com.smartestate.repo;

import com.smartestate.model.EmergencyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmergencyEventRepository extends JpaRepository<EmergencyEvent, Long> {
    Optional<EmergencyEvent> findFirstByActiveTrueOrderByDeclaredAtDesc();
    List<EmergencyEvent> findAllByOrderByDeclaredAtDesc();
}
