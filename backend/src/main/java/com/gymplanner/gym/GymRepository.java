package com.gymplanner.gym;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GymRepository extends JpaRepository<Gym, UUID> {

    /** Parametry są już znormalizowane ({@code TextNormalizer}); pusty string = brak filtra. */
    @Query("""
            select g from Gym g
            where (:name = '' or g.normalizedName like concat('%', :name, '%'))
              and (:city = '' or g.normalizedCity like concat(:city, '%'))
            """)
    Page<Gym> search(String name, String city, Pageable pageable);

    /** Siłownie o podobnej nazwie w tym samym mieście (trigramy pg_trgm lub zawieranie się nazw). */
    @Query(value = """
            select * from gyms g
            where g.normalized_city = :city
              and (similarity(g.normalized_name, :name) >= :threshold
                   or g.normalized_name like concat('%', :name, '%')
                   or :name like concat('%', g.normalized_name, '%'))
            order by similarity(g.normalized_name, :name) desc
            limit :limit
            """, nativeQuery = true)
    List<Gym> findSimilar(String name, String city, double threshold, int limit);
}
