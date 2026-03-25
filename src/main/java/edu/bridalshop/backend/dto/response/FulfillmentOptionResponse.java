package edu.bridalshop.backend.dto.response;

import edu.bridalshop.backend.entity.DressFulfillmentOption;

import java.math.BigDecimal;

public record FulfillmentOptionResponse(
        String fulfillmentType,
        BigDecimal priceOverride,
        BigDecimal rentalPricePerDay,
        BigDecimal rentalDeposit,
        Integer rentalPeriodDays,
        boolean isActive
) {
    public static FulfillmentOptionResponse from(DressFulfillmentOption o) {
        return new FulfillmentOptionResponse(
                o.getFulfillmentType(),
                o.getPriceOverride(),
                o.getRentalPricePerDay(),
                o.getRentalDeposit(),
                o.getRentalPeriodDays(),
                o.getIsActive()
        );
    }
}