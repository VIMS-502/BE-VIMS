package com.vims.user.repository;

import com.vims.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 이메일 중복 확인
    boolean existsByEmail(String email);

    // 이메일로 사용자 찾기
    Optional<User> findByEmail(String email);

    // 사용자명 중복 확인
    boolean existsByUsername(String username);

    // 소셜 로그인: provider+oauthId로 사용자 찾기
    Optional<User> findByOauthProviderAndOauthId(com.vims.user.entity.OAuthProvider oauthProvider, String oauthId);

    // 사용자아이디로 사용자 찾기
    Optional<User> findById(Long id);
}
