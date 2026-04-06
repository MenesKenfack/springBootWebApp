package com.kaksha.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyListResponse {
    private List<FavoriteResponse> favorites;
    private List<ReadingProgressResponse> reading;
    private List<RatingResponse> rated;
    private long favoritesCount;
    private long readingCount;
    private long ratedCount;
}
