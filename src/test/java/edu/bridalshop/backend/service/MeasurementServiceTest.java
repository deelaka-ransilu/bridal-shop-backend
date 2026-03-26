package edu.bridalshop.backend.service;

import edu.bridalshop.backend.dto.request.MeasurementRequest;
import edu.bridalshop.backend.dto.response.MeasurementResponse;
import edu.bridalshop.backend.entity.CustomerMeasurement;
import edu.bridalshop.backend.entity.User;
import edu.bridalshop.backend.entity.UserRole;
import edu.bridalshop.backend.exception.ResourceNotFoundException;
import edu.bridalshop.backend.repository.CustomerMeasurementRepository;
import edu.bridalshop.backend.repository.UserRepository;
import edu.bridalshop.backend.util.PayloadSanitizer;
import edu.bridalshop.backend.util.PublicIdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeasurementServiceTest {

    @Mock CustomerMeasurementRepository measurementRepository;
    @Mock UserRepository                userRepository;
    @Mock PublicIdGenerator             publicIdGenerator;
    @Mock PayloadSanitizer              sanitizer;

    @InjectMocks MeasurementService service;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User buildAdmin() {
        return User.builder()
                .userId(1).publicId("usr_admin")
                .email("admin@test.com").fullName("Test Admin")
                .role(UserRole.ADMIN).isActive(true).build();
    }

    private User buildEmployee() {
        return User.builder()
                .userId(3).publicId("usr_emp")
                .email("emp@test.com").fullName("Test Employee")
                .role(UserRole.EMPLOYEE).isActive(true).build();
    }

    private User buildCustomer() {
        return User.builder()
                .userId(2).publicId("usr_cust1")
                .email("cust@test.com").fullName("Test Customer")
                .role(UserRole.CUSTOMER).isActive(true).build();
    }

    private User buildOtherCustomer() {
        return User.builder()
                .userId(4).publicId("usr_cust2")
                .email("other@test.com").fullName("Other Customer")
                .role(UserRole.CUSTOMER).isActive(true).build();
    }

    private CustomerMeasurement buildMeasurement(User customer, User admin) {
        return CustomerMeasurement.builder()
                .publicId("msr_test")
                .customer(customer)
                .recordedBy(admin)
                .measuredAt(LocalDateTime.now()) // <-- CHANGED HERE
                .fullBust(new BigDecimal("90.0"))
                .naturalWaist(new BigDecimal("70.0"))
                .fullHip(new BigDecimal("95.0"))
                .build();
    }

    private MeasurementRequest buildMeasurementRequest() {
        return new MeasurementRequest(
                LocalDateTime.now(), null,
                new BigDecimal("165.0"), // heightWithShoes
                new BigDecimal("140.0"), // hollowToHem
                new BigDecimal("90.0"),  // fullBust
                new BigDecimal("80.0"),  // underBust
                new BigDecimal("70.0"),  // naturalWaist
                new BigDecimal("95.0"),  // fullHip
                new BigDecimal("38.0"),  // shoulderWidth
                new BigDecimal("42.0"),  // torsoLength
                new BigDecimal("55.0"),  // thighCircumference
                new BigDecimal("60.0"),  // waistToKnee
                new BigDecimal("100.0"), // waistToFloor
                new BigDecimal("40.0"),  // armhole
                new BigDecimal("28.0"),  // bicepCircumference
                new BigDecimal("24.0"),  // elbowCircumference
                new BigDecimal("16.0"),  // wristCircumference
                new BigDecimal("58.0"),  // sleeveLength
                new BigDecimal("88.0"),  // upperBust
                new BigDecimal("20.0"),  // bustApexDistance
                new BigDecimal("26.0"),  // shoulderToBustPoint
                new BigDecimal("35.0"),  // neckCircumference
                new BigDecimal("50.0")   // trainLength
        );
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    void getAll_adminRequestsAnyCustomer_returnsMeasurementList() {
        User admin    = buildAdmin();
        User customer = buildCustomer();
        CustomerMeasurement m = buildMeasurement(customer, admin);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(measurementRepository.findAllByCustomer_PublicIdOrderByMeasuredAtDesc("usr_cust1"))
                .thenReturn(List.of(m));

        List<MeasurementResponse> result = service.getAll("usr_cust1", "admin@test.com");

        assertEquals(1, result.size());
        assertEquals("msr_test", result.get(0).publicId());
    }

    @Test
    void getAll_employeeRequestsAnyCustomer_returnsMeasurementList() {
        User employee = buildEmployee();
        User customer = buildCustomer();
        CustomerMeasurement m = buildMeasurement(customer, buildAdmin());

        when(userRepository.findByEmail("emp@test.com")).thenReturn(Optional.of(employee));
        when(measurementRepository.findAllByCustomer_PublicIdOrderByMeasuredAtDesc("usr_cust1"))
                .thenReturn(List.of(m));

        List<MeasurementResponse> result = service.getAll("usr_cust1", "emp@test.com");

        assertFalse(result.isEmpty());
    }

    @Test
    void getAll_customerRequestsOwnData_returnsOwnList() {
        User customer = buildCustomer();
        CustomerMeasurement m = buildMeasurement(customer, buildAdmin());

        when(userRepository.findByEmail("cust@test.com")).thenReturn(Optional.of(customer));
        when(measurementRepository.findAllByCustomer_PublicIdOrderByMeasuredAtDesc("usr_cust1"))
                .thenReturn(List.of(m));

        // publicId matches customer's own publicId — should succeed
        List<MeasurementResponse> result = service.getAll("usr_cust1", "cust@test.com");

        assertEquals(1, result.size());
    }

    @Test
    void getAll_customerRequestsOtherCustomerData_throwsAccessDeniedException() {
        User customer = buildCustomer(); // publicId = "usr_cust1"

        when(userRepository.findByEmail("cust@test.com")).thenReturn(Optional.of(customer));

        // Requesting "usr_cust2" but logged in as "usr_cust1"
        assertThrows(AccessDeniedException.class,
                () -> service.getAll("usr_cust2", "cust@test.com"));
    }

    // ── getOne ────────────────────────────────────────────────────────────────

    @Test
    void getOne_validMeasurementCorrectCustomerScoping_returnsMeasurementResponse() {
        User admin    = buildAdmin();
        User customer = buildCustomer();
        CustomerMeasurement m = buildMeasurement(customer, admin);

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(measurementRepository.findByPublicIdAndCustomer_PublicId("msr_test", "usr_cust1"))
                .thenReturn(Optional.of(m));

        MeasurementResponse result = service.getOne("usr_cust1", "msr_test", "admin@test.com");

        assertEquals("msr_test", result.publicId());
    }

    @Test
    void getOne_measurementBelongsToDifferentCustomer_throwsResourceNotFoundException() {
        User admin = buildAdmin();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(measurementRepository.findByPublicIdAndCustomer_PublicId("msr_test", "usr_cust2"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getOne("usr_cust2", "msr_test", "admin@test.com"));
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_validAdminCreatesMeasurementForCustomer_returnsSavedResponse() {
        User admin    = buildAdmin();
        User customer = buildCustomer();
        MeasurementRequest request = buildMeasurementRequest();

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByPublicId("usr_cust1")).thenReturn(Optional.of(customer));
        when(publicIdGenerator.forMeasurement()).thenReturn("msr_newid");
        when(measurementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MeasurementResponse result = service.create("usr_cust1", request, "admin@test.com");

        assertNotNull(result.publicId());
        verify(measurementRepository).save(any(CustomerMeasurement.class));
    }

    @Test
    void create_customerPublicIdNotFound_throwsResourceNotFoundException() {
        User admin = buildAdmin();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByPublicId("usr_notexist")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create("usr_notexist", buildMeasurementRequest(), "admin@test.com"));
    }

    @Test
    void create_targetUserIsNotCustomerRole_throwsIllegalArgumentException() {
        User admin    = buildAdmin();
        User employee = buildEmployee(); // role = EMPLOYEE, not CUSTOMER

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByPublicId("usr_emp")).thenReturn(Optional.of(employee));

        assertThrows(IllegalArgumentException.class,
                () -> service.create("usr_emp", buildMeasurementRequest(), "admin@test.com"));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_validAdminUpdatesMeasurement_returnsUpdatedResponse() {
        User admin    = buildAdmin();
        User customer = buildCustomer();
        CustomerMeasurement existing = buildMeasurement(customer, admin);
        MeasurementRequest request = buildMeasurementRequest();

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(measurementRepository.findByPublicIdAndCustomer_PublicId("msr_test", "usr_cust1"))
                .thenReturn(Optional.of(existing));
        when(measurementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MeasurementResponse result = service.update("usr_cust1", "msr_test", request, "admin@test.com");

        assertNotNull(result);
        verify(measurementRepository).save(existing);
    }

    @Test
    void update_measurementNotFoundForCustomer_throwsResourceNotFoundException() {
        User admin = buildAdmin();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(measurementRepository.findByPublicIdAndCustomer_PublicId("msr_notexist", "usr_cust1"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update("usr_cust1", "msr_notexist", buildMeasurementRequest(), "admin@test.com"));
    }
}