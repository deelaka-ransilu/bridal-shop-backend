package edu.bridalshop.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dress_id")
    private Integer dressId;

    @Column(name = "public_id", nullable = false, unique = true, length = 20)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "dress_type", nullable = false, length = 20)
    private String dressType;             // "BRIDAL" | "PARTY"

    @Column(name = "retail_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal retailPrice;

    @Column(name = "fabric", length = 100)
    private String fabric;

    @Column(name = "color", length = 100)
    private String color;

    // For CUSTOM dresses only — tracks which customer it was made for
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "made_for_customer_id")
    private User madeForCustomer;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "dress", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DressFulfillmentOption> fulfillmentOptions = new ArrayList<>();

    @OneToMany(mappedBy = "dress", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DressImage> images = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt   = LocalDateTime.now();
        updatedAt   = LocalDateTime.now();
        if (isAvailable == null) isAvailable = true;
        if (isActive == null)    isActive    = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}