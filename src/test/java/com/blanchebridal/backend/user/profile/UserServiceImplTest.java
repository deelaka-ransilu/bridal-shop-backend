package com.blanchebridal.backend.user.profile;

import com.blanchebridal.backend.exception.ResourceNotFoundException;
import com.blanchebridal.backend.user.entity.User;
import com.blanchebridal.backend.user.repository.UserRepository;
import com.blanchebridal.backend.user.entity.UserRole;
import com.blanchebridal.backend.user.dto.req.MeasurementsRequest;
import com.blanchebridal.backend.user.dto.req.UpdateProfileRequest;
import com.blanchebridal.backend.user.dto.res.MeasurementsResponse;
import com.blanchebridal.backend.user.dto.res.UserResponse;
import com.blanchebridal.backend.user.entity.CustomerMeasurement;
import com.blanchebridal.backend.user.repository.CustomerMeasurementRepository;
import com.blanchebridal.backend.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerMeasurementRepository measurementRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User customer;
    private User admin;
    private UUID customerId;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        adminId    = UUID.randomUUID();

        customer = User.builder()
                .id(customerId)
                .email("customer@test.com")
                .firstName("Nimasha")
                .lastName("Perera")
                .phone("0771234567")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        admin = User.builder()
                .id(adminId)
                .email("admin@blanchebridal.com")
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PROFILE TESTS
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getProfile: success — returns correct user data")
    void getProfile_success() {
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        UserResponse result = userService.getProfile(customerId);

        // Check every field maps correctly from User → UserResponse
        assertThat(result.id()).isEqualTo(customerId);
        assertThat(result.email()).isEqualTo("customer@test.com");
        assertThat(result.firstName()).isEqualTo("Nimasha");
        assertThat(result.lastName()).isEqualTo("Perera");
        assertThat(result.role()).isEqualTo("CUSTOMER");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("getProfile: fail — unknown user throws ResourceNotFoundException")
    void getProfile_unknownUser_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("updateProfile: success — updates firstName, lastName, phone")
    void updateProfile_success() {
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        // Return whatever user was passed in to save()
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("Sanduni", "Silva", "0779999999");
        UserResponse result = userService.updateProfile(customerId, request);

        // The returned profile should have the new values
        assertThat(result.firstName()).isEqualTo("Sanduni");
        assertThat(result.lastName()).isEqualTo("Silva");
        assertThat(result.phone()).isEqualTo("0779999999");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MEASUREMENTS TESTS
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("saveMeasurements: success — creates new measurement record")
    void saveMeasurements_success() {
        // ARRANGE: both customer and admin exist
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        // countByCustomer_Id returns 0 — so public ID will be MEAS-0001
        when(measurementRepository.countByCustomer_Id(customerId)).thenReturn(0L);

        // Build a fake saved measurement to return from save()
        CustomerMeasurement saved = CustomerMeasurement.builder()
                .id(UUID.randomUUID())
                .publicId("MEAS-0001")
                .customer(customer)
                .recordedBy(admin)
                .fullBust(new BigDecimal("90.00"))
                .measuredAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(measurementRepository.save(any(CustomerMeasurement.class))).thenReturn(saved);

        // Build the request with just a few fields filled (rest are null — all are optional)
        MeasurementsRequest request = new MeasurementsRequest(
                null, null,                // heightWithShoes, hollowToHem
                new BigDecimal("90.00"),   // fullBust
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                "Standard fitting"         // notes
        );

        // ACT
        MeasurementsResponse result = userService.saveMeasurements(customerId, adminId, request);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.publicId()).isEqualTo("MEAS-0001");
        assertThat(result.fullBust()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(result.customerId()).isEqualTo(customerId);
    }

    @Test
    @DisplayName("saveMeasurements: publicId increments — second record is MEAS-0002")
    void saveMeasurements_publicIdIncrements() {
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        // countByCustomer_Id returns 1 — meaning 1 record already exists
        // So the new one should be MEAS-0002
        when(measurementRepository.countByCustomer_Id(customerId)).thenReturn(1L);

        CustomerMeasurement saved = CustomerMeasurement.builder()
                .id(UUID.randomUUID())
                .publicId("MEAS-0002")   // <-- second record
                .customer(customer)
                .recordedBy(admin)
                .measuredAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(measurementRepository.save(any(CustomerMeasurement.class))).thenReturn(saved);

        MeasurementsRequest request = new MeasurementsRequest(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, "Second fitting");

        MeasurementsResponse result = userService.saveMeasurements(customerId, adminId, request);

        assertThat(result.publicId()).isEqualTo("MEAS-0002");
    }

    @Test
    @DisplayName("getMeasurements: returns list ordered by measuredAt descending")
    void getMeasurements_returnsList() {
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        CustomerMeasurement m1 = CustomerMeasurement.builder()
                .id(UUID.randomUUID()).publicId("MEAS-0001")
                .customer(customer).recordedBy(admin)
                .measuredAt(LocalDateTime.now().minusDays(7))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        CustomerMeasurement m2 = CustomerMeasurement.builder()
                .id(UUID.randomUUID()).publicId("MEAS-0002")
                .customer(customer).recordedBy(admin)
                .measuredAt(LocalDateTime.now())   // more recent
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        // The repository returns newest first (OrderByMeasuredAtDesc)
        when(measurementRepository.findByCustomer_IdOrderByMeasuredAtDesc(customerId))
                .thenReturn(List.of(m2, m1));

        List<MeasurementsResponse> result = userService.getMeasurements(customerId);

        assertThat(result).hasSize(2);
        // Most recent record comes first
        assertThat(result.get(0).publicId()).isEqualTo("MEAS-0002");
        assertThat(result.get(1).publicId()).isEqualTo("MEAS-0001");
    }
}