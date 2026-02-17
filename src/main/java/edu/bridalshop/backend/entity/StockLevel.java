package edu.bridalshop.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "stock_levels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLevel {

    @Id
    @Column(name = "stock_item_id")
    private Integer stockItemId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "stock_item_id")
    private StockItem stockItem;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;
}