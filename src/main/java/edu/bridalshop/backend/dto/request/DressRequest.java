package edu.bridalshop.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record DressRequest(

        @NotBlank(message = "Category public ID is required")
        String categoryPublicId,

        @NotBlank(message = "Dress name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @Size(max = 5000, message = "Description is too long")
        String description,

        @NotBlank(message = "Dress type is required")
        @Pattern(regexp = "BRIDAL|PARTY", message = "Dress type must be BRIDAL or PARTY")
        String dressType,

        @NotNull(message = "Retail price is required")
        @DecimalMin(value = "0.01", message = "Retail price must be greater than 0")
        BigDecimal retailPrice,

        @Size(max = 100)
        String fabric,

        @Size(max = 100)
        String color,

        // For CUSTOM dresses only — publicId of the customer it was made for
        String madeForCustomerPublicId,

        @NotEmpty(message = "At least one fulfillment option is required")
        @Valid
        List<FulfillmentOptionRequest> fulfillmentOptions
) {}