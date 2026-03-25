package edu.bridalshop.backend.service;

import edu.bridalshop.backend.dto.request.CategoryRequest;
import edu.bridalshop.backend.dto.response.CategoryResponse;
import edu.bridalshop.backend.entity.Category;
import edu.bridalshop.backend.exception.ResourceNotFoundException;
import edu.bridalshop.backend.repository.CategoryRepository;
import edu.bridalshop.backend.util.PayloadSanitizer;
import edu.bridalshop.backend.util.PublicIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final PublicIdGenerator publicIdGenerator;
    private final PayloadSanitizer sanitizer;

    // -------------------------------------------------------------------------
    // PUBLIC --- list all active categories
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActive() {
        return categoryRepository.findAllByIsActiveTrue()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // ADMIN --- create category
    // -------------------------------------------------------------------------
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name      = sanitizer.sanitizeText(request.name());
        String dressType = request.dressType().toUpperCase();

        if (categoryRepository.existsByNameAndDressType(name, dressType)) {
            throw new IllegalArgumentException(
                    "A category named '" + name + "' already exists for type " + dressType);
        }

        Category category = Category.builder()
                .publicId(publicIdGenerator.forCategory())
                .name(name)
                .dressType(dressType)
                .description(request.description() != null
                        ? sanitizer.sanitizeText(request.description()) : null)
                .isActive(true)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created: {} [{}]", saved.getPublicId(), saved.getName());
        return CategoryResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // ADMIN --- update category
    // -------------------------------------------------------------------------
    @Transactional
    public CategoryResponse update(String publicId, CategoryRequest request) {
        Category category = findByPublicIdOrThrow(publicId);

        String name      = sanitizer.sanitizeText(request.name());
        String dressType = request.dressType().toUpperCase();

        // Check duplicate only if name/type actually changed
        boolean nameChanged = !category.getName().equals(name)
                || !category.getDressType().equals(dressType);
        if (nameChanged && categoryRepository.existsByNameAndDressType(name, dressType)) {
            throw new IllegalArgumentException(
                    "A category named '" + name + "' already exists for type " + dressType);
        }

        category.setName(name);
        category.setDressType(dressType);
        category.setDescription(request.description() != null
                ? sanitizer.sanitizeText(request.description()) : null);

        Category saved = categoryRepository.save(category);
        log.info("Category updated: {}", saved.getPublicId());
        return CategoryResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // ADMIN --- soft delete (deactivate)
    // -------------------------------------------------------------------------
    @Transactional
    public void deactivate(String publicId) {
        Category category = findByPublicIdOrThrow(publicId);
        category.setIsActive(false);
        categoryRepository.save(category);
        log.info("Category deactivated: {}", publicId);
    }

    // -------------------------------------------------------------------------
    // Internal helper
    // -------------------------------------------------------------------------
    private Category findByPublicIdOrThrow(String publicId) {
        return categoryRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + publicId));
    }
}