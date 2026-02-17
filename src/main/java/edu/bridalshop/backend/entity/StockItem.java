package edu.bridalshop.backend.entity;

import edu.bridalshop.backend.enums.ItemCategory;
import edu.bridalshop.backend.enums.StockUnit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_item_id")
    private Integer stockItemId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)                // 👈 add this
    @Column(name = "item_type", nullable = false)
    private ItemCategory itemType;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)                // 👈 add this
    @Column(name = "unit_of_measure", nullable = false)
    @Builder.Default
    private StockUnit unitOfMeasure = StockUnit.PCS;

    @Column(name = "cost_price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "reorder_level", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal reorderLevel = new BigDecimal("5.00");

    @Column(name = "is_stocked")
    @Builder.Default
    private Boolean isStocked = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}