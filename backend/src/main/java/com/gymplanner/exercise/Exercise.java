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
import java.util.Set;

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

    /** Typy sprzętu, na których można wykonać ćwiczenie (wystarczy jeden). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "exercise_equipment_types", joinColumns = @JoinColumn(name = "exercise_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_type_id"))
    private Set<EquipmentType> equipmentTypes = new HashSet<>();

    protected Exercise() {
    }

    public static Exercise custom(Gym gym, User author, String name, MuscleGroup primaryMuscle,
            Set<MuscleGroup> secondaryMuscles, String description, boolean bodyweight) {
        Exercise e = new Exercise();
        e.scope = ExerciseScope.CUSTOM;
        e.gym = gym;
        e.createdBy = author;
        e.name = name;
        e.normalizedName = TextNormalizer.normalize(name);
        e.primaryMuscle = primaryMuscle;
        e.secondaryMuscles = secondaryMuscles.isEmpty() ? new HashSet<>() : EnumSet.copyOf(secondaryMuscles);
        e.secondaryMuscles.remove(primaryMuscle);
        e.description = description;
        e.bodyweight = bodyweight;
        return e;
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

    /** Czy ćwiczenie jest widoczne w kontekście danej siłowni (globalne albo własne tej siłowni). */
    public boolean isVisibleInGym(java.util.UUID gymId) {
        return scope == ExerciseScope.GLOBAL || (gym != null && gym.getId().equals(gymId));
    }
}
