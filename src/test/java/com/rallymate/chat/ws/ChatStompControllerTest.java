package com.rallymate.chat.ws;

import com.rallymate.chat.dto.ChatMessageRequest;
import com.rallymate.chat.dto.ChatMessageResponse;
import com.rallymate.chat.entity.ChatMessage;
import com.rallymate.chat.entity.ChatRoom;
import com.rallymate.chat.service.ChatMessageService;
import com.rallymate.chat.service.ChatRoomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatStompControllerTest {

    @Mock
    ChatRoomService chatRoomService;

    @Mock
    ChatMessageService chatMessageService;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    ChatStompController controller;

    private Principal principalOf(String uid) {
        return () -> uid;
    }

    // ── sendMessage ───────────────────────────────────────────────────────────

    @Test
    void sendMessage_저장_후_브로드캐스트() {
        ChatRoom room = ChatRoom.of("room-ulid", 1L, "uid-sender", "uid-other", LocalDateTime.now());
        ChatMessage saved = ChatMessage.text(room.getId(), "uid-sender", "안녕하세요", LocalDateTime.now());
        given(chatRoomService.getByUlid("room-ulid")).willReturn(room);
        given(chatMessageService.saveText(any(), eq("uid-sender"), eq("안녕하세요"))).willReturn(saved);

        controller.sendMessage("room-ulid", new ChatMessageRequest("안녕하세요"), principalOf("uid-sender"));

        verify(chatMessageService).saveText(room.getId(), "uid-sender", "안녕하세요");
        verify(messagingTemplate).convertAndSend(eq("/sub/chat-rooms/room-ulid"), any(ChatMessageResponse.class));
    }
}
