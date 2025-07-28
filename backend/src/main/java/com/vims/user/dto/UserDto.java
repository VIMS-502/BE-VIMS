package com.vims.user.dto;

import com.vims.user.entity.OAuthProvider;
import com.vims.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    
    private Long id;
    private String username;
    private String email;
    private String profileImageUrl;
    private UserRole role;
    private OAuthProvider oauthProvider;
    private LocalDateTime createdAt;
} 