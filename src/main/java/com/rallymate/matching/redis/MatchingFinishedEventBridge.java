package com.rallymate.matching.redis;

import com.rallymate.matching.event.MatchSessionFinishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 매칭 세션 완료 트랜잭션이 커밋된 직후 Redis Pub/Sub으로 결과 이벤트를 발행합니다.
 * AFTER_COMMIT으로 등록하여 롤백 시 메시지가 나가지 않도록 보장합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchingFinishedEventBridge {

    private final MatchingRedisPublisher matchingRedisPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchFinished(MatchSessionFinishedEvent event) {
        log.debug("Publishing matching:finished for sessionId={}", event.getSessionId());
        matchingRedisPublisher.publishFinished(
                event.getSessionId(), event.getUidA(), event.getUidB(), event.isCancelled());
    }
}
