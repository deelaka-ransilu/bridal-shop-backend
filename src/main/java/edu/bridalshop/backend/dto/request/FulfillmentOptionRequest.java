package edu.bridalshop.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record FulfillmentOptionRequest(

        @NotBlank(message = "Fulfillment type is required")
        @Pattern(regexp = "CUSTOM|RENTAL|PURCHASE",
                message = "Fulfillment type must be CUSTOM, RENTAL, or PURCHASE")
        String fulfillmentType,

        // Optional price override (NULL = use retail price)
        @DecimalMin(value = "0.01", message = "Price override must be greater than 0")
        BigDecimal priceOverride,

        // RENTAL only fields
        @DecimalMin(value = "0.01", message = "Rental price per day must be greater than 0")
        BigDecimal rentalPricePerDay,

        @DecimalMin(value = "0.00", message = "Rental deposit cannot be negative")
        BigDecimal rentalDeposit,

        Integer rentalPeriodDays   // defaults to 3 if null
) {}