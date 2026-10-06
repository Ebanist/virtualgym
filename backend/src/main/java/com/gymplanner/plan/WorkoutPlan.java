package com.gymplanner.plan;

import com.gymplanner.common.persistence.BaseEntity;
import com.gymplanner.gym.Gym;
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
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Plan treningowy użytkownika przypisany do jednej siłowni. Usuwanie miękkie. */
@Entity
@Table(name = "workout_plans")
public class WorkoutPlan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gym_id")
    private Gym gym;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private boolean archived;

    /** Wersja płatna: plan oficjalny siłowni. W MVP zawsze PRIVATE. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanVisibility visibility = PlanVisibility.PRIVATE;

    /** Wersja płatna: trener – autor planu oficjalnego. W MVP zawsze null. */
    private UUID authorTrainerId;

    private Instant deletedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<PlanDay> days = new ArrayList<>();

    protected WorkoutPlan() {
    }

    public WorkoutPlan(User owner, Gym gym, String name, String description) {
        this.owner = owner;
        this.gym = gym;
        this.name = name;
        this.description = description;
    }

    public PlanDay addDay(String dayName) {
        PlanDay day = new PlanDay(this, dayName, days.size());
        days.add(day);
        return day;
    }

    public void removeDay(PlanDay day) {
        days.remove(day);
        renumber(days);
    }

    static <T extends Positioned> void renumber(List<T> list) {
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setPosition(i);
        }
    }

    public User getOwner() {
        return owner;
    }

    public Gym getGym() {
        return gym;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public PlanVisibility getVisibility() {
        return visibility;
    }

    public UUID getAuthorTrainerId() {
        return authorTrainerId;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markDeleted(Instant now) {
        this.deletedAt = now;
    }

    public long getVersion() {
        return version;
    }

    public List<PlanDay> getDays() {
        return days;
    }

    /** Wspólny interfejs elementów z kolejnością (dni, pozycje). */
    interface Positioned {
        UUID getId();

        void setPosition(int position);
    }
}
