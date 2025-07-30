package com.vims.user.service;

import com.vims.user.dto.*;
import com.vims.user.entity.OAuthProvider;
import com.vims.user.entity.User;
import com.vims.user.entity.UserRole;
import com.vims.user.repository.UserRepository;
import com.vims.user.config.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // UserDetailsService 구현
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }

    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }

    // 회원가입
    @Transactional
    public UserDto signup(SignupRequest request) {
        // 비밀번호 확인
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }

        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다");
        }

        // 사용자명 중복 확인
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 사용자명입니다");
        }

        // 사용자 생성
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .oauthProvider(OAuthProvider.LOCAL)
                .role(UserRole.GENERAL)
                .build();

        User savedUser = userRepository.save(user);
        log.info("새 사용자 가입: {}", savedUser.getEmail());

        return convertToDto(savedUser);
    }

    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }


    // 로그인
    @Transactional
    public UserDto login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + request.getEmail()));
    
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }

        return convertToDto(user);
    }


    //oauth 로그인 (가입이력 없으면 회원가입 )
    @Transactional
    public String oauthLoginOrSignup(String email, String username, String oauthProvider, String oauthId, String profileImageUrl) {
        // provider+oauthId로 먼저 조회
        com.vims.user.entity.OAuthProvider providerEnum = com.vims.user.entity.OAuthProvider.valueOf(oauthProvider.toUpperCase());
        User user = userRepository.findByOauthProviderAndOauthId(providerEnum, oauthId)
                .orElseGet(() -> {
                    // 없으면 회원가입
                    User newUser = User.builder()
                            .email(email)
                            .username(username)
                            .oauthProvider(providerEnum)
                            .oauthId(oauthId)
                            .profileImageUrl(profileImageUrl)
                            .role(UserRole.GENERAL)
                            .build();
                    return userRepository.save(newUser);
                });

        return jwtTokenProvider.generateAccessToken(user);
    }
    
    //비밀번호 변경
    @Transactional
    public void passwordChange(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        user.updatePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("비밀번호 변경 완료: {}", email);
    }
    
    //userName변경 (userId 기반)
    @Transactional
    public void userNameChangeById(String newUserName, Long userId) {
        User user = findById(userId);
        user.updateUserName(newUserName);
        userRepository.save(user);
        log.info("userName 변경 완료: {}", newUserName);
        log.info("유저 아이디 : {}", userId);
    }
    
    //password확인후, 유저 삭제
    @Transactional
    public void deleteUserWithPasswordCheck(Long userId, String rawPassword) {
        log.info("[회원탈퇴] 요청 userId: {}", userId);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
//                log.warn("[회원탈퇴] 존재하지 않는 사용자: {}", userId);
                return new IllegalArgumentException("존재하지 않는 사용자입니다.");
            });
        boolean passwordMatch = passwordEncoder.matches(rawPassword, user.getPasswordHash());
//        log.info("[회원탈퇴] 비밀번호 일치 여부: {}", passwordMatch);
        if (!passwordMatch) {
//            log.warn("[회원탈퇴] 비밀번호 불일치: userId={}, 입력값={}", userId, rawPassword);
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        userRepository.delete(user);
//        log.info("[회원탈퇴] 삭제 완료: userId={}, email={}", userId, user.getEmail());
    }


}