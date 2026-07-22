package com.smartestate.model;

import jakarta.persistence.*;

@Entity
public class Building {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String address;
    private String district;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
}
