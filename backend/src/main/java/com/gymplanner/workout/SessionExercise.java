package com.gymplanner.workout;

import com.gymplanner.common.persistence.BaseEntity;
import com.gymplanner.equipment.Equipment;
import com.gymplanner.exercise.Exercise;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "session_exercises")
public class SessionExercise extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private WorkoutSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    @Column(nullable = false)
    private int position;

    private Integer targetRepsMin;

    private Integer targetRepsMax;

    private BigDecimal targetWeightKg;

    @Column(nullable = false)
    private int restSeconds;

    private String note;

    @OneToMany(mappedBy = "sessionExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("setNumber")
    private List<SessionSet> sets = new ArrayList<>();

    /** Cel ćwiczenia w treningu (z pozycji planu lub domyślny dla ad hoc). */
    public record Target(Exercise exercise, Equipment equipment, int sets, Integer repsMin, Integer repsMax,
            BigDecimal weightKg, int restSeconds, String note) {
    }

    protected SessionExercise() {
    }

    SessionExercise(WorkoutSession session, int position, Target target) {
        this.session = session;
        this.position = position;
        this.exercise = target.exercise();
        this.equipment = target.equipment();
        this.targetRepsMin = target.repsMin();
        this.targetRepsMax = target.repsMax();
        this.targetWeightKg = target.weightKg();
        this.restSeconds = target.restSeconds();
        this.note = target.note();
        for (int i = 0; i < target.sets(); i++) {
            addSet(target.weightKg(), null);
        }
    }

    public SessionSet addSet(BigDecimal weightKg, Integer reps) {
        SessionSet set = new SessionSet(this, sets.size() + 1, weightKg, reps);
        sets.add(set);
        return set;
    }

    public void removeSet(SessionSet set) {
        sets.remove(set);
        for (int i = 0; i < sets.size(); i++) {
            sets.get(i).setSetNumber(i + 1);
        }
    }

    public WorkoutSession getSession() {
        return session;
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

    void setPosition(int position) {
        this.position = position;
    }

    public Integer getTargetRepsMin() {
        return targetRepsMin;
    }

    public Integer getTargetRepsMax() {
        return targetRepsMax;
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

    public List<SessionSet> getSets() {
        return sets;
    }
}
