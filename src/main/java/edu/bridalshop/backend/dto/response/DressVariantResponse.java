package edu.bridalshop.backend.dto.response;

import edu.bridalshop.backend.enums.VariantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DressVariantResponse {

    private Integer variantId;
    private Integer stockItemId;
    private String size;
    private String color;
    private VariantStatus status;
    private Boolean inStock; // true if quantity > 0 and status = ACTIVE
    private Boolean availableForRental; // true if status = ACTIVE and not currently rented
}