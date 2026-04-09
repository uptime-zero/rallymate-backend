package com.rallymate.matching.redis;

import com.rallymate.matching.constant.MatchingRedisChannels;
import com.rallymate.matching.event.MatchingJoinEvent;
import com.rallymate.matching.event.MatchingMatchedEvent;
import com.rallymate.matching.service.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * 매칭 Redis Pub/Sub 구독자입니다.
 * <p>
 * - {@link MatchingRedisChannels#EVENTS}: 참가 후 분산 매칭 시도
 * - {@link MatchingRedisChannels#MATCHED}: 매칭 성사 시 각 인스턴스에서 WebSocket 전달
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchingRedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final MatchingService matchingService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            if (MatchingRedisChannels.EVENTS.equals(channel)) {
                MatchingJoinEvent event = objectMapper.readValue(body, MatchingJoinEvent.class);
                if (MatchingJoinEvent.TYPE_JOIN.equals(event.getType())) {
                    matchingService.tryMatchAfterJoin(event.getSport(), event.getUid());
                }
            } else if (MatchingRedisChannels.MATCHED.equals(channel)) {
                MatchingMatchedEvent event = objectMapper.readValue(body, MatchingMatchedEvent.class);
                if (MatchingMatchedEvent.TYPE_MATCHED.equals(event.getType())) {
                    matchingService.notifyMatchedOnAllInstances(event.getSessionId(), event.getUidA(), event.getUidB());
                }
            }
        } catch (Exception e) {
            log.error("Redis matching message handling failed. channel={}, body={}", channel, body, e);
        }
    }
}
