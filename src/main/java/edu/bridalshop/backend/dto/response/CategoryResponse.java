package edu.bridalshop.backend.dto.response;

import edu.bridalshop.backend.entity.Category;

import java.time.LocalDateTime;

public record CategoryResponse(
        String publicId,
        String name,
        String dressType,
        String description,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(
                c.getPublicId(),
                c.getName(),
                c.getDressType(),
                c.getDescription(),
                c.getIsActive(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}