package edu.bridalshop.backend.controller;

import edu.bridalshop.backend.dto.request.CreateEmployeeRequest;
import edu.bridalshop.backend.dto.response.MessageResponse;
import edu.bridalshop.backend.dto.response.UserResponse;
import edu.bridalshop.backend.security.CustomUserDetails;
import edu.bridalshop.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponse response = userService.getCurrentUser(userDetails.getUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        UserResponse response = userService.createEmployee(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createAdmin(@Valid @RequestBody CreateEmployeeRequest request) {
        UserResponse response = userService.createAdmin(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deactivateUser(@PathVariable Integer userId) {
        MessageResponse response = userService.deactivateUser(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> activateUser(@PathVariable Integer userId) {
        MessageResponse response = userService.activateUser(userId);
        return ResponseEntity.ok(response);
    }
}
