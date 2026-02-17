package edu.bridalshop.backend.repository;

import edu.bridalshop.backend.entity.StockLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockLevelRepository extends JpaRepository<StockLevel, Integer> {

    Optional<StockLevel> findByStockItemStockItemId(Integer stockItemId);
}