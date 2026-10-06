package com.gymplanner.equipment;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentChangeRepository extends JpaRepository<EquipmentChange, UUID> {

    @EntityGraph(attributePaths = "user")
    Page<EquipmentChange> findByEquipmentId(UUID equipmentId, Pageable pageable);
}
