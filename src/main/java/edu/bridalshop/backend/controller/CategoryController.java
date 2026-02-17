package edu.bridalshop.backend.controller;

import edu.bridalshop.backend.dto.request.CategoryRequest;
import edu.bridalshop.backend.dto.response.CategoryResponse;
import edu.bridalshop.backend.dto.response.MessageResponse;
import edu.bridalshop.backend.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CatalogService catalogService;

    // PUBLIC ENDPOINTS

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = catalogService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Integer categoryId) {
        CategoryResponse category = catalogService.getCategoryById(categoryId);
        return ResponseEntity.ok(category);
    }

    // ADMIN ENDPOINTS

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = catalogService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Integer categoryId,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = catalogService.updateCategory(categoryId, request);
        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteCategory(@PathVariable Integer categoryId) {
        catalogService.deleteCategory(categoryId);
        return ResponseEntity.ok(new MessageResponse("Category deleted successfully"));
    }
}