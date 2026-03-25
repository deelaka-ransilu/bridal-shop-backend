package edu.bridalshop.backend.controller;

import edu.bridalshop.backend.dto.request.DressRequest;
import edu.bridalshop.backend.dto.response.DressImageResponse;
import edu.bridalshop.backend.dto.response.DressResponse;
import edu.bridalshop.backend.dto.response.MessageResponse;
import edu.bridalshop.backend.service.DressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/dresses")
@RequiredArgsConstructor
public class DressController {

    private final DressService dressService;

    // -------------------------------------------------------------------------
    // GET /api/dresses --- Public (prices hidden)
    // Optional filters: ?category=cat_xxx  ?dressType=BRIDAL  ?color=ivory
    // -------------------------------------------------------------------------
    @GetMapping
    public ResponseEntity<List<DressResponse>> getCatalog(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String dressType,
            @RequestParam(required = false) String color) {
        return ResponseEntity.ok(dressService.getCatalog(category, dressType, color));
    }

    // -------------------------------------------------------------------------
    // GET /api/dresses/featured --- Public (CUSTOM showcase dresses)
    // -------------------------------------------------------------------------
    @GetMapping("/featured")
    public ResponseEntity<List<DressResponse>> getFeatured() {
        return ResponseEntity.ok(dressService.getFeatured());
    }

    // -------------------------------------------------------------------------
    // GET /api/dresses/{publicId} --- Public (prices hidden unless authenticated)
    // -------------------------------------------------------------------------
    @GetMapping("/{publicId}")
    public ResponseEntity<DressResponse> getByPublicId(
            @PathVariable String publicId,
            Authentication authentication) {
        boolean authenticated = authentication != null && authentication.isAuthenticated();
        return ResponseEntity.ok(dressService.getByPublicId(publicId, authenticated));
    }

    // -------------------------------------------------------------------------
    // POST /api/dresses --- ADMIN only
    // -------------------------------------------------------------------------
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DressResponse> create(
            @Valid @RequestBody DressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dressService.create(request));
    }

    // -------------------------------------------------------------------------
    // PUT /api/dresses/{publicId} --- ADMIN only
    // -------------------------------------------------------------------------
    @PutMapping("/{publicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DressResponse> update(
            @PathVariable String publicId,
            @Valid @RequestBody DressRequest request) {
        return ResponseEntity.ok(dressService.update(publicId, request));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/dresses/{publicId} --- ADMIN only (soft delete)
    // -------------------------------------------------------------------------
    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deactivate(@PathVariable String publicId) {
        dressService.deactivate(publicId);
        return ResponseEntity.ok(new MessageResponse("Dress deactivated successfully"));
    }

    // -------------------------------------------------------------------------
    // POST /api/dresses/{publicId}/images --- ADMIN only (max 5 images)
    // -------------------------------------------------------------------------
    @PostMapping(value = "/{publicId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DressImageResponse> uploadImage(
            @PathVariable String publicId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dressService.uploadImage(publicId, file));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/dresses/{publicId}/images/{imageId} --- ADMIN only
    // -------------------------------------------------------------------------
    @DeleteMapping("/{publicId}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteImage(
            @PathVariable String publicId,
            @PathVariable String imageId) {
        dressService.deleteImage(publicId, imageId);
        return ResponseEntity.ok(new MessageResponse("Image deleted successfully"));
    }
}