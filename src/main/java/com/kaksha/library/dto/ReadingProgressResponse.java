package com.kaksha.library.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingProgressResponse {
    private Long progressId;
    private Long resourceId;
    private String title;
    private String author;
    private String category;
    private String resourceImage;
    private Integer progress;
    private LocalDateTime lastReadAt;
    private LocalDateTime updatedAt;
}
