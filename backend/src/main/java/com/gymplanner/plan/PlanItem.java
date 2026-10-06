package com.gymplanner.plan;

import com.gymplanner.common.persistence.BaseEntity;
import com.gymplanner.equipment.Equipment;
import com.gymplanner.exercise.Exercise;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Pozycja dnia treningowego: ćwiczenie + sprzęt (null = masa ciała) i parametry. */
@Entity
@Table(name = "plan_items")
public class PlanItem extends BaseEntity implements WorkoutPlan.Positioned {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "day_id")
    private PlanDay day;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private int sets;

    @Column(nullable = false)
    private int repsMin;

    @Column(nullable = false)
    private int repsMax;

    private BigDecimal targetWeightKg;

    @Column(nullable = false)
    private int restSeconds;

    private String note;

    /** Wartości pozycji (współdzielone przez tworzenie, edycję i kopiowanie). */
    public record Values(Exercise exercise, Equipment equipment, int sets, int repsMin, int repsMax,
            BigDecimal targetWeightKg, int restSeconds, String note) {
    }

    protected PlanItem() {
    }

    PlanItem(PlanDay day, int position, Values values) {
        this.day = day;
        this.position = position;
        apply(values);
    }

    public void apply(Values values) {
        this.exercise = values.exercise();
        this.equipment = values.equipment();
        this.sets = values.sets();
        this.repsMin = values.repsMin();
        this.repsMax = values.repsMax();
        this.targetWeightKg = values.targetWeightKg();
        this.restSeconds = values.restSeconds();
        this.note = values.note();
    }

    public Values values() {
        return new Values(exercise, equipment, sets, repsMin, repsMax, targetWeightKg, restSeconds, note);
    }

    /** Sprzęt użyty w pozycji został usunięty lub oznaczony jako usunięty z siłowni. */
    public boolean isEquipmentUnavailable() {
        return equipment != null && !equipment.isAvailable();
    }

    public PlanDay getDay() {
        return day;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public void setPosition(int position) {
        this.position = position;
    }

    public int getSets() {
        return sets;
    }

    public int getRepsMin() {
        return repsMin;
    }

    public int getRepsMax() {
        return repsMax;
    }

    public BigDecimal getTargetWeightKg() {
        return targetWeightKg;
    }

    public int getRestSeconds() {
        return restSeconds;
    }

    public String getNote() {
        return note;
    }
}
