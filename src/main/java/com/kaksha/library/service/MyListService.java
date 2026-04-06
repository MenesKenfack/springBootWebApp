package com.kaksha.library.service;

import com.kaksha.library.dto.*;
import com.kaksha.library.model.entity.*;
import com.kaksha.library.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MyListService {

    private final FavoriteRepository favoriteRepository;
    private final ReadingProgressRepository readingProgressRepository;
    private final RatingRepository ratingRepository;
    private final ClientRepository clientRepository;
    private final LibraryResourceRepository resourceRepository;

    // ==================== FAVORITES ====================

    @Transactional(readOnly = true)
    public List<FavoriteResponse> getFavorites(Long clientId) {
        log.info("Fetching favorites for client: {}", clientId);
        return favoriteRepository.findByClientUserID(clientId).stream()
                .map(this::mapToFavoriteResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FavoriteResponse addFavorite(Long clientId, Long resourceId) {
        log.info("Adding favorite - client: {}, resource: {}", clientId, resourceId);

        Objects.requireNonNull(clientId, "Client ID cannot be null");
        Objects.requireNonNull(resourceId, "Resource ID cannot be null");

        if (favoriteRepository.existsByClientUserIDAndResourceResourceID(clientId, resourceId)) {
            throw new IllegalStateException("Resource is already in favorites");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        LibraryResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found"));

        Favorite favorite = new Favorite();
        favorite.setClient(client);
        favorite.setResource(resource);

        Favorite saved = favoriteRepository.save(favorite);
        return mapToFavoriteResponse(saved);
    }

    @Transactional
    public void removeFavorite(Long clientId, Long resourceId) {
        log.info("Removing favorite - client: {}, resource: {}", clientId, resourceId);
        favoriteRepository.deleteByClientUserIDAndResourceResourceID(clientId, resourceId);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long clientId, Long resourceId) {
        return favoriteRepository.existsByClientUserIDAndResourceResourceID(clientId, resourceId);
    }

    @Transactional(readOnly = true)
    public long getFavoritesCount(Long clientId) {
        return favoriteRepository.countByClientUserID(clientId);
    }

    private FavoriteResponse mapToFavoriteResponse(Favorite favorite) {
        LibraryResource resource = favorite.getResource();
        return FavoriteResponse.builder()
                .favoriteId(favorite.getFavoriteId())
                .resourceId(resource.getResourceID())
                .title(resource.getTitle())
                .author(resource.getAuthor())
                .category(resource.getCategory() != null ? resource.getCategory().name() : null)
                .resourceImage(resource.getResourceImage())
                .addedAt(favorite.getCreatedAt())
                .build();
    }

    // ==================== READING PROGRESS ====================

    @Transactional(readOnly = true)
    public List<ReadingProgressResponse> getReadingProgress(Long clientId) {
        log.info("Fetching reading progress for client: {}", clientId);
        return readingProgressRepository.findByClientUserID(clientId).stream()
                .map(this::mapToReadingProgressResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReadingProgressResponse updateReadingProgress(Long clientId, Long resourceId, Integer progress) {
        log.info("Updating reading progress - client: {}, resource: {}, progress: {}", clientId, resourceId, progress);

        Objects.requireNonNull(clientId, "Client ID cannot be null");
        Objects.requireNonNull(resourceId, "Resource ID cannot be null");

        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("Progress must be between 0 and 100");
        }

        ReadingProgress readingProgress = readingProgressRepository
                .findByClientUserIDAndResourceResourceID(clientId, resourceId)
                .orElseGet(() -> {
                    Client client = clientRepository.findById(clientId)
                            .orElseThrow(() -> new RuntimeException("Client not found"));
                    LibraryResource resource = resourceRepository.findById(resourceId)
                            .orElseThrow(() -> new RuntimeException("Resource not found"));

                    ReadingProgress newProgress = new ReadingProgress();
                    newProgress.setClient(client);
                    newProgress.setResource(resource);
                    return newProgress;
                });

        readingProgress.setProgress(progress);
        readingProgress.setLastReadAt(LocalDateTime.now());

        ReadingProgress saved = readingProgressRepository.save(readingProgress);
        return mapToReadingProgressResponse(saved);
    }

    @Transactional(readOnly = true)
    public long getReadingCount(Long clientId) {
        return readingProgressRepository.countByClientUserIDAndProgressGreaterThan(clientId, 0);
    }

    private ReadingProgressResponse mapToReadingProgressResponse(ReadingProgress progress) {
        LibraryResource resource = progress.getResource();
        return ReadingProgressResponse.builder()
                .progressId(progress.getProgressId())
                .resourceId(resource.getResourceID())
                .title(resource.getTitle())
                .author(resource.getAuthor())
                .category(resource.getCategory() != null ? resource.getCategory().name() : null)
                .resourceImage(resource.getResourceImage())
                .progress(progress.getProgress())
                .lastReadAt(progress.getLastReadAt())
                .updatedAt(progress.getUpdatedAt())
                .build();
    }

    // ==================== RATINGS ====================

    @Transactional(readOnly = true)
    public List<RatingResponse> getMyRatings(Long clientId) {
        log.info("Fetching ratings for client: {}", clientId);
        return ratingRepository.findByClientUserID(clientId).stream()
                .map(this::mapToRatingResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RatingResponse rateResource(Long clientId, Long resourceId, Integer rating, String comment) {
        log.info("Rating resource - client: {}, resource: {}, rating: {}", clientId, resourceId, rating);

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Rating existingRating = ratingRepository
                .findByClientUserIDAndResourceResourceID(clientId, resourceId)
                .orElse(null);

        if (existingRating != null) {
            existingRating.setRating(rating);
            existingRating.setComment(comment);
            Rating saved = ratingRepository.save(existingRating);
            return mapToRatingResponse(saved);
        }

        Objects.requireNonNull(clientId, "Client ID cannot be null");
        Objects.requireNonNull(resourceId, "Resource ID cannot be null");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        LibraryResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found"));

        Rating newRating = new Rating();
        newRating.setClient(client);
        newRating.setResource(resource);
        newRating.setRating(rating);
        newRating.setComment(comment);

        Rating saved = ratingRepository.save(newRating);
        return mapToRatingResponse(saved);
    }

    @Transactional
    public void deleteRating(Long clientId, Long resourceId) {
        log.info("Deleting rating - client: {}, resource: {}", clientId, resourceId);
        ratingRepository.findByClientUserIDAndResourceResourceID(clientId, resourceId)
                .ifPresent(ratingRepository::delete);
    }

    @Transactional(readOnly = true)
    public RatingResponse getMyRating(Long clientId, Long resourceId) {
        return ratingRepository.findByClientUserIDAndResourceResourceID(clientId, resourceId)
                .map(this::mapToRatingResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public long getRatedCount(Long clientId) {
        return ratingRepository.countByClientUserID(clientId);
    }

    private RatingResponse mapToRatingResponse(Rating rating) {
        LibraryResource resource = rating.getResource();
        return RatingResponse.builder()
                .ratingId(rating.getRatingId())
                .resourceId(resource.getResourceID())
                .title(resource.getTitle())
                .author(resource.getAuthor())
                .category(resource.getCategory() != null ? resource.getCategory().name() : null)
                .resourceImage(resource.getResourceImage())
                .rating(rating.getRating())
                .comment(rating.getComment())
                .ratedAt(rating.getCreatedAt())
                .build();
    }

    // ==================== COMBINED ====================

    @Transactional(readOnly = true)
    public MyListResponse getMyList(Long clientId) {
        log.info("Fetching complete my list for client: {}", clientId);

        return MyListResponse.builder()
                .favorites(getFavorites(clientId))
                .reading(getReadingProgress(clientId))
                .rated(getMyRatings(clientId))
                .favoritesCount(getFavoritesCount(clientId))
                .readingCount(getReadingCount(clientId))
                .ratedCount(getRatedCount(clientId))
                .build();
    }
}
