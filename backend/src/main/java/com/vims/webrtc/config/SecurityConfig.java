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
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/chat-test.html").permitAll()
                .requestMatchers("/local-chat.html").permitAll()
                .requestMatchers("/simple-chat.html").permitAll()
                .requestMatchers("/comprehensive-chat-test.html").permitAll()
                .requestMatchers("/simple-test.html").permitAll()
                .requestMatchers("/websocket-debug.html").permitAll()
                .requestMatchers("/history-test.html").permitAll()
                .requestMatchers("/sockjs.min.js").permitAll()
                .requestMatchers("/stomp.min.js").permitAll()
                .requestMatchers("/api/lectures/**").permitAll()
                .requestMatchers("/api/chat/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions().disable());
        
        return http.build();
    }
}