package com.smartestate.repo;

import com.smartestate.model.AccessEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessEventRepository extends JpaRepository<AccessEvent, Long> {
    List<AccessEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
