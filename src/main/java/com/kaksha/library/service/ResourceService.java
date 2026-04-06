package com.kaksha.library.service;

import com.kaksha.library.dto.*;
import com.kaksha.library.dto.CatalogResponse;
import com.kaksha.library.exception.BadRequestException;
import com.kaksha.library.exception.ResourceNotFoundException;
import com.kaksha.library.exception.UnauthorizedException;
import com.kaksha.library.model.entity.Client;
import com.kaksha.library.model.entity.LibraryResource;
import com.kaksha.library.model.entity.ResourceCatalog;
import com.kaksha.library.model.enums.ResourceCategory;
import com.kaksha.library.model.enums.UserRole;
import com.kaksha.library.model.enums.UserTier;
import com.kaksha.library.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {
    
    private final LibraryResourceRepository resourceRepository;
    private final ResourceCatalogRepository catalogRepository;
    private final ClientRepository clientRepository;
    private final ClientResourceRepository clientResourceRepository;
    private final UserRepository userRepository;
    
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
    @Value("${app.upload.file-path:/uploads/files}")
    private String fileAccessPath;
    
    @Value("${app.upload.image-path:/uploads/images}")
    private String imageAccessPath;
    
    public Page<ResourceResponse> searchResources(String keyword, ResourceCategory category, Long catalogId, 
                                                   String author, Integer year, BigDecimal minPrice, 
                                                   BigDecimal maxPrice, String sortBy, int page, int size) {
        // Determine sort order
        Sort sort = Sort.by("createdAt").descending();
        if ("price-low".equals(sortBy)) {
            sort = Sort.by("price").ascending();
        } else if ("price-high".equals(sortBy)) {
            sort = Sort.by("price").descending();
        } else if ("oldest".equals(sortBy)) {
            sort = Sort.by("createdAt").ascending();
        }
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<LibraryResource> resources;
        
        // Build dynamic query based on provided filters
        if (keyword != null && !keyword.isEmpty()) {
            resources = resourceRepository.searchByKeyword(keyword, pageable);
        } else if (category != null) {
            resources = resourceRepository.findByCategory(category, pageable);
        } else if (catalogId != null) {
            resources = resourceRepository.findByCatalogId(catalogId, pageable);
        } else if (author != null && !author.isEmpty()) {
            resources = resourceRepository.findByAuthorContainingIgnoreCase(author, pageable);
        } else if (year != null) {
            resources = resourceRepository.findByPublicationYear(year, pageable);
        } else if (minPrice != null && maxPrice != null) {
            resources = resourceRepository.findByPriceBetween(minPrice, maxPrice, pageable);
        } else if (minPrice != null) {
            resources = resourceRepository.findByPriceGreaterThanEqual(minPrice, pageable);
        } else if (maxPrice != null) {
            resources = resourceRepository.findByPriceLessThanEqual(maxPrice, pageable);
        } else {
            resources = resourceRepository.findAll(pageable);
        }
        
        return resources.map(this::mapToResourceResponse);
    }
    
    public ResourceResponse getResourceContent(Long resourceId, String userEmail) {
        log.info("Fetching resource content for ID: {} by user: {}", resourceId, userEmail);
        
        Objects.requireNonNull(resourceId, "Resource ID cannot be null");
        
        LibraryResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", resourceId));
        
        Client client = clientRepository.findByEmail(userEmail)
                .orElse(null);
        
        boolean hasFullAccess = false;
        String previewContent = null;
        String fullContent = null;
        
        if (client != null) {
            // Check if user has PREMIUM tier
            if (client.getUserTier() == UserTier.PREMIUM) {
                hasFullAccess = true;
            }
            // Check if user has purchased this resource
            else if (clientResourceRepository.hasAccessToResource(client.getUserID(), resourceId)) {
                hasFullAccess = true;
            }
            // BASIC users get limited preview
            else if (client.getUserTier() == UserTier.BASIC && !resource.isPremiumOnly()) {
                hasFullAccess = false;
            }
        }
        
        // Prepare content based on access level
        if (hasFullAccess) {
            fullContent = resource.getResourceFile();
        } else {
            // BASIC users get encrypted preview (1/3 visible, 2/3 encrypted/blurred)
            previewContent = generateEncryptedPreview(resource.getResourceFile(), resource.getDescription());
        }
        
        // Add user tier info to response for frontend handling
        UserTier userTier = client != null ? client.getUserTier() : null;
        
        return ResourceResponse.builder()
                .resourceID(resource.getResourceID())
                .title(resource.getTitle())
                .isbn(resource.getIsbn())
                .publicationYear(resource.getPublicationYear())
                .author(resource.getAuthor())
                .category(resource.getCategory())
                .resourceImage(resource.getResourceImage())
                .description(resource.getDescription())
                .price(resource.getPrice())
                .isPremiumOnly(resource.isPremiumOnly())
                .catalogName(resource.getResourceCatalog() != null ? resource.getResourceCatalog().getCatalogName() : null)
                .hasFullAccess(hasFullAccess)
                .userTier(userTier != null ? userTier.name() : "BASIC")
                .previewContent(previewContent)
                .fullContent(fullContent)
                .createdAt(resource.getCreatedAt())
                .build();
    }
    
    /**
     * Generates an encrypted preview for BASIC tier users.
     * Shows 1/3 of the content, encrypts/blurs the remaining 2/3.
     */
    private String generateEncryptedPreview(String resourceFile, String description) {
        if (resourceFile == null || resourceFile.isEmpty()) {
            return description != null ? 
                    description.substring(0, Math.min(200, description.length())) + "..." : 
                    "Preview not available";
        }
        
        try {
            // Read file content (assuming text files for now)
            Path filePath = Paths.get(uploadDir, resourceFile.replace("/uploads/", ""));
            String content;
            
            if (Files.exists(filePath)) {
                content = Files.readString(filePath);
            } else {
                // Fallback to description if file doesn't exist
                return description != null ? 
                        description.substring(0, Math.min(200, description.length())) + "..." : 
                        "Preview not available";
            }
            
            if (content.length() <= 100) {
                return content; // Short content, show all
            }
            
            int totalLength = content.length();
            int visibleLength = totalLength / 3; // Show 1/3
            int blurStart = visibleLength;
            
            String visiblePart = content.substring(0, visibleLength);
            String encryptedPart = encryptContent(content.substring(blurStart));
            
            return visiblePart + "\n\n[ENCRYPTED_PREVIEW_START]\n" + encryptedPart + "\n[ENCRYPTED_PREVIEW_END]";
            
        } catch (Exception e) {
            log.error("Failed to generate encrypted preview", e);
            return description != null ? 
                    description.substring(0, Math.min(200, description.length())) + "..." : 
                    "Preview not available";
        }
    }
    
    /**
     * Simple encryption for preview content - Base64 encode with marker
     */
    private String encryptContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        // Truncate to reasonable preview size and encode
        String truncated = content.length() > 2000 ? content.substring(0, 2000) + "..." : content;
        return java.util.Base64.getEncoder().encodeToString(truncated.getBytes());
    }
    
    @Transactional
    @CacheEvict(value = {"resources", "catalogs"}, allEntries = true)
    public ResourceResponse createResource(CreateResourceRequest request, String creatorEmail) {
        log.info("Creating new resource by: {}", creatorEmail);
        
        validatePermissions(creatorEmail, "WRITE_RESOURCE");
        
        Long catalogId = Objects.requireNonNull(request.getCatalogId(), "Catalog ID cannot be null");
        
        ResourceCatalog catalog = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog", catalogId));
        
        LibraryResource resource = new LibraryResource();
        resource.setTitle(request.getTitle());
        resource.setIsbn(request.getIsbn());
        resource.setPublicationYear(request.getPublicationYear());
        resource.setAuthor(request.getAuthor());
        resource.setCategory(request.getCategory());
        resource.setResourceFile(request.getResourceFile());
        resource.setResourceImage(request.getResourceImage());
        resource.setDescription(request.getDescription());
        resource.setPrice(request.getPrice());
        resource.setPremiumOnly(request.isPremiumOnly());
        resource.setResourceCatalog(catalog);
        
        LibraryResource saved = resourceRepository.save(resource);
        log.info("Resource created successfully with ID: {}", saved.getResourceID());
        
        return mapToResourceResponse(saved);
    }
    
    @Transactional
    public ResourceResponse createResourceWithFiles(String title, String isbn, String author, 
                                                    Integer publicationYear, ResourceCategory category, 
                                                    Long catalogId, BigDecimal price, boolean isPremiumOnly,
                                                    String description, MultipartFile resourceFile, 
                                                    MultipartFile resourceImage, String creatorEmail) {
        log.info("Creating new resource with files by: {}", creatorEmail);
        
        validatePermissions(creatorEmail, "WRITE_RESOURCE");
        
        Long catId = Objects.requireNonNull(catalogId, "Catalog ID cannot be null");
        
        ResourceCatalog catalog = catalogRepository.findById(catId)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog", catId));
        
        LibraryResource resource = new LibraryResource();
        resource.setTitle(title);
        resource.setIsbn(isbn);
        resource.setPublicationYear(publicationYear);
        resource.setAuthor(author);
        resource.setCategory(category);
        resource.setDescription(description);
        resource.setPrice(price);
        resource.setPremiumOnly(isPremiumOnly);
        resource.setResourceCatalog(catalog);
        
        // Save files and set paths
        try {
            if (resourceFile != null && !resourceFile.isEmpty()) {
                String filePath = saveFile(resourceFile, "files");
                resource.setResourceFile(filePath);
            }
            
            if (resourceImage != null && !resourceImage.isEmpty()) {
                String imagePath = saveFile(resourceImage, "images");
                resource.setResourceImage(imagePath);
            }
        } catch (IOException e) {
            log.error("Failed to save file", e);
            throw new BadRequestException("Failed to save file: " + e.getMessage());
        }
        
        LibraryResource saved = resourceRepository.save(resource);
        log.info("Resource created successfully with ID: {}", saved.getResourceID());
        
        return mapToResourceResponse(saved);
    }
    
    @Transactional
    public ResourceResponse updateResource(Long resourceId, UpdateResourceRequest request, String updaterEmail) {
        log.info("Updating resource ID: {} by: {}", resourceId, updaterEmail);
        
        Objects.requireNonNull(resourceId, "Resource ID cannot be null");
        
        validatePermissions(updaterEmail, "WRITE_RESOURCE");
        
        LibraryResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", resourceId));
        
        if (request.getTitle() != null) {
            resource.setTitle(request.getTitle());
        }
        if (request.getIsbn() != null) {
            resource.setIsbn(request.getIsbn());
        }
        if (request.getPublicationYear() != null) {
            resource.setPublicationYear(request.getPublicationYear());
        }
        if (request.getAuthor() != null) {
            resource.setAuthor(request.getAuthor());
        }
        if (request.getResourceFile() != null) {
            resource.setResourceFile(request.getResourceFile());
        }
        if (request.getResourceImage() != null) {
            resource.setResourceImage(request.getResourceImage());
        }
        if (request.getDescription() != null) {
            resource.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            resource.setPrice(request.getPrice());
        }
        if (request.getIsPremiumOnly() != null) {
            resource.setPremiumOnly(request.getIsPremiumOnly());
        }
        if (request.getCatalogId() != null) {
            Long updateCatalogId = Objects.requireNonNull(request.getCatalogId(), "Catalog ID cannot be null");
            ResourceCatalog catalog = catalogRepository.findById(updateCatalogId)
                    .orElseThrow(() -> new ResourceNotFoundException("Catalog", updateCatalogId));
            resource.setResourceCatalog(catalog);
        }
        
        LibraryResource updated = resourceRepository.save(Objects.requireNonNull(resource, "Resource cannot be null"));
        log.info("Resource updated successfully: {}", resourceId);
        
        return mapToResourceResponse(updated);
    }
    
    @Transactional
    public ResourceResponse updateResourceWithFiles(Long resourceId, String title, String isbn, String author,
                                                    Integer publicationYear, ResourceCategory category,
                                                    Long catalogId, BigDecimal price, Boolean isPremiumOnly,
                                                    String description, MultipartFile resourceFile,
                                                    MultipartFile resourceImage, String updaterEmail) {
        log.info("Updating resource ID: {} with files by: {}", resourceId, updaterEmail);
        
        Objects.requireNonNull(resourceId, "Resource ID cannot be null");
        
        validatePermissions(updaterEmail, "WRITE_RESOURCE");
        
        LibraryResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", resourceId));
        
        if (title != null) {
            resource.setTitle(title);
        }
        if (isbn != null) {
            resource.setIsbn(isbn);
        }
        if (author != null) {
            resource.setAuthor(author);
        }
        if (publicationYear != null) {
            resource.setPublicationYear(publicationYear);
        }
        if (category != null) {
            resource.setCategory(category);
        }
        if (description != null) {
            resource.setDescription(description);
        }
        if (price != null) {
            resource.setPrice(price);
        }
        if (isPremiumOnly != null) {
            resource.setPremiumOnly(isPremiumOnly);
        }
        if (catalogId != null) {
            Long updateCatalogId = Objects.requireNonNull(catalogId, "Catalog ID cannot be null");
            ResourceCatalog catalog = catalogRepository.findById(updateCatalogId)
                    .orElseThrow(() -> new ResourceNotFoundException("Catalog", updateCatalogId));
            resource.setResourceCatalog(catalog);
        }
        
        // Save new files if provided
        try {
            if (resourceFile != null && !resourceFile.isEmpty()) {
                String filePath = saveFile(resourceFile, "files");
                resource.setResourceFile(filePath);
            }
            
            if (resourceImage != null && !resourceImage.isEmpty()) {
                String imagePath = saveFile(resourceImage, "images");
                resource.setResourceImage(imagePath);
            }
        } catch (IOException e) {
            log.error("Failed to save file", e);
            throw new BadRequestException("Failed to save file: " + e.getMessage());
        }
        
        LibraryResource updated = resourceRepository.save(Objects.requireNonNull(resource, "Resource cannot be null"));
        log.info("Resource updated successfully: {}", resourceId);
        
        return mapToResourceResponse(updated);
    }
    
    @Transactional
    @CacheEvict(value = {"resources", "catalogs"}, allEntries = true)
    public void deleteResource(Long resourceId, String deleterEmail) {
        log.info("Deleting resource ID: {} by: {}", resourceId, deleterEmail);
        
        Objects.requireNonNull(resourceId, "Resource ID cannot be null");
        
        validatePermissions(deleterEmail, "WRITE_RESOURCE");
        
        LibraryResource resource = Objects.requireNonNull(
            resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", resourceId)),
            "Resource cannot be null"
        );
        
        resourceRepository.delete(resource);
        log.info("Resource soft deleted: {}", resourceId);
    }
    
    public List<ResourceCatalog> getAllCatalogs() {
        return catalogRepository.findAll();
    }
    @Cacheable(value = "catalogs")
    
    public List<CatalogResponse> getAllCatalogsWithCount() {
        List<ResourceCatalog> catalogs = catalogRepository.findAll();
        return catalogs.stream()
                .map(catalog -> CatalogResponse.builder()
                        .catalogId(catalog.getCatalogId())
                        .catalogName(catalog.getCatalogName())
                        .resourceCount(resourceRepository.countByResourceCatalog_CatalogId(catalog.getCatalogId()))
                        .build())
                .collect(Collectors.toList());
    }
    
    @Transactional
    @CacheEvict(value = "catalogs", allEntries = true)
    public ResourceCatalog createCatalog(String name) {
        if (catalogRepository.existsByCatalogName(name)) {
            throw new BadRequestException("Catalog with this name already exists");
        }
        
        ResourceCatalog catalog = new ResourceCatalog();
        catalog.setCatalogName(name);
        return catalogRepository.save(catalog);
    }
    
    @Transactional
    @CacheEvict(value = {"catalogs", "resources"}, allEntries = true)
    @SuppressWarnings("null")
    public void deleteCatalog(Long catalogId) {
        Objects.requireNonNull(catalogId, "Catalog ID cannot be null");
        ResourceCatalog catalog = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog", catalogId));
        
        // Check if catalog has resources using repository query instead of lazy collection
        boolean hasResources = resourceRepository.existsByResourceCatalog_CatalogId(catalogId);
        if (hasResources) {
            throw new BadRequestException("Cannot delete catalog that contains resources. Remove resources first.");
        }
        
        catalogRepository.delete(catalog);
        log.info("Catalog deleted: {}", catalogId);
    }
    
    @Transactional
    @CacheEvict(value = {"catalogs", "resources"}, allEntries = true)
    public ResourceCatalog updateCatalog(Long catalogId, String newName) {
        Objects.requireNonNull(catalogId, "Catalog ID cannot be null");
        ResourceCatalog catalog = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog", catalogId));
        
        // Check if new name already exists and is different from current
        if (!catalog.getCatalogName().equals(newName) && catalogRepository.existsByCatalogName(newName)) {
            throw new BadRequestException("Catalog with this name already exists");
        }
        
        catalog.setCatalogName(newName);
        return catalogRepository.save(catalog);
    }
    
    public List<ResourceResponse> getRecentResources(int limit) {
        return resourceRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResourceResponse)
                .collect(Collectors.toList());
    }
    
    private ResourceResponse mapToResourceResponse(LibraryResource resource) {
        return ResourceResponse.builder()
                .resourceID(resource.getResourceID())
                .title(resource.getTitle())
                .isbn(resource.getIsbn())
                .publicationYear(resource.getPublicationYear())
                .author(resource.getAuthor())
                .category(resource.getCategory())
                .resourceImage(resource.getResourceImage())
                .description(resource.getDescription())
                .price(resource.getPrice())
                .isPremiumOnly(resource.isPremiumOnly())
                .catalogName(resource.getResourceCatalog() != null ? resource.getResourceCatalog().getCatalogName() : null)
                .createdAt(resource.getCreatedAt())
                .build();
    }
    
    private String saveFile(MultipartFile file, String subDir) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir, subDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID() + extension;
        
        // Save file
        Path targetPath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        // Return relative path for access
        return subDir.equals("images") ? imageAccessPath + "/" + filename : fileAccessPath + "/" + filename;
    }
    
    private void validatePermissions(String email, String permission) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        
        if (user.getRole() != UserRole.ROLE_LIBRARIAN && user.getRole() != UserRole.ROLE_MANAGER) {
            throw new UnauthorizedException("You don't have permission to " + permission);
        }
    }
    
    public List<Integer> getAvailableYears() {
        log.info("Fetching available publication years from database");
        return resourceRepository.findDistinctPublicationYears();
    }
    
    public List<Map<String, Object>> getAvailableCategories() {
        log.info("Fetching available categories from database");
        List<Map<String, Object>> result = new ArrayList<>();
        
        // Get ResourceCategory enum values
        for (ResourceCategory category : ResourceCategory.values()) {
            Map<String, Object> catMap = new HashMap<>();
            catMap.put("value", category.name());
            catMap.put("label", formatCategoryLabel(category.name()));
            catMap.put("icon", getCategoryIcon(category.name()));
            
            // Get count of resources in this category
            long count = resourceRepository.countByCategory(category);
            catMap.put("count", count);
            
            result.add(catMap);
        }
        
        return result;
    }
    
    public List<String> getAvailableAuthors() {
        log.info("Fetching available authors from database");
        return resourceRepository.findDistinctAuthors();
    }
    
    private String formatCategoryLabel(String category) {
        // Convert BOOK to Book, JOURNAL to Journal, etc.
        return category.substring(0, 1) + category.substring(1).toLowerCase();
    }
    
    private String getCategoryIcon(String category) {
        Map<String, String> icons = Map.of(
            "BOOK", "book",
            "JOURNAL", "journal-whills",
            "ARTICLE", "newspaper",
            "VIDEO", "video",
            "AUDIO", "headphones",
            "DOCUMENT", "file-alt"
        );
        return icons.getOrDefault(category, "book");
    }
}
