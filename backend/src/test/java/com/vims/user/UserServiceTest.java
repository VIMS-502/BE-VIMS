package com.vims.user;

import com.vims.user.dto.SignupRequest;
import com.vims.user.dto.UserDto;
import com.vims.user.entity.OAuthProvider;
import com.vims.user.entity.User;
import com.vims.user.entity.UserRole;
import com.vims.user.repository.UserRepository;
import com.vims.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;


import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
//유저 단위테스트
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
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
        
        //아직 가입이 되지 않은 이메일, 유저이름이라고 가정
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .id(1L)
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash("encodedPassword")
                .oauthProvider(OAuthProvider.LOCAL)
                .role(UserRole.GENERAL)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // when
        UserDto userDto = userService.signup(request);

        // then
        assertThat(userDto.getUsername()).isEqualTo("testuser");
        assertThat(userDto.getEmail()).isEqualTo("testuser@example.com");
        assertThat(userDto.getId()).isEqualTo(1L);
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
        SignupRequest request = new SignupRequest();
        request.setUsername("testuser");
        request.setEmail("testuser@example.com");
        request.setPassword("testpassword123!");
        request.setConfirmPassword("testpassword123!");

        //이메일 있다고 가정
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // when & then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.signup(request)
        );
        assertTrue(exception.getMessage().contains("이미 사용 중인 이메일"));
    }

    @Test
    @DisplayName("이미 존재하는 사용자명으로 회원가입 실패")
    void signupUserFailDueToExistingUsername() {
        // given
        SignupRequest request = new SignupRequest();
        request.setUsername("testuser");
        request.setEmail("testuser2@example.com");
        request.setPassword("testpassword123!");
        request.setConfirmPassword("testpassword123!");

        // 사용자명 중복 상황을 설정
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false); //이메일이 다르다고 가정
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(true); //이름은 같다고 가정

        // when & then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.signup(request)
        );
        assertTrue(exception.getMessage().contains("이미 사용 중인 사용자명"));
    }
    



}