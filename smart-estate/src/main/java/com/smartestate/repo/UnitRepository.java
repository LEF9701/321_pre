package com.smartestate.repo;

import com.smartestate.model.Floor;
import com.smartestate.model.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    List<Unit> findByFloorOrderByUnitCodeAsc(Floor floor);
    List<Unit> findByFloor_Block_IdOrderByUnitCodeAsc(Long blockId);
    List<Unit> findByFloor_Block_Building_Id(Long buildingId);
}
