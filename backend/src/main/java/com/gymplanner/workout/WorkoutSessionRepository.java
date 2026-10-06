package com.gymplanner.workout;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {

    @Query("select s from WorkoutSession s where s.id = :id and s.user.id = :userId")
    Optional<WorkoutSession> findOwned(UUID id, UUID userId);

    @Query("select s from WorkoutSession s where s.user.id = :userId "
            + "and s.status = com.gymplanner.workout.SessionStatus.IN_PROGRESS")
    Optional<WorkoutSession> findActive(UUID userId);

    @EntityGraph(attributePaths = "gym")
    @Query("""
            select s from WorkoutSession s
            where s.user.id = :userId and s.status = com.gymplanner.workout.SessionStatus.FINISHED
            order by s.finishedAt desc
            """)
    Page<WorkoutSession> findHistory(UUID userId, Pageable pageable);
}
