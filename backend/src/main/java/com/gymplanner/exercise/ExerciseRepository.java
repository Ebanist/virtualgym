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

    Optional<Exercise> findFirstByNameAndScope(String name, ExerciseScope scope);

    /**
     * Ćwiczenia widoczne dla użytkownika: biblioteka + nieusunięte własne ćwiczenia podanych siłowni –
     * publiczne lub jego prywatne. Reguła lustrzana do {@link Exercise#isVisibleTo}.
     */
    @EntityGraph(attributePaths = {"equipmentTypes", "secondaryMuscles", "gym", "createdBy"})
    @Query("""
            select distinct e from Exercise e
            where e.scope = com.gymplanner.exercise.ExerciseScope.GLOBAL
               or (e.gym.id in :gymIds and e.deletedAt is null
                   and (e.visibility = com.gymplanner.exercise.ExerciseVisibility.GYM or e.createdBy.id = :userId))
            order by e.name
            """)
    List<Exercise> findVisible(UUID userId, Collection<UUID> gymIds);

    /** Czy ćwiczenie jest użyte w planach (nieusuniętych) innych użytkowników niż autor. */
    @Query("""
            select count(i) > 0 from PlanItem i
            where i.exercise.id = :exerciseId and i.day.plan.owner.id <> :authorId and i.day.plan.deletedAt is null
            """)
    boolean isUsedInPlansOfOthers(UUID exerciseId, UUID authorId);
}
