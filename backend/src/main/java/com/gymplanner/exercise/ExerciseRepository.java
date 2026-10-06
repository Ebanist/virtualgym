package com.gymplanner.exercise;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    @EntityGraph(attributePaths = {"equipmentTypes", "secondaryMuscles", "gym"})
    @Query("select e from Exercise e where e.id = :id")
    Optional<Exercise> findWithDetails(UUID id);

    /** Globalne + własne ćwiczenia podanych siłowni. */
    @EntityGraph(attributePaths = {"equipmentTypes", "secondaryMuscles", "gym"})
    @Query("""
            select distinct e from Exercise e
            where e.scope = com.gymplanner.exercise.ExerciseScope.GLOBAL or e.gym.id in :gymIds
            order by e.name
            """)
    List<Exercise> findVisibleInGyms(Collection<UUID> gymIds);
}
