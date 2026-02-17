package edu.bridalshop.backend.controller;

import edu.bridalshop.backend.dto.request.DressImageRequest;
import edu.bridalshop.backend.dto.response.DressImageResponse;
import edu.bridalshop.backend.dto.response.MessageResponse;
import edu.bridalshop.backend.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dresses/{dressId}/images")
@RequiredArgsConstructor
public class DressImageController {

    private final CatalogService catalogService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DressImageResponse> addImage(
            @PathVariable Integer dressId,
            @Valid @RequestBody DressImageRequest request) {
        DressImageResponse image = catalogService.addDressImage(dressId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(image);
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteImage(@PathVariable Integer imageId) {
        catalogService.deleteImage(imageId);
        return ResponseEntity.ok(new MessageResponse("Image deleted successfully"));
    }
}