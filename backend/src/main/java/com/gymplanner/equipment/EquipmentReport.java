package com.gymplanner.equipment;

import com.gymplanner.common.persistence.BaseEntity;
import com.gymplanner.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Zgłoszenie problemu ze sprzętem (informacyjne – widoczne przy sprzęcie). */
@Entity
@Table(name = "equipment_reports")
public class EquipmentReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType type;

    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "duplicate_of_id")
    private Equipment duplicateOf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    private Instant resolvedAt;

    protected EquipmentReport() {
    }

    public EquipmentReport(Equipment equipment, User reporter, ReportType type, String comment,
            Equipment duplicateOf) {
        this.equipment = equipment;
        this.reporter = reporter;
        this.type = type;
        this.comment = comment;
        this.duplicateOf = duplicateOf;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public User getReporter() {
        return reporter;
    }

    public ReportType getType() {
        return type;
    }

    public String getComment() {
        return comment;
    }

    public Equipment getDuplicateOf() {
        return duplicateOf;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public User getResolvedBy() {
        return resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void resolve(User user, Instant now) {
        this.status = ReportStatus.RESOLVED;
        this.resolvedBy = user;
        this.resolvedAt = now;
    }
}
