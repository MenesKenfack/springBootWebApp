package com.kaksha.library.dto;

import com.kaksha.library.model.enums.UserRole;
import com.kaksha.library.model.enums.UserTier;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AuthResponse {
    
    private boolean success;
    private String message;
    private String token;
    private String refreshToken;
    private UserInfo user;
    
    @Data
    @Builder
    public static class UserInfo {
        private Long id;
        private String username;
        private String firstName;
        private String lastName;
        private String email;
        private LocalDate dateOfBirth;
        private UserRole role;
        private UserTier tier;
        private boolean verified;
    }
}
