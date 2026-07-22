package com.smartestate.repo;

import com.smartestate.model.Block;
import com.smartestate.model.Floor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FloorRepository extends JpaRepository<Floor, Long> {
    List<Floor> findByBlockOrderByFloorNumberAsc(Block block);
    List<Floor> findByBlock_IdOrderByFloorNumberDesc(Long blockId);
}
