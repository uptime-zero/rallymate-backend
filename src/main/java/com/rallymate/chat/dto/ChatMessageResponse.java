package com.rallymate.chat.dto;

import com.rallymate.chat.entity.ChatMessage;
import com.rallymate.chat.entity.ChatMessageType;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        String senderUid,
        ChatMessageType type,
        String content,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getSenderUid(),
                message.getType(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
