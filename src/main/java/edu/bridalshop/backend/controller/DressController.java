package edu.bridalshop.backend.controller;

import edu.bridalshop.backend.dto.request.CatalogFilterRequest;
import edu.bridalshop.backend.dto.request.DressRequest;
import edu.bridalshop.backend.dto.response.DressPageResponse;
import edu.bridalshop.backend.dto.response.DressResponse;
import edu.bridalshop.backend.dto.response.MessageResponse;
import edu.bridalshop.backend.enums.SortBy;
import edu.bridalshop.backend.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/dresses")
@RequiredArgsConstructor
public class DressController {

    private final CatalogService catalogService;

    // PUBLIC ENDPOINTS

    @GetMapping
    public ResponseEntity<DressPageResponse> browseCatalog(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) List<String> colors,
            @RequestParam(required = false) List<String> sizes,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean availableForSale,
            @RequestParam(required = false) Boolean availableForRental,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "NEWEST") SortBy sortBy,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        CatalogFilterRequest filter = CatalogFilterRequest.builder()
                .categoryId(categoryId)
                .colors(colors)
                .sizes(sizes)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .availableForSale(availableForSale)
                .availableForRental(availableForRental)
                .searchQuery(search)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build();

        DressPageResponse response = catalogService.browseCatalog(filter);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{dressId}")
    public ResponseEntity<DressResponse> getDressById(@PathVariable Integer dressId) {
        DressResponse dress = catalogService.getDressById(dressId);
        return ResponseEntity.ok(dress);
    }

    // ADMIN ENDPOINTS

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DressResponse> createDress(@Valid @RequestBody DressRequest request) {
        DressResponse dress = catalogService.createDress(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dress);
    }

    @PutMapping("/{dressId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DressResponse> updateDress(
            @PathVariable Integer dressId,
            @Valid @RequestBody DressRequest request) {
        DressResponse dress = catalogService.updateDress(dressId, request);
        return ResponseEntity.ok(dress);
    }

    @DeleteMapping("/{dressId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteDress(@PathVariable Integer dressId) {
        catalogService.deleteDress(dressId);
        return ResponseEntity.ok(new MessageResponse("Dress deleted successfully"));
    }
}