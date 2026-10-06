package com.gymplanner.equipment;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentTypeRepository extends JpaRepository<EquipmentType, UUID> {

    List<EquipmentType> findAllByOrderByCategoryAscNameAsc();

    java.util.Optional<EquipmentType> findByCode(String code);
}
