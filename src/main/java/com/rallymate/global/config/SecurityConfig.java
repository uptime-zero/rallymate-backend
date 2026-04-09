package com.rallymate.global.config;

import com.rallymate.global.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용, Stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 비활성화 (Flutter Application, Web이 아닌 앱 환경)
                .cors(AbstractHttpConfigurer::disable)

                // 세션 사용 안 함 (Stateless)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // URL 별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // /api/v1/auth/** - 인증 불필요 (회원가입, 로그인, SMS 인증, 토큰 재발급 등)
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // WebSocket 핸드셰이크는 모두 허용 (JWT는 STOMP 레벨에서 검증)
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/v1/connect").permitAll()

                        // /api/v1/admin/** - ADMIN 권한 필요
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // 그 외 /api/v1/** - 인증 필요 (USER 이상)
                        .requestMatchers("/api/v1/**").authenticated()

                        // 나머지 요청은 모두 거절
                        .anyRequest().authenticated()
                )

                // JWT 인증 필터 추가 (UsernamePasswordAuthenticationFilter 이전에 실행)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
