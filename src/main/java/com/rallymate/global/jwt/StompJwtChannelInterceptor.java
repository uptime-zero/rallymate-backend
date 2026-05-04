package com.rallymate.global.jwt;

import com.rallymate.global.exception.UnauthorizedException;
import com.rallymate.user.entity.User;
import com.rallymate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

import static com.rallymate.global.exception.ErrorCode.UNAUTHORIZED;

/**
 * STOMP CONNECT 시 Authorization 헤더의 JWT를 검증하고
 * {@link Principal} 에 사용자 UID를 설정합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            if (authHeaders == null || authHeaders.isEmpty()) {
                throw new UnauthorizedException(UNAUTHORIZED);
            }

            String header = authHeaders.getFirst();
            String token = header.startsWith("Bearer ") ? header.substring(7) : header;

            if (!jwtProvider.validateAccessToken(token)) {
                throw new UnauthorizedException(UNAUTHORIZED);
            }

            String uid = jwtProvider.extractSubject(token);
            User user = userRepository.findByUid(uid)
                    .orElseThrow(() -> new UnauthorizedException(UNAUTHORIZED));

            Principal principal = user::getUid;
            accessor.setUser(principal);

            log.debug("STOMP CONNECT authenticated uid={}", uid);
        }

        return message;
    }
}

