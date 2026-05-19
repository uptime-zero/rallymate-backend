package com.rallymate.chat.ws;

import com.rallymate.chat.entity.ChatRoom;
import com.rallymate.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRoomChannelInterceptor implements ChannelInterceptor {

    private static final String CHAT_ROOM_DESTINATION_PREFIX = "/sub/chat-rooms/";
    private static final String CHAT_ROOM_SEND_PREFIX = "/pub/chat-rooms/";

    private final ChatRoomService chatRoomService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        String destination = accessor.getDestination();
        String uid = extractUid(accessor);

        if (StompCommand.SUBSCRIBE.equals(command) && destination != null
                && destination.startsWith(CHAT_ROOM_DESTINATION_PREFIX)) {

            String roomUlid = destination.substring(CHAT_ROOM_DESTINATION_PREFIX.length());
            ChatRoom room = chatRoomService.getByUlid(roomUlid);
            room.requireParticipant(uid);
        }

        if (StompCommand.SEND.equals(command) && destination != null
                && destination.startsWith(CHAT_ROOM_SEND_PREFIX)) {

            String roomUlid = extractRoomUlidFromSendDestination(destination);
            if (roomUlid != null) {
                ChatRoom room = chatRoomService.getByUlid(roomUlid);
                room.requireParticipant(uid);
                chatRoomService.requireActiveOrSync(roomUlid);
            }
        }

        return message;
    }

    private String extractUid(StompHeaderAccessor accessor) {
        if (accessor.getUser() != null) {
            return accessor.getUser().getName();
        }
        return null;
    }

    private String extractRoomUlidFromSendDestination(String destination) {
        // /pub/chat-rooms/{roomUlid}/messages
        String withoutPrefix = destination.substring(CHAT_ROOM_SEND_PREFIX.length());
        int slashIdx = withoutPrefix.indexOf('/');
        if (slashIdx < 0) {
            return withoutPrefix;
        }

        return withoutPrefix.substring(0, slashIdx);
    }
}
