package edu.bridalshop.backend.repository;

import edu.bridalshop.backend.entity.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockItemRepository extends JpaRepository<StockItem, Integer> {

    Optional<StockItem> findBySku(String sku);

    boolean existsBySku(String sku);
}
