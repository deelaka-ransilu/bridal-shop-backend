package edu.bridalshop.backend.repository;

import edu.bridalshop.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    @Query("""
        SELECT c.categoryId, c.name, COUNT(d.dressId) as dressCount
        FROM Category c
        LEFT JOIN Dress d ON d.category.categoryId = c.categoryId AND d.isActive = true
        GROUP BY c.categoryId, c.name
        ORDER BY c.name
        """)
    List<Object[]> findAllWithDressCount();
}