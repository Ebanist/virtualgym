package com.gymplanner.equipment;

import com.gymplanner.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** Globalny słownik typów sprzętu (seed w migracji V3). */
@Entity
@Table(name = "equipment_types")
public class EquipmentType extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentCategory category;

    protected EquipmentType() {
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public EquipmentCategory getCategory() {
        return category;
    }
}
