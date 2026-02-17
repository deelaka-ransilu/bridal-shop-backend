package edu.bridalshop.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DressResponse {

    private Integer dressId;
    private Integer categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal baseSalePrice;
    private BigDecimal baseRentalPrice;
    private Boolean availableForSale;
    private Boolean availableForRental;
    private Integer orderCount; // For popularity
    private LocalDateTime createdAt;

    // Related data (included in detail view)
    private List<DressVariantResponse> variants;
    private List<DressImageResponse> images;

    // Computed fields
    private Boolean hasAvailableStock; // Any variant in stock
    private List<String> availableSizes;
    private List<String> availableColors;
}