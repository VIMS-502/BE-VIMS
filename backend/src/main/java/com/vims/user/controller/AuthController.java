package com.vims.user.controller;

import com.vims.user.config.JwtTokenProvider;
import com.vims.user.dto.*;
import com.vims.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        try {
            UserDto user = userService.signup(request);
            
            // JWT 토큰 생성
            UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
            String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);
            
            AuthResponse response = new AuthResponse(accessToken, refreshToken, user);
            
            log.info("회원가입 성공: {}", user.getEmail());
            log.info("accessToken: {}", accessToken);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("회원가입 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }


} 