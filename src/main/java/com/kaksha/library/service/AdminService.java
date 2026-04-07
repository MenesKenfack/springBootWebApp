package com.kaksha.library.service;

import com.kaksha.library.dto.*;
import com.kaksha.library.exception.BadRequestException;
import com.kaksha.library.exception.ResourceNotFoundException;
import com.kaksha.library.model.entity.*;
import com.kaksha.library.model.enums.UserRole;
import com.kaksha.library.model.enums.UserTier;
import com.kaksha.library.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    
    private final UserRepository userRepository;
    private final LibrarianRepository librarianRepository;
    private final ManagerRepository managerRepository;
    private final BackupRepository backupRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    
    // User Management
    public List<UserResponse> getAllUsers(String role) {
        List<User> users;
        if (role != null) {
            users = userRepository.findAll().stream()
                    .filter(u -> u.getRole().name().equalsIgnoreCase(role))
                    .toList();
        } else {
            users = userRepository.findAll();
        }
        
        return users.stream().map(this::mapToUserResponse).toList();
    }
    
    public UserResponse getUserById(Long id) {
        Objects.requireNonNull(id, "User ID cannot be null");
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return mapToUserResponse(user);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public UserResponse createLibrarian(RegisterRequest request) {
        log.info("Creating librarian: {}", request.getEmail());
        
        validateUserCreation(request);
        
        Librarian librarian = Librarian.builder()
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .userTier(UserTier.PREMIUM)
                .role(UserRole.ROLE_LIBRARIAN)
                .verified(true)
                .active(true)
                .build();
        
        Librarian saved = librarianRepository.save(librarian);
        Objects.requireNonNull(saved, "Saved librarian cannot be null");
        log.info("Librarian created: {}", saved.getUserID());
        
        // Send credentials email to librarian
        try {
            mailService.sendLibrarianCredentialsEmail(
                saved.getEmail(),
                request.getPassword(),
                request.getFirstName()
            );
            log.info("Librarian credentials email sent to: {}", saved.getEmail());
        } catch (Exception e) {
            log.error("Failed to send librarian credentials email: {}", e.getMessage());
            // Don't throw - librarian is already created
        }
        
        return mapToUserResponse(saved);
    }
    
    @Transactional
    @SuppressWarnings("null")
    public UserResponse createManager(RegisterRequest request) {
        log.info("Creating manager: {}", request.getEmail());
        
        validateUserCreation(request);
        
        Manager manager = Manager.builder()
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .userTier(UserTier.PREMIUM)
                .role(UserRole.ROLE_MANAGER)
                .verified(true)
                .active(true)
                .build();
        
        Manager saved = managerRepository.save(manager);
        log.info("Manager created: {}", saved.getUserID());
        
        return mapToUserResponse(saved);
    }
    
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        Objects.requireNonNull(id, "User ID cannot be null");
        Objects.requireNonNull(request, "Request cannot be null");
        log.info("Updating user: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        
        // Check for email uniqueness if changed
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }
        
        // Check for username uniqueness if changed
        if (!user.getUsername().equals(request.getUsername()) && userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }
        
        // Update fields
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setEmail(request.getEmail());
        
        // Update password only if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                throw new BadRequestException("Passwords do not match");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        User saved = userRepository.save(user);
        log.info("User updated: {}", id);
        
        return mapToUserResponse(saved);
    }
    
    @Transactional
    public void deleteUser(Long userId) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        log.info("Deleting user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        Objects.requireNonNull(user, "User cannot be null");
        userRepository.delete(user);
        log.info("User deleted: {}", userId);
    }
    
    @Transactional
    public void changeUserRole(Long userId, String newRole) {
        Objects.requireNonNull(userId, "User ID cannot be null");
        log.info("Changing role for user {} to {}", userId, newRole);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        try {
            UserRole role = UserRole.valueOf(newRole.toUpperCase());
            user.setRole(role);
            userRepository.save(user);
            log.info("User {} role changed to {}", userId, role);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + newRole);
        }
    }
    
    @Transactional
    public void changeUserTier(Long id, String newTier) {
        Objects.requireNonNull(id, "User ID cannot be null");
        log.info("Changing tier for user {} to {}", id, newTier);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        
        try {
            UserTier tier = UserTier.valueOf(newTier.toUpperCase());
            user.setUserTier(tier);
            userRepository.save(user);
            log.info("User {} tier changed to {}", id, tier);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid tier: " + newTier);
        }
    }
    
    // Backup Management
    public List<BackupResponse> getAllBackups() {
        return backupRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToBackupResponse)
                .toList();
    }
    
    @Transactional
    public BackupResponse createBackup(Long managerId, String backupName, String backupType) {
        Objects.requireNonNull(managerId, "Manager ID cannot be null");
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", managerId));
        
        Backup backup = new Backup();
        backup.setManager(manager);
        backup.setBackupName(backupName);
        backup.setBackupType(backupType);
        backup.setBackupPath("/backups/" + backupName + "_" + System.currentTimeMillis() + ".sql");
        backup.setStatus("COMPLETED");
        
        Backup saved = backupRepository.save(backup);
        log.info("Backup created: {} by manager {}", saved.getBackupID(), managerId);
        
        return mapToBackupResponse(saved);
    }
    
    private void validateUserCreation(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }
        
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }
    }
    
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .userID(user.getUserID())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dateOfBirth(user.getDateOfBirth())
                .email(user.getEmail())
                .role(user.getRole())
                .userTier(user.getUserTier())
                .verified(user.isVerified())
                .status(user instanceof Client ? ((Client) user).isStatus() : true)
                .createdAt(user.getCreatedAt())
                .build();
    }
    
    private BackupResponse mapToBackupResponse(Backup backup) {
        return BackupResponse.builder()
                .backupID(backup.getBackupID())
                .backupDate(backup.getBackupDate())
                .backupName(backup.getBackupName())
                .backupPath(backup.getBackupPath())
                .backupType(backup.getBackupType())
                .status(backup.getStatus())
                .createdAt(backup.getCreatedAt())
                .build();
    }
}
