package com.smartestate.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A physical IoT sensor in the estate. Location is flexible: an in-flat detector points
 * at a {@link Unit}, a corridor detector at a {@link Floor} + common-area label, a building-entrance
 * door at a {@link Block}. This granularity makes an alert actionable: it tells responders
 * where, and feeds the emergency's location note.
 */
@Entity
public class Sensor {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;                 // e.g. SMK-AZ-5-03, DOOR-AZ-ENTRANCE

    @Enumerated(EnumType.STRING)
    private SensorType type;

    @ManyToOne private Block block;      // tower, usually set
    @ManyToOne private Floor floor;      // nullable (corridor / floor-level)
    @ManyToOne private Unit unit;        // nullable (in-flat)
    private String commonAreaLabel;      // nullable ("Corridor", "Lift lobby", "Main entrance")

    @Enumerated(EnumType.STRING)
    private SensorStatus status = SensorStatus.NORMAL;

    @Enumerated(EnumType.STRING)
    private Severity lastSeverity;       // nullable until first event
    private LocalDateTime lastEventAt;

    /** Human-readable location, e.g. "Azalea Tower 5/F-03" or "Azalea Tower · 5/F · Corridor". */
    @Transient
    public String locationLabel() {
        if (unit != null) return unit.getFullCode();
        StringBuilder sb = new StringBuilder();
        if (block != null) sb.append(block.getBlockName());
        if (floor != null) sb.append(sb.length() > 0 ? " · " : "").append(floor.getFloorNumber()).append("/F");
        if (commonAreaLabel != null && !commonAreaLabel.isBlank()) {
            sb.append(sb.length() > 0 ? " · " : "").append(commonAreaLabel);
        }
        return sb.length() > 0 ? sb.toString() : code;
    }

    @Transient
    public boolean isFireType() { return type != null && type.isFire(); }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public SensorType getType() { return type; }
    public void setType(SensorType type) { this.type = type; }
    public Block getBlock() { return block; }
    public void setBlock(Block block) { this.block = block; }
    public Floor getFloor() { return floor; }
    public void setFloor(Floor floor) { this.floor = floor; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public String getCommonAreaLabel() { return commonAreaLabel; }
    public void setCommonAreaLabel(String commonAreaLabel) { this.commonAreaLabel = commonAreaLabel; }
    public SensorStatus getStatus() { return status; }
    public void setStatus(SensorStatus status) { this.status = status; }
    public Severity getLastSeverity() { return lastSeverity; }
    public void setLastSeverity(Severity lastSeverity) { this.lastSeverity = lastSeverity; }
    public LocalDateTime getLastEventAt() { return lastEventAt; }
    public void setLastEventAt(LocalDateTime lastEventAt) { this.lastEventAt = lastEventAt; }
}
