package com.rallymate.chat.dto;

import com.rallymate.chat.entity.ChatRoom;
import com.rallymate.chat.entity.ChatRoomStatus;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        String ulid,
        Long matchSessionId,
        String uidA,
        String uidB,
        ChatRoomStatus status,
        LocalDateTime createdAt,
        LocalDateTime lockedAt
) {
    public static ChatRoomResponse from(ChatRoom room) {
        return new ChatRoomResponse(
                room.getUlid(),
                room.getMatchSessionId(),
                room.getUidA(),
                room.getUidB(),
                room.getStatus(),
                room.getCreatedAt(),
                room.getLockedAt()
        );
    }
}
