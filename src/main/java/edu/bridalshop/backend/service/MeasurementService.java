package edu.bridalshop.backend.service;

import edu.bridalshop.backend.dto.request.MeasurementRequest;
import edu.bridalshop.backend.dto.response.CustomerProfileResponse;
import edu.bridalshop.backend.dto.response.MeasurementResponse;
import edu.bridalshop.backend.entity.CustomerMeasurement;
import edu.bridalshop.backend.entity.Employee;
import edu.bridalshop.backend.entity.User;
import edu.bridalshop.backend.enums.UserRole;
import edu.bridalshop.backend.exception.ResourceNotFoundException;
import edu.bridalshop.backend.exception.UnauthorizedException;
import edu.bridalshop.backend.repository.CustomerMeasurementRepository;
import edu.bridalshop.backend.repository.EmployeeRepository;
import edu.bridalshop.backend.repository.UserRepository;
import edu.bridalshop.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeasurementService {

    private final CustomerMeasurementRepository measurementRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    // ================================================================
    // GET CUSTOMER PROFILE
    // Accessible by: Admin (any customer), Employee (assigned only),
    //                Customer (own profile)
    // ================================================================

    @Transactional(readOnly = true)
    public CustomerProfileResponse getCustomerProfile(
            Integer customerId,
            CustomUserDetails currentUser) {

        // Verify customer exists
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Access control check
        checkProfileAccess(customerId, currentUser);

        // Get latest active measurement (null if none recorded yet)
        MeasurementResponse latestMeasurement = measurementRepository
                .findByCustomerUserIdAndIsActiveTrue(customerId)
                .map(this::mapToResponse)
                .orElse(null);

        // Build response based on role
        // Employee gets limited customer info (name, phone, email only - no address)
        return CustomerProfileResponse.builder()
                .userId(customer.getUserId())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .latestMeasurement(latestMeasurement)
                .build();
    }

    // ================================================================
    // GET MEASUREMENT
    // Accessible by: Admin (any customer), Employee (assigned only),
    //                Customer (own)
    // ================================================================

    @Transactional(readOnly = true)
    public MeasurementResponse getLatestMeasurement(
            Integer customerId,
            CustomUserDetails currentUser) {

        checkProfileAccess(customerId, currentUser);

        return measurementRepository
                .findByCustomerUserIdAndIsActiveTrue(customerId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No measurements found for this customer"));
    }

    // ================================================================
    // ADD MEASUREMENT (Admin only)
    // Always inserts new record, marks old one inactive
    // ================================================================

    @Transactional
    public MeasurementResponse addMeasurement(
            MeasurementRequest request,
            CustomUserDetails currentUser) {

        // Only ADMIN can add measurements
        if (currentUser.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new UnauthorizedException("Only admins can record measurements");
        }

        // Verify customer exists and is actually a customer
        User customer = userRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Get the admin user
        User adminUser = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        // Deactivate all current active measurements for this customer
        measurementRepository.deactivateAllForCustomer(request.getCustomerId());

        // Build new measurement record
        CustomerMeasurement measurement = CustomerMeasurement.builder()
                .customer(customer)
                .recordedByUser(adminUser)
                .heightWithShoes(request.getHeightWithShoes())
                .hollowToHem(request.getHollowToHem())
                .fullBust(request.getFullBust())
                .underBust(request.getUnderBust())
                .naturalWaist(request.getNaturalWaist())
                .fullHip(request.getFullHip())
                .shoulderWidth(request.getShoulderWidth())
                .torsoLength(request.getTorsoLength())
                .thighCircumference(request.getThighCircumference())
                .waistToKnee(request.getWaistToKnee())
                .waistToFloor(request.getWaistToFloor())
                .armhole(request.getArmhole())
                .bicepCircumference(request.getBicepCircumference())
                .elbowCircumference(request.getElbowCircumference())
                .wristCircumference(request.getWristCircumference())
                .sleeveLength(request.getSleeveLength())
                .upperBust(request.getUpperBust())
                .bustApexDistance(request.getBustApexDistance())
                .shoulderToBustPoint(request.getShoulderToBustPoint())
                .neckCircumference(request.getNeckCircumference())
                .trainLength(request.getTrainLength())
                .notes(request.getNotes())
                .isActive(true)
                .build();

        measurement = measurementRepository.save(measurement);
        log.info("Measurement recorded for customer {} by admin {}",
                request.getCustomerId(), currentUser.getUserId());

        return mapToResponse(measurement);
    }

    // ================================================================
    // ACCESS CONTROL HELPER
    // ================================================================

    private void checkProfileAccess(Integer customerId, CustomUserDetails currentUser) {
        UserRole role = extractRole(currentUser);

        switch (role) {
            case ADMIN -> {
                // Admin can access any customer profile - no restriction
            }
            case EMPLOYEE -> {
                // Employee can only access profiles of their assigned order's customer
                Employee employee = employeeRepository
                        .findByUser_UserId(currentUser.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("Employee record not found"));

                boolean isAssigned = measurementRepository
                        .isEmployeeAssignedToCustomer(employee.getEmployeeId(), customerId);

                if (!isAssigned) {
                    throw new UnauthorizedException(
                            "You can only view measurements for customers assigned to your orders");
                }
            }
            case CUSTOMER -> {
                // Customer can only view their own profile
                if (!currentUser.getUserId().equals(customerId)) {
                    throw new UnauthorizedException("You can only view your own measurements");
                }
            }
        }
    }

    private UserRole extractRole(CustomUserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> UserRole.valueOf(a.getAuthority().replace("ROLE_", "")))
                .orElseThrow(() -> new UnauthorizedException("No role found"));
    }

    // ================================================================
    // MAPPER
    // ================================================================

    private MeasurementResponse mapToResponse(CustomerMeasurement m) {
        return MeasurementResponse.builder()
                .measurementId(m.getMeasurementId())
                .customerId(m.getCustomer().getUserId())
                .customerName(m.getCustomer().getFullName())
                .recordedByUserId(m.getRecordedByUser() != null
                        ? m.getRecordedByUser().getUserId() : null)
                .recordedByName(m.getRecordedByUser() != null
                        ? m.getRecordedByUser().getFullName() : null)
                .heightWithShoes(m.getHeightWithShoes())
                .hollowToHem(m.getHollowToHem())
                .fullBust(m.getFullBust())
                .underBust(m.getUnderBust())
                .naturalWaist(m.getNaturalWaist())
                .fullHip(m.getFullHip())
                .shoulderWidth(m.getShoulderWidth())
                .torsoLength(m.getTorsoLength())
                .thighCircumference(m.getThighCircumference())
                .waistToKnee(m.getWaistToKnee())
                .waistToFloor(m.getWaistToFloor())
                .armhole(m.getArmhole())
                .bicepCircumference(m.getBicepCircumference())
                .elbowCircumference(m.getElbowCircumference())
                .wristCircumference(m.getWristCircumference())
                .sleeveLength(m.getSleeveLength())
                .upperBust(m.getUpperBust())
                .bustApexDistance(m.getBustApexDistance())
                .shoulderToBustPoint(m.getShoulderToBustPoint())
                .neckCircumference(m.getNeckCircumference())
                .trainLength(m.getTrainLength())
                .notes(m.getNotes())
                .isActive(m.getIsActive())
                .createdAt(m.getCreatedAt())
                .build();
    }
}