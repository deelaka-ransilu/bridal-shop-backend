package edu.bridalshop.backend.controller;

import edu.bridalshop.backend.dto.request.DressVariantRequest;
import edu.bridalshop.backend.dto.response.DressVariantResponse;
import edu.bridalshop.backend.dto.response.MessageResponse;
import edu.bridalshop.backend.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dresses/{dressId}/variants")
@RequiredArgsConstructor
public class DressVariantController {

    private final CatalogService catalogService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DressVariantResponse> addVariant(
            @PathVariable Integer dressId,
            @Valid @RequestBody DressVariantRequest request) {
        DressVariantResponse variant = catalogService.addDressVariant(dressId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(variant);
    }

    @DeleteMapping("/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteVariant(@PathVariable Integer variantId) {
        catalogService.deleteVariant(variantId);
        return ResponseEntity.ok(new MessageResponse("Variant deleted successfully"));
    }
}