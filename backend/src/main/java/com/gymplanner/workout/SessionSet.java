package com.gymplanner.workout;

import com.gymplanner.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "session_sets")
public class SessionSet extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_exercise_id")
    private SessionExercise sessionExercise;

    @Column(nullable = false)
    private int setNumber;

    private Integer reps;

    private BigDecimal weightKg;

    @Column(nullable = false)
    private boolean completed;

    private Instant completedAt;

    protected SessionSet() {
    }

    SessionSet(SessionExercise sessionExercise, int setNumber, BigDecimal weightKg, Integer reps) {
        this.sessionExercise = sessionExercise;
        this.setNumber = setNumber;
        this.weightKg = weightKg;
        this.reps = reps;
    }

    public void update(Integer reps, BigDecimal weightKg, boolean completed, Instant now) {
        this.reps = reps;
        this.weightKg = weightKg;
        if (completed && !this.completed) {
            this.completedAt = now;
        } else if (!completed) {
            this.completedAt = null;
        }
        this.completed = completed;
    }

    public SessionExercise getSessionExercise() {
        return sessionExercise;
    }

    public int getSetNumber() {
        return setNumber;
    }

    void setSetNumber(int setNumber) {
        this.setNumber = setNumber;
    }

    public Integer getReps() {
        return reps;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
