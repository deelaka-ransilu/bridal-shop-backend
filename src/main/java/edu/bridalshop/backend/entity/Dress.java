package edu.bridalshop.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dresses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dress_id")
    private Integer dressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_rental_price", precision = 10, scale = 2)
    private BigDecimal baseRentalPrice;

    @Column(name = "base_sale_price", precision = 10, scale = 2)
    private BigDecimal baseSalePrice;

    @Column(name = "available_for_sale")
    @Builder.Default
    private Boolean availableForSale = true;

    @Column(name = "available_for_rental")
    @Builder.Default
    private Boolean availableForRental = true;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "order_count")
    @Builder.Default
    private Integer orderCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "dress", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DressVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "dress", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DressImage> images = new ArrayList<>();
}
