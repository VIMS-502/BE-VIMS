package com.vims.user;


import com.vims.user.config.JwtTokenProvider;
import com.vims.user.dto.LoginRequest;
import com.vims.user.dto.SignupRequest;
import com.vims.user.service.EmailService;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


// MockMvc 관련
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.springframework.http.MediaType;

// ObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper;

// JUnit assertions
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// JsonPath 라이브러리 (com.jayway.jsonpath.JsonPath)
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@AutoConfigureMockMvc
//유저 통합테스트
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private EmailService emailService;


    @BeforeEach
    void setup() throws Exception {

        // 이메일 인증 상태 미리 true로 설정 (Mocking 시)
        when(emailService.isEmailVerified("testuser@example.com")).thenReturn(true);

        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setEmail("testuser@example.com");
        signupRequest.setPassword("testPassword123!");
        signupRequest.setConfirmPassword("testPassword123!");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(signupRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인 성공 및 JWT 토큰 반환, 토큰 내부 정보 검증")
    void loginAndJwtValidation() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("testuser@example.com");
        loginRequest.setPassword("testPassword123!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())  // $.token → $.accessToken
                .andExpect(jsonPath("$.user.username").value("testuser"))  // $.username → $.user.username
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        String token = JsonPath.read(responseJson, "$.accessToken");  // $.token → $.accessToken

        // JWT 토큰 내부 정보 검증
        String subject = jwtTokenProvider.getClaimFromToken(token, Claims::getSubject);
        assertEquals("1", subject);  // userId가 토큰 Subject

        // 토큰 유효성 검증
        assertTrue(jwtTokenProvider.validateToken(token));

        // 추가로 응답 데이터도 검증 가능
        String username = JsonPath.read(responseJson, "$.user.username");
        assertEquals("testuser", username);
    }


}