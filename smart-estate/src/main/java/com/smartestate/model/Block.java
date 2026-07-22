package com.smartestate.model;

import jakarta.persistence.*;

@Entity
public class Block {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Building building;

    private String blockName;
    private String blockType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Building getBuilding() { return building; }
    public void setBuilding(Building building) { this.building = building; }
    public String getBlockName() { return blockName; }
    public void setBlockName(String blockName) { this.blockName = blockName; }
    public String getBlockType() { return blockType; }
    public void setBlockType(String blockType) { this.blockType = blockType; }
}
