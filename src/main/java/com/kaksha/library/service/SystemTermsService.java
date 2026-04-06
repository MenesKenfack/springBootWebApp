package com.kaksha.library.service;

import com.kaksha.library.dto.SystemTermsRequest;
import com.kaksha.library.dto.SystemTermsResponse;
import com.kaksha.library.exception.BadRequestException;
import com.kaksha.library.exception.ResourceNotFoundException;
import com.kaksha.library.model.entity.SystemTerms;
import com.kaksha.library.model.entity.User;
import com.kaksha.library.repository.SystemTermsRepository;
import com.kaksha.library.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemTermsService {

    private final SystemTermsRepository termsRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SystemTermsResponse> getAllTerms() {
        return termsRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SystemTermsResponse getActiveTerms() {
        return termsRepository.findActiveTerms()
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No active terms found", 0L));
    }

    @Transactional(readOnly = true)
    public SystemTermsResponse getTermsById(@NonNull Long id) {
        return termsRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Terms", id));
    }

    @Transactional
    public SystemTermsResponse createTerms(SystemTermsRequest request, String adminEmail) {
        log.info("Creating new terms version {} by admin {}", request.getVersion(), adminEmail);

        if (termsRepository.existsByVersion(request.getVersion())) {
            throw new BadRequestException("Terms with version " + request.getVersion() + " already exists");
        }

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found", 0L));

        // Deactivate existing active terms if new terms should be active
        if (request.isActive()) {
            termsRepository.deactivateAllTerms();
        }

        SystemTerms terms = SystemTerms.builder()
                .version(request.getVersion())
                .title(request.getTitle())
                .content(request.getContent())
                .isActive(request.isActive())
                .effectiveDate(request.getEffectiveDate())
                .createdBy(admin)
                .updatedBy(admin)
                .build();

        SystemTerms saved = termsRepository.save(Objects.requireNonNull(terms, "Terms cannot be null"));
        log.info("Terms created successfully with ID: {}", saved.getTermsId());

        return mapToResponse(saved);
    }

    @Transactional
    public SystemTermsResponse updateTerms(@NonNull Long id, SystemTermsRequest request, String adminEmail) {
        log.info("Updating terms ID: {} by admin {}", id, adminEmail);

        SystemTerms terms = termsRepository.findById(Objects.requireNonNull(id, "ID cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("Terms", id));

        // Check if version is being changed to an existing one
        if (!terms.getVersion().equals(request.getVersion()) && termsRepository.existsByVersion(request.getVersion())) {
            throw new BadRequestException("Terms with version " + request.getVersion() + " already exists");
        }

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found", 0L));

        // If activating this terms, deactivate others
        if (request.isActive() && !terms.isActive()) {
            termsRepository.deactivateAllTerms();
        }

        terms.setVersion(request.getVersion());
        terms.setTitle(request.getTitle());
        terms.setContent(request.getContent());
        terms.setActive(request.isActive());
        if (request.getEffectiveDate() != null) {
            terms.setEffectiveDate(request.getEffectiveDate());
        }
        terms.setUpdatedBy(admin);

        SystemTerms updated = termsRepository.save(Objects.requireNonNull(terms, "Terms cannot be null"));
        log.info("Terms updated successfully: {}", id);

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteTerms(@NonNull Long id) {
        log.info("Deleting terms ID: {}", id);

        SystemTerms terms = termsRepository.findById(Objects.requireNonNull(id, "ID cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("Terms", id));

        termsRepository.delete(Objects.requireNonNull(terms, "Terms cannot be null"));
        log.info("Terms deleted successfully: {}", id);
    }

    @Transactional
    public SystemTermsResponse activateTerms(@NonNull Long id, String adminEmail) {
        log.info("Activating terms ID: {} by admin {}", id, adminEmail);

        SystemTerms terms = termsRepository.findById(Objects.requireNonNull(id, "ID cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("Terms", id));

        // Deactivate all existing terms
        termsRepository.deactivateAllTerms();

        terms.setActive(true);
        
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found", 0L));
        terms.setUpdatedBy(admin);

        SystemTerms updated = termsRepository.save(Objects.requireNonNull(terms, "Terms cannot be null"));
        log.info("Terms activated successfully: {}", id);

        return mapToResponse(updated);
    }

    private SystemTermsResponse mapToResponse(SystemTerms terms) {
        return SystemTermsResponse.builder()
                .termsId(terms.getTermsId())
                .version(terms.getVersion())
                .title(terms.getTitle())
                .content(terms.getContent())
                .isActive(terms.isActive())
                .effectiveDate(terms.getEffectiveDate())
                .createdAt(terms.getCreatedAt())
                .updatedAt(terms.getUpdatedAt())
                .createdBy(terms.getCreatedBy() != null ? terms.getCreatedBy().getUsername() : null)
                .updatedBy(terms.getUpdatedBy() != null ? terms.getUpdatedBy().getUsername() : null)
                .build();
    }
}
