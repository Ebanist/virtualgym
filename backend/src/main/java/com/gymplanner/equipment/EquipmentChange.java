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
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Wpis historii zmian sprzętu: kto, kiedy, co (diff pól w JSONB). */
@Entity
@Table(name = "equipment_changes")
public class EquipmentChange extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeType changeType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, FieldChange> changes = new LinkedHashMap<>();

    protected EquipmentChange() {
    }

    public EquipmentChange(Equipment equipment, User user, ChangeType changeType, Map<String, FieldChange> changes) {
        this.equipment = equipment;
        this.user = user;
        this.changeType = changeType;
        this.changes = new LinkedHashMap<>(changes);
    }

    public User getUser() {
        return user;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public Map<String, FieldChange> getChanges() {
        return changes;
    }
}
