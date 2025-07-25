package com.vims.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.vims.user.config.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
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
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}