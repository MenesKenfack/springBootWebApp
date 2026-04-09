package com.kaksha.library.controller;

import com.kaksha.library.dto.*;
import com.kaksha.library.service.ActivityLogService;
import com.kaksha.library.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    private final ActivityLogService activityLogService;
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        String email = authentication.getName();
        log.info("Logout request received for: {}", email);
        
        // Log logout activity before clearing context
        try {
            activityLogService.logActivity(
                authService.getCurrentUserId(email),
                "LOGOUT",
                "User logged out",
                null,
                null
            );
        } catch (Exception e) {
            log.error("Failed to log logout activity: {}", e.getMessage());
        }
        
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }
    
    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        log.info("Email verification request for: {}", request.getEmail());
        AuthResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                log.error("Authentication is null or name is null");
                return ResponseEntity.status(401).body(
                    AuthResponse.builder()
                        .success(false)
                        .message("Not authenticated")
                        .build()
                );
            }
            String email = authentication.getName();
            log.info("Get current user request for: {}", email);
            AuthResponse response = authService.getCurrentUser(email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                AuthResponse.builder()
                    .success(false)
                    .message("Failed to load profile: " + e.getMessage())
                    .build()
            );
        }
    }
    
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email) {
        log.info("Resend verification request for: {}", email);
        authService.resendVerificationCode(email);
        return ResponseEntity.ok(ApiResponse.success("Verification code resent successfully"));
    }
}
