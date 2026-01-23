package edu.bridalshop.backend.service;

import edu.bridalshop.backend.dto.request.*;
import edu.bridalshop.backend.dto.response.AuthResponse;
import edu.bridalshop.backend.dto.response.MessageResponse;
import edu.bridalshop.backend.dto.response.TokenRefreshResponse;
import edu.bridalshop.backend.dto.response.UserResponse;
import edu.bridalshop.backend.entity.*;
import edu.bridalshop.backend.enums.OAuthProvider;
import edu.bridalshop.backend.enums.UserRole;
import edu.bridalshop.backend.exception.*;
import edu.bridalshop.backend.repository.*;
import edu.bridalshop.backend.security.JwtTokenProvider;
import edu.bridalshop.backend.util.TokenGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        // Create user
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.CUSTOMER)
                .oauthProvider(OAuthProvider.EMAIL)
                .emailVerified(false)
                .phoneVerified(false)
                .profileCompleted(true) // All fields provided during registration
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        // Generate email verification token
        String token = TokenGenerator.generateToken();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        verificationTokenRepository.save(verificationToken);

        // Send verification email
        emailService.sendEmailVerification(user.getEmail(), token);

        // Generate JWT tokens (allow login but require email verification)
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .user(mapToUserResponse(user))
                .requiresEmailVerification(true)
                .requiresProfileCompletion(false)
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Check if user has password (OAuth users don't have password)
        if (user.getPasswordHash() == null) {
            throw new UnauthorizedException("Please use Google Sign-In for this account");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Check if account is active
        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        // Check if email is verified
        if (!user.getEmailVerified()) {
            throw new EmailNotVerifiedException("Please verify your email before logging in");
        }

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .user(mapToUserResponse(user))
                .requiresEmailVerification(false)
                .requiresProfileCompletion(!user.getProfileCompleted())
                .build();
    }

    @Transactional
    public AuthResponse googleLogin(String googleId, String email, String fullName) {
        // Check if user exists by Google ID
        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> {
                    // Check if email exists (user might have registered with email/password)
                    return userRepository.findByEmail(email)
                            .map(existingUser -> {
                                // Link Google account to existing user
                                existingUser.setGoogleId(googleId);
                                existingUser.setOauthProvider(OAuthProvider.GOOGLE);
                                existingUser.setEmailVerified(true);
                                return userRepository.save(existingUser);
                            })
                            .orElseGet(() -> {
                                // Create new user
                                User newUser = User.builder()
                                        .fullName(fullName)
                                        .email(email)
                                        .googleId(googleId)
                                        .role(UserRole.CUSTOMER)
                                        .oauthProvider(OAuthProvider.GOOGLE)
                                        .emailVerified(true)
                                        .phoneVerified(false)
                                        .profileCompleted(false) // Need to collect phone
                                        .isActive(true)
                                        .build();

                                log.info("New user registered via Google: {}", email);
                                return userRepository.save(newUser);
                            });
                });

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .user(mapToUserResponse(user))
                .requiresEmailVerification(false)
                .requiresProfileCompletion(!user.getProfileCompleted())
                .build();
    }

    @Transactional
    public MessageResponse completeProfile(Integer userId, CompleteProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPhone(request.getPhone());
        user.setProfileCompleted(true);
        userRepository.save(user);

        log.info("Profile completed for user: {}", user.getEmail());

        return new MessageResponse("Profile completed successfully");
    }

    @Transactional
    public MessageResponse verifyEmail(String token) {
        EmailVerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (verificationToken.getVerified()) {
            throw new InvalidTokenException("Email already verified");
        }

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Verification token has expired");
        }

        // Mark token as verified
        verificationToken.setVerified(true);
        verificationTokenRepository.save(verificationToken);

        // Mark user email as verified
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());

        return new MessageResponse("Email verified successfully");
    }

    @Transactional
    public MessageResponse resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getEmailVerified()) {
            throw new IllegalStateException("Email already verified");
        }

        // Generate new token
        String token = TokenGenerator.generateToken();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        verificationTokenRepository.save(verificationToken);
        emailService.sendEmailVerification(user.getEmail(), token);

        return new MessageResponse("Verification email sent");
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Generate reset token
        String token = TokenGenerator.generateToken();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        passwordResetRepository.save(resetToken);
        emailService.sendPasswordResetEmail(user.getEmail(), token);

        return new MessageResponse("Password reset email sent");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));

        if (resetToken.getUsed()) {
            throw new InvalidTokenException("Reset token already used");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Reset token has expired");
        }

        // Update password
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetRepository.save(resetToken);

        // Revoke all existing refresh tokens for security
        refreshTokenService.revokeAllUserTokens(user);

        log.info("Password reset for user: {}", user.getEmail());

        return new MessageResponse("Password reset successfully");
    }

    @Transactional
    public TokenRefreshResponse refreshAccessToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());

        // Rotate refresh token
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(refreshToken);

        // Generate new access token
        String accessToken = jwtTokenProvider.generateAccessToken(refreshToken.getUser());

        return TokenRefreshResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken.getToken())
                .build();
    }

    @Transactional
    public MessageResponse logout(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        refreshTokenService.revokeAllUserTokens(user);

        log.info("User logged out: {}", user.getEmail());

        return new MessageResponse("Logged out successfully");
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .role(user.getRole())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .profileCompleted(user.getProfileCompleted())
                .passwordChangeRequired(user.getPasswordChangeRequired())
                .build();
    }
}
