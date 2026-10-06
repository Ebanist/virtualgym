package com.gymplanner.equipment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {

    @EntityGraph(attributePaths = {"gym", "equipmentType", "photo", "createdBy"})
    @Query("select e from Equipment e where e.id = :id and e.deletedAt is null")
    Optional<Equipment> findActiveById(UUID id);

    /** Filtry opcjonalne: null = brak filtra; {@code name} już znormalizowane ('' = brak). */
    @EntityGraph(attributePaths = {"equipmentType", "photo"})
    @Query("""
            select e from Equipment e
            where e.gym.id = :gymId and e.deletedAt is null
              and (:name = '' or e.normalizedName like concat('%', :name, '%'))
              and (:category is null or e.category = :category)
              and (:status is null or e.status = :status)
            """)
    Page<Equipment> search(UUID gymId, String name, EquipmentCategory category, EquipmentStatus status,
            Pageable pageable);

    @Query(value = """
            select * from equipment e
            where e.gym_id = :gymId and e.deleted_at is null
              and (similarity(e.normalized_name, :name) >= :threshold
                   or e.normalized_name like concat('%', :name, '%')
                   or :name like concat('%', e.normalized_name, '%'))
            order by similarity(e.normalized_name, :name) desc
            limit :limit
            """, nativeQuery = true)
    List<Equipment> findSimilar(UUID gymId, String name, double threshold, int limit);

    @EntityGraph(attributePaths = {"equipmentType", "photo"})
    @Query("select e from Equipment e where e.gym.id = :gymId and e.deletedAt is null order by e.name")
    List<Equipment> findAllActiveInGym(UUID gymId);
}
