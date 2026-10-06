package com.gymplanner.gym;

import com.gymplanner.common.persistence.BaseEntity;
import com.gymplanner.common.text.TextNormalizer;
import com.gymplanner.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "gyms")
public class Gym extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String normalizedName;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String normalizedCity;

    @Column(nullable = false)
    private String address;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GymStatus status = GymStatus.COMMUNITY;

    /** Wersja płatna: organizacja, która przejęła profil siłowni. W MVP zawsze null. */
    private UUID claimedByOrganizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    protected Gym() {
    }

    public Gym(String name, String city, String address, String description, User createdBy) {
        setName(name);
        setCity(city);
        this.address = address;
        this.description = description;
        this.createdBy = createdBy;
    }

    public String getName() {
        return name;
    }

    private void setName(String name) {
        this.name = name;
        this.normalizedName = TextNormalizer.normalize(name);
    }

    public String getCity() {
        return city;
    }

    private void setCity(String city) {
        this.city = city;
        this.normalizedCity = TextNormalizer.normalize(city);
    }

    public String getAddress() {
        return address;
    }

    public String getDescription() {
        return description;
    }

    public GymStatus getStatus() {
        return status;
    }

    public UUID getClaimedByOrganizationId() {
        return claimedByOrganizationId;
    }

    public User getCreatedBy() {
        return createdBy;
    }
}
