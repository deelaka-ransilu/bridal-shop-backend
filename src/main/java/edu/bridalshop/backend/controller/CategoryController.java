package edu.bridalshop.backend.controller;

import edu.bridalshop.backend.dto.request.CategoryRequest;
import edu.bridalshop.backend.dto.response.CategoryResponse;
import edu.bridalshop.backend.dto.response.MessageResponse;
import edu.bridalshop.backend.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // -------------------------------------------------------------------------
    // GET /api/categories --- Public
    // -------------------------------------------------------------------------
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllActive() {
        return ResponseEntity.ok(categoryService.getAllActive());
    }

    // -------------------------------------------------------------------------
    // POST /api/categories --- ADMIN only
    // -------------------------------------------------------------------------
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.create(request));
    }

    // -------------------------------------------------------------------------
    // PUT /api/categories/{publicId} --- ADMIN only
    // -------------------------------------------------------------------------
    @PutMapping("/{publicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable String publicId,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(publicId, request));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/categories/{publicId} --- ADMIN only (soft delete)
    // -------------------------------------------------------------------------
    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deactivate(@PathVariable String publicId) {
        categoryService.deactivate(publicId);
        return ResponseEntity.ok(new MessageResponse("Category deactivated successfully"));
    }
}