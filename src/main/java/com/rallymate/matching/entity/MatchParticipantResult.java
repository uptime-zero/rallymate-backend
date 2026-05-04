package com.rallymate.matching.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "match_participant_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchParticipantResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long matchSessionId;

    @Column(nullable = false)
    private String uid;

    @Column(nullable = false)
    private int ratingBefore;

    @Column(nullable = false)
    private int ratingAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchResult result;

    @Builder(access = AccessLevel.PRIVATE)
    private MatchParticipantResult(Long matchSessionId, String uid, int ratingBefore, int ratingAfter, MatchResult result) {
        this.matchSessionId = matchSessionId;
        this.uid = uid;
        this.ratingBefore = ratingBefore;
        this.ratingAfter = ratingAfter;
        this.result = result;
    }

    public static MatchParticipantResult of(Long matchSessionId, String uid, int ratingBefore, int ratingAfter, MatchResult result) {
        return MatchParticipantResult.builder()
                .matchSessionId(matchSessionId)
                .uid(uid)
                .ratingBefore(ratingBefore)
                .ratingAfter(ratingAfter)
                .result(result)
                .build();
    }
}

