package com.smartestate.repo;

import com.smartestate.model.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, Long> {
    List<MessageTemplate> findByEmergencyOrderByNameAsc(boolean emergency);
    Optional<MessageTemplate> findByCode(String code);
    List<MessageTemplate> findAllByOrderByNameAsc();
}
