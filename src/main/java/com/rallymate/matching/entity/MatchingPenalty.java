package com.rallymate.matching.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "matching_penalties")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchingPenalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String uid;

    @Column(nullable = false)
    private int rejectCountTotal;

    @Column
    private LocalDateTime lastRejectAt;

    @Column
    private LocalDateTime penaltyUntil;

    @Builder(access = AccessLevel.PRIVATE)
    private MatchingPenalty(String uid, int rejectCountTotal, LocalDateTime lastRejectAt, LocalDateTime penaltyUntil) {
        this.uid = uid;
        this.rejectCountTotal = rejectCountTotal;
        this.lastRejectAt = lastRejectAt;
        this.penaltyUntil = penaltyUntil;
    }

    public static MatchingPenalty of(String uid) {
        return MatchingPenalty.builder()
                .uid(uid)
                .rejectCountTotal(0)
                .build();
    }

    public void updateForReject(LocalDateTime now) {
        this.rejectCountTotal += 1;
        this.lastRejectAt = now;
    }

    public void imposePenaltyUntil(LocalDateTime penaltyUntil) {
        this.penaltyUntil = penaltyUntil;
    }

    public void clearActivePenalty() {
        this.penaltyUntil = null;
    }
}

