package edu.bridalshop.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DressRequest {
    @NotNull(message = "Category ID is required")
    private Integer categoryId;

    @NotBlank(message = "Dress name is required")
    @Size(min = 2, max = 150, message = "Dress name must be between 2 and 150 characters")
    private String name;

    @Size(max = 2000, message = "Description must be less than 2000 characters")
    private String description;

    @DecimalMin(value = "0.0", message = "Sale price must be positive")
    private BigDecimal baseSalePrice;

    @DecimalMin(value = "0.0", message = "Rental price must be positive")
    private BigDecimal baseRentalPrice;

    private Boolean availableForSale = true;

    private Boolean availableForRental = true;
}
