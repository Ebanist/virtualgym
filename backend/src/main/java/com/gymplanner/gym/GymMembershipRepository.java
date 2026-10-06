package com.gymplanner.gym;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GymMembershipRepository extends JpaRepository<GymMembership, UUID> {

    boolean existsByUserIdAndGymId(UUID userId, UUID gymId);

    Optional<GymMembership> findByUserIdAndGymId(UUID userId, UUID gymId);

    long countByGymId(UUID gymId);

    @Query("select m from GymMembership m join fetch m.gym where m.user.id = :userId order by m.gym.name")
    List<GymMembership> findByUserIdWithGym(UUID userId);

    @Query("select m.gym.id as gymId, count(m) as count from GymMembership m where m.gym.id in :gymIds group by m.gym.id")
    List<GymMemberCount> countByGymIds(Collection<UUID> gymIds);

    @Query("select m.gym.id from GymMembership m where m.user.id = :userId and m.gym.id in :gymIds")
    List<UUID> findGymIdsOfUser(UUID userId, Collection<UUID> gymIds);

    interface GymMemberCount {
        UUID getGymId();

        long getCount();
    }
}
