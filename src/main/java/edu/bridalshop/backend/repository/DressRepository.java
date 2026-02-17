package edu.bridalshop.backend.repository;

import edu.bridalshop.backend.entity.Dress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DressRepository extends JpaRepository<Dress, Integer>, JpaSpecificationExecutor<Dress> {

    // Find active dresses
    Page<Dress> findByIsActiveTrue(Pageable pageable);

    // Find by category
    Page<Dress> findByCategoryCategoryIdAndIsActiveTrue(Integer categoryId, Pageable pageable);

    // Search by name or description
    @Query("""
        SELECT d FROM Dress d
        WHERE d.isActive = true
        AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(d.description) LIKE LOWER(CONCAT('%', :query, '%')))
        """)
    Page<Dress> searchDresses(@Param("query") String query, Pageable pageable);

    // Increment order count for popularity
    @Modifying
    @Query("UPDATE Dress d SET d.orderCount = d.orderCount + 1 WHERE d.dressId = :dressId")
    void incrementOrderCount(@Param("dressId") Integer dressId);
}