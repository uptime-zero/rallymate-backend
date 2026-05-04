package com.rallymate.matching.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Redis Pub/Sub {@code matching:matched} 에 발행되는 매칭 성사 이벤트입니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MatchingMatchedEvent {

    public static final String TYPE_MATCHED = "MATCHED";

    private String type;
    private Long sessionId;
    private String uidA;
    private String uidB;
}
