package com.smartestate.repo;

import com.smartestate.model.AccessPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccessPointRepository extends JpaRepository<AccessPoint, Long> {
    Optional<AccessPoint> findByCode(String code);
}
