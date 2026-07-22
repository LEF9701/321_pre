package com.smartestate.repo;

import com.smartestate.model.Block;
import com.smartestate.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BlockRepository extends JpaRepository<Block, Long> {
    List<Block> findByBuildingOrderByBlockNameAsc(Building building);
    List<Block> findByBuilding_IdOrderByBlockNameAsc(Long buildingId);
}
