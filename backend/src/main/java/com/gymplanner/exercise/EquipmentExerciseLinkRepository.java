package com.gymplanner.exercise;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EquipmentExerciseLinkRepository extends JpaRepository<EquipmentExerciseLink, UUID> {

    @Query("""
            select l from EquipmentExerciseLink l join fetch l.equipment eq
            where eq.gym.id = :gymId and eq.deletedAt is null
            """)
    List<EquipmentExerciseLink> findByGym(UUID gymId);

    @Query("select l from EquipmentExerciseLink l join fetch l.equipment where l.exercise.id = :exerciseId")
    List<EquipmentExerciseLink> findByExerciseId(UUID exerciseId);

    Optional<EquipmentExerciseLink> findByEquipmentIdAndExerciseId(UUID equipmentId, UUID exerciseId);

    boolean existsByEquipmentIdAndExerciseId(UUID equipmentId, UUID exerciseId);
}
