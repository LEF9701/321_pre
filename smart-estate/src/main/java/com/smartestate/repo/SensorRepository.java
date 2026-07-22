package com.smartestate.repo;

import com.smartestate.model.Sensor;
import com.smartestate.model.SensorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Long> {
    Optional<Sensor> findByCode(String code);
    List<Sensor> findAllByOrderByCodeAsc();
    List<Sensor> findByStatusOrderByLastEventAtDesc(SensorStatus status);
}
