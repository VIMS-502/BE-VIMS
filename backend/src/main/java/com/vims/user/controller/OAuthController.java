package com.vims.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vims.user.dto.UserDto;
import com.vims.user.entity.User;
import com.vims.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
@Slf4j
public class OAuthController {

    @Value("${google.client.id}")
    private String googleClientId;

    @Value("${google.client.secret}")
    private String googleClientSecret;

    @Value("${google.redirect.uri}")
    private String googleRedirectUri;

    private final UserService userService;

    @GetMapping("/google/login")
    public ResponseEntity<?> googleLogin(@RequestParam("code") String code) {
        try {
            // 1. 구글에 토큰 요청
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String tokenRequestBody =
                    "code=" + code +
                            "&client_id=" + googleClientId +
                            "&client_secret=" + googleClientSecret +
                            "&redirect_uri=" + googleRedirectUri +
                            "&grant_type=authorization_code";

            HttpEntity<String> tokenRequest = new HttpEntity<>(tokenRequestBody, headers);

            ResponseEntity<String> tokenResponse = restTemplate.exchange(
                    "https://oauth2.googleapis.com/token",
                    HttpMethod.POST,
                    tokenRequest,
                    String.class
            );

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode tokenJson = objectMapper.readTree(tokenResponse.getBody());
            String accessToken = tokenJson.get("access_token").asText();

            // 2. 구글에서 사용자 정보 요청
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.setBearerAuth(accessToken);
            HttpEntity<String> userInfoRequest = new HttpEntity<>(userInfoHeaders);

            ResponseEntity<String> userInfoResponse = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v2/userinfo",
                    HttpMethod.GET,
                    userInfoRequest,
                    String.class
            );

            JsonNode userInfo = objectMapper.readTree(userInfoResponse.getBody());
            String email = userInfo.get("email").asText();
            String username = userInfo.get("name").asText();
            String oauthId = userInfo.get("id").asText();
            String profileImageUrl = userInfo.has("picture") ? userInfo.get("picture").asText() : null;

            // 회원가입/로그인 + JWT 발급
            String jwt = userService.oauthLoginOrSignup(
                    email, username, "GOOGLE", oauthId, profileImageUrl
            );

            // 서비스에서 UserDto 조회
            UserDto userDto = userService.getUserDtoByEmail(email);

            // 🚀 토큰과 사용자 정보를 모두 포함하는 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", jwt);  // 프론트엔드에서 기대하는 필드명
            response.put("user", userDto);     // 사용자 정보

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("구글 로그인 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}