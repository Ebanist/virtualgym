package com.gymplanner.gym;

import com.gymplanner.common.persistence.BaseEntity;
import com.gymplanner.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "gym_memberships")
public class GymMembership extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gym_id")
    private Gym gym;

    protected GymMembership() {
    }

    public GymMembership(User user, Gym gym) {
        this.user = user;
        this.gym = gym;
    }

    public User getUser() {
        return user;
    }

    public Gym getGym() {
        return gym;
    }
}
