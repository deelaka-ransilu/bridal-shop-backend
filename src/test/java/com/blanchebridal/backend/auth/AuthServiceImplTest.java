package com.blanchebridal.backend.auth;

// ─── What these imports do ────────────────────────────────────────────────────
// JUnit 5: @Test marks a method as a test. @BeforeEach runs before every test.
// Mockito: lets us create fake versions of classes (mocks) so we don't need
//          a real database. When we call a mock method, we decide what it returns.
// AssertJ: gives us readable assertions like assertThat(x).isEqualTo(y)
// ─────────────────────────────────────────────────────────────────────────────

import com.blanchebridal.backend.auth.dto.req.LoginRequest;
import com.blanchebridal.backend.auth.dto.req.RegisterRequest;
import com.blanchebridal.backend.auth.dto.res.AuthResponse;
import com.blanchebridal.backend.auth.security.JwtUtil;
import com.blanchebridal.backend.auth.service.impl.AuthServiceImpl;
import com.blanchebridal.backend.exception.ConflictException;
import com.blanchebridal.backend.exception.UnauthorizedException;
import com.blanchebridal.backend.user.entity.User;
import com.blanchebridal.backend.user.repository.UserRepository;
import com.blanchebridal.backend.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// @ExtendWith tells JUnit to use Mockito when running this test class
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    // ── @Mock creates a FAKE version of these classes ─────────────────────────
    // They don't do anything by default — we tell them what to return in each test
    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    // ── @InjectMocks creates a REAL AuthServiceImpl but injects the mocks above ─
    // So when AuthServiceImpl calls userRepository.save(), it calls our fake one
    @InjectMocks
    private AuthServiceImpl authService;

    // ── A reusable User object we build once and use in many tests ────────────
    private User testUser;

    // @BeforeEach runs this method before EVERY single test below
    // Think of it as "reset the state before each test"
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("customer@test.com")
                .passwordHash("$2a$10$hashedpassword")
                .firstName("Nimasha")
                .lastName("Perera")
                .phone("0771234567")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // REGISTER TESTS
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    // @DisplayName gives the test a human-readable name shown in test results
    @DisplayName("register: success — new email creates customer and returns token")
    void register_success() {
        // ── ARRANGE: set up what our fake objects should return ───────────────
        // "When someone asks if this email exists, return false (it doesn't exist yet)"
        when(userRepository.existsByEmail("customer@test.com")).thenReturn(false);

        // "When someone tries to encode a password, return this fake hash"
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedpassword");

        // "When someone tries to save any User object, return our testUser"
        // any(User.class) means "I don't care what User is passed in, just return testUser"
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // "When someone asks for a token for any user, return this fake token string"
        when(jwtUtil.generateToken(any(User.class))).thenReturn("fake.jwt.token");

        // ── ACT: call the real method we are testing ──────────────────────────
        RegisterRequest request = new RegisterRequest(
                "customer@test.com", "password123", "Nimasha", "Perera", "0771234567");
        AuthResponse response = authService.register(request);

        // ── ASSERT: check the result is what we expect ────────────────────────
        // assertThat is from AssertJ — reads like English
        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("fake.jwt.token");
        assertThat(response.role()).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("register: fail — duplicate email throws ConflictException")
    void register_duplicateEmail_throwsConflict() {
        // ARRANGE: pretend the email already exists in the database
        when(userRepository.existsByEmail("customer@test.com")).thenReturn(true);

        RegisterRequest request = new RegisterRequest(
                "customer@test.com", "password123", "Nimasha", "Perera", "0771234567");

        // assertThatThrownBy checks that calling this code THROWS an exception
        // isInstanceOf checks it's the RIGHT type of exception
        // hasMessageContaining checks the message contains this text
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already in use");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // LOGIN TESTS
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("login: success — correct credentials return token")
    void login_success() {
        // ARRANGE
        // "When someone searches for this email, return our testUser wrapped in Optional"
        // Optional.of() means "the user was found"
        when(userRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(testUser));

        // "When someone checks if the password matches the hash, return true"
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword"))
                .thenReturn(true);

        when(jwtUtil.generateToken(any(User.class))).thenReturn("fake.jwt.token");

        // ACT
        LoginRequest request = new LoginRequest("customer@test.com", "password123");
        AuthResponse response = authService.login(request);

        // ASSERT
        assertThat(response.token()).isEqualTo("fake.jwt.token");
        assertThat(response.role()).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("login: fail — email not found throws UnauthorizedException")
    void login_emailNotFound_throwsUnauthorized() {
        // ARRANGE: Optional.empty() means "user was NOT found in the database"
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("nobody@test.com", "password123");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("login: fail — wrong password throws UnauthorizedException")
    void login_wrongPassword_throwsUnauthorized() {
        // ARRANGE: user exists but password check returns false
        when(userRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(testUser));

        // "When someone checks if 'wrongpassword' matches the hash, return false"
        when(passwordEncoder.matches("wrongpassword", "$2a$10$hashedpassword"))
                .thenReturn(false);

        LoginRequest request = new LoginRequest("customer@test.com", "wrongpassword");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("login: fail — deactivated account throws UnauthorizedException")
    void login_deactivatedAccount_throwsUnauthorized() {
        // ARRANGE: user exists but their account is deactivated (isActive = false)
        User deactivatedUser = User.builder()
                .id(UUID.randomUUID())
                .email("customer@test.com")
                .passwordHash("$2a$10$hashedpassword")
                .role(UserRole.CUSTOMER)
                .isActive(false)   // <-- account is deactivated
                .build();

        when(userRepository.findByEmail("customer@test.com"))
                .thenReturn(Optional.of(deactivatedUser));

        LoginRequest request = new LoginRequest("customer@test.com", "password123");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Account is deactivated");
    }
}