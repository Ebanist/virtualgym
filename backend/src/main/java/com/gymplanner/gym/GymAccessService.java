package com.gymplanner.gym;

import com.gymplanner.common.error.ForbiddenException;
import com.gymplanner.common.error.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reguły dostępu związane z członkostwem w siłowni – używane przez moduły sprzętu, ćwiczeń i planów. */
@Service
@Transactional(readOnly = true)
public class GymAccessService {

    private final GymRepository gyms;
    private final GymMembershipRepository memberships;

    public GymAccessService(GymRepository gyms, GymMembershipRepository memberships) {
        this.gyms = gyms;
        this.memberships = memberships;
    }

    public Gym getGym(UUID gymId) {
        return gyms.findById(gymId).orElseThrow(() -> new NotFoundException("Gym"));
    }

    public boolean isMember(UUID userId, UUID gymId) {
        return memberships.existsByUserIdAndGymId(userId, gymId);
    }

    /** Rzuca 403, jeśli użytkownik nie należy do siłowni. */
    public void requireMember(UUID userId, UUID gymId) {
        if (!isMember(userId, gymId)) {
            throw new ForbiddenException("gym_membership_required", "Only gym members can do this");
        }
    }
}
