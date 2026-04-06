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
public class ActivityLogResponse {
    private Long logId;
    private String activityType;
    private String description;
    private String resourceTitle;
    private LocalDateTime timestamp;
    private String icon;
    private String iconColor;
    private String formattedTime;
}
