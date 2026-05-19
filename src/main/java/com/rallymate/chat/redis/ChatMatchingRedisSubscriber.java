package com.rallymate.chat.redis;

import com.rallymate.chat.service.ChatMessageService;
import com.rallymate.chat.service.ChatRoomService;
import com.rallymate.matching.constant.MatchingRedisChannels;
import com.rallymate.matching.event.MatchSessionFinishedEvent;
import com.rallymate.matching.event.MatchingMatchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMatchingRedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            if (MatchingRedisChannels.MATCHED.equals(channel)) {
                MatchingMatchedEvent event = objectMapper.readValue(body, MatchingMatchedEvent.class);
                if (MatchingMatchedEvent.TYPE_MATCHED.equals(event.getType())) {
                    chatRoomService.createForMatch(event.getSessionId(), event.getUidA(), event.getUidB());
                }
            } else if (MatchingRedisChannels.FINISHED.equals(channel)) {
                MatchSessionFinishedEvent event = objectMapper.readValue(body, MatchSessionFinishedEvent.class);
                handleMatchFinished(event);
            }
        } catch (Exception e) {
            log.error("Chat Redis message handling failed. channel={}, body={}", channel, body, e);
        }
    }

    private void handleMatchFinished(MatchSessionFinishedEvent event) {
        chatRoomService.lockByMatchSessionId(event.getSessionId());

        chatRoomService.findByMatchSessionId(event.getSessionId()).ifPresent(room -> {
            String systemMsg = event.isCancelled() ? "경기가 취소되었습니다." : "경기가 종료되었습니다.";
            chatMessageService.saveSystem(room.getId(), systemMsg);
            messagingTemplate.convertAndSend("/sub/chat-rooms/" + room.getUlid(), systemMsg);
        });
    }
}
