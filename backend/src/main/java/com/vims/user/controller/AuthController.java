package com.vims.user.controller;

import com.vims.user.config.JwtTokenProvider;
import com.vims.user.dto.*;
import com.vims.user.service.UserService;
import com.vims.user.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.http.ResponseCookie;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        if (!emailService.isEmailVerified(request.getEmail())) {
            return ResponseEntity.status(403).body(null); // 또는 적절한 메시지
        }
        try {
            UserDto user = userService.signup(request);
            emailService.clearEmailVerification(user.getEmail());
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            log.warn("회원가입 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            UserDto user = userService.login(request);

            UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
            String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

            AuthResponse response = new AuthResponse(accessToken, refreshToken, user);

            // 쿠키 생성 및 응답에 추가
            ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(3600)
                    .sameSite("None")
                    .build();

            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(604800)
                    .sameSite("None")
                    .build();

            log.info("로그인 성공: {}", user.getEmail());
            log.info("accessToken: {}", accessToken);

            return ResponseEntity.ok()
                    .header("Set-Cookie", accessTokenCookie.toString())
                    .header("Set-Cookie", refreshTokenCookie.toString())
                    .body(response);

        } catch (IllegalArgumentException | UsernameNotFoundException e) {
            log.error("로그인 실패: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    // 이메일 코드 생성
    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestParam String email) {
        emailService.sendVerificationCode(email);
        return ResponseEntity.ok().build();
    }

    // 이메일 코드 일치여부확인
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestParam String email, @RequestParam String code) {
        boolean result = emailService.verifyCode(email, code);
        if (result) {

            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().body("인증코드가 일치하지 않습니다.");
        }
    }

}