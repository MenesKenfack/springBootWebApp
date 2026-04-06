package com.kaksha.library.controller;

import com.kaksha.library.dto.ApiResponse;
import com.kaksha.library.dto.SystemTermsRequest;
import com.kaksha.library.dto.SystemTermsResponse;
import com.kaksha.library.service.SystemTermsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
@Slf4j
public class SystemTermsController {

    private final SystemTermsService termsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemTermsResponse>>> getAllTerms() {
        log.info("Fetching all system terms");
        List<SystemTermsResponse> terms = termsService.getAllTerms();
        return ResponseEntity.ok(ApiResponse.success("Terms retrieved successfully", terms));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<SystemTermsResponse>> getActiveTerms() {
        log.info("Fetching active system terms");
        SystemTermsResponse terms = termsService.getActiveTerms();
        return ResponseEntity.ok(ApiResponse.success("Active terms retrieved successfully", terms));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemTermsResponse>> getTermsById(@PathVariable Long id) {
        log.info("Fetching terms with ID: {}", id);
        SystemTermsResponse terms = termsService.getTermsById(id);
        return ResponseEntity.ok(ApiResponse.success("Terms retrieved successfully", terms));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemTermsResponse>> createTerms(
            @Valid @RequestBody SystemTermsRequest request,
            Authentication authentication) {
        log.info("Creating new terms version {} by {}", request.getVersion(), authentication.getName());
        SystemTermsResponse created = termsService.createTerms(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Terms created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SystemTermsResponse>> updateTerms(
            @PathVariable Long id,
            @Valid @RequestBody SystemTermsRequest request,
            Authentication authentication) {
        log.info("Updating terms ID: {} by {}", id, authentication.getName());
        SystemTermsResponse updated = termsService.updateTerms(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Terms updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTerms(
            @PathVariable Long id,
            Authentication authentication) {
        log.info("Deleting terms ID: {} by {}", id, authentication.getName());
        termsService.deleteTerms(id);
        return ResponseEntity.ok(ApiResponse.success("Terms deleted successfully"));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<SystemTermsResponse>> activateTerms(
            @PathVariable Long id,
            Authentication authentication) {
        log.info("Activating terms ID: {} by {}", id, authentication.getName());
        SystemTermsResponse activated = termsService.activateTerms(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Terms activated successfully", activated));
    }
}
