package com.gymplanner.workout;

import com.gymplanner.common.persistence.BaseEntity;
import com.gymplanner.gym.Gym;
import com.gymplanner.plan.PlanDay;
import com.gymplanner.plan.WorkoutPlan;
import com.gymplanner.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Wykonywany lub zapisany trening (z dnia planu albo ad hoc). */
@Entity
@Table(name = "workout_sessions")
public class WorkoutSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gym_id")
    private Gym gym;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private WorkoutPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_day_id")
    private PlanDay planDay;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant finishedAt;

    private String note;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<SessionExercise> exercises = new ArrayList<>();

    protected WorkoutSession() {
    }

    public WorkoutSession(User user, Gym gym, WorkoutPlan plan, PlanDay planDay, String title, Instant startedAt) {
        this.user = user;
        this.gym = gym;
        this.plan = plan;
        this.planDay = planDay;
        this.title = title;
        this.startedAt = startedAt;
    }

    public SessionExercise addExercise(SessionExercise.Target target) {
        SessionExercise exercise = new SessionExercise(this, exercises.size(), target);
        exercises.add(exercise);
        return exercise;
    }

    public void removeExercise(SessionExercise exercise) {
        exercises.remove(exercise);
        for (int i = 0; i < exercises.size(); i++) {
            exercises.get(i).setPosition(i);
        }
    }

    public boolean isActive() {
        return status == SessionStatus.IN_PROGRESS;
    }

    public void finish(Instant now, String note) {
        this.status = SessionStatus.FINISHED;
        this.finishedAt = now;
        this.note = note;
    }

    public void abandon(Instant now) {
        this.status = SessionStatus.ABANDONED;
        this.finishedAt = now;
    }

    public User getUser() {
        return user;
    }

    public Gym getGym() {
        return gym;
    }

    public WorkoutPlan getPlan() {
        return plan;
    }

    public PlanDay getPlanDay() {
        return planDay;
    }

    public String getTitle() {
        return title;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public String getNote() {
        return note;
    }

    public List<SessionExercise> getExercises() {
        return exercises;
    }
}
