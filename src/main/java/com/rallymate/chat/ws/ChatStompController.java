package com.rallymate.chat.ws;

import com.rallymate.chat.dto.ChatMessageRequest;
import com.rallymate.chat.dto.ChatMessageResponse;
import com.rallymate.chat.entity.ChatMessage;
import com.rallymate.chat.entity.ChatRoom;
import com.rallymate.chat.service.ChatMessageService;
import com.rallymate.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat-rooms/{roomUlid}/messages")
    public void sendMessage(
            @DestinationVariable String roomUlid,
            ChatMessageRequest request,
            Principal principal
    ) {
        String senderUid = principal.getName();

        ChatRoom room = chatRoomService.getByUlid(roomUlid);
        ChatMessage saved = chatMessageService.saveText(room.getId(), senderUid, request.content());
        messagingTemplate.convertAndSend("/sub/chat-rooms/" + roomUlid, ChatMessageResponse.from(saved));
    }
}
