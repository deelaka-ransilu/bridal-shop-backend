package edu.bridalshop.backend.dto.response;

import edu.bridalshop.backend.entity.DressImage;

public record DressImageResponse(
        String publicId,
        String imageUrl,
        String thumbnailUrl,
        boolean isPrimary,
        int displayOrder
) {
    public static DressImageResponse from(DressImage i) {
        return new DressImageResponse(
                i.getPublicId(),
                i.getImageUrl(),
                i.getThumbnailUrl(),
                i.getIsPrimary(),
                i.getDisplayOrder()
        );
    }
}