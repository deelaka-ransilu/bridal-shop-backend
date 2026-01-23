package edu.bridalshop.backend.controller;

import edu.bridalshop.backend.dto.request.*;
import edu.bridalshop.backend.dto.response.AuthResponse;
import edu.bridalshop.backend.dto.response.MessageResponse;
import edu.bridalshop.backend.dto.response.TokenRefreshResponse;
import edu.bridalshop.backend.security.CustomUserDetails;
import edu.bridalshop.backend.service.AuthService;
import edu.bridalshop.backend.service.GoogleOAuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody Map<String, String> request) {
        String idToken = request.get("idToken");

        // Verify Google token
        GoogleIdToken.Payload payload = googleOAuthService.verifyGoogleToken(idToken);

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String fullName = (String) payload.get("name");

        AuthResponse response = authService.googleLogin(googleId, email, fullName);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete-profile")
    public ResponseEntity<MessageResponse> completeProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CompleteProfileRequest request) {
        MessageResponse response = authService.completeProfile(userDetails.getUserId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
        MessageResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        MessageResponse response = authService.resendVerificationEmail(email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        MessageResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        MessageResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenRefreshResponse response = authService.refreshAccessToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        MessageResponse response = authService.logout(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }
}
