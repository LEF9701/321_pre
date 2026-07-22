package com.smartestate.repo;

import com.smartestate.model.MaintenanceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, Long> {
    List<MaintenanceSchedule> findAllByOrderByScheduledDateAsc();
    List<MaintenanceSchedule> findByScheduledDateGreaterThanEqualOrderByScheduledDateAsc(LocalDate date);
}
