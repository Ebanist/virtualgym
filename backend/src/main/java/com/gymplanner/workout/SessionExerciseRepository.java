package com.gymplanner.workout;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SessionExerciseRepository extends JpaRepository<SessionExercise, UUID> {

    /** Wykonania ćwiczenia w zakończonych treningach użytkownika – od najnowszego. */
    @EntityGraph(attributePaths = "session")
    @Query("""
            select se from SessionExercise se
            where se.exercise.id = :exerciseId and se.session.user.id = :userId
              and se.session.status = com.gymplanner.workout.SessionStatus.FINISHED
            order by se.session.finishedAt desc
            """)
    List<SessionExercise> findFinishedByExercise(UUID userId, UUID exerciseId, Pageable pageable);
}
