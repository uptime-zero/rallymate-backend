package com.rallymate.matching.redis;

import com.rallymate.matching.constant.MatchingRedisChannels;
import com.rallymate.matching.event.MatchingJoinEvent;
import com.rallymate.matching.event.MatchingMatchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 매칭 관련 Redis Pub/Sub 발행기입니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchingRedisPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 대기열 참가 이벤트를 발행합니다. 모든 인스턴스가 수신 후 매칭 시도를 수행합니다.
     */
    public void publishJoin(String sport, String uid) {
        MatchingJoinEvent event = new MatchingJoinEvent(MatchingJoinEvent.TYPE_JOIN, sport, uid);
        try {
            stringRedisTemplate.convertAndSend(MatchingRedisChannels.EVENTS, objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Failed to serialize MatchingJoinEvent", e);
        }
    }

    /**
     * 매칭 성사 이벤트를 발행합니다. 모든 인스턴스가 수신 후 로컬 WebSocket으로 푸시합니다.
     */
    public void publishMatched(Long sessionId, String uidA, String uidB) {
        MatchingMatchedEvent event = new MatchingMatchedEvent(
                MatchingMatchedEvent.TYPE_MATCHED,
                sessionId,
                uidA,
                uidB
        );
        try {
            stringRedisTemplate.convertAndSend(MatchingRedisChannels.MATCHED, objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Failed to serialize MatchingMatchedEvent", e);
        }
    }
}
