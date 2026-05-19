package com.rallymate.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "chat_messages",
    indexes = @Index(name = "idx_chat_messages_room_created", columnList = "chat_room_id, created_at")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_room_id", nullable = false, updatable = false)
    private Long chatRoomId;

    @Column(length = 26, updatable = false)
    private String senderUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ChatMessageType type;

    @Column(nullable = false, updatable = false, length = 1000)
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private ChatMessage(Long chatRoomId, String senderUid, ChatMessageType type,
                        String content, LocalDateTime createdAt) {
        this.chatRoomId = chatRoomId;
        this.senderUid = senderUid;
        this.type = type;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static ChatMessage text(Long chatRoomId, String senderUid, String content,
                                   LocalDateTime now) {
        return ChatMessage.builder()
                .chatRoomId(chatRoomId)
                .senderUid(senderUid)
                .type(ChatMessageType.TEXT)
                .content(content)
                .createdAt(now)
                .build();
    }

    public static ChatMessage system(Long chatRoomId, String content, LocalDateTime now) {
        return ChatMessage.builder()
                .chatRoomId(chatRoomId)
                .type(ChatMessageType.SYSTEM)
                .content(content)
                .createdAt(now)
                .build();
    }
}
