package com.rallymate.chat.entity;

import com.rallymate.global.exception.BadRequestException;
import com.rallymate.global.exception.ErrorCode;
import com.rallymate.global.exception.ForbiddenException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 26)
    private String ulid;

    @Column(nullable = false, unique = true, updatable = false)
    private Long matchSessionId;

    @Column(nullable = false, updatable = false, length = 26)
    private String uidA;

    @Column(nullable = false, updatable = false, length = 26)
    private String uidB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lockedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private ChatRoom(String ulid, Long matchSessionId, String uidA, String uidB,
                     ChatRoomStatus status, LocalDateTime createdAt) {
        this.ulid = ulid;
        this.matchSessionId = matchSessionId;
        this.uidA = uidA;
        this.uidB = uidB;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static ChatRoom of(String ulid, Long matchSessionId, String uidA, String uidB,
                              LocalDateTime now) {
        return ChatRoom.builder()
                .ulid(ulid)
                .matchSessionId(matchSessionId)
                .uidA(uidA)
                .uidB(uidB)
                .status(ChatRoomStatus.ACTIVE)
                .createdAt(now)
                .build();
    }

    public boolean isParticipant(String uid) {
        return uidA.equals(uid) || uidB.equals(uid);
    }

    public void requireParticipant(String uid) {
        if (!isParticipant(uid)) {
            throw new ForbiddenException(ErrorCode.NOT_A_CHAT_ROOM_PARTICIPANT);
        }
    }

    public void requireActive() {
        if (status == ChatRoomStatus.LOCKED) {
            throw new BadRequestException(ErrorCode.CHAT_ROOM_LOCKED);
        }
    }

    public void lock(LocalDateTime now) {
        if (status == ChatRoomStatus.LOCKED) {
            return;
        }
        this.status = ChatRoomStatus.LOCKED;
        this.lockedAt = now;
    }
}
