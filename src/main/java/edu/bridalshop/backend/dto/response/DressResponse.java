package edu.bridalshop.backend.dto.response;

import edu.bridalshop.backend.entity.Dress;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DressResponse(
        String publicId,
        String categoryPublicId,
        String categoryName,
        String name,
        String description,
        String dressType,
        BigDecimal retailPrice,          // NULL for unauthenticated users
        String fabric,
        String color,
        String madeForCustomerPublicId,  // CUSTOM dresses only
        boolean isAvailable,
        boolean isActive,
        List<FulfillmentOptionResponse> fulfillmentOptions,
        List<DressImageResponse> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // Full response — for authenticated users (price included)
    public static DressResponse from(Dress d) {
        return build(d, d.getRetailPrice());
    }

    // Public response — price hidden (null)
    public static DressResponse fromPublic(Dress d) {
        return build(d, null);
    }

    private static DressResponse build(Dress d, BigDecimal price) {
        return new DressResponse(
                d.getPublicId(),
                d.getCategory().getPublicId(),
                d.getCategory().getName(),
                d.getName(),
                d.getDescription(),
                d.getDressType(),
                price,
                d.getFabric(),
                d.getColor(),
                d.getMadeForCustomer() != null ? d.getMadeForCustomer().getPublicId() : null,
                d.getIsAvailable(),
                d.getIsActive(),
                d.getFulfillmentOptions().stream()
                        .map(FulfillmentOptionResponse::from)
                        .toList(),
                d.getImages().stream()
                        .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                        .map(DressImageResponse::from)
                        .toList(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}