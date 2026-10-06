package com.gymplanner.equipment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EquipmentReportRepository extends JpaRepository<EquipmentReport, UUID> {

    @EntityGraph(attributePaths = {"reporter", "resolvedBy", "duplicateOf"})
    @Query("select r from EquipmentReport r where r.equipment.id = :equipmentId order by r.status asc, r.createdAt desc")
    List<EquipmentReport> findForEquipment(UUID equipmentId);

    boolean existsByEquipmentIdAndReporterIdAndTypeAndStatus(UUID equipmentId, UUID reporterId, ReportType type,
            ReportStatus status);

    long countByEquipmentIdAndStatus(UUID equipmentId, ReportStatus status);

    @Query("""
            select r.equipment.id as equipmentId, count(r) as count from EquipmentReport r
            where r.equipment.id in :ids and r.status = com.gymplanner.equipment.ReportStatus.OPEN
            group by r.equipment.id
            """)
    List<OpenReportCount> countOpenByEquipmentIds(Collection<UUID> ids);

    interface OpenReportCount {
        UUID getEquipmentId();

        long getCount();
    }
}
