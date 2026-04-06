package com.kaksha.library.controller;

import com.kaksha.library.dto.*;
import com.kaksha.library.model.entity.ResourceCatalog;
import com.kaksha.library.model.enums.ResourceCategory;
import com.kaksha.library.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Slf4j
public class ResourceController {
    
    private final ResourceService resourceService;
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ResourceResponse>>> searchResources(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ResourceCategory category,
            @RequestParam(required = false) Long catalogId,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        
        log.info("Search resources - keyword: {}, category: {}, year: {}, priceRange: {}-{}, page: {}", 
                keyword, category, year, minPrice, maxPrice, page);
        Page<ResourceResponse> resources = resourceService.searchResources(
                keyword, category, catalogId, author, year, minPrice, maxPrice, sortBy, page, size);
        return ResponseEntity.ok(ApiResponse.success("Resources retrieved successfully", resources));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResourceResponse>> getResourceById(@PathVariable Long id) {
        log.info("Get resource by ID: {}", id);
        ResourceResponse resource = resourceService.searchResources(null, null, null, null, null, null, null, null, 0, 1)
                .getContent().stream().filter(r -> r.getResourceID().equals(id)).findFirst()
                .orElseThrow(() -> new RuntimeException("Resource not found"));
        return ResponseEntity.ok(ApiResponse.success("Resource retrieved successfully", resource));
    }
    
    @GetMapping("/{id}/content")
    public ResponseEntity<ApiResponse<ResourceResponse>> getResourceContent(
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("Get resource content for ID: {} by user: {}", id, authentication.getName());
        ResourceResponse content = resourceService.getResourceContent(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Resource content retrieved", content));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<ResourceResponse>> createResource(
            @Valid @RequestBody CreateResourceRequest request,
            Authentication authentication) {
        
        log.info("Create resource request by: {}", authentication.getName());
        ResourceResponse resource = resourceService.createResource(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Resource created successfully", resource));
    }
    
    @PostMapping(value = "/with-files", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ResourceResponse>> createResourceWithFiles(
            @RequestParam String title,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer publicationYear,
            @RequestParam ResourceCategory category,
            @RequestParam Long catalogId,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(defaultValue = "false") boolean isPremiumOnly,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile resourceFile,
            @RequestParam(required = false) MultipartFile resourceImage,
            Authentication authentication) {
        
        log.info("Create resource with files by: {}", authentication.getName());
        ResourceResponse resource = resourceService.createResourceWithFiles(
                title, isbn, author, publicationYear, category, catalogId, 
                price, isPremiumOnly, description, resourceFile, resourceImage, 
                authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Resource created successfully", resource));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ResourceResponse>> updateResource(
            @PathVariable Long id,
            @RequestBody UpdateResourceRequest request,
            Authentication authentication) {
        
        log.info("Update resource {} by: {}", id, authentication.getName());
        ResourceResponse resource = resourceService.updateResource(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Resource updated successfully", resource));
    }
    
    @PutMapping(value = "/{id}/with-files", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ResourceResponse>> updateResourceWithFiles(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer publicationYear,
            @RequestParam(required = false) ResourceCategory category,
            @RequestParam(required = false) Long catalogId,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) Boolean isPremiumOnly,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile resourceFile,
            @RequestParam(required = false) MultipartFile resourceImage,
            Authentication authentication) {
        
        log.info("Update resource {} with files by: {}", id, authentication.getName());
        ResourceResponse resource = resourceService.updateResourceWithFiles(
                id, title, isbn, author, publicationYear, category, catalogId, 
                price, isPremiumOnly, description, resourceFile, resourceImage, 
                authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Resource updated successfully", resource));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteResource(
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("Delete resource {} by: {}", id, authentication.getName());
        resourceService.deleteResource(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Resource deleted successfully"));
    }
    
    @GetMapping("/catalogs")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_LIBRARIAN')")
    public ResponseEntity<ApiResponse<List<CatalogResponse>>> getAllCatalogs() {
        log.info("Get all catalogs");
        List<CatalogResponse> catalogs = resourceService.getAllCatalogsWithCount();
        return ResponseEntity.ok(ApiResponse.success("Catalogs retrieved successfully", catalogs));
    }
    
    @PostMapping("/catalogs")
    public ResponseEntity<ApiResponse<ResourceCatalog>> createCatalog(
            @RequestParam String name,
            Authentication authentication) {
        
        log.info("Create catalog {} by: {}", name, authentication.getName());
        ResourceCatalog catalog = resourceService.createCatalog(name);
        return ResponseEntity.ok(ApiResponse.success("Catalog created successfully", catalog));
    }
    
    @DeleteMapping("/catalogs/{id}")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_LIBRARIAN')")
    public ResponseEntity<ApiResponse<Void>> deleteCatalog(
            @PathVariable Long id,
            Authentication authentication) {
        
        log.info("Delete catalog {} by: {}", id, authentication.getName());
        resourceService.deleteCatalog(id);
        return ResponseEntity.ok(ApiResponse.success("Catalog deleted successfully"));
    }
    
    @PutMapping("/catalogs/{id}")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_LIBRARIAN')")
    public ResponseEntity<ApiResponse<ResourceCatalog>> updateCatalog(
            @PathVariable Long id,
            @RequestParam String name,
            Authentication authentication) {
        
        log.info("Update catalog {} to {} by: {}", id, name, authentication.getName());
        ResourceCatalog catalog = resourceService.updateCatalog(id, name);
        return ResponseEntity.ok(ApiResponse.success("Catalog updated successfully", catalog));
    }
    
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<ResourceResponse>>> getRecentResources() {
        log.info("Get recent resources");
        List<ResourceResponse> resources = resourceService.getRecentResources(5);
        return ResponseEntity.ok(ApiResponse.success("Recent resources retrieved", resources));
    }
    
    @GetMapping("/years")
    public ResponseEntity<ApiResponse<List<Integer>>> getAvailableYears() {
        log.info("Get available publication years");
        List<Integer> years = resourceService.getAvailableYears();
        return ResponseEntity.ok(ApiResponse.success("Available years retrieved", years));
    }
    
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableCategories() {
        log.info("Get available resource categories");
        List<Map<String, Object>> categories = resourceService.getAvailableCategories();
        return ResponseEntity.ok(ApiResponse.success("Available categories retrieved", categories));
    }
}
