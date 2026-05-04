package com.rallymate.matching.service;

import com.rallymate.global.exception.BadRequestException;
import com.rallymate.global.exception.ForbiddenException;
import com.rallymate.global.exception.NotFoundException;
import com.rallymate.global.response.ApiResponse;
import com.rallymate.matching.dto.MatchCompletionsRequest;
import com.rallymate.matching.dto.MatchResponsesRequest;
import com.rallymate.matching.dto.MatchStartRequest;
import com.rallymate.matching.entity.MatchParticipantResult;
import com.rallymate.matching.entity.MatchResult;
import com.rallymate.matching.entity.MatchSession;
import com.rallymate.matching.entity.MatchingPenalty;
import com.rallymate.matching.redis.MatchingQueueRedisService;
import com.rallymate.matching.redis.MatchingQueueRedisService.WaitingUserSnapshot;
import com.rallymate.matching.redis.MatchingRedisPublisher;
import com.rallymate.matching.repository.MatchParticipantResultRepository;
import com.rallymate.matching.repository.MatchSessionRepository;
import com.rallymate.matching.repository.MatchingPenaltyRepository;
import com.rallymate.user.entity.User;
import com.rallymate.user.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.rallymate.global.exception.ErrorCode.*;

/**
 * 실시간 매칭 로직을 담당하는 서비스입니다.
 * <p>
 * - 대기열은 Redis(ZSET + Hash)로 관리하며, 인스턴스 간 공유됩니다.
 * - 참가 시 Redis Pub/Sub으로 이벤트를 발행하고, 구독자가 분산 락 하에 매칭을 시도합니다.
 * - 매칭 성사 시 다시 Pub/Sub으로 브로드캐스트하여 각 인스턴스가 로컬 WebSocket으로 전달합니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MatchingService {

    private final UserRepository userRepository;
    private final MatchSessionRepository matchSessionRepository;
    private final MatchParticipantResultRepository resultRepository;
    private final MatchingPenaltyRepository penaltyRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final MatchingQueueRedisService queueRedis;
    private final MatchingRedisPublisher matchingRedisPublisher;

    /**
     * 매칭 시작 요청을 처리합니다.
     *
     * @param uid     현재 사용자 UID
     * @param request 매칭 시작 정보 (종목, 위치)
     */
    @Transactional
    public void startMatching(String uid, MatchStartRequest request) {
        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));

        if (queueRedis.isUserInQueue(uid)) {
            throw new BadRequestException(ALREADY_MATCHING);
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<MatchingPenalty> penaltyOpt = penaltyRepository.findByUid(uid);
        if (penaltyOpt.isPresent()) {
            MatchingPenalty penalty = penaltyOpt.get();
            if (penalty.getPenaltyUntil() != null && penalty.getPenaltyUntil().isAfter(now)) {
                throw new ForbiddenException(MATCHING_UNAVAILABLE);
            }
        }

        long sinceEpochMillis = System.currentTimeMillis();
        queueRedis.enqueue(
                uid,
                request.getSport(),
                user.getEloRating(),
                request.getLatitude(),
                request.getLongitude(),
                sinceEpochMillis
        );

        matchingRedisPublisher.publishJoin(request.getSport(), uid);
    }

    /**
     * Redis Pub/Sub {@code matching:events} 수신 후 호출됩니다.
     * <p>종목별 분산 락을 획득한 인스턴스만 매칭 알고리즘을 실행합니다.</p>
     *
     * @param sport     종목
     * @param anchorUid 방금 참가한 사용자 UID (앵커)
     */
    @Transactional
    public void tryMatchAfterJoin(String sport, String anchorUid) {
        if (!queueRedis.tryLockSport(sport)) {
            return;
        }

        WaitingUserSnapshot anchor = queueRedis.loadUser(anchorUid);
        if (anchor == null) {
            return;
        }

        List<String> uidList = queueRedis.listWaitingUidsOrdered(sport);
        List<WaitingUserSnapshot> candidates = new ArrayList<>();
        for (String u : uidList) {
            WaitingUserSnapshot snap = queueRedis.loadUser(u);
            if (snap != null) {
                candidates.add(snap);
            }
        }

        candidates.removeIf(w -> w.getUid().equals(anchor.getUid()));
        candidates.removeIf(w -> !w.getSport().equals(anchor.getSport()));

        if (candidates.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int newStage = calculateStage(anchor.getWaitingSince(), now);

        List<WaitingUserSnapshot> filtered = new ArrayList<>();
        for (WaitingUserSnapshot other : candidates) {
            int otherStage = calculateStage(other.getWaitingSince(), now);
            int stage = Math.max(newStage, otherStage);
            int ratingTolerance = getRatingTolerance(stage);
            double radiusKm = getRadiusKm(stage);

            int ratingDiff = Math.abs(anchor.getRating() - other.getRating());
            double distanceKm = distanceInKm(
                    anchor.getLatitude(), anchor.getLongitude(),
                    other.getLatitude(), other.getLongitude()
            );

            if (ratingDiff <= ratingTolerance && distanceKm <= radiusKm) {
                filtered.add(other);
            }
        }

        if (filtered.isEmpty()) {
            return;
        }

        filtered.sort(Comparator
                .comparingInt((WaitingUserSnapshot w) -> Math.abs(anchor.getRating() - w.getRating()))
                .thenComparingDouble(w -> distanceInKm(
                        anchor.getLatitude(), anchor.getLongitude(),
                        w.getLatitude(), w.getLongitude()))
                .thenComparing(WaitingUserSnapshot::getWaitingSince));

        WaitingUserSnapshot opponent = filtered.getFirst();

        LocalDateTime expiresAt = now.plusMinutes(2);
        int finalStage = Math.max(newStage, calculateStage(opponent.getWaitingSince(), now));

        MatchSession session = MatchSession.of(
                anchor.getSport(),
                finalStage,
                anchor.getUid(),
                opponent.getUid(),
                now,
                expiresAt
        );
        matchSessionRepository.save(session);

        queueRedis.removePair(sport, anchor.getUid(), opponent.getUid());

        matchingRedisPublisher.publishMatched(session.getId(), anchor.getUid(), opponent.getUid());
    }

    /**
     * Redis Pub/Sub {@code matching:matched} 수신 후 각 인스턴스에서 호출됩니다.
     * <p>해당 인스턴스에 WebSocket 세션이 붙어 있는 사용자에게만 전달됩니다.</p>
     */
    public void notifyMatchedOnAllInstances(Long sessionId, String uidA, String uidB) {
        sendMatchFound(uidA, uidB, sessionId);
        sendMatchFound(uidB, uidA, sessionId);
    }

    /**
     * 매칭 수락/거절 응답을 처리합니다.
     *
     * @param uid     현재 사용자 UID
     * @param request 응답 정보 (세션 ID, 수락 여부)
     */
    @Transactional
    public void responsesMatch(String uid, MatchResponsesRequest request) {
        MatchSession session = matchSessionRepository.findById(request.getMatchSessionId())
                .orElseThrow(() -> new NotFoundException(MATCHING_SESSION_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        if (session.getExpiresAt().isBefore(now)) {
            handleReject(uid, session, now);
            return;
        }

        boolean accept = Boolean.TRUE.equals(request.getAccept());
        if (!accept) {
            handleReject(uid, session, now);
            return;
        }

        session.recordAccept(uid); // 수락 기록
        matchSessionRepository.save(session);

        if (session.isBothAccepted()) {
            // 양쪽 수락 확정 알림
            if (session.getChatRoomId() == null) {
                session.attachChatRoom("room-" + session.getId());
            }
            sendToUser(session.getUidA(), "/queue/match/confirmed", ApiResponse.ok(session.getId()));
            sendToUser(session.getUidB(), "/queue/match/confirmed", ApiResponse.ok(session.getId()));
        } else {
            // 나만 수락한 상태 (상대방 대기 중)
            sendToUser(uid, "/queue/match/accepted", ApiResponse.ok(session.getId()));
        }
    }

    /**
     * 매칭 종료 및 결과 제출을 처리합니다.
     *
     * @param uid     현재 사용자 UID
     * @param request 종료 정보 (세션 ID, 결과)
     */
    @Transactional
    public void completionsMatch(String uid, MatchCompletionsRequest request) {
        MatchSession session = matchSessionRepository.findById(request.getMatchSessionId())
                .orElseThrow(() -> new NotFoundException(MATCHING_SESSION_NOT_FOUND));

        List<MatchParticipantResult> existing = resultRepository.findByMatchSessionId(session.getId());

        if (existing.size() >= 2) {
            throw new BadRequestException(INVALID_REQUEST);
        }

        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));

        // TODO: 매칭 종료 후 매너 점수 및 레이팅 증감 로직 추가 구현 필요함
        MatchResult result = request.getResult();
        int before = user.getEloRating();
        int after = before;

        resultRepository.save(MatchParticipantResult.of(session.getId(), uid, before, after, result));

        existing = resultRepository.findByMatchSessionId(session.getId());
        if (existing.size() == 2) {
            MatchParticipantResult r1 = existing.get(0);
            MatchParticipantResult r2 = existing.get(1);
            if (r1.getResult() == MatchResult.WIN && r2.getResult() == MatchResult.WIN) {
                throw new BadRequestException(INVALID_REQUEST, "두 참가자 모두 WIN을 제출했습니다.");
            }
            if (r1.getResult() == MatchResult.LOSE && r2.getResult() == MatchResult.LOSE) {
                throw new BadRequestException(INVALID_REQUEST, "두 참가자 모두 LOSE를 제출했습니다.");
            }

            session.markFinished();
            matchSessionRepository.save(session);

            // 양쪽에 완료 알림
            sendToUser(r1.getUid(), "/queue/match/finished", ApiResponse.ok(session.getId()));
            sendToUser(r2.getUid(), "/queue/match/finished", ApiResponse.ok(session.getId()));
        }
    }

    private void handleReject(String uid, MatchSession session, LocalDateTime now) {
        queueRedis.dequeue(uid);

        // 상대방에게 거절 알림 및 대기열 복귀
        String opponentUid = session.getOpponentUid(uid);
        queueRedis.requeueFromStoredSnapshot(opponentUid);

        sendToUser(opponentUid, "/queue/match/declined", ApiResponse.ok(session.getId()));
        sendToUser(uid, "/queue/match/declined", ApiResponse.ok(session.getId()));

        MatchingPenalty penalty = penaltyRepository.findByUid(uid)
                .orElseGet(() -> MatchingPenalty.of(uid));

        penalty.updateForReject(now);

        LocalDateTime penaltyUntil;
        if (penalty.getRejectCountTotal() == 1) {
            penaltyUntil = now.plusMinutes(5);
        } else if (penalty.getRejectCountTotal() == 2) {
            penaltyUntil = now.plusMinutes(30);
        } else {
            penaltyUntil = LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 59, 59));
        }
        penalty.imposePenaltyUntil(penaltyUntil);

        penaltyRepository.save(penalty);
    }

    private int calculateStage(java.time.LocalDateTime waitingSince, LocalDateTime now) {
        long seconds = Duration.between(waitingSince, now).getSeconds();
        if (seconds < 85) {
            return 1;
        } else if (seconds < 170) {
            return 2;
        } else if (seconds < 255) {
            return 3;
        }
        return 4;
    }

    private int getRatingTolerance(int stage) {
        return switch (stage) {
            case 1 -> 5;
            case 2 -> 20;
            case 3 -> 35;
            default -> 50;
        };
    }

    private double getRadiusKm(int stage) {
        return switch (stage) {
            case 1 -> 3.0;
            case 2 -> 5.0;
            case 3 -> 7.0;
            default -> 10.0;
        };
    }

    private void sendMatchFound(String meUid, String opponentUid, Long sessionId) {
        MatchFoundPayload payload = new MatchFoundPayload(sessionId, opponentUid);
        sendToUser(meUid, "/queue/match", ApiResponse.ok(payload));
    }

    private void sendToUser(String uid, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(uid, destination, payload);
    }

    private double distanceInKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Getter
    private static class MatchFoundPayload {
        private final Long matchSessionId;
        private final String opponentUid;

        private MatchFoundPayload(Long matchSessionId, String opponentUid) {
            this.matchSessionId = matchSessionId;
            this.opponentUid = opponentUid;
        }
    }
}
