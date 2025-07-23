package com.vims.webrtc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll() 
                // .requestMatchers("/ws/**").permitAll()
                // .requestMatchers("/comprehensive-chat-test.html").permitAll()
                // .requestMatchers("/sockjs.min.js").permitAll()
                // .requestMatchers("/stomp.min.js").permitAll()
                // .requestMatchers("/api/chat/**").permitAll()
                // .requestMatchers("/api/rooms/**").permitAll()
                // .requestMatchers("/h2-console/**").permitAll()
                // //시그널링 + webRTC
                // .requestMatchers("/signaling").permitAll()  // 이 줄 추가
                // .requestMatchers("/ws/**").permitAll()
                // .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}