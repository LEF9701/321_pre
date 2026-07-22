package com.smartestate.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "estate_unit")
public class Unit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Floor floor;

    private String unitCode;

    @Enumerated(EnumType.STRING)
    private UnitStatus status = UnitStatus.VACANT;

    @Enumerated(EnumType.STRING)
    private FeeStatus feeStatus = FeeStatus.PAID;

    private BigDecimal balance = BigDecimal.ZERO;
    private LocalDate dueDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Floor getFloor() { return floor; }
    public void setFloor(Floor floor) { this.floor = floor; }
    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }
    public UnitStatus getStatus() { return status; }
    public void setStatus(UnitStatus status) { this.status = status; }
    public FeeStatus getFeeStatus() { return feeStatus; }
    public void setFeeStatus(FeeStatus feeStatus) { this.feeStatus = feeStatus; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    // convenience accessors for templates
    @Transient
    public String getBlockName() { return floor != null && floor.getBlock() != null ? floor.getBlock().getBlockName() : ""; }
    @Transient
    public int getFloorNumber() { return floor != null ? floor.getFloorNumber() : 0; }
    @Transient
    public String getFullCode() {
        if (floor == null || floor.getBlock() == null) return unitCode;
        return floor.getBlock().getBlockName() + " " + floor.getFloorNumber() + "/F-" + unitCode;
    }
}
