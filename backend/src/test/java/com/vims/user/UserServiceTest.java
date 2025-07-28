package com.vims.user;

import com.vims.user.dto.SignupRequest;
import com.vims.user.dto.UserDto;
import com.vims.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signupUserSuccess() {
        // given
        SignupRequest request = new SignupRequest();
        request.setUsername("testuser");
        request.setEmail("testuser@example.com");
        request.setPassword("testpassword123!");
        request.setConfirmPassword("testpassword123!");

        // when
        UserDto userDto = userService.signup(request);

        // then
        assertThat(userDto.getUsername()).isEqualTo("testuser");
        assertThat(userDto.getEmail()).isEqualTo("testuser@example.com");
        assertThat(userDto.getId()).isNotNull();
    }

    @Test
    @DisplayName("비밀번호 달라서 회원가입 실패")
    void signupUserFailDueToPasswordMismatch() {
        // given
        SignupRequest request = new SignupRequest();
        request.setUsername("testuser");
        request.setEmail("testuser@example.com");
        request.setPassword("testpassword123!");
        request.setConfirmPassword("testpassword123!!!");

        // when
        UserDto userDto = userService.signup(request);

        // then
        assertThat(userDto.getUsername()).isEqualTo("testuser");
        assertThat(userDto.getEmail()).isEqualTo("testuser@example.com");
        assertThat(userDto.getId()).isNotNull();
    }

}