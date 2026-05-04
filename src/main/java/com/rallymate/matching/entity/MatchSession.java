package com.rallymate.matching.entity;

import com.rallymate.global.exception.ErrorCode;
import com.rallymate.global.exception.ForbiddenException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sport;

    @Column(nullable = false)
    private int stage;

    @Column(nullable = false, name = "uid_a")
    private String uidA;

    @Column(nullable = false, name = "uid_b")
    private String uidB;

    @Column(nullable = false, name = "is_accepted_a")
    private boolean isAcceptedA;

    @Column(nullable = false, name = "is_accepted_b")
    private boolean isAcceptedB;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column
    private String chatRoomId;

    @Column(nullable = false)
    private boolean finished;

    @Builder(access = AccessLevel.PRIVATE)
    private MatchSession(String sport, int stage, String uidA, String uidB,
                         LocalDateTime createdAt, LocalDateTime expiresAt, boolean finished) {
        this.sport = sport;
        this.stage = stage;
        this.uidA = uidA;
        this.uidB = uidB;
        this.isAcceptedA = false;
        this.isAcceptedB = false;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.finished = finished;
    }

    public static MatchSession of(String sport, int stage, String uidA, String uidB,
                                  LocalDateTime now, LocalDateTime expiresAt) {
        return MatchSession.builder()
                .sport(sport)
                .stage(stage)
                .uidA(uidA)
                .uidB(uidB)
                .createdAt(now)
                .expiresAt(expiresAt)
                .finished(false)
                .build();
    }

    public String getOpponentUid(String myUid) {
        if (myUid.equals(uidA)) {
            return uidB;
        }

        if (myUid.equals(uidB)) {
            return uidA;
        }

        throw new ForbiddenException(ErrorCode.NOT_A_MATCH_PARTICIPANT);
    }

    public void recordAccept(String uid) {
        if (uid.equals(uidA)) {
            this.isAcceptedA = true;
        } else if (uid.equals(uidB)) {
            this.isAcceptedB = true;
        } else {
            throw new ForbiddenException(ErrorCode.NOT_A_MATCH_PARTICIPANT);
        }
    }

    public boolean isBothAccepted() {
        return isAcceptedA && isAcceptedB;
    }

    public void attachChatRoom(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public void markFinished() {
        this.finished = true;
    }
}