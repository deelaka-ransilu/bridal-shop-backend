package edu.bridalshop.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "dress_fulfillment_options",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_dress_fulfillment_type",
                columnNames = {"dress_id", "fulfillment_type"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DressFulfillmentOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Integer optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dress_id", nullable = false)
    private Dress dress;

    @Column(name = "fulfillment_type", nullable = false, length = 20)
    private String fulfillmentType;       // "CUSTOM" | "RENTAL" | "PURCHASE"

    @Column(name = "price_override", precision = 12, scale = 2)
    private BigDecimal priceOverride;     // NULL → use dress.retailPrice

    @Column(name = "rental_price_per_day", precision = 12, scale = 2)
    private BigDecimal rentalPricePerDay; // RENTAL only

    @Column(name = "rental_deposit", precision = 12, scale = 2)
    private BigDecimal rentalDeposit;     // RENTAL only

    @Column(name = "rental_period_days")
    private Integer rentalPeriodDays;     // RENTAL only — default 3

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (isActive == null)          isActive          = true;
        if (rentalPeriodDays == null)  rentalPeriodDays  = 3;
    }
}