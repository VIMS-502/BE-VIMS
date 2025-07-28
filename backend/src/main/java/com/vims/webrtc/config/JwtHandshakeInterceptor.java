package com.vims.webrtc.config;

import com.vims.user.config.JwtTokenProvider;
import com.vims.user.entity.User;
import com.vims.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        String token = getTokenFromRequest(request);
        
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            User user = userRepository.findById(userId).orElse(null);
            
            if (user != null) {
                // 세션 attributes에 사용자 정보 저장
                attributes.put("userId", user.getId());
                attributes.put("userName", user.getUsername());
                attributes.put("userEmail", user.getEmail());
                attributes.put("userRole", user.getRole().name());
                attributes.put("connectionTime", LocalDateTime.now());
                
                log.info("Signaling WebSocket JWT 인증 성공: userId={}, userName={}", 
                        user.getId(), user.getUsername());
                return true;
            } else {
                log.warn("Signaling WebSocket JWT 인증 실패: 사용자를 찾을 수 없음. userId={}", userId);
                return false;
            }
        } else {
            log.warn("Signaling WebSocket 연결 시 유효하지 않은 JWT 토큰");
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 핸드셰이크 완료 후 처리할 로직이 있다면 여기에 추가
    }

    private String getTokenFromRequest(ServerHttpRequest request) {
        // Authorization 헤더에서 토큰 추출
        String bearerToken = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // 쿼리 파라미터에서 토큰 추출 (fallback)
        URI uri = request.getURI();
        String query = uri.getQuery();
        if (StringUtils.hasText(query)) {
            Map<String, String> queryParams = UriComponentsBuilder.fromUriString("?" + query)
                    .build()
                    .getQueryParams()
                    .toSingleValueMap();
            
            String token = queryParams.get("token");
            if (StringUtils.hasText(token)) {
                return token;
            }
        }
        
        return null;
    }
}