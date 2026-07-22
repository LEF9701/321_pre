package com.smartestate.model;

import jakarta.persistence.*;

/**
 * A controlled door / reader in the estate. Its type + location decide who a tapped card is
 * authorised for: a building entrance or elevator admits any resident; a flat door admits only the
 * resident of that unit.
 */
@Entity
public class AccessPoint {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    private AccessPointType type;

    @ManyToOne private Block block;   // for entrance / elevator
    @ManyToOne private Unit unit;     // for a flat door

    private String label;

    @Transient
    public String locationLabel() {
        if (unit != null) return unit.getFullCode();
        if (block != null) return block.getBlockName();
        return label != null ? label : code;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public AccessPointType getType() { return type; }
    public void setType(AccessPointType type) { this.type = type; }
    public Block getBlock() { return block; }
    public void setBlock(Block block) { this.block = block; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
