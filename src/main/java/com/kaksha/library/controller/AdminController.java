package com.kaksha.library.controller;

import com.kaksha.library.dto.*;
import com.kaksha.library.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final AdminService adminService;
    
    // User Management
    @GetMapping("/users")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String role) {
        
        log.info("Get all users with role filter: {}", role);
        List<UserResponse> users = adminService.getAllUsers(role);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }
    
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        log.info("Get user by ID: {}", id);
        UserResponse user = adminService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }
    
    @PostMapping("/librarians")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<UserResponse>> createLibrarian(
            @Valid @RequestBody RegisterRequest request) {
        
        log.info("Create librarian: {}", request.getEmail());
        UserResponse librarian = adminService.createLibrarian(request);
        return ResponseEntity.ok(ApiResponse.success("Librarian created successfully", librarian));
    }
    
    @PostMapping("/managers")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<UserResponse>> createManager(
            @Valid @RequestBody RegisterRequest request) {
        
        log.info("Create manager: {}", request.getEmail());
        UserResponse manager = adminService.createManager(request);
        return ResponseEntity.ok(ApiResponse.success("Manager created successfully", manager));
    }
    
    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> changeUserRole(
            @PathVariable Long id,
            @RequestParam String role) {
        
        log.info("Change user {} role to {}", id, role);
        adminService.changeUserRole(id, role);
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully"));
    }
    
    @PutMapping("/users/{id}/tier")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> changeUserTier(
            @PathVariable Long id,
            @RequestParam String tier) {
        
        log.info("Change user {} tier to {}", id, tier);
        adminService.changeUserTier(id, tier);
        return ResponseEntity.ok(ApiResponse.success("User tier updated successfully"));
    }
    
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        log.info("Delete user: {}", id);
        adminService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }
    
    // Backup Management
    @GetMapping("/backups")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<List<BackupResponse>>> getAllBackups() {
        log.info("Get all backups");
        List<BackupResponse> backups = adminService.getAllBackups();
        return ResponseEntity.ok(ApiResponse.success("Backups retrieved successfully", backups));
    }
    
    @PostMapping("/backups")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<BackupResponse>> createBackup(
            @RequestParam Long managerId,
            @RequestParam String backupName,
            @RequestParam String backupType) {
        
        log.info("Create backup {} by manager {}", backupName, managerId);
        BackupResponse backup = adminService.createBackup(managerId, backupName, backupType);
        return ResponseEntity.ok(ApiResponse.success("Backup created successfully", backup));
    }
}
