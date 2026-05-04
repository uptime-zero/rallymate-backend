package com.rallymate.matching.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 매칭 대기열을 Redis(ZSET + Hash)로 관리합니다.
 */
@Service
@RequiredArgsConstructor
public class MatchingQueueRedisService {

    private static final String USER_KEY = "matching:user:";
    private static final String WAITING_ZSET = "matching:waiting:";
    private static final String LOCK_KEY = "matching:lock:";

    private final StringRedisTemplate stringRedisTemplate;

    public boolean isUserInQueue(String uid) {
        Boolean exists = stringRedisTemplate.hasKey(USER_KEY + uid);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 대기열에 등록합니다.
     */
    public void enqueue(String uid, String sport, int rating, double lat, double lng, long sinceEpochMillis) {
        String userKey = USER_KEY + uid;
        String zsetKey = WAITING_ZSET + sport;

        stringRedisTemplate.opsForHash().putAll(userKey, Map.of(
                "sport", sport,
                "rating", String.valueOf(rating),
                "lat", String.valueOf(lat),
                "lng", String.valueOf(lng),
                "since", String.valueOf(sinceEpochMillis)
        ));

        stringRedisTemplate.opsForZSet().add(zsetKey, uid, sinceEpochMillis);
    }

    /**
     * 대기열에서 제거합니다 (거절·타임아웃 등).
     */
    public void dequeue(String uid) {
        String userKey = USER_KEY + uid;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(userKey);
        if (entries.isEmpty()) {
            return;
        }
        String sport = (String) entries.get("sport");
        if (sport != null) {
            stringRedisTemplate.opsForZSet().remove(WAITING_ZSET + sport, uid);
        }
        stringRedisTemplate.delete(userKey);
    }

    /**
     * 종목별 대기 uid 목록을 대기 시작 시각(오래된 순)으로 조회합니다.
     */
    public List<String> listWaitingUidsOrdered(String sport) {
        Set<String> range = stringRedisTemplate.opsForZSet().range(WAITING_ZSET + sport, 0, -1);
        if (range == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(range);
    }

    public WaitingUserSnapshot loadUser(String uid) {
        String userKey = USER_KEY + uid;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(userKey);
        if (entries.isEmpty()) {
            return null;
        }
        String sport = (String) entries.get("sport");
        int rating = Integer.parseInt((String) entries.get("rating"));
        double lat = Double.parseDouble((String) entries.get("lat"));
        double lng = Double.parseDouble((String) entries.get("lng"));
        long since = Long.parseLong((String) entries.get("since"));
        LocalDateTime waitingSince = LocalDateTime.ofInstant(Instant.ofEpochMilli(since), ZoneId.systemDefault());
        return new WaitingUserSnapshot(uid, rating, sport, lat, lng, waitingSince);
    }

    /**
     * 매칭 성사 시 두 명을 대기열에서 제거합니다.
     */
    public void removePair(String sport, String uidA, String uidB) {
        stringRedisTemplate.opsForZSet().remove(WAITING_ZSET + sport, uidA, uidB);
    }

    /**
     * 기존에 저장된 사용자 스냅샷(Hash)을 기반으로 다시 대기열(ZSET)에 등록합니다.
     * <p>
     * 매칭 성사 후 한쪽이 거절했을 때, 상대방을 이전 대기 정보(종목/대기 시작 시각)를
     * 유지한 채로 다시 대기열에 복귀시키는 용도로 사용됩니다.
     * </p>
     */
    public void requeueFromStoredSnapshot(String uid) {
        String userKey = USER_KEY + uid;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(userKey);
        if (entries.isEmpty()) {
            return;
        }
        String sport = (String) entries.get("sport");
        String sinceStr = (String) entries.get("since");
        if (sport == null || sinceStr == null) {
            return;
        }
        long since = Long.parseLong(sinceStr);
        stringRedisTemplate.opsForZSet().add(WAITING_ZSET + sport, uid, since);
    }

    /**
     * 종목별 매칭 시도 분산 락 (짧은 TTL).
     */
    public boolean tryLockSport(String sport) {
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(
                LOCK_KEY + sport,
                "1",
                Duration.ofSeconds(5)
        );
        return Boolean.TRUE.equals(ok);
    }

    @Getter
    @RequiredArgsConstructor
    public static class WaitingUserSnapshot {
        private final String uid;
        private final int rating;
        private final String sport;
        private final double latitude;
        private final double longitude;
        private final LocalDateTime waitingSince;
    }
}
