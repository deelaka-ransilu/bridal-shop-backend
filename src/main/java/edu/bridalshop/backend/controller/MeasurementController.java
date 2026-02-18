package edu.bridalshop.backend.controller;

import edu.bridalshop.backend.dto.request.MeasurementRequest;
import edu.bridalshop.backend.dto.response.CustomerProfileResponse;
import edu.bridalshop.backend.dto.response.MeasurementResponse;
import edu.bridalshop.backend.security.CustomUserDetails;
import edu.bridalshop.backend.service.MeasurementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class MeasurementController {

    private final MeasurementService measurementService;

    // ================================================================
    // GET CUSTOMER PROFILE
    // Admin: any customer
    // Employee: only assigned order's customer
    // Customer: own profile only
    //
    // GET /customers/{customerId}/profile
    // ================================================================

    @GetMapping("/{customerId}/profile")
    public ResponseEntity<CustomerProfileResponse> getCustomerProfile(
            @PathVariable Integer customerId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        CustomerProfileResponse profile =
                measurementService.getCustomerProfile(customerId, currentUser);

        return ResponseEntity.ok(profile);
    }

    // ================================================================
    // GET LATEST MEASUREMENT ONLY
    // Admin: any customer
    // Employee: only assigned order's customer
    // Customer: own measurement only
    //
    // GET /customers/{customerId}/measurements
    // ================================================================

    @GetMapping("/{customerId}/measurements")
    public ResponseEntity<MeasurementResponse> getLatestMeasurement(
            @PathVariable Integer customerId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        MeasurementResponse measurement =
                measurementService.getLatestMeasurement(customerId, currentUser);

        return ResponseEntity.ok(measurement);
    }

    // ================================================================
    // ADD NEW MEASUREMENT (Admin only)
    // Always inserts new record, marks previous one inactive
    //
    // POST /customers/measurements
    // ================================================================

    @PostMapping("/measurements")
    public ResponseEntity<MeasurementResponse> addMeasurement(
            @Valid @RequestBody MeasurementRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        MeasurementResponse measurement =
                measurementService.addMeasurement(request, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(measurement);
    }
}
