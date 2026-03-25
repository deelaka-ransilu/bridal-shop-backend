package edu.bridalshop.backend.repository;

import edu.bridalshop.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    List<Category> findAllByIsActiveTrue();

    List<Category> findAllByDressTypeAndIsActiveTrue(String dressType);

    Optional<Category> findByPublicId(String publicId);

    boolean existsByNameAndDressType(String name, String dressType);
}