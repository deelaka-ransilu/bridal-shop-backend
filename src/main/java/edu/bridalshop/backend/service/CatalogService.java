package edu.bridalshop.backend.service;

import edu.bridalshop.backend.dto.request.*;
import edu.bridalshop.backend.dto.response.*;
import edu.bridalshop.backend.entity.*;
import edu.bridalshop.backend.enums.ItemCategory;
import edu.bridalshop.backend.enums.SortBy;
import edu.bridalshop.backend.enums.StockUnit;
import edu.bridalshop.backend.enums.VariantStatus;
import edu.bridalshop.backend.exception.EmailAlreadyExistsException;
import edu.bridalshop.backend.exception.ResourceNotFoundException;
import edu.bridalshop.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final DressRepository dressRepository;
    private final DressVariantRepository variantRepository;
    private final DressImageRepository imageRepository;
    private final StockItemRepository stockItemRepository;
    private final StockLevelRepository stockLevelRepository;

    // ================================================================
    // CATEGORY OPERATIONS
    // ================================================================

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        List<Object[]> results = categoryRepository.findAllWithDressCount();

        return results.stream()
                .map(row -> CategoryResponse.builder()
                        .categoryId((Integer) row[0])
                        .name((String) row[1])
                        .dressCount(((Number) row[2]).intValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        long dressCount = dressRepository.count((root, query, cb) ->
                cb.and(
                        cb.equal(root.get("category").get("categoryId"), categoryId),
                        cb.isTrue(root.get("isActive"))
                )
        );

        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .dressCount((int) dressCount)
                .build();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new EmailAlreadyExistsException("Category with this name already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .build();

        category = categoryRepository.save(category);
        log.info("Category created: {}", category.getName());

        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .dressCount(0)
                .build();
    }

    @Transactional
    public CategoryResponse updateCategory(Integer categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // Check if name already exists (excluding current category)
        if (!category.getName().equals(request.getName()) &&
                categoryRepository.existsByName(request.getName())) {
            throw new EmailAlreadyExistsException("Category with this name already exists");
        }

        category.setName(request.getName());
        categoryRepository.save(category);

        log.info("Category updated: {}", category.getName());

        return getCategoryById(categoryId);
    }

    @Transactional
    public void deleteCategory(Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        // Check if category has dresses
        long dressCount = dressRepository.count((root, query, cb) ->
                cb.equal(root.get("category").get("categoryId"), categoryId)
        );

        if (dressCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete category with existing dresses. Please reassign or delete the dresses first."
            );
        }

        categoryRepository.delete(category);
        log.info("Category deleted: {}", category.getName());
    }

    // ================================================================
    // DRESS OPERATIONS
    // ================================================================

    @Transactional(readOnly = true)
    public DressPageResponse browseCatalog(CatalogFilterRequest filter) {
        Specification<Dress> spec = buildDressSpecification(filter);
        Sort sort = buildSort(filter.getSortBy());
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Page<Dress> dressPage = dressRepository.findAll(spec, pageable);

        List<DressResponse> dressResponses = dressPage.getContent().stream()
                .map(this::mapToDressResponse)
                .collect(Collectors.toList());

        return DressPageResponse.builder()
                .dresses(dressResponses)
                .currentPage(dressPage.getNumber())
                .totalPages(dressPage.getTotalPages())
                .totalItems(dressPage.getTotalElements())
                .pageSize(dressPage.getSize())
                .build();
    }

    @Transactional(readOnly = true)
    public DressResponse getDressById(Integer dressId) {
        Dress dress = dressRepository.findById(dressId)
                .orElseThrow(() -> new ResourceNotFoundException("Dress not found"));

        return mapToDressDetailResponse(dress);
    }

    @Transactional
    public DressResponse createDress(DressRequest request) {
        // Validate category exists
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Dress dress = Dress.builder()
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .baseSalePrice(request.getBaseSalePrice())
                .baseRentalPrice(request.getBaseRentalPrice())
                .availableForSale(request.getAvailableForSale())
                .availableForRental(request.getAvailableForRental())
                .isActive(true)
                .orderCount(0)
                .build();

        dress = dressRepository.save(dress);
        log.info("Dress created: {} (ID: {})", dress.getName(), dress.getDressId());

        return mapToDressDetailResponse(dress);
    }

    @Transactional
    public DressResponse updateDress(Integer dressId, DressRequest request) {
        Dress dress = dressRepository.findById(dressId)
                .orElseThrow(() -> new ResourceNotFoundException("Dress not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        dress.setCategory(category);
        dress.setName(request.getName());
        dress.setDescription(request.getDescription());
        dress.setBaseSalePrice(request.getBaseSalePrice());
        dress.setBaseRentalPrice(request.getBaseRentalPrice());
        dress.setAvailableForSale(request.getAvailableForSale());
        dress.setAvailableForRental(request.getAvailableForRental());

        dressRepository.save(dress);
        log.info("Dress updated: {} (ID: {})", dress.getName(), dress.getDressId());

        return mapToDressDetailResponse(dress);
    }

    @Transactional
    public void deleteDress(Integer dressId) {
        Dress dress = dressRepository.findById(dressId)
                .orElseThrow(() -> new ResourceNotFoundException("Dress not found"));

        // Soft delete
        dress.setIsActive(false);
        dressRepository.save(dress);

        log.info("Dress soft-deleted: {} (ID: {})", dress.getName(), dress.getDressId());
    }

    // ================================================================
    // HELPER METHODS
    // ================================================================

    private Specification<Dress> buildDressSpecification(CatalogFilterRequest filter) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            // Always filter active dresses
            predicates.add(cb.isTrue(root.get("isActive")));

            // Category filter
            if (filter.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), filter.getCategoryId()));
            }

            // Price range filter (check both sale and rental prices)
            if (filter.getMinPrice() != null || filter.getMaxPrice() != null) {
                jakarta.persistence.criteria.Predicate pricePredicate = null;

                if (filter.getMinPrice() != null && filter.getMaxPrice() != null) {
                    pricePredicate = cb.or(
                            cb.between(root.get("baseSalePrice"), filter.getMinPrice(), filter.getMaxPrice()),
                            cb.between(root.get("baseRentalPrice"), filter.getMinPrice(), filter.getMaxPrice())
                    );
                } else if (filter.getMinPrice() != null) {
                    pricePredicate = cb.or(
                            cb.greaterThanOrEqualTo(root.get("baseSalePrice"), filter.getMinPrice()),
                            cb.greaterThanOrEqualTo(root.get("baseRentalPrice"), filter.getMinPrice())
                    );
                } else {
                    pricePredicate = cb.or(
                            cb.lessThanOrEqualTo(root.get("baseSalePrice"), filter.getMaxPrice()),
                            cb.lessThanOrEqualTo(root.get("baseRentalPrice"), filter.getMaxPrice())
                    );
                }

                predicates.add(pricePredicate);
            }

            // Available for sale/rental filter
            if (filter.getAvailableForSale() != null && filter.getAvailableForSale()) {
                predicates.add(cb.isTrue(root.get("availableForSale")));
            }

            if (filter.getAvailableForRental() != null && filter.getAvailableForRental()) {
                predicates.add(cb.isTrue(root.get("availableForRental")));
            }

            // Search query
            if (filter.getSearchQuery() != null && !filter.getSearchQuery().trim().isEmpty()) {
                String searchPattern = "%" + filter.getSearchQuery().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), searchPattern),
                        cb.like(cb.lower(root.get("description")), searchPattern)
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Sort buildSort(SortBy sortBy) {
        return switch (sortBy) {
            case PRICE_LOW_TO_HIGH -> Sort.by(Sort.Direction.ASC, "baseSalePrice");
            case PRICE_HIGH_TO_LOW -> Sort.by(Sort.Direction.DESC, "baseSalePrice");
            case POPULARITY -> Sort.by(Sort.Direction.DESC, "orderCount");
            case NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    @Transactional
    public DressVariantResponse addDressVariant(Integer dressId, DressVariantRequest request) {
        Dress dress = dressRepository.findById(dressId)
                .orElseThrow(() -> new ResourceNotFoundException("Dress not found"));

        // Check if variant with same size and color already exists
        if (variantRepository.existsByDressDressIdAndSizeAndColor(dressId, request.getSize(), request.getColor())) {
            throw new IllegalStateException("Variant with this size and color already exists for this dress");
        }

        // Check if SKU already exists
        if (stockItemRepository.existsBySku(request.getSku())) {
            throw new IllegalStateException("SKU already exists");
        }

        // Create stock item for this variant
        StockItem stockItem = StockItem.builder()
                .itemType(ItemCategory.DRESS_VARIANT)
                .name(dress.getName() + " - " + request.getSize() + " " + request.getColor())
                .sku(request.getSku())
                .unitOfMeasure(StockUnit.PCS)
                .isStocked(true)
                .build();

        stockItem = stockItemRepository.save(stockItem);

        // Create stock level (quantity = 1 for unique dress)
        StockLevel stockLevel = StockLevel.builder()
                .stockItem(stockItem)
                .quantity(BigDecimal.ONE)
                .build();

        stockLevelRepository.save(stockLevel);

        // Create variant
        DressVariant variant = DressVariant.builder()
                .dress(dress)
                .stockItem(stockItem)
                .size(request.getSize())
                .color(request.getColor())
                .status(VariantStatus.ACTIVE)
                .build();

        variant = variantRepository.save(variant);

        log.info("Variant added to dress {}: {} {}", dressId, request.getSize(), request.getColor());

        return mapToVariantResponse(variant, stockLevel);
    }

    @Transactional
    public void deleteVariant(Integer variantId) {
        DressVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        // TODO: Check if variant is used in any orders before deleting

        // Delete variant (stock item will remain for audit trail)
        variant.setStatus(VariantStatus.RETIRED);
        variantRepository.save(variant);

        log.info("Variant retired: ID {}", variantId);
    }

    // ================================================================
    // DRESS IMAGE OPERATIONS
    // ================================================================

    @Transactional
    public DressImageResponse addDressImage(Integer dressId, DressImageRequest request) {
        Dress dress = dressRepository.findById(dressId)
                .orElseThrow(() -> new ResourceNotFoundException("Dress not found"));

        DressImage image = DressImage.builder()
                .dress(dress)
                .url(request.getUrl())
                .displayOrder(request.getDisplayOrder())
                .build();

        image = imageRepository.save(image);

        log.info("Image added to dress {}: {}", dressId, request.getUrl());

        return DressImageResponse.builder()
                .imageId(image.getImageId())
                .url(image.getUrl())
                .displayOrder(image.getDisplayOrder())
                .build();
    }

    @Transactional
    public void deleteImage(Integer imageId) {
        DressImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        imageRepository.delete(image);

        log.info("Image deleted: ID {}", imageId);
    }

    // ================================================================
    // MAPPER METHODS
    // ================================================================

    private DressResponse mapToDressResponse(Dress dress) {
        // Get first image for listing
        String firstImageUrl = dress.getImages().isEmpty() ? null :
                dress.getImages().stream()
                        .sorted((i1, i2) -> i1.getDisplayOrder().compareTo(i2.getDisplayOrder()))
                        .findFirst()
                        .map(DressImage::getUrl)
                        .orElse(null);

        // Check if any variant is in stock
        boolean hasStock = dress.getVariants().stream()
                .anyMatch(v -> {
                    StockLevel level = stockLevelRepository.findByStockItemStockItemId(v.getStockItem().getStockItemId()).orElse(null);
                    return level != null && level.getQuantity().compareTo(BigDecimal.ZERO) > 0 && v.getStatus() == VariantStatus.ACTIVE;
                });

        return DressResponse.builder()
                .dressId(dress.getDressId())
                .categoryId(dress.getCategory() != null ? dress.getCategory().getCategoryId() : null)
                .categoryName(dress.getCategory() != null ? dress.getCategory().getName() : null)
                .name(dress.getName())
                .description(dress.getDescription())
                .baseSalePrice(dress.getBaseSalePrice())
                .baseRentalPrice(dress.getBaseRentalPrice())
                .availableForSale(dress.getAvailableForSale())
                .availableForRental(dress.getAvailableForRental())
                .orderCount(dress.getOrderCount())
                .createdAt(dress.getCreatedAt())
                .hasAvailableStock(hasStock)
                .images(firstImageUrl != null ? List.of(DressImageResponse.builder().url(firstImageUrl).build()) : List.of())
                .build();
    }

    private DressResponse mapToDressDetailResponse(Dress dress) {
        List<DressVariantResponse> variants = dress.getVariants().stream()
                .map(v -> {
                    StockLevel level = stockLevelRepository.findByStockItemStockItemId(v.getStockItem().getStockItemId()).orElse(null);
                    return mapToVariantResponse(v, level);
                })
                .collect(Collectors.toList());

        List<DressImageResponse> images = dress.getImages().stream()
                .sorted((i1, i2) -> i1.getDisplayOrder().compareTo(i2.getDisplayOrder()))
                .map(img -> DressImageResponse.builder()
                        .imageId(img.getImageId())
                        .url(img.getUrl())
                        .displayOrder(img.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());

        List<String> availableSizes = variantRepository.findDistinctSizesByDressId(dress.getDressId());
        List<String> availableColors = variantRepository.findDistinctColorsByDressId(dress.getDressId());

        boolean hasStock = variants.stream().anyMatch(DressVariantResponse::getInStock);

        return DressResponse.builder()
                .dressId(dress.getDressId())
                .categoryId(dress.getCategory() != null ? dress.getCategory().getCategoryId() : null)
                .categoryName(dress.getCategory() != null ? dress.getCategory().getName() : null)
                .name(dress.getName())
                .description(dress.getDescription())
                .baseSalePrice(dress.getBaseSalePrice())
                .baseRentalPrice(dress.getBaseRentalPrice())
                .availableForSale(dress.getAvailableForSale())
                .availableForRental(dress.getAvailableForRental())
                .orderCount(dress.getOrderCount())
                .createdAt(dress.getCreatedAt())
                .variants(variants)
                .images(images)
                .hasAvailableStock(hasStock)
                .availableSizes(availableSizes)
                .availableColors(availableColors)
                .build();
    }

    private DressVariantResponse mapToVariantResponse(DressVariant variant, StockLevel stockLevel) {
        boolean inStock = stockLevel != null &&
                stockLevel.getQuantity().compareTo(BigDecimal.ZERO) > 0 &&
                variant.getStatus() == VariantStatus.ACTIVE;

        boolean availableForRental = variant.getStatus() == VariantStatus.ACTIVE &&
                variant.getStatus() != VariantStatus.RENTED_OUT;

        return DressVariantResponse.builder()
                .variantId(variant.getVariantId())
                .stockItemId(variant.getStockItem().getStockItemId())
                .size(variant.getSize())
                .color(variant.getColor())
                .status(variant.getStatus())
                .inStock(inStock)
                .availableForRental(availableForRental)
                .build();
    }
}