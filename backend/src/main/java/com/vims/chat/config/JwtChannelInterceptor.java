package com.vims.chat.config;

import com.vims.user.config.JwtTokenProvider;
import com.vims.user.entity.User;
import com.vims.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = getTokenFromHeaders(accessor);
            
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                User user = userRepository.findById(userId).orElse(null);
                
                if (user != null) {
                    // 세션에 사용자 정보 저장
                    accessor.getSessionAttributes().put("userId", user.getId());
                    accessor.getSessionAttributes().put("userName", user.getUsername());
                    accessor.getSessionAttributes().put("userEmail", user.getEmail());
                    accessor.getSessionAttributes().put("userRole", user.getRole().name());
                    accessor.getSessionAttributes().put("connectionTime", LocalDateTime.now());
                    
                    // STOMP Principal 설정 (개인 메시지용)
                    Principal principal = () -> user.getId().toString();
                    accessor.setUser(principal);
                    
                    // Spring Security 컨텍스트 설정
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.info("STOMP WebSocket JWT 인증 성공: userId={}, userName={}, principal={}", 
                            user.getId(), user.getUsername(), principal.getName());
                } else {
                    log.warn("STOMP WebSocket JWT 인증 실패: 사용자를 찾을 수 없음. userId={}", userId);
                    throw new IllegalArgumentException("유효하지 않은 사용자입니다.");
                }
            } else {
                log.warn("STOMP WebSocket 연결 시 유효하지 않은 JWT 토큰");
                throw new IllegalArgumentException("유효하지 않은 인증 토큰입니다.");
            }
        }
        
        return message;
    }

    private String getTokenFromHeaders(StompHeaderAccessor accessor) {
        // Authorization 헤더에서 토큰 추출
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String bearerToken = authHeaders.get(0);
            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
        }
        
        // 쿼리 파라미터에서 토큰 추출 (fallback)
        List<String> tokenParams = accessor.getNativeHeader("token");
        if (tokenParams != null && !tokenParams.isEmpty()) {
            return tokenParams.get(0);
        }
        
        return null;
    }
}