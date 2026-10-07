package com.gymplanner.exercise;

import static org.assertj.core.api.Assertions.assertThat;

import com.gymplanner.gym.Gym;
import com.gymplanner.user.User;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExerciseVisibilityTest {

    final UUID authorId = UUID.randomUUID();
    final UUID otherId = UUID.randomUUID();
    final Gym gym = withId(new Gym("Siłownia", "Miasto", "ul. 1", null, null), UUID.randomUUID());
    final User author = withId(new User("a@x.pl", "h", "A"), authorId);

    @Test
    void privateExerciseVisibleOnlyToAuthorInItsGym() {
        Exercise e = Exercise.custom(gym, author, details(ExerciseVisibility.PRIVATE));
        assertThat(e.isVisibleTo(authorId, gym.getId())).isTrue();
        assertThat(e.isVisibleTo(otherId, gym.getId())).isFalse();
        assertThat(e.isVisibleTo(authorId, UUID.randomUUID())).isFalse();
    }

    @Test
    void publicExerciseVisibleToGymMembersUntilDeleted() {
        Exercise e = Exercise.custom(gym, author, details(ExerciseVisibility.GYM));
        assertThat(e.isVisibleTo(otherId, gym.getId())).isTrue();
        e.markDeleted(Instant.now());
        assertThat(e.isVisibleTo(otherId, gym.getId())).isFalse();
        assertThat(e.isVisibleTo(authorId, gym.getId())).isFalse();
    }

    @Test
    void primaryMuscleIsNotDuplicatedInSecondary() {
        Exercise e = Exercise.custom(gym, author, new Exercise.Details("X", MuscleGroup.CARDIO,
                Set.of(MuscleGroup.CARDIO, MuscleGroup.CALVES), null, false, ExerciseVisibility.PRIVATE));
        assertThat(e.getSecondaryMuscles()).containsExactly(MuscleGroup.CALVES);
    }

    private static Exercise.Details details(ExerciseVisibility visibility) {
        return new Exercise.Details("Chodzenie na bieżni", MuscleGroup.CARDIO, Set.of(), null, false, visibility);
    }

    private static <T> T withId(T entity, UUID id) {
        try {
            Field field = com.gymplanner.common.persistence.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
