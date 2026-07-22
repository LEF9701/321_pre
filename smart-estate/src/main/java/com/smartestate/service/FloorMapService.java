package com.smartestate.service;

import com.smartestate.model.*;
import com.smartestate.repo.*;
import org.springframework.stereotype.Service;

import java.util.*;

/** Builds the colour-coded floor map view models (FR-4.4, FR-4.5, FR-4.6). */
@Service
public class FloorMapService {

    private final BlockRepository blockRepository;
    private final FloorRepository floorRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final IssueRepository issueRepository;

    public FloorMapService(BlockRepository blockRepository, FloorRepository floorRepository,
                           UnitRepository unitRepository, UserRepository userRepository,
                           IssueRepository issueRepository) {
        this.blockRepository = blockRepository;
        this.floorRepository = floorRepository;
        this.unitRepository = unitRepository;
        this.userRepository = userRepository;
        this.issueRepository = issueRepository;
    }

    public List<BlockMap> mapsForBuilding(Long buildingId) {
        // unit -> occupant name
        Map<Long, String> occupant = new HashMap<>();
        for (User u : userRepository.findByRole(Role.RESIDENT)) {
            if (u.getUnit() != null) occupant.put(u.getUnit().getId(), u.getName());
        }
        // units with at least one open issue
        Set<Long> openIssueUnits = new HashSet<>();
        for (Issue i : issueRepository.findAllByOrderByCreatedAtDesc()) {
            if (i.getUnit() != null && i.getStatus() != IssueStatus.RESOLVED) {
                openIssueUnits.add(i.getUnit().getId());
            }
        }

        List<BlockMap> result = new ArrayList<>();
        for (Block block : blockRepository.findByBuilding_IdOrderByBlockNameAsc(buildingId)) {
            BlockMap bm = new BlockMap();
            bm.blockId = block.getId();
            bm.blockName = block.getBlockName();
            bm.blockType = block.getBlockType();

            List<Floor> floors = floorRepository.findByBlockOrderByFloorNumberAsc(block);
            // show highest floor at the top
            floors.sort(Comparator.comparingInt(Floor::getFloorNumber).reversed());
            for (Floor f : floors) {
                FloorRow row = new FloorRow();
                row.floorNumber = f.getFloorNumber();
                for (Unit u : unitRepository.findByFloorOrderByUnitCodeAsc(f)) {
                    Cell cell = new Cell();
                    cell.unitId = u.getId();
                    cell.unitCode = u.getUnitCode();
                    cell.occupant = occupant.get(u.getId());
                    cell.feeStatus = u.getFeeStatus();
                    boolean hasIssue = openIssueUnits.contains(u.getId());
                    cell.hasOpenIssue = hasIssue;

                    if (u.getStatus() == UnitStatus.VACANT) {
                        cell.state = "vacant"; cell.letter = "V"; cell.stateLabel = "Vacant";
                        bm.vacant++;
                    } else {
                        bm.occupied++;
                        if (hasIssue) {
                            cell.state = "issue"; cell.letter = "I"; cell.stateLabel = "Open issue";
                            bm.openIssues++;
                        } else if (u.getFeeStatus() == FeeStatus.OVERDUE) {
                            cell.state = "overdue"; cell.letter = "$"; cell.stateLabel = "Fee overdue";
                            bm.overdue++;
                        } else {
                            cell.state = "occupied"; cell.letter = "O"; cell.stateLabel = "Occupied";
                        }
                    }
                    row.cells.add(cell);
                }
                bm.rows.add(row);
            }
            result.add(bm);
        }
        return result;
    }

    // ---- view models ------------------------------------------------------

    public static class BlockMap {
        public Long blockId;
        public String blockName;
        public String blockType;
        public List<FloorRow> rows = new ArrayList<>();
        public int occupied, vacant, openIssues, overdue;

        public Long getBlockId() { return blockId; }
        public String getBlockName() { return blockName; }
        public String getBlockType() { return blockType; }
        public List<FloorRow> getRows() { return rows; }
        public int getOccupied() { return occupied; }
        public int getVacant() { return vacant; }
        public int getOpenIssues() { return openIssues; }
        public int getOverdue() { return overdue; }
        public int getTotalUnits() { return occupied + vacant; }
        public int getOccupancyPct() {
            int total = getTotalUnits();
            return total == 0 ? 0 : Math.round(occupied * 100f / total);
        }
    }

    public static class FloorRow {
        public int floorNumber;
        public List<Cell> cells = new ArrayList<>();
        public int getFloorNumber() { return floorNumber; }
        public List<Cell> getCells() { return cells; }
    }

    public static class Cell {
        public Long unitId;
        public String unitCode;
        public String state;       // vacant | occupied | issue | overdue
        public String stateLabel;
        public String letter;
        public String occupant;
        public FeeStatus feeStatus;
        public boolean hasOpenIssue;

        public Long getUnitId() { return unitId; }
        public String getUnitCode() { return unitCode; }
        public String getState() { return state; }
        public String getStateLabel() { return stateLabel; }
        public String getLetter() { return letter; }
        public String getOccupant() { return occupant; }
        public FeeStatus getFeeStatus() { return feeStatus; }
        public boolean isHasOpenIssue() { return hasOpenIssue; }
    }
}
