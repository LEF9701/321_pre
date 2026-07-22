package com.smartestate.repo;

import com.smartestate.model.Sensor;
import com.smartestate.model.SensorEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SensorEventRepository extends JpaRepository<SensorEvent, Long> {
    List<SensorEvent> findBySensorOrderByCreatedAtDesc(Sensor sensor, Pageable pageable);
    List<SensorEvent> findTop20ByOrderByCreatedAtDesc();
}
