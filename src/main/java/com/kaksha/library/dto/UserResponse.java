package com.kaksha.library.dto;

import com.kaksha.library.model.enums.UserRole;
import com.kaksha.library.model.enums.UserTier;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    
    private Long userID;
    private String username;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String email;
    private UserRole role;
    private UserTier userTier;
    private boolean verified;
    private boolean status;
    private LocalDateTime createdAt;
}
