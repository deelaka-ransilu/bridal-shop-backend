package edu.bridalshop.backend.service;

import edu.bridalshop.backend.dto.request.DressRequest;
import edu.bridalshop.backend.dto.request.FulfillmentOptionRequest;
import edu.bridalshop.backend.dto.response.DressImageResponse;
import edu.bridalshop.backend.dto.response.DressResponse;
import edu.bridalshop.backend.entity.*;
import edu.bridalshop.backend.exception.ResourceNotFoundException;
import edu.bridalshop.backend.repository.*;
import edu.bridalshop.backend.util.PayloadSanitizer;
import edu.bridalshop.backend.util.PublicIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DressService {

    private static final int MAX_IMAGES_PER_DRESS = 5;

    private final DressRepository            dressRepository;
    private final DressFulfillmentOptionRepository fulfillmentRepository;
    private final DressImageRepository       imageRepository;
    private final CategoryRepository         categoryRepository;
    private final UserRepository             userRepository;
    private final CloudinaryService          cloudinaryService;
    private final PublicIdGenerator          publicIdGenerator;
    private final PayloadSanitizer           sanitizer;

    // =========================================================================
    // PUBLIC --- Browse catalog (RENTAL + PURCHASE dresses, price hidden)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<DressResponse> getCatalog(String categoryPublicId,
                                          String dressType,
                                          String color) {
        List<Dress> dresses;

        if (categoryPublicId != null) {
            dresses = dressRepository.findCatalogDressesByCategory(categoryPublicId);
        } else if (dressType != null) {
            dresses = dressRepository.findCatalogDressesByType(dressType.toUpperCase());
        } else if (color != null) {
            dresses = dressRepository.findCatalogDressesByColor(color);
        } else {
            dresses = dressRepository.findCatalogDresses();
        }

        return dresses.stream().map(DressResponse::fromPublic).toList();
    }

    @Transactional(readOnly = true)
    public List<DressResponse> getFeatured() {
        return dressRepository.findFeaturedDresses()
                .stream()
                .map(DressResponse::fromPublic)
                .toList();
    }

    // =========================================================================
    // PUBLIC with auth check --- Get single dress
    // Price included if authenticated, hidden if not
    // =========================================================================

    @Transactional(readOnly = true)
    public DressResponse getByPublicId(String publicId, boolean authenticated) {
        Dress dress = findByPublicIdOrThrow(publicId);
        return authenticated
                ? DressResponse.from(dress)
                : DressResponse.fromPublic(dress);
    }

    // =========================================================================
    // ADMIN --- Create dress
    // =========================================================================

    @Transactional
    public DressResponse create(DressRequest request) {
        Category category = categoryRepository.findByPublicId(request.categoryPublicId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + request.categoryPublicId()));

        User madeForCustomer = null;
        if (request.madeForCustomerPublicId() != null) {
            madeForCustomer = userRepository.findByPublicId(request.madeForCustomerPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Customer not found: " + request.madeForCustomerPublicId()));
        }

        Dress dress = Dress.builder()
                .publicId(publicIdGenerator.forDress())
                .category(category)
                .name(sanitizer.sanitizeText(request.name()))
                .description(request.description() != null
                        ? sanitizer.sanitizeRichText(request.description()) : null)
                .dressType(request.dressType().toUpperCase())
                .retailPrice(request.retailPrice())
                .fabric(request.fabric() != null
                        ? sanitizer.sanitizeText(request.fabric()) : null)
                .color(request.color() != null
                        ? sanitizer.sanitizeText(request.color()) : null)
                .madeForCustomer(madeForCustomer)
                .isAvailable(true)
                .isActive(true)
                .build();

        Dress saved = dressRepository.save(dress);

        // Attach fulfillment options
        for (FulfillmentOptionRequest optReq : request.fulfillmentOptions()) {
            validateFulfillmentOption(optReq);
            DressFulfillmentOption option = buildFulfillmentOption(saved, optReq);
            fulfillmentRepository.save(option);
            saved.getFulfillmentOptions().add(option);
        }

        log.info("Dress created: {} [{}]", saved.getPublicId(), saved.getName());
        return DressResponse.from(saved);
    }

    // =========================================================================
    // ADMIN --- Update dress (metadata only — images managed separately)
    // =========================================================================

    @Transactional
    public DressResponse update(String publicId, DressRequest request) {
        Dress dress = findByPublicIdOrThrow(publicId);

        Category category = categoryRepository.findByPublicId(request.categoryPublicId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + request.categoryPublicId()));

        User madeForCustomer = null;
        if (request.madeForCustomerPublicId() != null) {
            madeForCustomer = userRepository.findByPublicId(request.madeForCustomerPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Customer not found: " + request.madeForCustomerPublicId()));
        }

        dress.setCategory(category);
        dress.setName(sanitizer.sanitizeText(request.name()));
        dress.setDescription(request.description() != null
                ? sanitizer.sanitizeRichText(request.description()) : null);
        dress.setDressType(request.dressType().toUpperCase());
        dress.setRetailPrice(request.retailPrice());
        dress.setFabric(request.fabric() != null
                ? sanitizer.sanitizeText(request.fabric()) : null);
        dress.setColor(request.color() != null
                ? sanitizer.sanitizeText(request.color()) : null);
        dress.setMadeForCustomer(madeForCustomer);

        // Replace fulfillment options — remove old, add new
        // AFTER ✅
        fulfillmentRepository.deleteAll(
                fulfillmentRepository.findAllByDress_DressId(dress.getDressId()));
        fulfillmentRepository.flush();   // ← ADD THIS LINE
        dress.getFulfillmentOptions().clear();

        for (FulfillmentOptionRequest optReq : request.fulfillmentOptions()) {
            validateFulfillmentOption(optReq);
            DressFulfillmentOption option = buildFulfillmentOption(dress, optReq);
            fulfillmentRepository.save(option);
            dress.getFulfillmentOptions().add(option);
        }

        Dress saved = dressRepository.save(dress);
        log.info("Dress updated: {}", saved.getPublicId());
        return DressResponse.from(saved);
    }

    // =========================================================================
    // ADMIN --- Soft delete dress
    // =========================================================================

    @Transactional
    public void deactivate(String publicId) {
        Dress dress = findByPublicIdOrThrow(publicId);
        dress.setIsActive(false);
        dressRepository.save(dress);
        log.info("Dress deactivated: {}", publicId);
    }

    // =========================================================================
    // ADMIN --- Upload dress image (max 5)
    // =========================================================================

    @Transactional
    public DressImageResponse uploadImage(String dressPublicId, MultipartFile file) {
        Dress dress = findByPublicIdOrThrow(dressPublicId);

        int currentCount = imageRepository.countByDress_DressId(dress.getDressId());
        if (currentCount >= MAX_IMAGES_PER_DRESS) {
            throw new IllegalStateException(
                    "Maximum of " + MAX_IMAGES_PER_DRESS + " images allowed per dress");
        }

        // Upload full image to Cloudinary
        String imageUrl;
        try {
            imageUrl = cloudinaryService.uploadDressImage(file);
        } catch (IOException e) {
            log.error("Failed to upload dress image", e);
            throw new RuntimeException("Image upload failed");
        }

        // Build thumbnail URL via Cloudinary URL transformation (w_400,h_400,c_fill)
        String thumbnailUrl = buildThumbnailUrl(imageUrl);

        // First image uploaded becomes primary automatically
        boolean isPrimary = currentCount == 0;

        DressImage image = DressImage.builder()
                .publicId(publicIdGenerator.forImage())
                .dress(dress)
                .imageUrl(imageUrl)
                .thumbnailUrl(thumbnailUrl)
                .isPrimary(isPrimary)
                .displayOrder(currentCount) // append to end
                .build();

        DressImage saved = imageRepository.save(image);
        log.info("Image uploaded for dress {}: {} (primary={})",
                dressPublicId, saved.getPublicId(), isPrimary);
        return DressImageResponse.from(saved);
    }

    // =========================================================================
    // ADMIN --- Delete dress image
    // =========================================================================

    @Transactional
    public void deleteImage(String dressPublicId, String imagePublicId) {
        findByPublicIdOrThrow(dressPublicId); // verify dress exists

        DressImage image = imageRepository.findByPublicId(imagePublicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Image not found: " + imagePublicId));

        boolean wasPrimary = image.getIsPrimary();
        imageRepository.delete(image);

        // If deleted image was primary, promote the next image
        if (wasPrimary) {
            imageRepository.findAllByDress_DressIdOrderByDisplayOrderAsc(
                            image.getDress().getDressId())
                    .stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setIsPrimary(true);
                        imageRepository.save(next);
                    });
        }

        log.info("Image deleted: {} from dress {}", imagePublicId, dressPublicId);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Dress findByPublicIdOrThrow(String publicId) {
        return dressRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dress not found: " + publicId));
    }

    private void validateFulfillmentOption(FulfillmentOptionRequest req) {
        if ("RENTAL".equals(req.fulfillmentType())) {
            if (req.rentalPricePerDay() == null) {
                throw new IllegalArgumentException(
                        "rentalPricePerDay is required for RENTAL fulfillment type");
            }
            if (req.rentalDeposit() == null) {
                throw new IllegalArgumentException(
                        "rentalDeposit is required for RENTAL fulfillment type");
            }
        }
    }

    private DressFulfillmentOption buildFulfillmentOption(Dress dress,
                                                          FulfillmentOptionRequest req) {
        return DressFulfillmentOption.builder()
                .dress(dress)
                .fulfillmentType(req.fulfillmentType().toUpperCase())
                .priceOverride(req.priceOverride())
                .rentalPricePerDay(req.rentalPricePerDay())
                .rentalDeposit(req.rentalDeposit())
                .rentalPeriodDays(req.rentalPeriodDays() != null
                        ? req.rentalPeriodDays() : 3)
                .isActive(true)
                .build();
    }

    /**
     * Converts a full Cloudinary URL to a thumbnail URL by injecting
     * the w_400,h_400,c_fill transformation before the version/filename segment.
     *
     * Example:
     *   Input:  https://res.cloudinary.com/demo/image/upload/v1234/bridal-shop/dress.jpg
     *   Output: https://res.cloudinary.com/demo/image/upload/w_400,h_400,c_fill/v1234/bridal-shop/dress.jpg
     */
    private String buildThumbnailUrl(String imageUrl) {
        return imageUrl.replace("/upload/", "/upload/w_400,h_400,c_fill/");
    }
}