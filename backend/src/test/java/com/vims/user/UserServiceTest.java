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
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class UserServiceTest {

    @Autowired
    private UserService userService;

    //given => when => then 순서
    
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
    @DisplayName("비밀번호 불일치로 회원가입 실패")
    void signupUserFailDueToPasswordMismatch() {
        // given
        SignupRequest request = new SignupRequest();
        request.setUsername("testuser");
        request.setEmail("testuser@example.com");
        request.setPassword("testpassword123!");
        request.setConfirmPassword("testpassword123!!!");

        // when & then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.signup(request)
        );
        assertTrue(exception.getMessage().contains("비밀번호가 일치하지 않습니다"));
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 회원가입 실패")
    void signupUserFailDueToExistingEmail() {
        // given
        SignupRequest request1 = new SignupRequest();
        request1.setUsername("user1");
        request1.setEmail("testuser@example.com");
        request1.setPassword("testpassword123!");
        request1.setConfirmPassword("testpassword123!");

        SignupRequest request2 = new SignupRequest();
        request2.setUsername("user2");
        request2.setEmail("testuser@example.com"); // 같은 이메일
        request2.setPassword("testpassword123!");
        request2.setConfirmPassword("testpassword123!");

        // 첫 번째 회원가입은 성공
        userService.signup(request1);

        // 두 번째 회원가입은 예외 발생해야 함
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.signup(request2)
        );
        assertTrue(exception.getMessage().contains("이미 사용 중인 이메일"));
    }


}