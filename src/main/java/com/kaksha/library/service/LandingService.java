package com.kaksha.library.service;

import com.kaksha.library.dto.LandingResourceResponse;
import com.kaksha.library.model.entity.LibraryResource;
import com.kaksha.library.model.entity.Rating;
import com.kaksha.library.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LandingService {
    
    private final LibraryResourceRepository resourceRepository;
    private final RatingRepository ratingRepository;
    private final PaymentsRepository paymentsRepository;
    private final ResourceAccessLogRepository accessLogRepository;
    private final ResourceSearchLogRepository searchLogRepository;
    
    @Transactional(readOnly = true)
    public List<LandingResourceResponse> getTrendingNow(int limit) {
        log.info("Fetching trending resources (most rated with high average rating)");
        Pageable pageable = PageRequest.of(0, limit);
        
        List<Object[]> results = resourceRepository.findTrendingResources(pageable);
        
        return results.stream()
                .map(result -> {
                    LibraryResource resource = (LibraryResource) result[0];
                    Double avgRating = (Double) result[1];
                    Long ratingCount = (Long) result[2];
                    return buildLandingResponse(resource, avgRating, ratingCount, null, null, null);
                })
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<LandingResourceResponse> getBestSelling(int limit) {
        log.info("Fetching best selling resources (most purchased)");
        Pageable pageable = PageRequest.of(0, limit);
        
        List<Object[]> results = paymentsRepository.findBestSellingResources(pageable);
        
        return results.stream()
                .map(result -> {
                    Long resourceId = (Long) result[0];
                    Long salesCount = (Long) result[1];
                    
                    if (resourceId == null) return null;
                    
                    return resourceRepository.findById(resourceId)
                            .map(resource -> {
                                Double avgRating = getAverageRating(resourceId);
                                Long ratingCount = getRatingCount(resourceId);
                                return buildLandingResponse(resource, avgRating, ratingCount, salesCount, null, null);
                            })
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<LandingResourceResponse> getPopularBooks(int limit) {
        log.info("Fetching popular books (most accessed, most searched, and 5-star rated)");
        Pageable pageable = PageRequest.of(0, limit);
        
        // Get popular resources from rating criteria (5+ ratings with 4.0+ average)
        List<Object[]> popularResults = resourceRepository.findPopularResources(pageable);
        
        Set<Long> includedResourceIds = new HashSet<>();
        List<LandingResourceResponse> popularBooks = new ArrayList<>();
        
        // Add popular resources first
        for (Object[] result : popularResults) {
            LibraryResource resource = (LibraryResource) result[0];
            Double avgRating = (Double) result[1];
            Long ratingCount = (Long) result[2];
            
            Long resourceId = resource.getResourceID();
            includedResourceIds.add(resourceId);
            
            Long salesCount = getSalesCount(resourceId);
            Long accessCount = accessLogRepository.countByResourceResourceID(resourceId);
            Long searchCount = searchLogRepository.countByResourceResourceID(resourceId);
            
            popularBooks.add(buildLandingResponse(resource, avgRating, ratingCount, salesCount, accessCount, searchCount));
        }
        
        // Fill remaining slots with most accessed resources
        if (popularBooks.size() < limit) {
            List<Object[]> accessedResults = accessLogRepository.findMostAccessedResources();
            int remainingSlots = limit - popularBooks.size();
            
            for (Object[] result : accessedResults) {
                Long resourceId = (Long) result[0];
                
                if (resourceId == null) continue;
                
                if (!includedResourceIds.contains(resourceId)) {
                    Long accessCount = (Long) result[1];
                    
                    resourceRepository.findById(resourceId).ifPresent(resource -> {
                        Double avgRating = getAverageRating(resourceId);
                        Long ratingCount = getRatingCount(resourceId);
                        Long salesCount = getSalesCount(resourceId);
                        Long searchCount = searchLogRepository.countByResourceResourceID(resourceId);
                        
                        popularBooks.add(buildLandingResponse(resource, avgRating, ratingCount, salesCount, accessCount, searchCount));
                    });
                    
                    includedResourceIds.add(resourceId);
                    remainingSlots--;
                    
                    if (remainingSlots <= 0) break;
                }
            }
        }
        
        // Fill remaining slots with most searched resources
        if (popularBooks.size() < limit) {
            List<Object[]> searchedResults = searchLogRepository.findMostSearchedResources();
            int remainingSlots = limit - popularBooks.size();
            
            for (Object[] result : searchedResults) {
                Long resourceId = (Long) result[0];
                
                if (!includedResourceIds.contains(resourceId)) {
                    Long searchCount = (Long) result[1];
                    
                    if (resourceId == null) continue;
                    
                    resourceRepository.findById(resourceId).ifPresent(resource -> {
                        Double avgRating = getAverageRating(resourceId);
                        Long ratingCount = getRatingCount(resourceId);
                        Long salesCount = getSalesCount(resourceId);
                        Long accessCount = accessLogRepository.countByResourceResourceID(resourceId);
                        
                        popularBooks.add(buildLandingResponse(resource, avgRating, ratingCount, salesCount, accessCount, searchCount));
                    });
                    
                    includedResourceIds.add(resourceId);
                    remainingSlots--;
                    
                    if (remainingSlots <= 0) break;
                }
            }
        }
        
        // Fill remaining slots with 5-star rated resources
        if (popularBooks.size() < limit) {
            Pageable remainingPageable = PageRequest.of(0, limit);
            List<Object[]> fiveStarResults = resourceRepository.findFiveStarResources(remainingPageable);
            int remainingSlots = limit - popularBooks.size();
            
            for (Object[] result : fiveStarResults) {
                LibraryResource resource = (LibraryResource) result[0];
                Long resourceId = resource.getResourceID();
                
                if (!includedResourceIds.contains(resourceId)) {
                    Double avgRating = getAverageRating(resourceId);
                    Long ratingCount = getRatingCount(resourceId);
                    Long salesCount = getSalesCount(resourceId);
                    Long accessCount = accessLogRepository.countByResourceResourceID(resourceId);
                    Long searchCount = searchLogRepository.countByResourceResourceID(resourceId);
                    
                    popularBooks.add(buildLandingResponse(resource, avgRating, ratingCount, salesCount, accessCount, searchCount));
                    
                    includedResourceIds.add(resourceId);
                    remainingSlots--;
                    
                    if (remainingSlots <= 0) break;
                }
            }
        }
        
        return popularBooks;
    }
    
    @Transactional(readOnly = true)
    public List<LandingResourceResponse> getNewlyAdded(int limit) {
        log.info("Fetching newly added resources");
        
        List<LibraryResource> resources = resourceRepository.findNewlyAdded(limit);
        
        return resources.stream()
                .map(resource -> {
                    Long resourceId = resource.getResourceID();
                    Double avgRating = getAverageRating(resourceId);
                    Long ratingCount = getRatingCount(resourceId);
                    Long salesCount = getSalesCount(resourceId);
                    Long accessCount = accessLogRepository.countByResourceResourceID(resourceId);
                    Long searchCount = searchLogRepository.countByResourceResourceID(resourceId);
                    
                    return buildLandingResponse(resource, avgRating, ratingCount, salesCount, accessCount, searchCount);
                })
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public LandingResourceResponse getResourceDetail(Long resourceId) {
        log.info("Fetching resource detail for ID: {}", resourceId);
        
        LibraryResource resource = resourceRepository.findById(Objects.requireNonNull(resourceId, "Resource ID cannot be null"))
                .orElseThrow(() -> new RuntimeException("Resource not found: " + resourceId));
        
        Double avgRating = getAverageRating(resourceId);
        Long ratingCount = getRatingCount(resourceId);
        Long salesCount = getSalesCount(resourceId);
        Long accessCount = accessLogRepository.countByResourceResourceID(resourceId);
        Long searchCount = searchLogRepository.countByResourceResourceID(resourceId);
        
        return buildLandingResponse(resource, avgRating, ratingCount, salesCount, accessCount, searchCount);
    }
    
    @Transactional(readOnly = true)
    public List<LandingResourceResponse> getRelatedBooksByAuthor(String author, Long excludeResourceId, int limit) {
        log.info("Fetching related books by author: {}, excluding resource ID: {}", author, excludeResourceId);
        
        List<LibraryResource> resources = resourceRepository.findByAuthorAndResourceIDNot(author, excludeResourceId);
        
        return resources.stream()
                .limit(limit)
                .map(resource -> {
                    Long resourceId = resource.getResourceID();
                    Double avgRating = getAverageRating(resourceId);
                    Long ratingCount = getRatingCount(resourceId);
                    Long salesCount = getSalesCount(resourceId);
                    Long accessCount = accessLogRepository.countByResourceResourceID(resourceId);
                    Long searchCount = searchLogRepository.countByResourceResourceID(resourceId);
                    
                    return buildLandingResponse(resource, avgRating, ratingCount, salesCount, accessCount, searchCount);
                })
                .collect(Collectors.toList());
    }
    
    private Double getAverageRating(Long resourceId) {
        List<Rating> ratings = ratingRepository.findByResourceResourceID(resourceId);
        if (ratings.isEmpty()) {
            return 0.0;
        }
        
        double sum = ratings.stream()
                .mapToInt(Rating::getRating)
                .sum();
        
        return BigDecimal.valueOf(sum / ratings.size())
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
    
    private Long getRatingCount(Long resourceId) {
        return ratingRepository.countByResourceResourceID(resourceId);
    }
    
    private Long getSalesCount(Long resourceId) {
        return paymentsRepository.countSalesByResourceId(resourceId);
    }
    
    private LandingResourceResponse buildLandingResponse(LibraryResource resource, Double avgRating, 
                                                          Long totalRatings, Long totalSales, 
                                                          Long totalAccesses, Long totalSearches) {
        return LandingResourceResponse.builder()
                .resourceID(resource.getResourceID())
                .title(resource.getTitle())
                .isbn(resource.getIsbn())
                .publicationYear(resource.getPublicationYear())
                .author(resource.getAuthor())
                .category(resource.getCategory())
                .resourceImage(resource.getResourceImage())
                .resourceFile(resource.getResourceFile())
                .description(resource.getDescription())
                .price(resource.getPrice())
                .isPremiumOnly(resource.isPremiumOnly())
                .catalogName(resource.getResourceCatalog() != null ? resource.getResourceCatalog().getCatalogName() : null)
                .createdAt(resource.getCreatedAt())
                .averageRating(avgRating != null ? avgRating : 0.0)
                .totalRatings(totalRatings != null ? totalRatings : 0L)
                .totalSales(totalSales != null ? totalSales : 0L)
                .totalAccesses(totalAccesses != null ? totalAccesses : 0L)
                .totalSearches(totalSearches != null ? totalSearches : 0L)
                .build();
    }
}
