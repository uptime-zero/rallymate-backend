package com.rallymate.global.jwt;

import com.rallymate.global.exception.ErrorCode;
import com.rallymate.global.response.ApiResponse;
import com.rallymate.user.entity.User;
import com.rallymate.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        // Authorization 헤더가 없거나 Bearer로 시작하지 않으면 다음 필터로 진행
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            // Access Token 검증
            if (!jwtProvider.validateAccessToken(token)) {
                sendErrorResponse(response, ErrorCode.INVALID_TOKEN);
                return;
            }

            // UID 추출 및 사용자 조회
            String uid = jwtProvider.extractSubject(token);
            Optional<User> findUser = userRepository.findByUid(uid);

            if (findUser.isEmpty()) {
                sendErrorResponse(response, ErrorCode.USER_NOT_FOUND);
                return;
            }

            User user = findUser.get();

            // JwtAuthenticationToken 생성 및 SecurityContext에 저장
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                    user,
                    token,
                    "ACCESS",
                    Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getRole()))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            sendErrorResponse(response, ErrorCode.EXPIRED_TOKEN);
        } catch (Exception e) {
            sendErrorResponse(response, ErrorCode.INVALID_TOKEN);
        }
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<?> apiResponse = ApiResponse.fail(errorCode);
        String jsonResponse = objectMapper.writeValueAsString(apiResponse);

        response.getWriter().write(jsonResponse);
    }
}
