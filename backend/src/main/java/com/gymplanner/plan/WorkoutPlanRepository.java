package com.gymplanner.plan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, UUID> {

    @Query("select p from WorkoutPlan p where p.id = :id and p.owner.id = :ownerId and p.deletedAt is null")
    Optional<WorkoutPlan> findOwned(UUID id, UUID ownerId);

    @EntityGraph(attributePaths = "gym")
    @Query("""
            select p from WorkoutPlan p
            where p.owner.id = :ownerId and p.deletedAt is null and p.archived = :archived
              and (:gymId is null or p.gym.id = :gymId)
            order by p.updatedAt desc
            """)
    List<WorkoutPlan> findOwnedList(UUID ownerId, boolean archived, UUID gymId);
}
