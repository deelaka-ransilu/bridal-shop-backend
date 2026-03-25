package edu.bridalshop.backend.repository;

import edu.bridalshop.backend.entity.Dress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DressRepository extends JpaRepository<Dress, Integer> {

    Optional<Dress> findByPublicId(String publicId);

    // Main catalog — RENTAL or PURCHASE dresses (active only)
    @Query("""
        SELECT DISTINCT d FROM Dress d
        JOIN d.fulfillmentOptions fo
        WHERE d.isActive = true
          AND fo.isActive = true
          AND fo.fulfillmentType IN ('RENTAL', 'PURCHASE')
        """)
    List<Dress> findCatalogDresses();

    // Featured styles — CUSTOM dresses (active only)
    @Query("""
        SELECT DISTINCT d FROM Dress d
        JOIN d.fulfillmentOptions fo
        WHERE d.isActive = true
          AND fo.isActive = true
          AND fo.fulfillmentType = 'CUSTOM'
        """)
    List<Dress> findFeaturedDresses();

    // Filter catalog by category publicId
    @Query("""
        SELECT DISTINCT d FROM Dress d
        JOIN d.fulfillmentOptions fo
        WHERE d.isActive = true
          AND fo.isActive = true
          AND fo.fulfillmentType IN ('RENTAL', 'PURCHASE')
          AND d.category.publicId = :categoryPublicId
        """)
    List<Dress> findCatalogDressesByCategory(@Param("categoryPublicId") String categoryPublicId);

    // Filter catalog by dress type (BRIDAL | PARTY)
    @Query("""
        SELECT DISTINCT d FROM Dress d
        JOIN d.fulfillmentOptions fo
        WHERE d.isActive = true
          AND fo.isActive = true
          AND fo.fulfillmentType IN ('RENTAL', 'PURCHASE')
          AND d.dressType = :dressType
        """)
    List<Dress> findCatalogDressesByType(@Param("dressType") String dressType);

    // Filter catalog by color
    @Query("""
        SELECT DISTINCT d FROM Dress d
        JOIN d.fulfillmentOptions fo
        WHERE d.isActive = true
          AND fo.isActive = true
          AND fo.fulfillmentType IN ('RENTAL', 'PURCHASE')
          AND LOWER(d.color) LIKE LOWER(CONCAT('%', :color, '%'))
        """)
    List<Dress> findCatalogDressesByColor(@Param("color") String color);
}