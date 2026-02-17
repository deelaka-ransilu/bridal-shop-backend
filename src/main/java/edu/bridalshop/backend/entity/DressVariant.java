package edu.bridalshop.backend.entity;

import edu.bridalshop.backend.enums.VariantStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;      // 👈 add
import org.hibernate.type.SqlTypes;                 // 👈 add

@Entity
@Table(name = "dress_variants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DressVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private Integer variantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dress_id", nullable = false)
    private Dress dress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_item_id", nullable = false)
    private StockItem stockItem;

    @Column(name = "size", nullable = false, length = 20)
    private String size;

    @Column(name = "color", nullable = false, length = 50)
    private String color;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)              // 👈 add
    @Column(name = "status")
    @Builder.Default
    private VariantStatus status = VariantStatus.ACTIVE;
}