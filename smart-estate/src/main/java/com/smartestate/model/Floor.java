package com.smartestate.model;

import jakarta.persistence.*;

@Entity
public class Floor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Block block;

    private int floorNumber;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Block getBlock() { return block; }
    public void setBlock(Block block) { this.block = block; }
    public int getFloorNumber() { return floorNumber; }
    public void setFloorNumber(int floorNumber) { this.floorNumber = floorNumber; }
}
