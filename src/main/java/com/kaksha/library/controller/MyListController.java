package com.kaksha.library.controller;

import com.kaksha.library.dto.*;
import com.kaksha.library.model.entity.Client;
import com.kaksha.library.repository.ClientRepository;
import com.kaksha.library.service.ActivityLogService;
import com.kaksha.library.service.MyListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/my-list")
@RequiredArgsConstructor
@Slf4j
public class MyListController {

    private final MyListService myListService;
    private final ClientRepository clientRepository;
    private final ActivityLogService activityLogService;

    private Long getClientIdFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return clientRepository.findByEmail(email)
                .map(Client::getUserID)
                .orElseThrow(() -> new RuntimeException("Client not found"));
    }

    // ==================== COMBINED ====================

    @GetMapping
    public ResponseEntity<ApiResponse<MyListResponse>> getMyList(Authentication authentication) {
        log.info("Get my list for user: {}", authentication.getName());
        Long clientId = getClientIdFromAuth(authentication);
        MyListResponse myList = myListService.getMyList(clientId);
        return ResponseEntity.ok(ApiResponse.success("My list retrieved successfully", myList));
    }

    // ==================== FAVORITES ====================

    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<List<FavoriteResponse>>> getFavorites(Authentication authentication) {
        log.info("Get favorites for user: {}", authentication.getName());
        Long clientId = getClientIdFromAuth(authentication);
        List<FavoriteResponse> favorites = myListService.getFavorites(clientId);
        return ResponseEntity.ok(ApiResponse.success("Favorites retrieved successfully", favorites));
    }

    @PostMapping("/favorites")
    public ResponseEntity<ApiResponse<FavoriteResponse>> addFavorite(
            @RequestBody Map<String, Long> request,
            Authentication authentication) {
        Long clientId = getClientIdFromAuth(authentication);
        Long resourceId = request.get("resourceId");
        log.info("Add favorite - client: {}, resource: {}", clientId, resourceId);
        FavoriteResponse favorite = myListService.addFavorite(clientId, resourceId);
        
        // Log activity
        activityLogService.logActivity(
            clientId,
            "FAVORITE_ADD",
            "Added resource to favorites",
            favorite.getTitle(),
            null
        );
        
        return ResponseEntity.ok(ApiResponse.success("Added to favorites", favorite));
    }

    @DeleteMapping("/favorites/{resourceId}")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable Long resourceId,
            Authentication authentication) {
        Long clientId = getClientIdFromAuth(authentication);
        log.info("Remove favorite - client: {}, resource: {}", clientId, resourceId);
        myListService.removeFavorite(clientId, resourceId);
        
        // Log activity
        activityLogService.logActivity(
            clientId,
            "FAVORITE_REMOVE",
            "Removed resource from favorites",
            null,
            null
        );
        
        return ResponseEntity.ok(ApiResponse.success("Removed from favorites"));
    }

    @GetMapping("/favorites/{resourceId}/check")
    public ResponseEntity<ApiResponse<Boolean>> isFavorite(
            @PathVariable Long resourceId,
            Authentication authentication) {
        Long clientId = getClientIdFromAuth(authentication);
        boolean isFavorite = myListService.isFavorite(clientId, resourceId);
        return ResponseEntity.ok(ApiResponse.success("Favorite status retrieved", isFavorite));
    }

    // ==================== READING PROGRESS ====================

    @GetMapping("/reading")
    public ResponseEntity<ApiResponse<List<ReadingProgressResponse>>> getReadingProgress(Authentication authentication) {
        log.info("Get reading progress for user: {}", authentication.getName());
        Long clientId = getClientIdFromAuth(authentication);
        List<ReadingProgressResponse> reading = myListService.getReadingProgress(clientId);
        return ResponseEntity.ok(ApiResponse.success("Reading progress retrieved successfully", reading));
    }

    @PutMapping("/reading")
    public ResponseEntity<ApiResponse<ReadingProgressResponse>> updateReadingProgress(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long clientId = getClientIdFromAuth(authentication);
        Long resourceId = Long.valueOf(request.get("resourceId").toString());
        Integer progress = Integer.valueOf(request.get("progress").toString());
        log.info("Update reading progress - client: {}, resource: {}, progress: {}", clientId, resourceId, progress);
        ReadingProgressResponse updated = myListService.updateReadingProgress(clientId, resourceId, progress);
        
        // Log activity - different message based on progress
        String activityType = progress == 100 ? "RESOURCE_READ" : "RESOURCE_VIEW";
        String description = progress == 100 ? "Completed reading resource" : "Updated reading progress to " + progress + "%";
        
        activityLogService.logActivity(
            clientId,
            activityType,
            description,
            updated.getTitle(),
            null
        );
        
        return ResponseEntity.ok(ApiResponse.success("Reading progress updated", updated));
    }

    // ==================== RATINGS ====================

    @GetMapping("/ratings")
    public ResponseEntity<ApiResponse<List<RatingResponse>>> getMyRatings(Authentication authentication) {
        log.info("Get ratings for user: {}", authentication.getName());
        Long clientId = getClientIdFromAuth(authentication);
        List<RatingResponse> ratings = myListService.getMyRatings(clientId);
        return ResponseEntity.ok(ApiResponse.success("Ratings retrieved successfully", ratings));
    }

    @PostMapping("/ratings")
    public ResponseEntity<ApiResponse<RatingResponse>> rateResource(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long clientId = getClientIdFromAuth(authentication);
        Long resourceId = Long.valueOf(request.get("resourceId").toString());
        Integer rating = Integer.valueOf(request.get("rating").toString());
        String comment = (String) request.getOrDefault("comment", "");
        log.info("Rate resource - client: {}, resource: {}, rating: {}", clientId, resourceId, rating);
        RatingResponse rated = myListService.rateResource(clientId, resourceId, rating, comment);
        
        // Log activity
        activityLogService.logActivity(
            clientId,
            "RATING_SUBMIT",
            "Rated resource " + rating + " stars",
            rated.getTitle(),
            null
        );
        
        // If comment provided, also log review
        if (comment != null && !comment.trim().isEmpty()) {
            activityLogService.logActivity(
                clientId,
                "REVIEW_ADD",
                "Added a review",
                rated.getTitle(),
                null
            );
        }
        
        return ResponseEntity.ok(ApiResponse.success("Rating submitted", rated));
    }

    @PutMapping("/ratings")
    public ResponseEntity<ApiResponse<RatingResponse>> updateRating(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        Long clientId = getClientIdFromAuth(authentication);
        Long resourceId = Long.valueOf(request.get("resourceId").toString());
        Integer rating = Integer.valueOf(request.get("rating").toString());
        String comment = (String) request.getOrDefault("comment", "");
        log.info("Update rating - client: {}, resource: {}, rating: {}", clientId, resourceId, rating);
        RatingResponse updated = myListService.rateResource(clientId, resourceId, rating, comment);
        return ResponseEntity.ok(ApiResponse.success("Rating updated", updated));
    }

    @GetMapping("/ratings/{resourceId}")
    public ResponseEntity<ApiResponse<RatingResponse>> getMyRating(
            @PathVariable Long resourceId,
            Authentication authentication) {
        Long clientId = getClientIdFromAuth(authentication);
        RatingResponse rating = myListService.getMyRating(clientId, resourceId);
        return ResponseEntity.ok(ApiResponse.success("Rating retrieved", rating));
    }
}
