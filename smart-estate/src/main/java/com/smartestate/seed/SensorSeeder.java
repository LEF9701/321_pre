package com.smartestate.seed;

import com.smartestate.model.*;
import com.smartestate.repo.BlockRepository;
import com.smartestate.repo.FloorRepository;
import com.smartestate.repo.SensorRepository;
import com.smartestate.repo.UnitRepository;
import org.springframework.core.annotation.Order;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Seeds the estate's IoT sensor fleet so the dashboard is populated from the first page load:
 * per-unit in-flat smoke detectors, per-floor corridor detectors, and a per-block entrance door
 * sensor. Runs after {@link DataSeeder} (which builds the topology). Sensors are estate
 * infrastructure, not resident PII, so they seed regardless of {@code app.seed.residents}.
 */
@Component
@Order(2)
public class SensorSeeder implements CommandLineRunner {

    private final SensorRepository sensorRepo;
    private final BlockRepository blockRepo;
    private final FloorRepository floorRepo;
    private final UnitRepository unitRepo;

    public SensorSeeder(SensorRepository sensorRepo, BlockRepository blockRepo,
                        FloorRepository floorRepo, UnitRepository unitRepo) {
        this.sensorRepo = sensorRepo;
        this.blockRepo = blockRepo;
        this.floorRepo = floorRepo;
        this.unitRepo = unitRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (sensorRepo.count() > 0) return;
        List<Block> blocks = blockRepo.findAll();
        if (blocks.isEmpty()) return;
        Block lead = blocks.get(0);

        // Every tower: a lobby smoke detector + a ground-floor STAIRCASE FIRE DOOR contact.
        // (The main entrance is an Octopus card reader, an access point, not a door sensor.)
        for (Block b : blocks) {
            List<Floor> floors = floorRepo.findByBlockOrderByFloorNumberAsc(b);
            Floor ground = floors.isEmpty() ? null : floors.get(0);
            save(SensorType.SMOKE, "SMK-" + abbr(b) + "-LOBBY", b, ground, null, "Lobby");
            if (!b.getId().equals(lead.getId())) {   // the lead tower gets per-floor fire doors below
                save(SensorType.FIRE_DOOR, "FD-" + abbr(b) + "-G", b, ground, null, "Ground-floor staircase");
            }
        }

        // Rich coverage for the lead tower: per-floor corridor smoke + staircase fire door,
        // per-unit smoke, a couple of water-leak sensors, and one general service-room door.
        int leak = 0;
        for (Floor f : floorRepo.findByBlockOrderByFloorNumberAsc(lead)) {
            save(SensorType.SMOKE, "SMK-" + abbr(lead) + "-" + f.getFloorNumber() + "-COR",
                    lead, f, null, "Corridor");
            save(SensorType.FIRE_DOOR, "FD-" + abbr(lead) + "-" + f.getFloorNumber(),
                    lead, f, null, "Staircase fire door");
            for (Unit u : unitRepo.findByFloorOrderByUnitCodeAsc(f)) {
                save(SensorType.SMOKE, "SMK-" + abbr(lead) + "-" + f.getFloorNumber() + "-" + u.getUnitCode(),
                        lead, f, u, null);
                if (leak < 2 && u.getUnitCode().endsWith("1")) {
                    save(SensorType.WATER_LEAK, "WTR-" + abbr(lead) + "-" + f.getFloorNumber() + "-" + u.getUnitCode(),
                            lead, f, u, null);
                    leak++;
                }
            }
        }
        // A general (non-fire) door contact on a service room.
        save(SensorType.DOOR, "DR-" + abbr(lead) + "-REFUSE", lead, null, null, "Refuse room");
    }

    private void save(SensorType type, String code, Block block, Floor floor, Unit unit, String common) {
        Sensor s = new Sensor();
        s.setType(type);
        s.setCode(code);
        s.setBlock(block);
        s.setFloor(floor);
        s.setUnit(unit);
        s.setCommonAreaLabel(common);
        s.setStatus(SensorStatus.NORMAL);
        sensorRepo.save(s);
    }

    /** Two-letter tower abbreviation for readable sensor codes, e.g. "Azalea Tower" -> "AZ". */
    private String abbr(Block b) {
        String name = b.getBlockName() == null ? "BLK" : b.getBlockName().trim();
        return name.substring(0, Math.min(2, name.length())).toUpperCase(Locale.ROOT);
    }
}
