package edu.bridalshop.backend.controller;

import edu.bridalshop.backend.dto.response.UserResponse;
import edu.bridalshop.backend.security.CustomUserDetails;
import edu.bridalshop.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    // GET /api/users/me
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                authService.getProfile(userDetails.getUserId()));
    }

    // POST /api/users/me/picture
    @PostMapping("/me/picture")
    public ResponseEntity<UserResponse> uploadProfilePicture(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(
                authService.uploadProfilePicture(userDetails.getUserId(), file));
    }
}