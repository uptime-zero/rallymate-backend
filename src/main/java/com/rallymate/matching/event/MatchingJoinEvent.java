package com.rallymate.matching.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Redis Pub/Sub {@code matching:events} 에 발행되는 참가 이벤트입니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MatchingJoinEvent {

    public static final String TYPE_JOIN = "JOIN";

    private String type;
    private String sport;
    private String uid;
}
