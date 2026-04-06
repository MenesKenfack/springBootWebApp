package com.kaksha.library.dto;

import com.kaksha.library.model.enums.ResourceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateResourceRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String isbn;
    
    private Integer publicationYear;
    
    private String author;
    
    @NotNull(message = "Category is required")
    private ResourceCategory category;
    
    private String resourceFile;
    
    private String resourceImage;
    
    private String description;
    
    @Positive(message = "Price must be positive")
    private BigDecimal price;
    
    private boolean isPremiumOnly;
    
    @NotNull(message = "Catalog ID is required")
    private Long catalogId;
}
