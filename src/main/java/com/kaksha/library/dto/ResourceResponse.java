package com.kaksha.library.dto;

import com.kaksha.library.model.enums.ResourceCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ResourceResponse {
    
    private Long resourceID;
    private String title;
    private String isbn;
    private Integer publicationYear;
    private String author;
    private ResourceCategory category;
    private String resourceImage;
    private String description;
    private BigDecimal price;
    private boolean isPremiumOnly;
    private String catalogName;
    private boolean hasFullAccess;
    private String userTier;
    private String previewContent;
    private String fullContent;
    private String resourceFile;
    private LocalDateTime createdAt;
}
