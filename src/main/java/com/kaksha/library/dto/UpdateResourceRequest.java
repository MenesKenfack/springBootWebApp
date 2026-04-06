package com.kaksha.library.dto;

import lombok.Data;

@Data
public class UpdateResourceRequest {
    
    private String title;
    private String isbn;
    private Integer publicationYear;
    private String author;
    private String resourceFile;
    private String resourceImage;
    private String description;
    private java.math.BigDecimal price;
    private Boolean isPremiumOnly;
    private Long catalogId;
}
