package com.gymplanner.equipment;

import com.gymplanner.common.persistence.BaseEntity;
import com.gymplanner.common.text.TextNormalizer;
import com.gymplanner.gym.Gym;
import com.gymplanner.storage.StoredFile;
import com.gymplanner.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** Sprzęt w siłowni dodawany przez społeczność. Usuwanie miękkie ({@code deletedAt}). */
@Entity
@Table(name = "equipment")
public class Equipment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gym_id")
    private Gym gym;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_type_id")
    private EquipmentType equipmentType;

    private String description;

    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_id")
    private StoredFile photo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentStatus status = EquipmentStatus.ACTIVE;

    /** Wersja płatna: źródło wpisu. W MVP zawsze COMMUNITY. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentSource source = EquipmentSource.COMMUNITY;

    /** Wersja płatna: sprzęt zweryfikowany przez siłownię. W MVP zawsze false. */
    @Column(nullable = false)
    private boolean verified;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    private Instant deletedAt;

    @Version
    private long version;

    protected Equipment() {
    }

    public Equipment(Gym gym, String name, EquipmentCategory category, User createdBy) {
        this.gym = gym;
        this.category = category;
        this.createdBy = createdBy;
        setName(name);
    }

    public Gym getGym() {
        return gym;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.normalizedName = TextNormalizer.normalize(name);
    }

    public EquipmentCategory getCategory() {
        return category;
    }

    public void setCategory(EquipmentCategory category) {
        this.category = category;
    }

    public EquipmentType getEquipmentType() {
        return equipmentType;
    }

    public void setEquipmentType(EquipmentType equipmentType) {
        this.equipmentType = equipmentType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public StoredFile getPhoto() {
        return photo;
    }

    public void setPhoto(StoredFile photo) {
        this.photo = photo;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = status;
    }

    public EquipmentSource getSource() {
        return source;
    }

    public boolean isVerified() {
        return verified;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markDeleted(Instant now) {
        this.deletedAt = now;
    }

    /** Sprzęt nadaje się do użycia w planach (istnieje i jest w siłowni). */
    public boolean isAvailable() {
        return deletedAt == null && status == EquipmentStatus.ACTIVE;
    }

    public long getVersion() {
        return version;
    }
}
