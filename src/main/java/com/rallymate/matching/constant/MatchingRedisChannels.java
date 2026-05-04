package com.rallymate.matching.constant;

/**
 * 매칭 Redis Pub/Sub 채널 이름 상수입니다.
 */
public final class MatchingRedisChannels {

    /**
     * 대기열 참가/매칭 시도 트리거 (payload: JSON {@code MatchingJoinEvent})
     */
    public static final String EVENTS = "matching:events";

    /**
     * 매칭 성사 브로드캐스트 (payload: JSON {@code MatchingMatchedEvent})
     * <p>모든 애플리케이션 인스턴스가 수신 후 로컬 WebSocket으로 전달합니다.</p>
     */
    public static final String MATCHED = "matching:matched";

    private MatchingRedisChannels() {
    }
}
