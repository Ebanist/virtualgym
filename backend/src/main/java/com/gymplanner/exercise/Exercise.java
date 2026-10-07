package com.gymplanner.exercise;

import com.gymplanner.common.persistence.BaseEntity;
import com.gymplanner.common.text.TextNormalizer;
import com.gymplanner.equipment.EquipmentType;
import com.gymplanner.gym.Gym;
import com.gymplanner.user.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.EnumSet;
import java.util.HashSet;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "exercises")
public class Exercise extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MuscleGroup primaryMuscle;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "exercise_secondary_muscles", joinColumns = @JoinColumn(name = "exercise_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "muscle")
    private Set<MuscleGroup> secondaryMuscles = new HashSet<>();

    private String description;

    /** Ćwiczenie z masą ciała – dostępne zawsze, niezależnie od sprzętu. */
    @Column(nullable = false)
    private boolean bodyweight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExerciseScope scope;

    /** Siłownia ćwiczenia CUSTOM (widoczne dla jej członków). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id")
    private Gym gym;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /** Dla ćwiczeń CUSTOM: kto je widzi. Dla GLOBAL bez znaczenia (zawsze widoczne). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExerciseVisibility visibility = ExerciseVisibility.GYM;

    private Instant deletedAt;

    /** Typy sprzętu, na których można wykonać ćwiczenie (wystarczy jeden). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "exercise_equipment_types", joinColumns = @JoinColumn(name = "exercise_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_type_id"))
    private Set<EquipmentType> equipmentTypes = new HashSet<>();

    protected Exercise() {
    }

    public static Exercise custom(Gym gym, User author, Details details) {
        Exercise e = new Exercise();
        e.scope = ExerciseScope.CUSTOM;
        e.gym = gym;
        e.createdBy = author;
        e.apply(details);
        return e;
    }

    /** Edytowalne pola ćwiczenia własnego. */
    public record Details(String name, MuscleGroup primaryMuscle, Set<MuscleGroup> secondaryMuscles,
            String description, boolean bodyweight, ExerciseVisibility visibility) {
    }

    public void apply(Details details) {
        this.name = details.name();
        this.normalizedName = TextNormalizer.normalize(details.name());
        this.primaryMuscle = details.primaryMuscle();
        this.secondaryMuscles = details.secondaryMuscles().isEmpty() ? new HashSet<>()
                : EnumSet.copyOf(details.secondaryMuscles());
        this.secondaryMuscles.remove(details.primaryMuscle());
        this.description = details.description();
        this.bodyweight = details.bodyweight();
        this.visibility = details.visibility();
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public MuscleGroup getPrimaryMuscle() {
        return primaryMuscle;
    }

    public Set<MuscleGroup> getSecondaryMuscles() {
        return secondaryMuscles;
    }

    public String getDescription() {
        return description;
    }

    public boolean isBodyweight() {
        return bodyweight;
    }

    public ExerciseScope getScope() {
        return scope;
    }

    public Gym getGym() {
        return gym;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Set<EquipmentType> getEquipmentTypes() {
        return equipmentTypes;
    }

    public ExerciseVisibility getVisibility() {
        return visibility;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markDeleted(Instant now) {
        this.deletedAt = now;
    }

    public boolean isAuthor(UUID userId) {
        return createdBy != null && createdBy.getId().equals(userId);
    }

    /**
     * Czy użytkownik widzi ćwiczenie w kontekście siłowni: biblioteka zawsze; własne – tylko nieusunięte
     * z tej siłowni, publiczne albo jego własne prywatne.
     */
    public boolean isVisibleTo(UUID userId, UUID gymId) {
        if (scope == ExerciseScope.GLOBAL) {
            return true;
        }
        return !isDeleted() && gym != null && gym.getId().equals(gymId)
                && (visibility == ExerciseVisibility.GYM || isAuthor(userId));
    }
}
