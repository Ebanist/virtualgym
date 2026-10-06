package com.gymplanner.exercise;

import com.gymplanner.common.persistence.BaseEntity;
import com.gymplanner.equipment.Equipment;
import com.gymplanner.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Jawne powiązanie konkretnego sprzętu w siłowni z ćwiczeniem. */
@Entity
@Table(name = "equipment_exercises")
public class EquipmentExerciseLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    protected EquipmentExerciseLink() {
    }

    public EquipmentExerciseLink(Equipment equipment, Exercise exercise, User createdBy) {
        this.equipment = equipment;
        this.exercise = exercise;
        this.createdBy = createdBy;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public Exercise getExercise() {
        return exercise;
    }
}
