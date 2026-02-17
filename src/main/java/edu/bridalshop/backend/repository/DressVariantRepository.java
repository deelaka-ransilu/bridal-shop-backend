package edu.bridalshop.backend.repository;

import edu.bridalshop.backend.entity.DressVariant;
import edu.bridalshop.backend.enums.VariantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DressVariantRepository extends JpaRepository<DressVariant, Integer> {

    List<DressVariant> findByDressDressId(Integer dressId);

    List<DressVariant> findByDressDressIdAndStatus(Integer dressId, VariantStatus status);

    Optional<DressVariant> findByStockItemStockItemId(Integer stockItemId);

    boolean existsByDressDressIdAndSizeAndColor(Integer dressId, String size, String color);

    @Query("""
        SELECT DISTINCT dv.size FROM DressVariant dv
        WHERE dv.dress.dressId = :dressId
        AND dv.status = 'ACTIVE'
        ORDER BY dv.size
        """)
    List<String> findDistinctSizesByDressId(@Param("dressId") Integer dressId);

    @Query("""
        SELECT DISTINCT dv.color FROM DressVariant dv
        WHERE dv.dress.dressId = :dressId
        AND dv.status = 'ACTIVE'
        ORDER BY dv.color
        """)
    List<String> findDistinctColorsByDressId(@Param("dressId") Integer dressId);
}