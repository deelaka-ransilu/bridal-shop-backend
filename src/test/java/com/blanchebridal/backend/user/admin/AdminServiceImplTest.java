package com.blanchebridal.backend.user.admin;

import com.blanchebridal.backend.exception.ConflictException;
import com.blanchebridal.backend.exception.ResourceNotFoundException;
import com.blanchebridal.backend.user.entity.User;
import com.blanchebridal.backend.user.repository.UserRepository;
import com.blanchebridal.backend.user.entity.UserRole;
import com.blanchebridal.backend.user.dto.req.CreateUserRequest;
import com.blanchebridal.backend.user.dto.res.UserResponse;
import com.blanchebridal.backend.user.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User adminUser;
    private User employeeUser;
    private User customerUser;

    @BeforeEach
    void setUp() {
        // Build sample users for each role — reused across tests
        adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@blanchebridal.com")
                .passwordHash("hashed")
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        employeeUser = User.builder()
                .id(UUID.randomUUID())
                .email("employee@blanchebridal.com")
                .passwordHash("hashed")
                .firstName("Employee")
                .lastName("User")
                .role(UserRole.EMPLOYEE)
                .isActive(true)
                .build();

        customerUser = User.builder()
                .id(UUID.randomUUID())
                .email("customer@test.com")
                .passwordHash("hashed")
                .firstName("Nimasha")
                .lastName("Perera")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ADMIN MANAGEMENT TESTS (done by superadmin)
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("listAdmins: returns all admin users")
    void listAdmins_returnsAllAdmins() {
        // ARRANGE: fake the database returning a list with one admin
        when(userRepository.findByRole(UserRole.ADMIN))
                .thenReturn(List.of(adminUser));

        // ACT
        List<UserResponse> result = adminService.listAdmins();

        // ASSERT: list has 1 item, and that item has the correct email
        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("admin@blanchebridal.com");
        assertThat(result.get(0).role()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("createAdmin: success — creates a new admin account")
    void createAdmin_success() {
        // ARRANGE
        when(userRepository.existsByEmail("newadmin@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        CreateUserRequest request = new CreateUserRequest(
                "newadmin@test.com", "password123", "New", "Admin", "0771234567");

        // ACT
        UserResponse result = adminService.createAdmin(request);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.role()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("createAdmin: fail — duplicate email throws ConflictException")
    void createAdmin_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("newadmin@test.com")).thenReturn(true);

        CreateUserRequest request = new CreateUserRequest(
                "newadmin@test.com", "password123", "New", "Admin", "0771234567");

        assertThatThrownBy(() -> adminService.createAdmin(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    @DisplayName("deactivateAdmin: success — sets isActive to false")
    void deactivateAdmin_success() {
        // ARRANGE: admin exists and is currently active
        when(userRepository.findById(adminUser.getId()))
                .thenReturn(Optional.of(adminUser));

        // When save is called, return the same user (now deactivated)
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        // ^ inv.getArgument(0) means "return whatever was passed in to save()"
        //   This way we see the actual modified user, not a hardcoded one

        // ACT
        UserResponse result = adminService.deactivateAdmin(adminUser.getId());

        // ASSERT: isActive should now be false
        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("deactivateAdmin: fail — wrong role throws ResourceNotFoundException")
    void deactivateAdmin_wrongRole_throwsNotFound() {
        // ARRANGE: try to deactivate a CUSTOMER using the admin deactivation method
        // This should fail because we check the role
        when(userRepository.findById(customerUser.getId()))
                .thenReturn(Optional.of(customerUser));

        assertThatThrownBy(() -> adminService.deactivateAdmin(customerUser.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // EMPLOYEE MANAGEMENT TESTS (done by admin)
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("listEmployees: returns all employee users")
    void listEmployees_returnsAllEmployees() {
        when(userRepository.findByRole(UserRole.EMPLOYEE))
                .thenReturn(List.of(employeeUser));

        List<UserResponse> result = adminService.listEmployees();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).role()).isEqualTo("EMPLOYEE");
    }

    @Test
    @DisplayName("createEmployee: success — creates new employee account")
    void createEmployee_success() {
        when(userRepository.existsByEmail("emp@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(employeeUser);

        CreateUserRequest request = new CreateUserRequest(
                "emp@test.com", "password123", "New", "Employee", "0771234567");

        UserResponse result = adminService.createEmployee(request);

        assertThat(result.role()).isEqualTo("EMPLOYEE");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CUSTOMER VIEW TESTS (done by admin)
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("listCustomers: returns all customers")
    void listCustomers_returnsAll() {
        when(userRepository.findByRole(UserRole.CUSTOMER))
                .thenReturn(List.of(customerUser));

        List<UserResponse> result = adminService.listCustomers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("customer@test.com");
    }

    @Test
    @DisplayName("getCustomer: fail — non-existent ID throws ResourceNotFoundException")
    void getCustomer_notFound_throwsException() {
        UUID randomId = UUID.randomUUID();
        // Optional.empty() means nothing was found in the database
        when(userRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getCustomer(randomId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found");
    }
}