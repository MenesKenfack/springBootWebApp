package com.kaksha.library.service;

import com.kaksha.library.config.JwtUtil;
import com.kaksha.library.dto.*;
import com.kaksha.library.exception.BadRequestException;
import com.kaksha.library.exception.ResourceNotFoundException;
import com.kaksha.library.model.entity.Client;
import com.kaksha.library.model.entity.User;
import com.kaksha.library.model.enums.UserRole;
import com.kaksha.library.model.enums.UserTier;
import com.kaksha.library.repository.ClientRepository;
import com.kaksha.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final MailService mailService;
    private final ActivityLogService activityLogService;
    
    @Transactional
    @SuppressWarnings("null")
    public AuthResponse register(RegisterRequest request) {
        log.info("Processing registration for email: {}", request.getEmail());
        
        // Validation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }
        
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }
        
        // Generate verification code
        String verificationCode = generateVerificationCode();
        
        // Create client
        Client client = Client.builder()
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .userTier(UserTier.BASIC)
                .role(UserRole.ROLE_CLIENT)
                .verified(false)
                .verificationCode(verificationCode)
                .verificationCodeExpiry(LocalDateTime.now().plusMinutes(30))
                .status(true)
                .active(true)
                .build();
        
        Client savedClient = Objects.requireNonNull(clientRepository.save(client), "Saved client cannot be null");
        
        // Send verification email
        try {
            mailService.sendVerificationEmail(savedClient.getEmail(), verificationCode);
        } catch (Exception e) {
            log.error("Failed to send verification email: {}", e.getMessage());
        }
        
        log.info("Client registered successfully: {}", savedClient.getEmail());
        
        return AuthResponse.builder()
                .success(true)
                .message("Registration successful. Please check your email for verification code.")
                .build();
    }
    
    public AuthResponse login(LoginRequest request) {
        log.info("Processing login for email: {}", request.getEmail());
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            User user = userDetailsService.findUserByEmail(request.getEmail());
            
            // Generate tokens with extra claims
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("role", user.getRole().name());
            extraClaims.put("tier", user.getUserTier().name());
            extraClaims.put("userId", user.getUserID());
            
            String token = jwtUtil.generateToken(userDetails, extraClaims);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);
            
            log.info("Login successful for: {}", request.getEmail());
            
            // Log login activity
            try {
                String sessionId = token.substring(0, Math.min(20, token.length()));
                activityLogService.logActivity(
                    user.getUserID(),
                    "LOGIN",
                    "User logged in successfully",
                    null,
                    sessionId
                );
            } catch (Exception e) {
                log.error("Failed to log login activity: {}", e.getMessage());
            }
            
            return AuthResponse.builder()
                    .success(true)
                    .message("Login successful")
                    .token(token)
                    .refreshToken(refreshToken)
                    .user(AuthResponse.UserInfo.builder()
                            .id(user.getUserID())
                            .username(user.getUsername())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .email(user.getEmail())
                            .dateOfBirth(user.getDateOfBirth())
                            .role(user.getRole())
                            .tier(user.getUserTier())
                            .verified(user.isVerified())
                            .build())
                    .build();
                    
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }
    }
    
    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        log.info("Processing email verification for: {}", request.getEmail());
        
        User user = userRepository.findByEmailAndVerificationCode(
                request.getEmail(), 
                request.getVerificationCode()
        ).orElseThrow(() -> new BadRequestException("Invalid verification code"));
        
        if (user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Verification code has expired. Please request a new one.");
        }
        
        user.setVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        userRepository.save(user);
        
        log.info("Email verified successfully for: {}", request.getEmail());
        
        return AuthResponse.builder()
                .success(true)
                .message("Email verified successfully. You can now log in.")
                .build();
    }
    
    public AuthResponse getCurrentUser(String email) {
        log.info("Getting current user for email: {}", email);
        try {
            User user = userDetailsService.findUserByEmail(email);
            
            if (user == null) {
                log.error("User not found for email: {}", email);
                return AuthResponse.builder()
                        .success(false)
                        .message("User not found")
                        .build();
            }
            
            log.info("Found user: {} with role: {}", user.getEmail(), user.getRole());
            
            return AuthResponse.builder()
                    .success(true)
                    .message("User retrieved successfully")
                    .user(AuthResponse.UserInfo.builder()
                            .id(user.getUserID())
                            .username(user.getUsername())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .email(user.getEmail())
                            .dateOfBirth(user.getDateOfBirth())
                            .role(user.getRole())
                            .tier(user.getUserTier())
                            .verified(user.isVerified())
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("Error retrieving user with email {}: {}", email, e.getMessage(), e);
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
    }
    
    public Long getCurrentUserId(String email) {
        User user = userDetailsService.findUserByEmail(email);
        return user.getUserID();
    }
    
    @Transactional
    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", 0L));
        
        if (user.isVerified()) {
            throw new BadRequestException("Email is already verified");
        }
        
        String newCode = generateVerificationCode();
        user.setVerificationCode(newCode);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);
        
        mailService.sendVerificationEmail(email, newCode);
        
        log.info("Verification code resent to: {}", email);
    }
    
    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(999999));
    }
}
